package fr.antenia.automation

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.compound.CompoundRunConfiguration
import com.intellij.execution.compound.CompoundRunConfigurationType
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.javaee.appServers.run.configuration.CommonModel
import com.intellij.javaee.appServers.run.configuration.J2EEConfigurationFactory
import com.intellij.javaee.appServers.run.configuration.CommonStrategy
import com.intellij.javaee.appServers.serverInstances.ApplicationServersManager
import com.intellij.lang.javascript.buildTools.npm.rc.NpmCommand
import com.intellij.lang.javascript.buildTools.npm.beforeRun.NpmBeforeRunTaskProvider
import com.intellij.lang.javascript.buildTools.npm.rc.NpmConfigurationType
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.packaging.artifacts.Artifact
import com.intellij.packaging.artifacts.ArtifactManager
import com.intellij.packaging.artifacts.ArtifactType
import com.intellij.packaging.elements.PackagingElementFactory
import fr.antenia.config.ProjectConfigurationState
import fr.antenia.project.NeoProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.tomcat.TomcatModuleDeploymentModel
import org.jetbrains.idea.tomcat.server.TomcatConfiguration
import org.jetbrains.idea.tomcat.server.TomcatLocalModel
import java.nio.file.Path

object NeoRunConfigurationManager {
    fun configure(project: Project, neoProject: NeoProject) {
        if (project.isDisposed) return
        val artifact = ensureArtifact(project, neoProject) ?: return
        val webapp = ensureWebappConfiguration(project, neoProject, artifact) ?: return
        ensureArtifactDeployment(webapp, artifact, neoProject)
        if (neoProject.hasReact) {
            val react = ensureReactConfiguration(project, neoProject)
            if (react != null) ensureCompoundConfiguration(project, react, webapp)
        }
    }

    private fun ensureArtifact(project: Project, neoProject: NeoProject): Artifact? {
        val artifactManager = ArtifactManager.getInstance(project)
        artifactManager.findArtifact(neoProject.type.explodedArtifactName)?.let { return it }
        val modules = ModuleManager.getInstance(project).modules
        val mavenProjectsManager = MavenProjectsManager.getInstance(project)
        val module = modules.firstOrNull { mavenProjectsManager.findProject(it)?.mavenId?.artifactId == neoProject.type.artifactId }
            ?: modules.firstOrNull { it.name == neoProject.type.artifactId || it.name.substringAfterLast('.') == neoProject.type.artifactId }
            ?: ModuleManager.getInstance(project).modules.singleOrNull()
            ?: return null
        val artifactType = ArtifactType.findById("exploded-war") ?: return null
        val elementFactory = PackagingElementFactory.getInstance()
        val root = elementFactory.createArtifactRootElement()
        val webInf = elementFactory.createDirectory("WEB-INF")
        val classes = elementFactory.createDirectory("classes")
        classes.addOrFindChild(elementFactory.createModuleOutput(module))
        webInf.addOrFindChild(classes)
        val libraries = elementFactory.createDirectory("lib")
        OrderEnumerator.orderEntries(module).librariesOnly().forEachLibrary { library ->
            elementFactory.createLibraryElements(library).forEach(libraries::addOrFindChild)
            true
        }
        webInf.addOrFindChild(libraries)
        root.addOrFindChild(webInf)
        val moduleRoot = ModuleRootManager.getInstance(module).contentRoots.firstOrNull()?.toNioPath()
            ?: Path.of(requireNotNull(project.basePath))
        val webResources = moduleRoot.resolve("src").resolve("main").resolve("webapp").toString()
        root.addOrFindChild(elementFactory.createDirectoryCopyWithParentDirectories(webResources, ""))

        ApplicationManager.getApplication().runWriteAction {
            val model = artifactManager.createModifiableModel()
            try {
                model.addArtifact(neoProject.type.explodedArtifactName, artifactType, root).apply {
                    setBuildOnMake(true)
                    outputPath = moduleRoot.resolve("target").resolve(neoProject.type.contextPath).toString()
                }
                model.commit()
            } catch (exception: Throwable) {
                model.dispose()
                throw exception
            }
        }
        return artifactManager.findArtifact(neoProject.type.explodedArtifactName)
    }

    private fun ensureWebappConfiguration(project: Project, neoProject: NeoProject, artifact: Artifact): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)
        runManager.findConfigurationByName("webapp")?.let { return it }
        val tomcatType = TomcatConfiguration.getInstance()
        val targetVersion = when {
            neoProject.javaVersion <= 8 -> "9"
            neoProject.javaVersion <= 17 -> "10.1"
            else -> "11"
        }
        val server = ApplicationServersManager.getInstance().getApplicationServers(tomcatType.integration)
            .filter { serverMatches(it.name, targetVersion) }
            .maxWithOrNull(Comparator { first, second -> compareVersions(versionNumbers(first.name), versionNumbers(second.name)) })
            ?: return null
        val settings = J2EEConfigurationFactory.getInstance().addAppServerConfiguration(project, tomcatType.localFactory, server)
        settings.name = "webapp"
        val model = settings.configuration as? CommonModel ?: return null
        (model as? CommonStrategy)?.apply {
            setAlternativeJreEnabled(false)
            settingsBean.OPEN_IN_BROWSER = true
            settingsBean.UPDATE_ON_FRAME_DEACTIVATION = true
            settingsBean.UPDATE_CLASSES_ON_FRAME_DEACTIVATION = true
            settingsBean.UPDATING_POLICY = "restart-server"
        }
        val tomcatModel = model.serverModel as? TomcatLocalModel ?: return null
        tomcatModel.HTTP_PORT = neoProject.type.httpPort
        tomcatModel.HTTPS_PORT = neoProject.type.httpsPort
        tomcatModel.JNDI_PORT = neoProject.type.jmxPort
        val browserPort = if (neoProject.hasReact) 8888 else neoProject.type.httpsPort
        model.setUrlToOpenInBrowser("https://localhost:$browserPort/${neoProject.type.contextPath}/")
        ensureArtifactDeployment(settings, artifact, neoProject)
        if (neoProject.hasReact) configureReactBuildTasks(project, settings, neoProject)
        return settings
    }

    private fun ensureArtifactDeployment(settings: RunnerAndConfigurationSettings, artifact: Artifact, neoProject: NeoProject) {
        val model = settings.configuration as? CommonModel ?: return
        val deploymentSettings = model.deploymentSettings ?: return
        deploymentSettings.deploymentModels
            .filter { it.artifact != null && it.artifact != artifact }
            .forEach(deploymentSettings::removeModel)
        val deployment = deploymentSettings.getOrCreateModel(artifact)
        if (deployment is TomcatModuleDeploymentModel) deployment.CONTEXT_PATH = "/${neoProject.type.contextPath}"
    }

    private fun ensureReactConfiguration(project: Project, neoProject: NeoProject): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)
        runManager.findConfigurationByName("react")?.let { return it }
        val factory = NpmConfigurationType.getInstance().configurationFactories.firstOrNull() ?: return null
        val settings = runManager.createConfiguration("react", factory)
        val configuration = settings.configuration as? NpmRunConfiguration ?: return null
        val root = Path.of(requireNotNull(project.basePath))
        val environment = mapOf(neoProject.type.environmentVariable to ProjectConfigurationState.getInstance(project).root(project).toString())
        configuration.runSettings = NpmRunSettings.builder()
            .setPackageJsonPath(root.resolve("novanet-react").resolve("package.json").toString())
            .setCommand(NpmCommand.RUN_SCRIPT)
            .setScriptNames(listOf("dev"))
            .setEnvData(EnvironmentVariablesData.create(environment, true))
            .build()
        runManager.addConfiguration(settings)
        return settings
    }

    private fun configureReactBuildTasks(project: Project, settings: RunnerAndConfigurationSettings, neoProject: NeoProject) {
        val provider = NpmBeforeRunTaskProvider()
        val root = Path.of(requireNotNull(project.basePath))
        val packageJson = root.resolve("novanet-react").resolve("package.json").toString()
        val environment = EnvironmentVariablesData.create(
            mapOf(neoProject.type.environmentVariable to ProjectConfigurationState.getInstance(project).root(project).toString()),
            true,
        )
        val install = provider.createTask(settings.configuration)?.apply {
            isEnabled = true
            this.settings = NpmRunSettings.builder()
                .setPackageJsonPath(packageJson)
                .setCommand(NpmCommand.INSTALL)
                .setEnvData(environment)
                .build()
        } ?: return
        val build = provider.createTask(settings.configuration)?.apply {
            isEnabled = true
            this.settings = NpmRunSettings.builder()
                .setPackageJsonPath(packageJson)
                .setCommand(NpmCommand.RUN_SCRIPT)
                .setScriptNames(listOf("build"))
                .setEnvData(environment)
                .build()
        } ?: return
        val manager = RunManagerEx.getInstanceEx(project)
        manager.setBeforeRunTasks(settings.configuration, listOf(install, build) + manager.getBeforeRunTasks(settings.configuration))
    }

    private fun ensureCompoundConfiguration(
        project: Project,
        react: RunnerAndConfigurationSettings,
        webapp: RunnerAndConfigurationSettings,
    ) {
        val runManager = RunManager.getInstance(project)
        if (runManager.findConfigurationByName("webapp + react") != null) return
        val factory = ConfigurationTypeUtil.findConfigurationType(CompoundRunConfigurationType::class.java).configurationFactories.firstOrNull() ?: return
        val settings = runManager.createConfiguration("webapp + react", factory)
        val compound = settings.configuration as? CompoundRunConfiguration ?: return
        compound.setConfigurationsWithoutTargets(listOf(react.configuration, webapp.configuration))
        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings
    }

    private fun serverMatches(name: String, target: String): Boolean {
        val numbers = versionNumbers(name)
        val targetNumbers = target.split('.').map(String::toInt)
        return numbers.take(targetNumbers.size) == targetNumbers
    }

    private fun versionNumbers(value: String): List<Int> = Regex("\\d+").findAll(value).map { it.value.toInt() }.toList()

    private fun compareVersions(first: List<Int>, second: List<Int>): Int {
        for (index in 0 until maxOf(first.size, second.size)) {
            val comparison = first.getOrElse(index) { 0 }.compareTo(second.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }
}
