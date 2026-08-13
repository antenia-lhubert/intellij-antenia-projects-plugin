# Antenia Projects

Antenia Projects is an IntelliJ IDEA Ultimate plugin for configuring and running Antenia Neo Maven web applications. It recognizes supported projects, creates their local configuration, and prepares the IntelliJ SDK, artifact, database, and run configuration needed for development.

## Install and Enable Updates

1. Open **Settings > Plugins** in IntelliJ IDEA Ultimate.
2. Open the gear menu and choose **Manage Plugin Repositories**.
3. Add `https://raw.githubusercontent.com/antenia-lhubert/intellij-antenia-projects-plugin/main/updatePlugins.xml`.
4. Return to the **Marketplace** tab and search for **Antenia Projects**.
5. Install the plugin and restart IntelliJ IDEA when prompted.

IntelliJ IDEA will use the same repository to discover future plugin updates automatically. Its plugin manager will also offer to install or enable the required **Subversion** and **Jakarta EE: Server Pages (JSP)** plugins from JetBrains Marketplace.

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
| 25 and later | 10.1 |

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
- Adds organization commit templates and live, non-blocking commit-message validation to every VCS commit prompt.

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

Configure a shared database username and password under **Settings > Tools > Antenia > Credentials**. Secrets are stored through IntelliJ Password Safe and are synchronized to supported projects unless **Override global credentials** is enabled in the project's database form. Disabling an override keeps its project credentials in Password Safe so they are restored if the override is enabled again.

The database host, port, and database name are always project-specific. The port defaults to `3306`.

### Commit Templates

Use **Commit Template** in the commit toolbar to prefill an Evolution, Bug, Transversal Bug, Structure, Code Review, or Merge message. The adjacent status shows the complete message character count. A message is valid when it contains at least 25 characters in total and its first line matches an Antenia format; the status never prevents a commit.

Create blank custom templates or clone an existing template under **Settings > Tools > Antenia > Commit Templates**. Custom templates can be edited, dragged to reorder, moved with the toolbar, or removed; the built-in Antenia templates are read-only and cannot be removed. Commit templates are available in every project, including projects that are not recognized as Neo projects.

### Run Configuration

At application startup, the plugin discovers JDK installations under `%PROGRAMFILES%\Java`, the system and user-local Eclipse Adoptium directories, and through `mise`, then adds missing installations to IntelliJ before assigning a project SDK. Mise-managed JDKs use names such as `temurin-25 (mise)`.

The plugin also discovers Tomcat installations directly below `C:\tools` and through `mise`, then adds missing installations to IntelliJ's global application servers. Mise-managed servers use names such as `Tomcat 10.1.57 (mise)`. The plugin creates a `webapp` local Tomcat configuration after the Maven project and exploded WAR artifact are available, chooses the highest compatible Tomcat version, deploys the project artifact at the expected context, and opens the HTTPS application URL after launch.

For Neo Core projects with a `novanet-react` package, the plugin also creates:

- `react`, which runs the package's `dev` npm script;
- `webapp + react`, which starts the frontend and Tomcat together;
- `npm install` and `npm run build` before-launch steps for the web application.

The compound configuration opens the application through `https://localhost:8888/novanet/`.

## Installation

This plugin requires IntelliJ IDEA Ultimate 2026.2.1 or later because it uses the Tomcat, Java EE, JavaScript, Maven, and Database integrations. It also requires the independently distributed **Subversion** and **Jakarta EE: Server Pages (JSP)** plugins; IntelliJ's plugin manager installs or enables compatible versions during installation.

To install the plugin and receive updates from this repository:

1. Open **Settings > Plugins** in IntelliJ IDEA Ultimate.
2. Open the gear menu, choose **Manage Plugin Repositories**, and add `https://raw.githubusercontent.com/antenia-lhubert/intellij-antenia-projects-plugin/main/updatePlugins.xml`.
3. Find and install **Antenia Projects** from the **Marketplace** tab.
4. Restart the IDE and open a supported Neo project at its Maven root.

The repository feed points directly to a manually uploaded GitHub release ZIP. It does not trigger or perform a build.

To install a locally built distribution instead, choose **Install Plugin from Disk** in the Plugins gear menu and select the ZIP in `build/distributions/`.

Compatible JDK installations must be configured in the IDE for automatic selection. Tomcat installations under `C:\tools` or managed by `mise` are configured automatically.

## Development

The project uses Kotlin, Gradle, and the IntelliJ Platform Gradle Plugin. The current development platform is IntelliJ IDEA Ultimate 2026.2.1.

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

### Changelog

Add user-visible changes to the appropriate `Added`, `Changed`, or `Fixed` group under `Unreleased` in `CHANGELOG.md`. Keep entries concise and written as imperative statements. Do not edit a published version's notes.

When preparing a version:

1. Set `version` in `gradle.properties` to the exact version being published, including the `-SNAPSHOT` suffix used by this repository.
2. Run `./gradlew --no-daemon patchChangelog` or `gradlew.bat --no-daemon patchChangelog` on Windows. This promotes `Unreleased` to a dated section for the Gradle project version and leaves a new empty `Unreleased` section.
3. Keep the generated release heading in the repository's `## [<version>] - YYYY-MM-DD` format.
4. Update the comparison links at the end of `CHANGELOG.md`: point `Unreleased` from the new tag to `HEAD`, and add the new version comparison from the preceding tag.

The IntelliJ Platform Gradle Plugin integrates automatically with the Gradle Changelog Plugin. `patchPluginXml`, which runs as part of `buildPlugin`, writes the current version and its changelog entry into the generated `plugin.xml` as `<version>` and `<change-notes>`. If `CHANGELOG.md` has no section matching the project version, it uses `Unreleased`. Do not add these generated elements manually to `src/main/resources/META-INF/plugin.xml`.

To inspect the patched metadata without building the ZIP, run `./gradlew --no-daemon patchPluginXml` or `gradlew.bat --no-daemon patchPluginXml` and review `build/tmp/patchPluginXml/plugin.xml`. Content under `build/` is generated and must not be committed.

### Manual Releases

Releases are built and published manually; this repository has no automated release build.

1. Set and patch the version and changelog as described above.
2. Update `updatePlugins.xml` so its `version`, release tag, artifact filename, and `idea-version` match the generated plugin metadata.
3. Run `./gradlew --no-daemon clean buildPlugin` or `gradlew.bat --no-daemon clean buildPlugin` on Windows.
4. Verify `build/tmp/patchPluginXml/plugin.xml` contains the expected version, change notes, dependencies, and IDE compatibility.
5. Commit and push the release metadata to `main`.
6. Create the GitHub release tagged `v<version>` from that commit and upload `build/distributions/antenia-projects-<version>.zip` without renaming it.

## Specification

The requirements in [`spec/`](spec/) are the source of truth for project behavior. See [`AGENT.md`](AGENT.md) for repository architecture and contribution guidance.
