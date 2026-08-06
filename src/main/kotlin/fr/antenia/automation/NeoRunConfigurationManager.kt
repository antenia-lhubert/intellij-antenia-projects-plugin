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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.packaging.artifacts.Artifact
import com.intellij.packaging.artifacts.ArtifactManager
import com.intellij.packaging.artifacts.ArtifactType
import com.intellij.packaging.elements.PackagingElementFactory
import fr.antenia.config.ProjectConfigurationState
import fr.antenia.notifications.AnteniaNotifications
import fr.antenia.project.NeoProject
import fr.antenia.settings.TomcatRunConfigurationSettings
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.tomcat.TomcatModuleDeploymentModel
import org.jetbrains.idea.tomcat.server.TomcatConfiguration
import org.jetbrains.idea.tomcat.server.TomcatLocalModel
import java.nio.file.Path

object NeoRunConfigurationManager {
    private val logger = Logger.getInstance(NeoRunConfigurationManager::class.java)

    fun configure(project: Project, neoProject: NeoProject) {
        if (project.isDisposed) {
            logger.debug("Run configuration automation skipped for '${project.name}': project is disposed")
            return
        }
        logger.info("Configuring owned run configurations for '${project.name}': react=${neoProject.hasReact}")
        val artifact = ensureArtifact(project, neoProject)
        if (artifact == null) {
            logger.warn("Run configuration automation stopped for '${project.name}': exploded artifact is unavailable")
            return
        }
        val webapp = ensureWebappConfiguration(project, neoProject, artifact)
        if (webapp == null) {
            logger.warn("Run configuration automation stopped for '${project.name}': webapp configuration is unavailable")
            return
        }
        ensureArtifactDeployment(project, webapp, artifact, neoProject)
        if (neoProject.hasReact) {
            val react = ensureReactConfiguration(project, neoProject)
            if (react != null) {
                ensureCompoundConfiguration(project, react, webapp)
            } else {
                logger.warn("React run configuration was not created for '${project.name}'")
            }
        }
        logger.info("Finished owned run configuration automation for '${project.name}'")
    }

    private fun ensureArtifact(project: Project, neoProject: NeoProject): Artifact? {
        val artifactManager = ArtifactManager.getInstance(project)
        artifactManager.findArtifact(neoProject.type.explodedArtifactName)?.let {
            logger.info("Found existing exploded artifact for '${project.name}': ${it.name}")
            return it
        }
        val modules = ModuleManager.getInstance(project).modules
        val mavenProjectsManager = MavenProjectsManager.getInstance(project)
        val module = modules.firstOrNull { mavenProjectsManager.findProject(it)?.mavenId?.artifactId == neoProject.type.artifactId }
            ?: modules.firstOrNull { it.name == neoProject.type.artifactId || it.name.substringAfterLast('.') == neoProject.type.artifactId }
            ?: ModuleManager.getInstance(project).modules.singleOrNull()
        if (module == null) {
            failure(
                project,
                "artifact-module-unavailable",
                "Exploded artifact could not be created",
                "Module '${neoProject.type.artifactId}' was not found. Import the Maven project and see the IDE log for details.",
            )
            return null
        }
        val artifactType = ArtifactType.findById("exploded-war")
        if (artifactType == null) {
            failure(
                project,
                "exploded-war-type-unavailable",
                "Exploded artifact could not be created",
                "IntelliJ artifact type 'exploded-war' is unavailable. See the IDE log for details.",
            )
            return null
        }
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
        return artifactManager.findArtifact(neoProject.type.explodedArtifactName).also {
            if (it == null) {
                failure(
                    project,
                    "artifact-missing-after-commit",
                    "Exploded artifact could not be created",
                    "Artifact '${neoProject.type.explodedArtifactName}' was missing after the model was committed. See the IDE log for details.",
                )
            } else {
                logger.info(
                    "Created exploded artifact for '${project.name}': name=${it.name}, module=${module.name}, " +
                        "output=${it.outputPath}",
                )
            }
        }
    }

    private fun ensureWebappConfiguration(project: Project, neoProject: NeoProject, artifact: Artifact): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)
        runManager.findConfigurationByName("webapp")?.let {
            logger.info("Found existing 'webapp' run configuration for '${project.name}'; preserving user settings")
            return it
        }
        val tomcatType = TomcatConfiguration.getInstance()
        val targetVersion = when {
            neoProject.javaVersion <= 8 -> "9"
            else -> "10.1"
        }
        val candidates = ApplicationServersManager.getInstance().getApplicationServers(tomcatType.integration)
            .filter { serverMatches(it.name, targetVersion) }
        val server = candidates.maxWithOrNull(Comparator { first, second -> compareVersions(versionNumbers(first.name), versionNumbers(second.name)) })
        if (server == null) {
            val availableServers = ApplicationServersManager.getInstance().getApplicationServers(tomcatType.integration).map { it.name }
            failure(
                project,
                "tomcat-$targetVersion-unavailable",
                "Webapp run configuration could not be created",
                "No Tomcat $targetVersion server is configured. Available servers: ${availableServers.ifEmpty { listOf("none") }.joinToString()}.",
            )
            return null
        }
        logger.info("Selected ${server.name} for '${project.name}' from ${candidates.map { it.name }}")
        val settings = J2EEConfigurationFactory.getInstance().addAppServerConfiguration(project, tomcatType.localFactory, server)
        settings.name = "webapp"
        val model = settings.configuration as? CommonModel
        if (model == null) {
            failure(
                project,
                "webapp-javaee-model-unavailable",
                "Webapp run configuration could not be created",
                "The created configuration does not expose a Java EE model. See the IDE log for details.",
            )
            return null
        }
        val tomcatSettings = TomcatRunConfigurationSettings.getInstance().options()
        (model as? CommonStrategy)?.apply {
            setAlternativeJreEnabled(false)
            settingsBean.OPEN_IN_BROWSER = tomcatSettings.openInBrowser
            settingsBean.UPDATE_ON_FRAME_DEACTIVATION = tomcatSettings.updateOnFrameDeactivation
            settingsBean.UPDATE_CLASSES_ON_FRAME_DEACTIVATION = tomcatSettings.updateClassesOnFrameDeactivation
            settingsBean.UPDATING_POLICY = tomcatSettings.updatingPolicy
        }
        val tomcatModel = model.serverModel as? TomcatLocalModel
        if (tomcatModel == null) {
            failure(
                project,
                "webapp-tomcat-model-unavailable",
                "Webapp run configuration could not be created",
                "The created configuration does not expose a local Tomcat model. See the IDE log for details.",
            )
            return null
        }
        tomcatModel.HTTP_PORT = neoProject.type.httpPort
        tomcatModel.HTTPS_PORT = neoProject.type.httpsPort
        tomcatModel.JNDI_PORT = neoProject.type.jmxPort
        val browserPort = if (neoProject.hasReact) 8888 else neoProject.type.httpsPort
        model.setUrlToOpenInBrowser("https://localhost:$browserPort/${neoProject.type.contextPath}/")
        ensureArtifactDeployment(project, settings, artifact, neoProject)
        if (neoProject.hasReact) configureReactBuildTasks(project, settings, neoProject)
        logger.info(
            "Created 'webapp' run configuration for '${project.name}': http=${neoProject.type.httpPort}, " +
                "https=${neoProject.type.httpsPort}, jmx=${neoProject.type.jmxPort}, browserPort=$browserPort",
        )
        return settings
    }

    private fun ensureArtifactDeployment(
        project: Project,
        settings: RunnerAndConfigurationSettings,
        artifact: Artifact,
        neoProject: NeoProject,
    ) {
        val model = settings.configuration as? CommonModel
        if (model == null) {
            failure(
                project,
                "artifact-deployment-javaee-model-unavailable",
                "Artifact deployment could not be configured",
                "Run configuration '${settings.name}' is not a Java EE configuration.",
            )
            return
        }
        val deploymentSettings = model.deploymentSettings
        if (deploymentSettings == null) {
            failure(
                project,
                "artifact-deployment-settings-unavailable",
                "Artifact deployment could not be configured",
                "Deployment settings are unavailable for run configuration '${settings.name}'.",
            )
            return
        }
        val obsoleteDeployments = deploymentSettings.deploymentModels
            .filter { it.artifact != null && it.artifact != artifact }
        obsoleteDeployments.forEach(deploymentSettings::removeModel)
        val deployment = deploymentSettings.getOrCreateModel(artifact)
        if (deployment is TomcatModuleDeploymentModel) deployment.CONTEXT_PATH = "/${neoProject.type.contextPath}"
        logger.info(
            "Ensured artifact deployment for '${settings.name}': artifact=${artifact.name}, " +
                "context=/${neoProject.type.contextPath}, removedObsoleteDeployments=${obsoleteDeployments.size}",
        )
    }

    private fun ensureReactConfiguration(project: Project, neoProject: NeoProject): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)
        runManager.findConfigurationByName("react")?.let {
            logger.info("Found existing 'react' run configuration for '${project.name}'; preserving user settings")
            return it
        }
        val factory = NpmConfigurationType.getInstance().configurationFactories.firstOrNull()
        if (factory == null) {
            failure(
                project,
                "npm-configuration-factory-unavailable",
                "React run configuration could not be created",
                "The npm run configuration factory is unavailable. See the IDE log for details.",
            )
            return null
        }
        val settings = runManager.createConfiguration("react", factory)
        val configuration = settings.configuration as? NpmRunConfiguration
        if (configuration == null) {
            failure(
                project,
                "unexpected-npm-configuration",
                "React run configuration could not be created",
                "The created run configuration is not an npm configuration. See the IDE log for details.",
            )
            return null
        }
        val root = Path.of(requireNotNull(project.basePath))
        val packageJson = root.resolve("novanet-react").resolve("package.json").toString()
        val environment = mapOf(neoProject.type.environmentVariable to ProjectConfigurationState.getInstance(project).root(project).toString())
        configuration.runSettings = NpmRunSettings.builder()
            .setPackageJsonPath(packageJson)
            .setCommand(NpmCommand.RUN_SCRIPT)
            .setScriptNames(listOf("dev"))
            .setEnvData(EnvironmentVariablesData.create(environment, true))
            .build()
        runManager.addConfiguration(settings)
        logger.info("Created 'react' run configuration for '${project.name}': packageJson=$packageJson")
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
        }
        if (install == null) {
            failure(
                project,
                "npm-install-task-unavailable",
                "React build tasks could not be configured",
                "The npm install before-run task could not be created. See the IDE log for details.",
            )
            return
        }
        val build = provider.createTask(settings.configuration)?.apply {
            isEnabled = true
            this.settings = NpmRunSettings.builder()
                .setPackageJsonPath(packageJson)
                .setCommand(NpmCommand.RUN_SCRIPT)
                .setScriptNames(listOf("build"))
                .setEnvData(environment)
                .build()
        }
        if (build == null) {
            failure(
                project,
                "npm-build-task-unavailable",
                "React build tasks could not be configured",
                "The npm build before-run task could not be created. See the IDE log for details.",
            )
            return
        }
        val manager = RunManagerEx.getInstanceEx(project)
        manager.setBeforeRunTasks(settings.configuration, listOf(install, build) + manager.getBeforeRunTasks(settings.configuration))
        logger.info("Added npm install and build tasks before 'webapp' for '${project.name}'")
    }

    private fun ensureCompoundConfiguration(
        project: Project,
        react: RunnerAndConfigurationSettings,
        webapp: RunnerAndConfigurationSettings,
    ) {
        val runManager = RunManager.getInstance(project)
        if (runManager.findConfigurationByName("webapp + react") != null) {
            logger.info("Found existing 'webapp + react' compound configuration for '${project.name}'")
            return
        }
        val factory = ConfigurationTypeUtil.findConfigurationType(CompoundRunConfigurationType::class.java).configurationFactories.firstOrNull()
        if (factory == null) {
            failure(
                project,
                "compound-configuration-factory-unavailable",
                "Compound run configuration could not be created",
                "The compound run configuration factory is unavailable. See the IDE log for details.",
            )
            return
        }
        val settings = runManager.createConfiguration("webapp + react", factory)
        val compound = settings.configuration as? CompoundRunConfiguration
        if (compound == null) {
            failure(
                project,
                "unexpected-compound-configuration",
                "Compound run configuration could not be created",
                "The created run configuration has an unexpected type. See the IDE log for details.",
            )
            return
        }
        compound.setConfigurationsWithoutTargets(listOf(react.configuration, webapp.configuration))
        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings
        logger.info("Created and selected 'webapp + react' compound configuration for '${project.name}'")
    }

    private fun failure(project: Project, key: String, title: String, message: String) {
        logger.warn("$title for '${project.name}': $message")
        AnteniaNotifications.failure(project, "run-configuration-$key", title, message)
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
