# Changelog

All notable changes to Antenia Projects are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Version names retain the snapshot identifiers used by the repository.

## [Unreleased]

## [1.0.5-SNAPSHOT] - 2026-08-13

### Added

- Add an action to comment or uncomment the selected key-value property in the Neo Configuration editor.
- Add drag-and-drop row reordering for configuration lines and custom commit templates.

### Changed

- Align configuration tables, forms, toolbars, validation, settings navigation, commit-template editing, accessibility, and plugin icons with IntelliJ UI guidelines.

### Fixed

- Avoid Tomcat registration failures when a generated application-server name conflicts with an existing application-server library name.

## [1.0.4-SNAPSHOT] - 2026-08-12

### Added

- Declare Subversion and Jakarta EE: Server Pages (JSP) as required plugins so IntelliJ can offer to install or enable them automatically.

### Changed

- Require IntelliJ IDEA Ultimate 2026.2.1 or later.
- Require an IDE restart when installing or updating the plugin and its dependencies.

### Fixed

- Wait for Tomcat discovery to finish before configuring a Neo project, preventing startup races where compatible servers were incorrectly reported as missing.

## [1.0.3-SNAPSHOT] - 2026-08-06

### Added

- Add a custom plugin repository descriptor for installation and automatic updates.
- Add configurable defaults for generated Tomcat run configurations, including browser launch, frame-deactivation behavior, and update actions.
- Add configuration search with keyboard navigation and match counts.
- Add header actions to open Antenia settings, reapply automatic setup, and reset all Antenia-managed project files and run configurations.
- Add a central Antenia settings page linking credentials, commit templates, and run-configuration defaults.

### Changed

- Use Tomcat 10.1 rather than Tomcat 11 for projects targeting Java 25 or later.
- Rename the global database settings page from Projects to Credentials.
- Apply user-selected Tomcat defaults when creating new `webapp` run configurations.

### Fixed

- Keep the Neo Configuration tool window synchronized with property-file edits made through IntelliJ editor documents.

## [1.0.2-SNAPSHOT] - 2026-08-05

### Added

- Discover and register Tomcat installations under `C:\tools` and managed by `mise`.
- Discover and register JDK installations under Program Files, Eclipse Adoptium directories, and `mise`.
- Prefer the newest matching JDK, with Temurin or Adoptium installations preferred when available.
- Add built-in Antenia commit-message templates for evolutions, bugs, transversal bugs, structural changes, code reviews, and merges.
- Add editable custom commit templates and live, non-blocking commit-message validation.
- Group Antenia settings under `Settings > Tools > Antenia`.

### Changed

- Give `mise`-managed JDK and Tomcat installations distinct names.
- Organize global database configuration as the Projects page beneath the Antenia settings group.

## [1.0.1-SNAPSHOT] - 2026-08-05

### Added

- Detect Neo Core, Neo GED, and Neo Selfcare projects from their root Maven artifact IDs.
- Generate project-specific configuration and logging files under `.local` from bundled templates.
- Add the Neo Configuration tool window with ordered property editing, comments, blank lines, key completion, reordering, and reset support.
- Add dedicated database and environment forms with live file synchronization.
- Store global and project-specific database credentials in IntelliJ Password Safe.
- Synchronize supported project credentials and IntelliJ MySQL data-source profiles.
- Select a compatible project SDK and configure the compiler heap.
- Create exploded WAR artifacts and local `webapp` Tomcat run configurations.
- Create npm and compound run configurations for Neo Core projects with a React frontend.
- Inject the project configuration directory and local RMI hostname into supported run configurations.
- Add user-visible notifications for automatic configuration failures.

### Changed

- Preserve existing user run configurations instead of replacing them.
- Preserve project-specific database credentials when switching back to global credentials.
- Select the database credentials row when opening the configuration editor.

## [1.0.0-SNAPSHOT] - 2026-08-04

### Added

- Create the initial IntelliJ Platform plugin scaffold.
- Add the generated demonstration tool window and build, test, and verification configurations.

[Unreleased]: https://github.com/antenia-lhubert/intellij-antenia-projects-plugin/compare/v1.0.5-SNAPSHOT...HEAD
[1.0.5-SNAPSHOT]: https://github.com/antenia-lhubert/intellij-antenia-projects-plugin/compare/v1.0.4-SNAPSHOT...v1.0.5-SNAPSHOT
[1.0.4-SNAPSHOT]: https://github.com/antenia-lhubert/intellij-antenia-projects-plugin/compare/v1.0.3-SNAPSHOT...v1.0.4-SNAPSHOT
[1.0.3-SNAPSHOT]: https://github.com/antenia-lhubert/intellij-antenia-projects-plugin/compare/v1.0.2-SNAPSHOT...v1.0.3-SNAPSHOT
[1.0.2-SNAPSHOT]: https://github.com/antenia-lhubert/intellij-antenia-projects-plugin/compare/e80563d8ae62404caaa61671216f4b9ac731bcfa...v1.0.2-SNAPSHOT
[1.0.1-SNAPSHOT]: https://github.com/antenia-lhubert/intellij-antenia-projects-plugin/compare/be16651209e9ee84ebf562703a6f3249756b087f...e80563d8ae62404caaa61671216f4b9ac731bcfa
[1.0.0-SNAPSHOT]: https://github.com/antenia-lhubert/intellij-antenia-projects-plugin/tree/be16651209e9ee84ebf562703a6f3249756b087f
