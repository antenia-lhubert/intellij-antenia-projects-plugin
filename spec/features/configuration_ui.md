# Configuration UI

There should be a configuration UI that allows the user to easily configure project properties.

Some properties should have a special UI/form (credentials, database, environment...), while others should be configurable in a key value table.

User should be able to freely add/remove keys.

User should ba able to add comments and empty lines.

Key order should be preserved and user configurable.

User should be able to reset configuration

Adding a key should have known key autocompletion.

Configuration UI should sync with the config file (both ways) be live applied.

Configuration UI should be a tool window.
Its embedded toolbar controls should remain visible when the tool window's `Show Toolbar` option is disabled.
Its footer should always contain only the detected project type and the configuration file path.
The configuration file path should be a hyperlink that opens the file in the editor.
Errors should be displayed as notifications rather than replacing the footer contents.

User should be able to configure global database credentials that are safely stored and usable across projects.
(global configuration should be done in a separate UI in `Settings > Tools > Antenia > Database > Credentials`)
Global database credentials should contain only a username and password; connection details remain project-specific.
Changes to global credentials should immediately update every open project that does not override them, and synchronize other projects when they are opened later.

Users should be able to manage reusable database connection profiles in `Settings > Tools > Antenia > Database > Database Profiles`.
A profile contains a unique name, host, port, optional database, optional advanced `databaseEdi`, and the global-credential override section.
The override decision is stored with the profile. Profile override credentials are stored in IntelliJ Password Safe and never in plugin state XML.
Profile names are unique case-insensitively. Custom profiles are stored globally and can be created, edited, deleted, and cloned.
No profiles are provided by the plugin. When the tool window first opens, the selector displays a user profile when its connection details match the project, and remains empty when none matches. Profile inference is not repeated while fields are edited or when the configuration is reloaded.
The selector contains a `New profile...` entry. Selecting it asks for a non-empty, case-insensitively unique name, creates and selects the profile with the preferred default host, port 3306, empty database fields, and global credentials. After selecting or matching a profile, connection fields can be edited while that profile remains selected. A Save action explicitly saves edited values back to the profile; project edits never implicitly save the profile.

The following immutable hosts are always provided as suggestions:
- `antenia-dev-mysql5.leaderinfo.com`
- `antenia-dev-mysql8.leaderinfo.com`
- `mysql8-4-5-dev.antenia.com`

Users can enter any host and save a host that is not yet in the suggestion list for reuse across projects.
Hosts can be managed in `Settings > Tools > Antenia > Database > Database Hosts`. Provided hosts are read-only and cannot be deleted; custom hosts can be created, edited, and deleted.
The database-profile settings page and project database form should reuse the same connection, host, and credential controls so validation and host actions behave consistently.
Profile and host quick actions are icon buttons with accessible names and tooltips. Their action strip remains fixed on the right while selectors consume only the available width; long values must not expand the form or introduce horizontal scrolling.
The profile settings host control has the same save-host and manage-host actions as the project form.

Users should be able to configure global Tomcat run configuration values in `Settings > Tools > Antenia > Run Configurations`:
- `OPEN_IN_BROWSER`, defaulting to `true`
- On frame deactivation, defaulting to update resources, with choices for nothing, update resources, and
  update resources and classes. This sets `UPDATE_ON_FRAME_DEACTIVATION` and `UPDATE_CLASSES_ON_FRAME_DEACTIVATION`.
- On update action, defaulting to update resources and classes, with choices for update resources, update resources and classes,
  redeploy, and restart server. This sets `UPDATING_POLICY`.

On frame deactivation and on update action are separate, independent settings.
These settings should be stored globally and applied when the plugin creates a project's `webapp` run configuration.
Existing run configurations should not be modified.

DB Credentials form should have:
- a database profile selector with access to the profile management page
- host (free input so that user can put in an unknown host)
- port (defaults to 3306)
- database
- an optional advanced `databaseEdi` field for project types that support it
- have the option to override global credentials
- disabling the override should retain the project credentials securely so they are restored when the override is enabled again

Selecting a profile copies all its connection details into the project form, including empty database and `databaseEdi` values.
The form infers a user profile from matching connection details only when the tool window is first opened. Directly editing a selected profile keeps it selected so the edited values can be saved.
Selecting or resetting a profile applies its credential override decision and securely stored override credentials. Automatically matching a profile marks it active without overwriting the project's current credential section; differences can then be saved to or reset from the profile.
An active profile's Save and Reset actions compare all connection and credential fields to the profile's latest saved state. Reset restores that saved state.
Applying or editing a profile must not silently change projects that previously used its values; the user explicitly selects it to apply it again.

Env form should have:
- an env select (should be a select to chose amongst known envs + a free input so that user can put in an unknown env)

Keys used in those forms should not show as key value in the table.
Instead, the table should have a special line for each form (e.g. `db credentials` and `environment` lines)
Those lines cannot be created or deleted, but can be moved.
Keys in a group should always be grouped in the config file too.
If they are not, they should be regrouped/moved to the first key of the group.


