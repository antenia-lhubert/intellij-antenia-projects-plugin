package fr.antenia.database

import com.intellij.credentialStore.OneTimeString
import com.intellij.database.access.DatabaseCredentials as IntelliJDatabaseCredentials
import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.dataSource.SchemaControl
import com.intellij.database.util.TreePatternUtils
import com.intellij.openapi.project.Project
import fr.antenia.config.ConfigurationFiles
import fr.antenia.config.OrderedProperties
import fr.antenia.credentials.DatabaseCredentials
import fr.antenia.project.DatabaseKeys
import fr.antenia.project.NeoProject
import fr.antenia.project.NeoSchema

internal data class MysqlConnection(
    val host: String,
    val port: Int,
    val database: String,
    val query: String,
) {
    val url: String get() = build(host, port, database, query)

    companion object {
        private val pattern = Regex("^jdbc:mysql://([^/:?]*)(?::(\\d+))?/([^?]*)(\\?.*)?$")

        fun parse(value: String): MysqlConnection? {
            val match = pattern.matchEntire(value) ?: return null
            return MysqlConnection(
                host = match.groupValues[1],
                port = match.groupValues[2].toIntOrNull() ?: 3306,
                database = match.groupValues[3],
                query = match.groupValues[4],
            )
        }

        fun build(host: String, port: Int, database: String, query: String): String =
            "jdbc:mysql://$host${if (port == 3306) "" else ":$port"}/$database$query"
    }
}

internal fun configuredCredentials(document: OrderedProperties, keys: DatabaseKeys): DatabaseCredentials = DatabaseCredentials(
    username = keys.usernames.firstNotNullOfOrNull { document.value(it)?.takeIf(String::isNotBlank) }.orEmpty(),
    password = keys.passwords.firstNotNullOfOrNull { document.value(it)?.takeIf(String::isNotEmpty) }.orEmpty(),
)

object DatabaseProfileSynchronizer {
    fun update(project: Project, neoProject: NeoProject) {
        if (project.isDisposed) return
        val keys = NeoSchema.forType(neoProject.type).database ?: return
        val document = ConfigurationFiles.read(ConfigurationFiles.ensureCreated(project, neoProject.type))
        update(project, document, keys)
    }

    private fun update(project: Project, document: OrderedProperties, keys: DatabaseKeys) {
        val connection = MysqlConnection.parse(document.value(keys.url).orEmpty()) ?: return
        if (connection.host.isBlank() || connection.database.isBlank()) return
        val credentials = configuredCredentials(document, keys)

        val driver = DatabaseDriverManager.getInstance().getDriver("mysql.8") ?: return
        val manager = LocalDataSourceManager.getInstance(project)
        val existing = manager.dataSources.firstOrNull { it.name == connection.host }
        val dataSource = existing ?: LocalDataSource.fromDriver(driver, connection.url, false).apply {
            name = connection.host
            isAutoSynchronize = true
        }

        if (dataSource.driverRef != driver.id) dataSource.setDatabaseDriver(driver)
        dataSource.setUrlSmart(connection.url)
        dataSource.username = credentials.username
        dataSource.passwordStorage = LocalDataSource.Storage.PERSIST
        IntelliJDatabaseCredentials.getInstance().storePassword(dataSource, OneTimeString(credentials.password))
        dataSource.setSchemaControl(SchemaControl.MANUAL)
        val databaseScope = TreePatternUtils.parse(false, "*:${TreePatternUtils.escape(connection.database)}")
        dataSource.setIntrospectionScope(TreePatternUtils.union(dataSource.introspectionScope, databaseScope))

        if (existing == null) manager.addDataSource(dataSource) else manager.fireDataSourceUpdated(dataSource)
    }
}
