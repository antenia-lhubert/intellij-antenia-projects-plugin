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

DB Credentials form should have:
- host (should be a select to chose amongst known servers + a free input so that user can put in an unknown host)
- port (defaults to 3306)
- database
- have the option to override global credentials

Env form should have:
- an env select (should be a select to chose amongst known envs + a free input so that user can put in an unknown env)

Keys used in those forms should not show as key value in the table.
Instead, the table should have a special line for each form (e.g. `db credentials` and `environment` lines)
Those lines cannot be created or deleted, but can be moved.
Keys in a group should always be grouped in the config file too.
If they are not, they should be regrouped/moved to the first key of the group.


