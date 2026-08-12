# Plugin Dependencies

Antenia Projects requires IntelliJ IDEA 2026.2.1 or later.

The following JetBrains plugins are required runtime dependencies:

| Plugin | Plugin ID |
| --- | --- |
| Subversion | `Subversion` |
| Jakarta EE: Server Pages (JSP) | `com.intellij.jsp` |

These plugins are distributed independently from IntelliJ IDEA as of 2026.2.1. They must be declared as required dependencies in the Antenia Projects plugin descriptor so IntelliJ's plugin manager offers to download, install, or enable compatible versions when Antenia Projects is installed or enabled.

Antenia Projects must require an IDE restart. Repository installation then stages Antenia Projects and its required plugins for the same restart instead of activating the dependencies dynamically.

The plugin must not download plugin archives itself, modify the IDE plugin directory, or bypass the plugin manager's dependency confirmation.

Build dependencies use versions compatible with the targeted IntelliJ IDEA release. Published dependency declarations remain versionless so IntelliJ can resolve the appropriate compatible versions from JetBrains Marketplace.
