package fr.antenia.project

data class DatabaseKeys(
    val url: String,
    val database: String?,
    val usernames: List<String>,
    val passwords: List<String>,
)

data class NeoSchema(
    val type: NeoProjectType,
    val knownKeys: List<String>,
    val database: DatabaseKeys? = null,
    val environmentKey: String? = null,
) {
    val specialKeys: Set<String> = buildSet {
        database?.let {
            add(it.url)
            it.database?.let(::add)
            addAll(it.usernames)
            addAll(it.passwords)
        }
        environmentKey?.let(::add)
    }

    companion object {
        fun forType(type: NeoProjectType): NeoSchema = when (type) {
            NeoProjectType.CORE -> NeoSchema(
                type,
                listOf("url", "database", "databaseEdi", "environnement", "usernameNovanet", "passwordNovanet", "usernameSpring", "passwordSpring", "driver", "webapps", "hibernate.show_sql", "hibernate.dialect", "checkoutTimeout", "maxIdleTime", "maxConnectionAge", "acquireIncrement", "maxStatements", "propertyCycle", "unreturnedConnectionTimeout", "autoCommitOnClose", "preferredTestQuery", "switchToManual", "minPoolSizeNormal", "maxPoolSizeNormal", "minPoolSizeSpring", "maxPoolSizeSpring", "smtp", "urlext", "liquibaseEnabled", "neohub.upload.maxfilesize", "neohub.upload.maxgroupedfilesize"),
                DatabaseKeys("url", "database", listOf("usernameNovanet", "usernameSpring"), listOf("passwordNovanet", "passwordSpring")),
                "environnement",
            )
            NeoProjectType.GED -> NeoSchema(
                type,
                listOf("jdbc.driverClassName", "jdbc.url", "jdbc.username", "jdbc.password", "hibernate.show_sql", "hibernate.dialect", "urlExt", "urlWSock", "webapps", "smtp", "dossierRoot", "rest.sessionTime", "pspdfkitKey"),
                DatabaseKeys("jdbc.url", null, listOf("jdbc.username"), listOf("jdbc.password")),
            )
            NeoProjectType.SELFCARE -> NeoSchema(type, listOf("urlWebService", "login", "password", "cssSocieteUser", "keyPrivateCaptcha", "keyPublicCaptcha"))
        }
    }
}
