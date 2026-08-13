# Coding style

If the project is a Neo project, the plugin should configure the following coding style options:

```
Editor > Code Style > Java:
	Onglet "Imports":
		General:
			- Class count to use import with '*': 999
			- Names count to use static import with '*': 999
		Import Layout:
            Layout static imports separately: enabled
			1. import static all other imports
			2. <blank line>
			3. import java.* // with subpackages
			4. <blank line>
			5. import javax.* // with subpackages
			6. <blank line>
			7. import org.* // with subpackages
			8. <blank line>
			9. import com.* // with subpackages
			10. <blank line>
			11. import all other omports
			12. import module imports
Editor > General:
	On Save:
		Remove trailing spaces on: disabled
```

If the project is a Neo project and no `<project_root>/.editorconfig` is found, also apply the following configs:

```
Editor > File encodings:
    Global Encoding: windows-1252
    Project Encoding: windows-1252
```

When setting the project encoding to windows-1252, first set it to US-ASCII and then to windows-1252 to work around IntelliJ not applying the value directly.

For a Neo project with a root `.editorconfig`, or for any non-Neo project:

```
Editor > File encodings:
    Global Encoding: UTF-8
    Project Encoding: UTF-8
```
