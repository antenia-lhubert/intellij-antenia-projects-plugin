package fr.antenia.project

import fr.antenia.config.PropertyLayoutGroup

data class DatabaseProperty(
    val key: String,
    val defaultValue: String,
)

data class DatabaseKeys(
    val url: String,
    val database: String?,
    val usernames: List<String>,
    val passwords: List<String>,
    val advanced: List<DatabaseProperty> = emptyList(),
    val layoutGroups: List<PropertyLayoutGroup> = emptyList(),
) {
    val allKeys: Set<String> = buildSet {
        add(url)
        database?.let(::add)
        addAll(usernames)
        addAll(passwords)
        advanced.mapTo(this) { it.key }
    }

    val advancedDefaults: Map<String, String>
        get() = advanced.associate { it.key to it.defaultValue }
}

data class NeoSchema(
    val type: NeoProjectType,
    val knownKeys: List<String>,
    val database: DatabaseKeys? = null,
    val environmentKey: String? = null,
) {
    val specialKeys: Set<String> = buildSet {
        database?.allKeys?.let(::addAll)
        environmentKey?.let(::add)
    }

    companion object {
        fun forType(type: NeoProjectType): NeoSchema = when (type) {
            NeoProjectType.CORE -> NeoSchema(
                type,
                listOf("url", "database", "databaseEdi", "environnement", "usernameNovanet", "passwordNovanet", "usernameSpring", "passwordSpring", "driver", "webapps", "hibernate.show_sql", "hibernate.dialect", "checkoutTimeout", "maxIdleTime", "maxConnectionAge", "acquireIncrement", "maxStatements", "propertyCycle", "unreturnedConnectionTimeout", "autoCommitOnClose", "preferredTestQuery", "switchToManual", "minPoolSizeNormal", "maxPoolSizeNormal", "minPoolSizeSpring", "maxPoolSizeSpring", "smtp", "SGBD", "urlext", "liquibaseEnabled", "neohub.upload.maxfilesize", "neohub.upload.maxgroupedfilesize"),
                DatabaseKeys(
                    "url",
                    "database",
                    listOf("usernameNovanet", "usernameSpring"),
                    listOf("passwordNovanet", "passwordSpring"),
                    listOf(
                        DatabaseProperty("databaseEdi", ""),
                        DatabaseProperty("driver", "com.mysql.jdbc.Driver"),
                        DatabaseProperty("hibernate.show_sql", "false"),
                        DatabaseProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect"),
                        DatabaseProperty("checkoutTimeout", "10000"),
                        DatabaseProperty("maxIdleTime", "1800"),
                        DatabaseProperty("maxConnectionAge", "25200"),
                        DatabaseProperty("acquireIncrement", "5"),
                        DatabaseProperty("maxStatements", "180"),
                        DatabaseProperty("propertyCycle", "3"),
                        DatabaseProperty("unreturnedConnectionTimeout", "240"),
                        DatabaseProperty("autoCommitOnClose", "true"),
                        DatabaseProperty("preferredTestQuery", "SELECT nomSociete FROM societe WHERE 1=2"),
                        DatabaseProperty("switchToManual", "false"),
                        DatabaseProperty("minPoolSizeNormal", "10"),
                        DatabaseProperty("maxPoolSizeNormal", "30"),
                        DatabaseProperty("minPoolSizeSpring", "2"),
                        DatabaseProperty("maxPoolSizeSpring", "5"),
                        DatabaseProperty("SGBD", "MySQL"),
                        DatabaseProperty("liquibaseEnabled", "non"),
                    ),
                    listOf(
                        PropertyLayoutGroup(
                            listOf(
                                "url", "database", "databaseEdi", "usernameNovanet", "passwordNovanet",
                                "usernameSpring", "passwordSpring", "driver",
                            ),
                            "#PARAMETRES CONNEXION BASE DE DONNEES",
                        ),
                        PropertyLayoutGroup(
                            listOf("hibernate.show_sql", "hibernate.dialect"),
                            "# PARAMETRES SPECIFIQUE A HIBERNATE",
                        ),
                        PropertyLayoutGroup(
                            listOf(
                                "checkoutTimeout", "maxIdleTime", "maxConnectionAge", "acquireIncrement",
                                "maxStatements", "propertyCycle", "unreturnedConnectionTimeout", "autoCommitOnClose",
                                "preferredTestQuery", "switchToManual",
                            ),
                            "# PARAMETRES DU POOL DE CONNEXIONS",
                        ),
                        PropertyLayoutGroup(
                            listOf("minPoolSizeNormal", "maxPoolSizeNormal"),
                            "# VALEURS SPECIFQUES POUR CONNEXIONS JDBC ET HIBERNATE NOVANET (ANCIEN OBJETS)",
                        ),
                        PropertyLayoutGroup(
                            listOf("minPoolSizeSpring", "maxPoolSizeSpring"),
                            "# VALEURS SPECIFIQUES POUR CONNEXIONS HIBERNATE SPRING (NOUVEAUX SERVICES)",
                        ),
                        PropertyLayoutGroup(
                            listOf("SGBD", "liquibaseEnabled"),
                            "# AUTRES PARAMETRES BASE DE DONNEES",
                        ),
                    ),
                ),
                "environnement",
            )
            NeoProjectType.GED -> NeoSchema(
                type,
                listOf("jdbc.driverClassName", "jdbc.url", "jdbc.username", "jdbc.password", "hibernate.show_sql", "hibernate.dialect", "urlExt", "urlWSock", "webapps", "smtp", "dossierRoot", "rest.sessionTime", "pspdfkitKey"),
                DatabaseKeys(
                    "jdbc.url",
                    null,
                    listOf("jdbc.username"),
                    listOf("jdbc.password"),
                    listOf(
                        DatabaseProperty("jdbc.driverClassName", "com.mysql.jdbc.Driver"),
                        DatabaseProperty("hibernate.show_sql", "false"),
                        DatabaseProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect"),
                    ),
                    listOf(
                        PropertyLayoutGroup(
                            listOf("jdbc.driverClassName", "jdbc.url"),
                            "# PARAMETRES CONNEXION BASE DE DONNEES",
                        ),
                        PropertyLayoutGroup(
                            listOf("jdbc.username", "jdbc.password"),
                            "# Identifiants base de donnee",
                        ),
                        PropertyLayoutGroup(
                            listOf("hibernate.show_sql", "hibernate.dialect"),
                            "# PARAMETRES SPECIFIQUE A HIBERNATE",
                        ),
                    ),
                ),
            )
            NeoProjectType.SELFCARE -> NeoSchema(type, listOf("urlWebService", "login", "password", "cssSocieteUser", "keyPrivateCaptcha", "keyPublicCaptcha"))
        }
    }
}
