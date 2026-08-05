# Antenia Projects

Antenia Projects is an IntelliJ IDEA Ultimate plugin for configuring and running Antenia Neo Maven web applications. It recognizes supported projects, creates their local configuration, and prepares the IntelliJ SDK, artifact, database, and run configuration needed for development.

## Supported Projects

Projects are identified from the root `pom.xml` artifact ID.

| Project | Artifact ID | Configuration variable | Web context | Ports (HTTP/HTTPS/JMX) |
| --- | --- | --- | --- | --- |
| Neo Core | `webapp-novanet` | `NOVANET_DIR` | `/novanet` | `8080` / `8443` / `1999` |
| Neo GED | `webapp-ged` | `GED_DIR` | `/ged` | `8081` / `8444` / `2000` |
| Neo Selfcare | `webapp-owlnet` | `OWLNET_DIR` | `/owlnet` | `8082` / `8445` / `2001` |

The Java version declared in the Maven properties determines the expected development stack:

| Java | Tomcat |
| --- | --- |
| 8 | 9 |
| 17 | 10.1 |
| 25 and later | 11 |

The plugin checks `java.version`, `jdk.version`, `version.compiler`, `maven.compiler.release`, `maven.compiler.target`, then `maven.compiler.source`. If none is present, Java 8 is assumed.

## Features

- Creates project configuration under `<project>/.local/` from bundled templates.
- Provides a **Neo Configuration** tool window with live, bidirectional file synchronization.
- Preserves property order, comments, blank lines, and manually added keys.
- Provides dedicated database and environment editors plus known-key completion.
- Stores reusable global database credentials in IntelliJ Password Safe.
- Creates or updates an IntelliJ MySQL data source for the configured host and database.
- Selects a matching project SDK and configures a 4096 MB compiler heap.
- Creates the exploded WAR artifact and a local `webapp` Tomcat run configuration.
- Injects the project configuration directory and `JAVA_TOOL_OPTIONS=-Djava.rmi.server.hostname=127.0.0.1` when a run configuration starts.
- Creates npm and compound run configurations when Neo Core contains `novanet-react/package.json`.

Projects that do not match a supported artifact ID are left unchanged. The global settings page remains available.

## Configuration

Open **View > Tool Windows > Neo Configuration** in a supported project. Changes made in the tool window are immediately written to the corresponding file, and external file changes are reloaded into the editor.

Generated files are organized as follows:

```text
.local/
|-- novanet/
|   |-- novanet.properties
|   `-- novaLog.xml
|-- ged/
|   |-- configuration.properties
|   `-- gedLog.xml
`-- owlnet/
    `-- owlnet.properties
```

Only the directory for the detected project type is created. Use **Reset** in the tool window to restore the bundled property template.

### Global Database Credentials

Configure a shared database username and password under **Settings > Tools > Antenia Projects**. Secrets are stored through IntelliJ Password Safe and are synchronized to supported projects unless **Override global credentials** is enabled in the project's database form.

The database host, port, and database name are always project-specific. The port defaults to `3306`.

### Run Configuration

The plugin creates a `webapp` local Tomcat configuration after the Maven project and exploded WAR artifact are available. It chooses a compatible configured Tomcat installation, deploys the project artifact at the expected context, and opens the HTTPS application URL after launch.

For Neo Core projects with a `novanet-react` package, the plugin also creates:

- `react`, which runs the package's `dev` npm script;
- `webapp + react`, which starts the frontend and Tomcat together;
- `npm install` and `npm run build` before-launch steps for the web application.

The compound configuration opens the application through `https://localhost:8888/novanet/`.

## Installation

This plugin requires IntelliJ IDEA Ultimate because it uses the Tomcat, Java EE, JavaScript, Maven, and Database integrations bundled with Ultimate.

To install a locally built distribution:

1. Build the plugin with `./gradlew --no-daemon buildPlugin` or `gradlew.bat --no-daemon buildPlugin` on Windows.
2. Open **Settings > Plugins** in IntelliJ IDEA Ultimate.
3. Choose **Install Plugin from Disk** and select the ZIP in `build/distributions/`.
4. Restart the IDE and open a supported Neo project at its Maven root.

Compatible JDK and Tomcat installations must be configured in the IDE for automatic selection.

## Development

The project uses Kotlin, Gradle, and the IntelliJ Platform Gradle Plugin. The current development platform is IntelliJ IDEA Ultimate 2025.3.5.

```shell
# Run unit tests and checks
./gradlew --no-daemon check

# Start a sandbox IDE with the plugin installed
./gradlew --no-daemon runIde

# Run IntelliJ Plugin Verifier
./gradlew --no-daemon verifyPlugin

# Build the installable plugin ZIP
./gradlew --no-daemon buildPlugin
```

On Windows, use `gradlew.bat` instead of `./gradlew`. Always pass `--no-daemon`; `gradle.properties` also disables the daemon as a safeguard.

Shared IntelliJ run configurations are available for the sandbox IDE, tests, and plugin verification.

## Specification

The requirements in [`spec/`](spec/) are the source of truth for project behavior. See [`AGENT.md`](AGENT.md) for repository architecture and contribution guidance.
