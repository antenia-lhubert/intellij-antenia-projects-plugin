# Database profile

Reusable database connection profiles are defined in [configuration_ui.md](configuration_ui.md). They are connection presets and are distinct from IntelliJ data sources.

When a database (host & db) is configured, IntelliJ should have a corresponding data-source profile with a mysql driver.

The data-source profile should have the name of the host, and the database as its connection.

When the configuration changes, it should not delete the data-source profile.

If a data-source profile already exists for the host, complete the profile by adding the db in it.
