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

User should be able to configure global database credentials that are safely stored and usable across projects.
(global configuration should be done in a separate UI in `Settings > Tools`)
Global database credentials should contain only a username and password; connection details remain project-specific.
Changes to global credentials should immediately update every open project that does not override them, and synchronize other projects when they are opened later.

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
- host (should be a select to chose amongst known servers + a free input so that user can put in an unknown host)
- port (defaults to 3306)
- database
- have the option to override global credentials
- disabling the override should retain the project credentials securely so they are restored when the override is enabled again

Env form should have:
- an env select (should be a select to chose amongst known envs + a free input so that user can put in an unknown env)

Keys used in those forms should not show as key value in the table.
Instead, the table should have a special line for each form (e.g. `db credentials` and `environment` lines)
Those lines cannot be created or deleted, but can be moved.
Keys in a group should always be grouped in the config file too.
If they are not, they should be regrouped/moved to the first key of the group.


