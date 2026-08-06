# Agent Guide

## Project Purpose

This repository contains **Antenia Projects**, an IntelliJ IDEA Ultimate plugin that configures and runs Antenia Neo Maven/Tomcat projects. The plugin is written in Kotlin and targets IntelliJ IDEA Ultimate 2025.3.5 through the IntelliJ Platform Gradle Plugin.

Read [`spec/`](spec/) before changing behavior. The specification is the only source of truth; implementation and documentation must follow it. If code and spec disagree, implement the spec rather than preserving accidental behavior. Do not change the spec merely to match the current code unless the product requirement itself has intentionally changed.

## Core Invariants

- Use official IntelliJ Platform APIs; do not manipulate `.idea` XML directly or add API workarounds when a supported extension point exists.
- Enable Neo-specific behavior only when the root `pom.xml` identifies a supported project. Global settings are the sole exception.
- Store generated project configuration below `<project>/.local/`.
- Preserve user-authored property ordering, comments, blank lines, escaping, and unknown keys.
- Keep configuration file and tool-window edits synchronized in both directions and apply changes live.
- Keep global secrets in IntelliJ Password Safe. Never place credentials in logs, source code, tests, plugin state XML, or documentation.
- Do not remove existing database profiles when project database settings change. Extend a host profile with additional databases.
- Do not modify unrelated run configurations. Automation owns only the spec-defined `webapp`, `react`, and compound configurations.
- Existing user run settings are preserved after initial creation except where the spec requires the exploded artifact reference to remain valid.
- Register listeners and asynchronous work with the project or UI disposable, and perform IntelliJ model mutations with the required write action and thread.

## Supported Project Matrix

| Type | Root artifact ID | Local directory | Properties file | Environment variable | Context | Ports (HTTP/HTTPS/JMX) |
| --- | --- | --- | --- | --- | --- | --- |
| Core | `webapp-novanet` | `novanet` | `novanet.properties` | `NOVANET_DIR` | `/novanet` | `8080` / `8443` / `1999` |
| GED | `webapp-ged` | `ged` | `configuration.properties` | `GED_DIR` | `/ged` | `8081` / `8444` / `2000` |
| Selfcare | `webapp-owlnet` | `owlnet` | `owlnet.properties` | `OWLNET_DIR` | `/owlnet` | `8082` / `8445` / `2001` |

Java/Tomcat mapping is Java 8/Tomcat 9 and Java 17+/Tomcat 10.1. Core may contain a `novanet-react` subproject. Add future project types and frontend variants to the spec-driven model rather than scattering additional type checks.

## Repository Map

- `spec/`: authoritative product requirements, supported projects, templates, and feature behavior.
- `src/main/kotlin/fr/antenia/project/`: project detection and per-project schemas.
- `src/main/kotlin/fr/antenia/config/`: ordered properties codec, file/template operations, and project state.
- `src/main/kotlin/fr/antenia/ui/`: Neo Configuration tool window and live editor.
- `src/main/kotlin/fr/antenia/credentials/`: global settings UI, Password Safe access, and project synchronization.
- `src/main/kotlin/fr/antenia/database/`: MySQL URL parsing and IntelliJ data-source synchronization.
- `src/main/kotlin/fr/antenia/run/`: run-time environment injection.
- `src/main/kotlin/fr/antenia/automation/`: startup/Maven-import orchestration, SDK selection, artifacts, and run configurations.
- `src/main/resources/templates/`: initial project properties and logging configurations.
- `src/main/resources/META-INF/plugin.xml`: plugin metadata, dependencies, and extension registrations.
- `src/test/kotlin/`: JUnit 4 unit tests arranged by production package.
- `.run/`: shared Gradle run configurations for the sandbox IDE, checks, and plugin verification.

Do not edit generated content in `build/`, `.gradle/`, or `.intellijPlatform/`.

## Architecture Notes

`NeoProjectActivity` is the project startup coordinator. It detects the project, ensures local files exist, synchronizes credentials and database profiles, configures the SDK/compiler, and delegates artifact and run-configuration work. Keep this class orchestration-focused; put parsing, persistence, UI, database, and run-model behavior in their existing components.

`NeoProjectDetector` reads only the root Maven project. Detection must remain safe for untrusted XML: external entities and schemas stay disabled. Java properties are evaluated in the priority order defined by `spec/projects.md`.

`OrderedProperties` is a round-trip document model, not a `java.util.Properties` replacement. Changes must retain layout and escaping semantics. Special database/environment rows are projections over grouped keys and must not duplicate those keys in the generic table.

`ConfigurationFiles` owns template creation and disk I/O. Templates must stay aligned with `spec/projects.md`; when a template changes, verify both initial creation and reset behavior.

Global credentials are application-scoped and project overrides are project-scoped. Password Safe remains the credential authority. Host, port, database, and the override decision remain project-specific.

`DatabaseProfileSynchronizer` uses the IntelliJ Database API and the bundled MySQL driver. Data-source updates must be additive with respect to previously introspected databases.

`NeoRunConfigurationManager` operates on IntelliJ artifacts and run-manager models. Artifact creation must complete before a configuration references it. Existing named configurations should not be broadly rewritten after creation, and configurations outside the owned names must never be touched.

`NeoEnvironmentExtension` injects the project-specific `.local/` root and the RMI hostname option. Preserve existing `JAVA_TOOL_OPTIONS` and avoid inserting duplicate JVM options.

## Development Workflow

Use the checked-in Gradle wrapper and always pass `--no-daemon`.

```shell
# Linux/macOS
./gradlew --no-daemon check
./gradlew --no-daemon runIde
./gradlew --no-daemon verifyPlugin
./gradlew --no-daemon buildPlugin

# Windows
gradlew.bat --no-daemon check
gradlew.bat --no-daemon runIde
gradlew.bat --no-daemon verifyPlugin
gradlew.bat --no-daemon buildPlugin
```

Use the smallest relevant verification while iterating, then run `check` before finishing. Run `verifyPlugin` when changing plugin metadata, extension registration, IntelliJ API usage, or platform dependencies. Use `runIde` for UI, lifecycle, Password Safe, database, artifact, and run-configuration changes that unit tests cannot exercise adequately.

## Testing Guidelines

- Use JUnit 4, matching the existing tests and `libs.junit` dependency.
- Place tests in the production package's corresponding path under `src/test/kotlin`.
- Add focused regression tests for detection, parsers, codecs, URL handling, environment merging, and other logic that can be isolated from the IDE.
- Test round trips for ordered properties; assert comments, blank lines, ordering, escapes, newline style, and trailing newline when relevant.
- Include all supported project types and Java property aliases when changing detection or schema behavior.
- Prefer IntelliJ test fixtures or sandbox verification for platform model changes rather than mocking large IntelliJ API surfaces.
- Never use real credentials, production hosts, or developer-specific absolute paths in fixtures.

Current automated coverage includes project detection, ordered-properties round trips, MySQL connection parsing, and `JAVA_TOOL_OPTIONS` merging. Treat Swing UI, Password Safe propagation, startup/import activity, data-source integration, artifact generation, and Tomcat/React run configurations as areas requiring explicit sandbox verification unless suitable platform tests are added.

## Change Checklist

1. Read the relevant files in `spec/` and identify affected project types.
2. Trace the existing component and its tests before editing.
3. Keep changes modular and avoid type-specific duplication outside the project model/schema.
4. Update templates, plugin metadata, tests, and user documentation when behavior requires it.
5. Run the relevant tests with the Gradle wrapper and `--no-daemon`.
6. Confirm non-Neo projects and unrelated run/database configurations remain untouched.
7. Confirm no credentials, machine-local paths, generated files, or IDE state are included in the change.
