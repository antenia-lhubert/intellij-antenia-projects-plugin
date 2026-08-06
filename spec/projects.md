# Project types

There exist different project types, one for each type of software we develop.
They can be identified by their name in their respective pom.xml artifactId.

- Neo Core: `webapp-novanet`
- Neo GED: `webapp-ged`
- Neo Selfcare: `webapp-owlnet`

Those are Maven Java + Tomcat artifact project

If a project is none, no Neo feature (except global configuration) should be enabled.

# Project versions

Each of those projects have versions:

- 0-1.4: Java 8, Tomcat 9
- 1.5: Java 17, tomcat 10.1
- 1.6+: Java 25, tomcat 10.1

Those versions are not specified as version number in the pom.xml.
Instead, they can be deduced from the java version in the pom (in priority order: `java.version`, `jdk.version`, `version.compiler`, `maven.compiler.release`, `maven.compiler.target`, `maven.compiler.source`).

# Project configurations

Each project type have specific configurations at runtime:

- Neo Core:

`novanet.properties`
```properties
#PARAMETRES CONNEXION BASE DE DONNEES
url=jdbc\:mysql\:///?autoReconnect\=true
database=
databaseEdi=
environnement=DEV

usernameNovanet=
passwordNovanet=
usernameSpring=
passwordSpring=
driver=com.mysql.jdbc.Driver
webapps=novanet

# PARAMETRES SPECIFIQUE A HIBERNATE
hibernate.show_sql=false
hibernate.dialect=org.hibernate.dialect.MySQLDialect

# PARAMETRES DU POOL DE CONNEXIONS
checkoutTimeout=10000
maxIdleTime=1800
maxConnectionAge=25200
acquireIncrement=5
maxStatements=180
propertyCycle=3
unreturnedConnectionTimeout=240
autoCommitOnClose=true
preferredTestQuery=SELECT nomSociete FROM societe WHERE 1=2
switchToManual=false

# VALEURS SPECIFQUES POUR CONNEXIONS JDBC ET HIBERNATE NOVANET (ANCIEN OBJETS)
minPoolSizeNormal=10
maxPoolSizeNormal=30

# VALEURS SPECIFIQUES POUR CONNEXIONS HIBERNATE SPRING (NOUVEAUX SERVICES)
minPoolSizeSpring=2
maxPoolSizeSpring=5

# AUTRES PARAMETRES
boiteEnvoiBRB=E_MAIL
CouperRequetes=oui
connexionCourrier=
destanows=E_MAIL
Encoding=ISO-8859-1
Thawte=non
ipLocalGED=localhost:8444
from=novanet@leaderinfo.com
ipRemoteOOo=localhost:8443
ipLocalServeur=localhost:8443
ipLocalRequete=localhost:8443
ipRemoteServeur=localhost:8443
ipRemoteRequete=localhost:8443
LDAP=non
logsNovanet=non
MessageRefuDroit="Vous n'avez pas les droits !"
PropertyCycle=3
smtp=postfix.leaderinfo.com
serveurws=localhost:8443
SGBD=MySQL
SiteRetour=http\://www.leaderinfo.com
urlext=https\://localhost:8443/novanet/

saltValue=KuRJtEPlba
liquibaseEnabled=non

GlobalSSOLoginRedirectAuto=non
GlobalSSOLoginCreateAuto=oui
GlobalSSOLoginCredentialsForce=non

# PARAMETRES LIAISON NOVANET-WEBACTION
WebActionUrlDataBase=
WebActionDriverDataBase=com.mysql.jdbc.Driver
WebActionLoginDataBase=LOGIN_BDD
WebActionPasswordDataBase=PASSWORD_BDD
WebActionUrlServerApplication=https://localhost:8443
WebActionUrlServerApplicationTest=https://localhost:8443
WebActionUrlServerApplicationInterne=https://localhost:8443

#LOG
timeOutLogger=30000

#NovaNetCourrier
mailmerge=

# CFT
ipServerCFT=

# NEOHUB (en Mo)
neohub.upload.maxfilesize=1000
neohub.upload.maxgroupedfilesize=1000
```

`novaLog.xml`
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">

<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/" threshold="all" debug="true">
    <!-- Parametrage du fichier de logs CONNECTIONS_FAILED, concernant les problemes de connection -->
    <logger name="com.mchange" additivity="false">
        <level value="WARN"/>
        <appender-ref ref="c3p0failed"/>
    </logger>
    <appender name="c3p0failed" class="org.apache.log4j.DailyRollingFileAppender">
        <param name="file" value="${dirNovanet}/logs/novanet/c3p0/CONNECTIONS_FAILED"/>
        <param name="DatePattern" value="'_'yyyy-MM-dd'.LOG'"/>
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="[NOVA] %d{dd/MM/yyyy HH:mm:ss} [%p] %c ==> ligne: %L ==> %m%n"/>
        </layout>
    </appender>

    <!-- Parametrage du fichier de logs CONNECTIONS_FAILED, concernant les problemes de connection -->
    <logger name="batch.">
        <level value="ALL"/>
        <appender-ref ref="traceBatch"/>
    </logger>
    <logger name="com.leaderinfo.novanet.batch">
        <level value="ALL"/>
        <appender-ref ref="traceBatch"/>
    </logger>
    <appender name="traceBatch" class="org.apache.log4j.DailyRollingFileAppender">
        <param name="file" value="${dirNovanet}/logs/novanet/batch/TRACES_BATCH"/>
        <param name="DatePattern" value="'_'yyyy-MM-dd-HH'.LOG'"/>
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="[NOVA] %d{dd/MM/yyyy HH:mm:ss} [%p] %c ==> ligne: %L ==> %m%n"/>
        </layout>
    </appender>

    <logger name="com.leaderinfo.novanet.">
        <level value="ALL"/>
        <appender-ref ref="traceBatch"/>
    </logger>
    <logger name="org.hibernate.orm.deprecation" additivity="false" level="WARN"></logger>

    <root>
        <level value="INFO"/>
        <appender-ref ref="stdout"/>
        <appender-ref ref="logsNovanet"/>
    </root>
    <!-- Precise que les logger vont sur la console -->
    <appender name="stdout" class="org.apache.log4j.ConsoleAppender">
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="[NOVA] %d{dd/MM/yyyy HH:mm:ss} [%p] %c ==> ligne: %L ==> %m%n"/>
        </layout>
    </appender>
    <!-- Parametrage du fichier de logs NOVANET_WARN, contenant les erreurs des de NovaNet-->
    <appender name="logsNovanet" class="org.apache.log4j.DailyRollingFileAppender">
        <param name="file" value="${dirNovanet}/logs/novanet/novanet/NOVANET_WARN"/>
        <param name="DatePattern" value="'_'yyyy-MM-dd'.LOG'"/>
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="[NOVA] %d{dd/MM/yyyy HH:mm:ss} [%p] %c ==> ligne: %L ==> %m%n"/>
        </layout>
    </appender>
</log4j:configuration>
```

- Neo GED

`configuration.properties`
```properties
# PARAMETRES CONNEXION BASE DE DONNEES
jdbc.driverClassName=com.mysql.jdbc.Driver

jdbc.url=jdbc\:mysql\:///?autoReconnect\=true

# Identifiants base de donnée
jdbc.username=
jdbc.password=

hibernate.show_sql=false
hibernate.dialect=org.hibernate.dialect.MySQLDialect

urlExt=https://localhost:8444
urlWSock=wss://localhost:8444
webapps=ged
smtp=postfix.leaderinfo.com
dossierRoot=C:/tools/applications/ged/data/

# Temps des sessions REST (exprimé en milliseconde, 0=infini)
rest.sessionTime=600000

# Sert a annoter des PDF directement depuis la GED.
pspdfkitKey=
```

`gedLog.xml`
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">

<log4j:configuration xmlns:log4j="http://jakarta.apache.org/log4j/" threshold="all" debug="true">
	<!-- Paramètrage du fichier de logs GED_WARN, contenant les erreurs de G.E.D.-->
	<appender name="fichier" class="org.apache.log4j.DailyRollingFileAppender">
	 	<param name="file" value="${dirGed}/logs/ged/GED_WARN"/>
		<param name="DatePattern" value="'_'yyyy-MM-dd'.LOG'"/>
		<layout class="org.apache.log4j.PatternLayout">
			<param name="ConversionPattern" value="[GED] %d{dd/MM/yyyy HH:mm:ss} [%p] %c ==> ligne: %L ==> %m%n" />
		</layout>
	</appender>
	<!-- Précise que les logger vont sur la console -->
	<appender name="stdout" class="org.apache.log4j.ConsoleAppender">
		<layout class="org.apache.log4j.PatternLayout">
			<param name="ConversionPattern" value="[GED] %d{dd/MM/yyyy HH:mm:ss} [%p] %c ==> ligne: %L ==> %m%n" />
		</layout>
	</appender>
	<root>
		<level value="INFO"/>
		<appender-ref ref="stdout" />
		<appender-ref ref="fichier" />
	</root>
</log4j:configuration> 
```

- Neo Selfcare

`owlnet.properties`
```properties
#PARAMETRES AUTHENTIFICATION WEBSERVICES
urlWebService=https://localhost:8443/novanet/

# Identifiants webservice Neo Core
login=
password=

# Style spécifique
cssSocieteUser=

# Identifiants captcha google
keyPrivateCaptcha=6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe
keyPublicCaptcha=6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI
```

# Project env vars

Project configuration root folder path is passed as env var when running a project.

- Neo Core:
  `NOVANET_DIR`
  configs location should be
  `%NOVANET_DIR%/novanet/novaLog.xml`
  `%NOVANET_DIR%/novanet/novanet.properties`

- Neo GED:
  `GED_DIR`
  configs location should be
  `%GED_DIR%/ged/gedLog.xml`
  `%GED_DIR%/ged/configuration.properties`

- Neo Selfcare:
  `OWLNET_DIR`
  configs location should be
  `%OWLNET_DIR%/owlnet/owlnet.properties`

# React subproject

Some projects have a react subproject

- Neo Core: `novanet-react` at project root

(there might be more uin the future)

Those projects need an extra step at compilation before compiling the rest:
`npm i && npm run build`