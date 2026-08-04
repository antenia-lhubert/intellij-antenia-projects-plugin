# Antenia Projects

An IntelliJ IDEA Ultimate plugin for Antenia Neo Maven web applications.

## Features

- Detects Neo Core, GED, and Selfcare projects from their Maven artifact IDs.
- Selects the matching Java SDK and configures a 4096 MB compiler heap.
- Creates the exploded WAR artifact and a compatible local Tomcat configuration.
- Creates React npm and compound run configurations when `novanet-react` is present.
- Injects the project configuration root as `NOVANET_DIR`, `GED_DIR`, or `OWLNET_DIR` at run time.
- Provides a live ordered-properties editor with database/environment forms, comments, blank lines, reordering, completion, and reset.
- Stores reusable global database secrets in IntelliJ Password Safe under `Settings > Tools > Antenia Projects`.

Project configuration is stored under `<project>/.local`.

## Development

```shell
./gradlew --no-daemon check
./gradlew --no-daemon runIde
```

Always pass `--no-daemon` to Gradle commands in this repository. `gradle.properties` also disables the daemon as a safeguard.

The implementation requirements are maintained in [`spec/`](spec/).
