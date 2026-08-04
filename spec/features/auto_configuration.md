# Auto configuration

The IntelliJ project should be automatically configured with values depending on the project type, version, and whether it has react subproject or not.

- Configure the Java version that match the project version
  chose amongst possible java versions in order of preference (temurin, highest version first, then other jdks, highest version first)
- Compilation shared heap size: 4096

# Artifact

All projects should have the auto generated artifact `webapp-{project}:war exploded` present, or it should be regenerated.
- Neo Core: `webapp-novanet:war exploded`
- Neo GED: `webapp-ged:war exploded`
- Neo Selfcare: `webapp-owlnet:war exploded`

e.g.
```xml
<component name="ArtifactManager">
  <artifact type="exploded-war" build-on-make="true" name="webapp-novanet">
    <output-path>$PROJECT_DIR$/target/novanet</output-path>
    <root id="root">
      <element id="directory" name="WEB-INF">
        <element id="directory" name="classes">
          <element id="module-output" name="webapp-novanet" />
        </element>
        <element id="directory" name="lib">
          <element id="library" level="project" name="Maven: jakarta.annotation:jakarta.annotation-api:2.1.1" />
          <element id="library" level="project" name="Maven: jakarta.mail:jakarta.mail-api:2.1.0" />
          <element id="library" level="project" name="Maven: jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.2" />
          <element id="library" level="project" name="Maven: jakarta.el:jakarta.el-api:5.0.0" />
          <element id="library" level="project" name="Maven: org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1" />
          <element id="library" level="project" name="Maven: org.springframework:spring-orm:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-beans:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-core:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-jdbc:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-tx:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-test:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-webmvc:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-aop:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-context:6.2.8" />
          <element id="library" level="project" name="Maven: io.micrometer:micrometer-observation:1.14.8" />
          <element id="library" level="project" name="Maven: io.micrometer:micrometer-commons:1.14.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-expression:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework:spring-web:6.2.8" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-config:6.4.6" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-core:6.4.6" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-crypto:6.4.6" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-ldap:6.4.6" />
          <element id="library" level="project" name="Maven: org.springframework.ldap:spring-ldap-core:3.2.12" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-taglibs:6.4.6" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-acl:6.4.6" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-web:6.4.6" />
          <element id="library" level="project" name="Maven: org.hibernate.orm:hibernate-core:6.6.22.Final" />
          <element id="library" level="project" name="Maven: jakarta.persistence:jakarta.persistence-api:3.1.0" />
          <element id="library" level="project" name="Maven: jakarta.transaction:jakarta.transaction-api:2.0.1" />
          <element id="library" level="project" name="Maven: org.jboss.logging:jboss-logging:3.5.0.Final" />
          <element id="library" level="project" name="Maven: org.hibernate.common:hibernate-commons-annotations:7.0.3.Final" />
          <element id="library" level="project" name="Maven: io.smallrye:jandex:3.2.0" />
          <element id="library" level="project" name="Maven: com.fasterxml:classmate:1.5.1" />
          <element id="library" level="project" name="Maven: net.bytebuddy:byte-buddy:1.15.11" />
          <element id="library" level="project" name="Maven: jakarta.inject:jakarta.inject-api:2.0.1" />
          <element id="library" level="project" name="Maven: org.antlr:antlr4-runtime:4.13.0" />
          <element id="library" level="project" name="Maven: com.mysql:mysql-connector-j:9.2.0" />
          <element id="library" level="project" name="Maven: com.google.protobuf:protobuf-java:4.29.0" />
          <element id="library" level="project" name="Maven: jakarta.validation:jakarta.validation-api:3.1.1" />
          <element id="library" level="project" name="Maven: org.hibernate.validator:hibernate-validator:8.0.2.Final" />
          <element id="library" level="project" name="Maven: jakarta.activation:jakarta.activation-api:2.1.3" />
          <element id="library" level="project" name="Maven: net.bull.javamelody:javamelody-core:2.5.0" />
          <element id="library" level="project" name="Maven: org.jrobin:jrobin:1.5.9" />
          <element id="library" level="project" name="Maven: nitehawk42.dwr:dwr:4.0.0-RELEASE" />
          <element id="library" level="project" name="Maven: org.apache.logging.log4j:log4j-slf4j-impl:2.24.3" />
          <element id="library" level="project" name="Maven: org.apache.logging.log4j:log4j-api:2.24.3" />
          <element id="library" level="project" name="Maven: org.slf4j:slf4j-api:1.7.36" />
          <element id="library" level="project" name="Maven: org.apache.logging.log4j:log4j-core:2.24.3" />
          <element id="library" level="project" name="Maven: org.apache.logging.log4j:log4j-jakarta-web:2.24.3" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-email2-jakarta:2.0.0-M1" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-email2-core:2.0.0-M1" />
          <element id="library" level="project" name="Maven: com.sun.mail:jakarta.mail:2.0.1" />
          <element id="library" level="project" name="Maven: com.sun.activation:jakarta.activation:2.0.1" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-text:1.13.0" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-lang3:3.17.0" />
          <element id="library" level="project" name="Maven: commons-io:commons-io:2.18.0" />
          <element id="library" level="project" name="Maven: commons-logging:commons-logging:1.3.5" />
          <element id="library" level="project" name="Maven: commons-net:commons-net:3.11.1" />
          <element id="library" level="project" name="Maven: commons-validator:commons-validator:1.9.0" />
          <element id="library" level="project" name="Maven: commons-beanutils:commons-beanutils:1.9.4" />
          <element id="library" level="project" name="Maven: commons-digester:commons-digester:2.1" />
          <element id="library" level="project" name="Maven: commons-collections:commons-collections:3.2.2" />
          <element id="library" level="project" name="Maven: org.glassfish.metro:webservices-rt:4.0.4" />
          <element id="library" level="project" name="Maven: org.glassfish.metro:webservices-api:4.0.4" />
          <element id="library" level="project" name="Maven: org.apache.santuario:xmlsec:4.0.2" />
          <element id="library" level="project" name="Maven: com.fasterxml.woodstox:woodstox-core:7.0.0" />
          <element id="library" level="project" name="Maven: org.codehaus.woodstox:stax2-api:4.2.2" />
          <element id="library" level="project" name="Maven: org.glassfish.metro:webservices-tools:4.0.4" />
          <element id="library" level="project" name="Maven: org.glassfish.metro:webservices-extra:4.0.4" />
          <element id="library" level="project" name="Maven: org.glassfish.metro:webservices-extra-api:2.4.10" />
          <element id="library" level="project" name="Maven: jakarta.xml.soap:jakarta.xml.soap-api:1.4.2" />
          <element id="library" level="project" name="Maven: jakarta.xml.bind:jakarta.xml.bind-api:4.0.2" />
          <element id="library" level="project" name="Maven: jakarta.xml.ws:jakarta.xml.ws-api:4.0.2" />
          <element id="library" level="project" name="Maven: com.sun.xml.bind:jaxb-impl:4.0.5" />
          <element id="file-copy" path="$PROJECT_DIR$/../../../../Users/LéopoldHubert/.m2/repository/com/sun/xml/bind/jaxb-core/4.0.5/jaxb-core-4.0.5.jar" output-file-name="com.sun.xml.bind-jaxb-core-4.0.5.jar" />
          <element id="library" level="project" name="Maven: org.eclipse.angus:angus-activation:2.0.2" />
          <element id="library" level="project" name="Maven: org.apache.xmlrpc:xmlrpc-client:3.1.3" />
          <element id="library" level="project" name="Maven: org.apache.xmlrpc:xmlrpc-common:3.1.3" />
          <element id="library" level="project" name="Maven: org.apache.ws.commons.util:ws-commons-util:1.0.2" />
          <element id="library" level="project" name="Maven: junit:junit:3.8.1" />
          <element id="library" level="project" name="Maven: org.apache.xmlrpc:xmlrpc-server:3.1.3" />
          <element id="library" level="project" name="Maven: org.aspectj:aspectjrt:1.9.22.1" />
          <element id="library" level="project" name="Maven: org.aspectj:aspectjweaver:1.9.22.1" />
          <element id="library" level="project" name="Maven: org.aspectj:aspectjtools:1.9.22.1" />
          <element id="library" level="project" name="Maven: org.owasp.esapi:esapi:jakarta:2.6.0.0" />
          <element id="library" level="project" name="Maven: xom:xom:1.3.9" />
          <element id="library" level="project" name="Maven: commons-configuration:commons-configuration:1.10" />
          <element id="library" level="project" name="Maven: commons-lang:commons-lang:2.6" />
          <element id="library" level="project" name="Maven: commons-fileupload:commons-fileupload:1.5" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-collections4:4.5.0-M2" />
          <element id="library" level="project" name="Maven: org.apache-extras.beanshell:bsh:2.0b6" />
          <element id="library" level="project" name="Maven: org.owasp.antisamy:antisamy:1.7.7" />
          <element id="library" level="project" name="Maven: org.apache.httpcomponents.core5:httpcore5:5.3.1" />
          <element id="library" level="project" name="Maven: org.htmlunit:neko-htmlunit:4.6.0" />
          <element id="library" level="project" name="Maven: xerces:xercesImpl:2.12.2" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-fileupload2-jakarta-servlet6:2.0.0-M2" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-fileupload2-core:2.0.0-M2" />
          <element id="library" level="project" name="Maven: net.sourceforge.jexcelapi:jxl:2.6.12" />
          <element id="library" level="project" name="Maven: log4j:log4j:1.2.14" />
          <element id="library" level="project" name="Maven: org.jxls:jxls:2.14.0" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-jexl3:3.2" />
          <element id="library" level="project" name="Maven: ch.qos.logback:logback-core:1.2.13" />
          <element id="library" level="project" name="Maven: org.apache.poi:poi-ooxml:5.4.0" />
          <element id="library" level="project" name="Maven: org.apache.poi:poi:5.4.0" />
          <element id="library" level="project" name="Maven: com.zaxxer:SparseBitSet:1.3" />
          <element id="library" level="project" name="Maven: org.apache.poi:poi-ooxml-lite:5.4.0" />
          <element id="library" level="project" name="Maven: org.apache.xmlbeans:xmlbeans:5.3.0" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-compress:1.27.1" />
          <element id="library" level="project" name="Maven: com.github.virtuald:curvesapi:1.08" />
          <element id="library" level="project" name="Maven: org.apache.poi:poi-scratchpad:5.4.0" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-math3:3.6.1" />
          <element id="library" level="project" name="Maven: commons-codec:commons-codec:1.17.1" />
          <element id="library" level="project" name="Maven: com.itextpdf:itextpdf:5.5.13.4" />
          <element id="library" level="project" name="Maven: org.apache.pdfbox:pdfbox:2.0.27" />
          <element id="library" level="project" name="Maven: org.apache.pdfbox:fontbox:2.0.27" />
          <element id="library" level="project" name="Maven: com.hierynomus:smbj:0.14.0" />
          <element id="library" level="project" name="Maven: org.bouncycastle:bcprov-jdk18on:1.79" />
          <element id="library" level="project" name="Maven: net.engio:mbassador:1.3.0" />
          <element id="library" level="project" name="Maven: com.hierynomus:asn-one:0.6.0" />
          <element id="library" level="project" name="Maven: jcifs:jcifs:1.3.17" />
          <element id="library" level="project" name="Maven: javax.servlet:servlet-api:2.4" />
          <element id="library" level="project" name="Maven: org.glassfish:jakarta.json:2.0.1" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:2.18.2" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.core:jackson-core:2.18.2" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.core:jackson-databind:2.18.2" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:2.18.2" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.18.2" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.core:jackson-annotations:2.18.2" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.datatype:jackson-datatype-hibernate5:2.18.2" />
          <element id="library" level="project" name="Maven: javax.transaction:javax.transaction-api:1.3" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.module:jackson-module-jsonSchema:2.18.2" />
          <element id="library" level="project" name="Maven: javax.validation:validation-api:1.1.0.Final" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.module:jackson-module-scala_2.13:2.18.2" />
          <element id="library" level="project" name="Maven: org.scala-lang:scala-library:2.13.15" />
          <element id="library" level="project" name="Maven: com.thoughtworks.paranamer:paranamer:2.8" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.datatype:jackson-datatype-joda:2.18.2" />
          <element id="library" level="project" name="Maven: joda-time:joda-time:2.12.7" />
          <element id="library" level="project" name="Maven: io.jsonwebtoken:jjwt-api:0.12.6" />
          <element id="library" level="project" name="Maven: io.jsonwebtoken:jjwt-impl:0.12.6" />
          <element id="library" level="project" name="Maven: io.jsonwebtoken:jjwt-jackson:0.12.6" />
          <element id="library" level="project" name="Maven: io.jsonwebtoken:jjwt-gson:0.12.6" />
          <element id="library" level="project" name="Maven: org.apache.httpcomponents:httpclient:4.5.14" />
          <element id="library" level="project" name="Maven: org.apache.httpcomponents:httpcore:4.4.16" />
          <element id="library" level="project" name="Maven: org.apache.httpcomponents:httpmime:4.5.14" />
          <element id="library" level="project" name="Maven: org.apache.chemistry.opencmis:chemistry-opencmis-client-impl:1.1.0" />
          <element id="library" level="project" name="Maven: org.apache.chemistry.opencmis:chemistry-opencmis-client-api:1.1.0" />
          <element id="library" level="project" name="Maven: org.apache.chemistry.opencmis:chemistry-opencmis-commons-api:1.1.0" />
          <element id="library" level="project" name="Maven: org.apache.chemistry.opencmis:chemistry-opencmis-commons-impl:1.1.0" />
          <element id="library" level="project" name="Maven: org.apache.chemistry.opencmis:chemistry-opencmis-client-bindings:1.1.0" />
          <element id="library" level="project" name="Maven: org.apache.cxf:cxf-rt-transports-http:3.0.12" />
          <element id="library" level="project" name="Maven: org.apache.cxf:cxf-rt-ws-policy:3.0.12" />
          <element id="library" level="project" name="Maven: wsdl4j:wsdl4j:1.6.3" />
          <element id="library" level="project" name="Maven: org.apache.neethi:neethi:3.0.3" />
          <element id="library" level="project" name="Maven: org.apache.cxf:cxf-rt-frontend-simple:4.1.0" />
          <element id="library" level="project" name="Maven: org.apache.cxf:cxf-core:4.1.0" />
          <element id="library" level="project" name="Maven: org.glassfish.jaxb:jaxb-runtime:4.0.5" />
          <element id="file-copy" path="$PROJECT_DIR$/../../../../Users/LéopoldHubert/.m2/repository/org/glassfish/jaxb/jaxb-core/4.0.5/jaxb-core-4.0.5.jar" output-file-name="org.glassfish.jaxb-jaxb-core-4.0.5.jar" />
          <element id="library" level="project" name="Maven: org.glassfish.jaxb:txw2:4.0.5" />
          <element id="library" level="project" name="Maven: com.sun.istack:istack-commons-runtime:4.1.2" />
          <element id="library" level="project" name="Maven: org.apache.ws.xmlschema:xmlschema-core:2.3.1" />
          <element id="library" level="project" name="Maven: org.apache.cxf:cxf-rt-bindings-soap:4.1.0" />
          <element id="library" level="project" name="Maven: jakarta.jws:jakarta.jws-api:3.0.0" />
          <element id="library" level="project" name="Maven: org.apache.cxf:cxf-rt-databinding-jaxb:4.1.0" />
          <element id="library" level="project" name="Maven: org.apache.cxf:cxf-rt-wsdl:4.1.0" />
          <element id="library" level="project" name="Maven: org.ow2.asm:asm:9.7.1" />
          <element id="library" level="project" name="Maven: org.eclipse.angus:angus-mail:2.0.3" />
          <element id="library" level="project" name="Maven: jakarta.ws.rs:jakarta.ws.rs-api:4.0.0" />
          <element id="library" level="project" name="Maven: org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6" />
          <element id="library" level="project" name="Maven: org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.6" />
          <element id="library" level="project" name="Maven: org.springdoc:springdoc-openapi-starter-common:2.8.6" />
          <element id="library" level="project" name="Maven: org.springframework.boot:spring-boot-autoconfigure:3.4.4" />
          <element id="library" level="project" name="Maven: org.springframework.boot:spring-boot:3.4.4" />
          <element id="library" level="project" name="Maven: io.swagger.core.v3:swagger-core-jakarta:2.2.29" />
          <element id="library" level="project" name="Maven: io.swagger.core.v3:swagger-annotations-jakarta:2.2.29" />
          <element id="library" level="project" name="Maven: io.swagger.core.v3:swagger-models-jakarta:2.2.29" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2" />
          <element id="library" level="project" name="Maven: org.webjars:swagger-ui:5.20.1" />
          <element id="library" level="project" name="Maven: org.webjars:webjars-locator-lite:1.0.1" />
          <element id="library" level="project" name="Maven: org.jspecify:jspecify:1.0.0" />
          <element id="library" level="project" name="Maven: com.artofsolving:jodconverter:2.2.1" />
          <element id="library" level="project" name="Maven: org.openoffice:juh:2.3.0" />
          <element id="library" level="project" name="Maven: org.openoffice:jurt:2.3.0" />
          <element id="library" level="project" name="Maven: org.openoffice:ridl:2.3.0" />
          <element id="library" level="project" name="Maven: org.openoffice:unoil:2.3.0" />
          <element id="library" level="project" name="Maven: net.sf.jasperreports:jasperreports:7.0.3" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.2" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-anim:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-ext:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-parser:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-shared-resources:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-bridge:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-script:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-xml:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:xmlgraphics-commons:2.10" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-dom:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-svg-dom:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-awt-util:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-gvt:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-util:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-i18n:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-constants:1.18" />
          <element id="library" level="project" name="Maven: org.apache.xmlgraphics:batik-css:1.18" />
          <element id="library" level="project" name="Maven: xml-apis:xml-apis-ext:1.3.04" />
          <element id="library" level="project" name="Maven: net.sf.jasperreports:jasperreports-pdf:7.0.3" />
          <element id="library" level="project" name="Maven: com.github.librepdf:openpdf:1.3.32" />
          <element id="library" level="project" name="Maven: com.adobe.xmp:xmpcore:6.1.11" />
          <element id="library" level="project" name="Maven: net.sf.jasperreports:jasperreports-jaxen:7.0.3" />
          <element id="library" level="project" name="Maven: com.zaxxer:HikariCP:7.0.2" />
          <element id="library" level="project" name="Maven: com.ibm.icu:icu4j:76.1" />
          <element id="library" level="project" name="Maven: com.microsoft.ews-java-api:ews-java-api:2.0" />
          <element id="library" level="project" name="Maven: org.jsoup:jsoup:1.18.3" />
          <element id="library" level="project" name="Maven: org.assertj:assertj-core:3.27.3" />
          <element id="library" level="project" name="Maven: xalan:xalan:2.7.3" />
          <element id="library" level="project" name="Maven: xalan:serializer:2.7.3" />
          <element id="library" level="project" name="Maven: com.github.mwiede:jsch:0.2.19" />
          <element id="library" level="project" name="Maven: org.jdom:jdom:1.1" />
          <element id="library" level="project" name="Maven: jaxen:jaxen:2.0.0" />
          <element id="library" level="project" name="Maven: saxpath:saxpath:1.0-FCS" />
          <element id="library" level="project" name="Maven: com.mailjet:mailjet-client:5.2.5" />
          <element id="library" level="project" name="Maven: com.squareup.okhttp3:okhttp:4.10.0" />
          <element id="library" level="project" name="Maven: com.squareup.okio:okio-jvm:3.0.0" />
          <element id="library" level="project" name="Maven: org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.31" />
          <element id="library" level="project" name="Maven: org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.31" />
          <element id="library" level="project" name="Maven: org.jetbrains.kotlin:kotlin-stdlib-common:1.5.31" />
          <element id="library" level="project" name="Maven: org.jetbrains.kotlin:kotlin-stdlib:1.6.20" />
          <element id="library" level="project" name="Maven: org.jetbrains:annotations:13.0" />
          <element id="library" level="project" name="Maven: org.json:json:20231013" />
          <element id="library" level="project" name="Maven: com.docusign:docusign-esign-java:4.1.0" />
          <element id="library" level="project" name="Maven: org.glassfish.jersey.core:jersey-client:3.0.9" />
          <element id="library" level="project" name="Maven: org.glassfish.jersey.core:jersey-common:3.0.9" />
          <element id="library" level="project" name="Maven: org.glassfish.hk2:osgi-resource-locator:1.0.3" />
          <element id="library" level="project" name="Maven: org.glassfish.jersey.media:jersey-media-multipart:3.0.9" />
          <element id="library" level="project" name="Maven: org.jvnet.mimepull:mimepull:1.9.13" />
          <element id="library" level="project" name="Maven: org.glassfish.jersey.media:jersey-media-json-jackson:3.0.9" />
          <element id="library" level="project" name="Maven: org.glassfish.jersey.ext:jersey-entity-filtering:3.0.9" />
          <element id="library" level="project" name="Maven: com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations:2.13.3" />
          <element id="library" level="project" name="Maven: org.glassfish.jersey.inject:jersey-hk2:3.0.9" />
          <element id="library" level="project" name="Maven: org.glassfish.hk2:hk2-locator:3.0.3" />
          <element id="library" level="project" name="Maven: org.glassfish.hk2.external:aopalliance-repackaged:3.0.3" />
          <element id="library" level="project" name="Maven: org.glassfish.hk2:hk2-api:3.0.3" />
          <element id="library" level="project" name="Maven: org.glassfish.hk2:hk2-utils:3.0.3" />
          <element id="library" level="project" name="Maven: org.javassist:javassist:3.29.0-GA" />
          <element id="library" level="project" name="Maven: org.apache.oltu.oauth2:org.apache.oltu.oauth2.client:1.0.2" />
          <element id="library" level="project" name="Maven: org.apache.oltu.oauth2:org.apache.oltu.oauth2.common:1.0.2" />
          <element id="library" level="project" name="Maven: io.swagger:swagger-annotations:1.5.18" />
          <element id="library" level="project" name="Maven: com.auth0:java-jwt:3.4.1" />
          <element id="library" level="project" name="Maven: org.bouncycastle:bcprov-jdk15to18:1.80" />
          <element id="library" level="project" name="Maven: com.google.code.findbugs:jsr305:3.0.2" />
          <element id="library" level="project" name="Maven: com.rabbitmq:amqp-client:5.25.0" />
          <element id="library" level="project" name="Maven: com.aspose:aspose-words:14.5.jdk16" />
          <element id="library" level="project" name="Maven: net.sf.jxls:jxls-core:1.0.6-LI" />
          <element id="library" level="project" name="Maven: org.jxls:jxls-poi:1.0.14" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-jexl:2.1.1" />
          <element id="library" level="project" name="Maven: org.jxls:jxls-jexcel:1.0.6" />
          <element id="library" level="project" name="Maven: net.sf.jxls:jxls-reader:1.0.6" />
          <element id="library" level="project" name="Maven: li:li-xsd-camca_cartes_pibos-jakarta:1.0" />
          <element id="library" level="project" name="Maven: li:li-xsd-dsn_fiche_parametrage:1.0.0" />
          <element id="library" level="project" name="Maven: li:li-xsd-fdj_cautionnement:1.C" />
          <element id="library" level="project" name="Maven: li:li-xsd-fdj_cautionnement2023-jakarta:1.0" />
          <element id="library" level="project" name="Maven: li:li-xsd-maileva:1.0.0" />
          <element id="library" level="project" name="Maven: li:li-xsd-netquarks_sinistre-jakarta:1.0" />
          <element id="library" level="project" name="Maven: li:li-wsdl-airbus:1.0.44695" />
          <element id="library" level="project" name="Maven: li:li-wsdl-covea_consultation_delegataire:1.0.0" />
          <element id="library" level="project" name="Maven: li:li-wsdl-ovh:1.0.0" />
          <element id="library" level="project" name="Maven: li:li-wsdl-salesforces-saam:1.0.60333" />
          <element id="library" level="project" name="Maven: com.force.api:force-wsc:42.0.0" />
          <element id="library" level="project" name="Maven: org.antlr:ST4:4.0.7" />
          <element id="library" level="project" name="Maven: org.antlr:antlr-runtime:3.5" />
          <element id="library" level="project" name="Maven: org.antlr:stringtemplate:3.2.1" />
          <element id="library" level="project" name="Maven: antlr:antlr:2.7.7" />
          <element id="library" level="project" name="Maven: org.codehaus.jackson:jackson-core-asl:1.9.13" />
          <element id="library" level="project" name="Maven: org.codehaus.jackson:jackson-mapper-asl:1.9.13" />
          <element id="library" level="project" name="Maven: li:li-wsdl-selligent:1.0.3" />
          <element id="library" level="project" name="Maven: li:li-wsdl-altares-jakarta:3.0.0" />
          <element id="library" level="project" name="Maven: li:li-api-nuxeo:1.0.0" />
          <element id="library" level="project" name="Maven: com.squareup.retrofit2:retrofit:2.4.0" />
          <element id="library" level="project" name="Maven: li:li-api-agorapay:1.10.4" />
          <element id="library" level="project" name="Maven: org.apache.httpcomponents.client5:httpclient5:5.4.1" />
          <element id="library" level="project" name="Maven: org.apache.httpcomponents.core5:httpcore5-h2:5.3.1" />
          <element id="library" level="project" name="Maven: org.openapitools:jackson-databind-nullable:0.2.6" />
          <element id="library" level="project" name="Maven: com.github.joschi.jackson:jackson-datatype-threetenbp:2.9.10" />
          <element id="library" level="project" name="Maven: org.threeten:threetenbp:1.4.0" />
          <element id="library" level="project" name="Maven: SwisSQL:SwisSQL-api:1.0.0" />
          <element id="library" level="project" name="Maven: SwisSQL:SwisSQL-jdbc:1.0.0" />
          <element id="library" level="project" name="Maven: WordWriter:WordWriter:3.5.1" />
          <element id="library" level="project" name="Maven: WordWriter:WordWriter-License:3" />
          <element id="library" level="project" name="Maven: zehon:zehon_file_transfer:1.1.6" />
          <element id="library" level="project" name="Maven: commons-httpclient:commons-httpclient:3.1" />
          <element id="library" level="project" name="Maven: org.apache.commons:commons-vfs:2.0" />
          <element id="library" level="project" name="Maven: com.google.api-client:google-api-client:2.7.2" />
          <element id="library" level="project" name="Maven: com.google.oauth-client:google-oauth-client:1.36.0" />
          <element id="library" level="project" name="Maven: com.google.auth:google-auth-library-credentials:1.30.0" />
          <element id="library" level="project" name="Maven: com.google.http-client:google-http-client-gson:1.45.2" />
          <element id="library" level="project" name="Maven: com.google.http-client:google-http-client-apache-v2:1.45.2" />
          <element id="library" level="project" name="Maven: com.google.http-client:google-http-client:1.45.2" />
          <element id="library" level="project" name="Maven: io.grpc:grpc-context:1.68.2" />
          <element id="library" level="project" name="Maven: io.grpc:grpc-api:1.68.2" />
          <element id="library" level="project" name="Maven: io.opencensus:opencensus-api:0.31.1" />
          <element id="library" level="project" name="Maven: io.opencensus:opencensus-contrib-http-util:0.31.1" />
          <element id="library" level="project" name="Maven: com.google.oauth-client:google-oauth-client-jetty:1.38.0" />
          <element id="library" level="project" name="Maven: com.google.oauth-client:google-oauth-client-java6:1.38.0" />
          <element id="library" level="project" name="Maven: com.google.guava:guava:33.4.0-jre" />
          <element id="library" level="project" name="Maven: com.google.guava:failureaccess:1.0.2" />
          <element id="library" level="project" name="Maven: com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava" />
          <element id="library" level="project" name="Maven: org.checkerframework:checker-qual:3.43.0" />
          <element id="library" level="project" name="Maven: com.google.errorprone:error_prone_annotations:2.36.0" />
          <element id="library" level="project" name="Maven: com.google.j2objc:j2objc-annotations:3.0.0" />
          <element id="library" level="project" name="Maven: com.google.auth:google-auth-library-oauth2-http:1.33.1" />
          <element id="library" level="project" name="Maven: com.google.auto.value:auto-value-annotations:1.11.0" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-oauth2-jose:6.4.6" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-oauth2-core:6.4.6" />
          <element id="library" level="project" name="Maven: com.nimbusds:nimbus-jose-jwt:9.37.3" />
          <element id="library" level="project" name="Maven: com.github.stephenc.jcip:jcip-annotations:1.0-1" />
          <element id="library" level="project" name="Maven: org.springframework.security:spring-security-oauth2-client:6.4.6" />
          <element id="library" level="project" name="Maven: com.nimbusds:oauth2-oidc-sdk:9.43.6" />
          <element id="library" level="project" name="Maven: com.nimbusds:content-type:2.2" />
          <element id="library" level="project" name="Maven: net.minidev:json-smart:2.5.2" />
          <element id="library" level="project" name="Maven: net.minidev:accessors-smart:2.5.2" />
          <element id="library" level="project" name="Maven: com.nimbusds:lang-tag:1.7" />
          <element id="library" level="project" name="Maven: org.liquibase:liquibase-core:4.31.1" />
          <element id="library" level="project" name="Maven: com.opencsv:opencsv:5.9" />
          <element id="library" level="project" name="Maven: org.yaml:snakeyaml:2.3" />
          <element id="library" level="project" name="Maven: javax.xml.bind:jaxb-api:2.3.1" />
          <element id="library" level="project" name="Maven: io.github.java-diff-utils:java-diff-utils:4.15" />
          <element id="library" level="project" name="Maven: com.google.zxing:core:3.5.3" />
          <element id="library" level="project" name="Maven: com.google.zxing:javase:3.5.3" />
          <element id="library" level="project" name="Maven: com.beust:jcommander:1.82" />
          <element id="library" level="project" name="Maven: com.github.jai-imageio:jai-imageio-core:1.4.0" />
          <element id="library" level="project" name="Maven: javax.persistence:javax.persistence-api:2.2" />
          <element id="library" level="project" name="Maven: com.google.code.gson:gson:2.14.0" />
        </element>
      </element>
      <element id="directory" name="META-INF">
        <element id="file-copy" path="$PROJECT_DIR$/target/novanet/META-INF/MANIFEST.MF" />
      </element>
      <element id="javaee-facet-resources" facet="webapp-novanet/web/Web" />
    </root>
  </artifact>
</component>
```

# Run configuration

All projects should have a valid tomcat server local run configuration `webapp` at all time.
It should always:
- have the proper tomcat server (highest version first)
- have the java version set to project default
- have the correct artifact (verify validity!) with the application context
  - Neo Core: `/novanet`
  - Neo Ged: `/ged`
  - Neo Selfcare: `/owlnet`
- Have the corresponding ports:
  - Neo Core: `http: 8080, https: 8443, JMX: 1999`
  - Neo GED: `http: 8081, https: 8444, JMX: 2000`
  - Neo Selfcare: `http: 8082, https: 8445, JMX: 2001`
- Have after launch enabled on default browser with corresponding url `https://localhost:[https_port]/[application_context]/`
- Have on frame deactivation to relead classes and resources

e.g.
```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="webapp" type="#com.intellij.j2ee.web.tomcat.TomcatRunConfigurationFactory" factoryName="Local" APPLICATION_SERVER_NAME="Tomcat 10.1.57" ALTERNATIVE_JRE_ENABLED="false">
    <option name="OPEN_IN_BROWSER_URL" value="https://localhost:8443/novanet/" />
    <option name="UPDATE_ON_FRAME_DEACTIVATION" value="true" />
    <option name="UPDATE_CLASSES_ON_FRAME_DEACTIVATION" value="true" />
    <option name="UPDATING_POLICY" value="restart-server" />
    <deployment>
      <artifact name="webapp-novanet:war exploded">
        <settings>
          <option name="CONTEXT_PATH" value="/novanet" />
        </settings>
      </artifact>
    </deployment>
    <server-settings>
      <option name="BASE_DIRECTORY_NAME" value="322d655d-f894-4909-aba0-030c7b4791e6" />
      <option name="HTTPS_PORT" value="8443" />
    </server-settings>
    <predefined_log_file enabled="true" id="Tomcat" />
    <predefined_log_file enabled="true" id="Tomcat Catalina" />
    <predefined_log_file id="Tomcat Manager" />
    <predefined_log_file id="Tomcat Host Manager" />
    <predefined_log_file id="Tomcat Localhost Access" />
    <RunnerSettings RunnerId="Debug">
      <option name="DEBUG_PORT" value="51380" />
    </RunnerSettings>
    <ConfigurationWrapper VM_VAR="JAVA_OPTS" RunnerId="Cover">
      <option name="USE_ENV_VARIABLES" value="true" />
      <STARTUP>
        <option name="USE_DEFAULT" value="true" />
        <option name="SCRIPT" value="" />
        <option name="VM_PARAMETERS" value="" />
        <option name="PROGRAM_PARAMETERS" value="" />
      </STARTUP>
      <SHUTDOWN>
        <option name="USE_DEFAULT" value="true" />
        <option name="SCRIPT" value="" />
        <option name="VM_PARAMETERS" value="" />
        <option name="PROGRAM_PARAMETERS" value="" />
      </SHUTDOWN>
    </ConfigurationWrapper>
    <ConfigurationWrapper VM_VAR="JAVA_OPTS" RunnerId="Debug">
      <option name="USE_ENV_VARIABLES" value="true" />
      <STARTUP>
        <option name="USE_DEFAULT" value="true" />
        <option name="SCRIPT" value="" />
        <option name="VM_PARAMETERS" value="" />
        <option name="PROGRAM_PARAMETERS" value="" />
      </STARTUP>
      <SHUTDOWN>
        <option name="USE_DEFAULT" value="true" />
        <option name="SCRIPT" value="" />
        <option name="VM_PARAMETERS" value="" />
        <option name="PROGRAM_PARAMETERS" value="" />
      </SHUTDOWN>
    </ConfigurationWrapper>
    <ConfigurationWrapper VM_VAR="JAVA_OPTS" RunnerId="Profile">
      <option name="USE_ENV_VARIABLES" value="true" />
      <STARTUP>
        <option name="USE_DEFAULT" value="true" />
        <option name="SCRIPT" value="" />
        <option name="VM_PARAMETERS" value="" />
        <option name="PROGRAM_PARAMETERS" value="" />
      </STARTUP>
      <SHUTDOWN>
        <option name="USE_DEFAULT" value="true" />
        <option name="SCRIPT" value="" />
        <option name="VM_PARAMETERS" value="" />
        <option name="PROGRAM_PARAMETERS" value="" />
      </SHUTDOWN>
    </ConfigurationWrapper>
    <ConfigurationWrapper VM_VAR="JAVA_OPTS" RunnerId="Run">
      <option name="USE_ENV_VARIABLES" value="true" />
      <STARTUP>
        <option name="USE_DEFAULT" value="true" />
        <option name="SCRIPT" value="" />
        <option name="VM_PARAMETERS" value="" />
        <option name="PROGRAM_PARAMETERS" value="" />
      </STARTUP>
      <SHUTDOWN>
        <option name="USE_DEFAULT" value="true" />
        <option name="SCRIPT" value="" />
        <option name="VM_PARAMETERS" value="" />
        <option name="PROGRAM_PARAMETERS" value="" />
      </SHUTDOWN>
    </ConfigurationWrapper>
    <method v="2">
      <option name="Make" enabled="true" />
      <option name="BuildArtifacts" enabled="true">
        <artifact name="webapp-novanet:war exploded" />
      </option>
    </method>
  </configuration>
</component>
```

If the project has a react subproject, (e.g. `novanet-react`), it should create a new `react` run configuration:

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="react" type="js.build_tools.npm">
    <package-json value="$PROJECT_DIR$/novanet-react/package.json" />
    <command value="run" />
    <scripts>
      <script value="dev" />
    </scripts>
    <node-interpreter value="project" />
    <envs />
    <method v="2" />
  </configuration>
</component>
```

and create a compound run configuration that starts the react part first, then tomcat.

Additionally, the launch URL should be changed to `https://localhost:8888/[application_context]/`.

Run configurations should wait for the artifact to exist before referencing it.

It should never touch other run configurations other than those listed here.

Once created, it should not change the run configuration settings, except for the artifact that should always properly reference the artifact.