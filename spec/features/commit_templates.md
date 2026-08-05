# Commit templates

This is global and is not dependant of the Neo project detection.

Our organization have very specific commit format.

Here are the possible formats:

evo:
```
[EVO] - Mantis : MANTIS_NO : TITRE

> MESSAGE
```

bug:
```
[BUG] - Mantis : MANTIS_NO : TITRE

> MESSAGE
```

bug transversal
```
[BUG_TRANSVERSAL] - Mantis : MANTIS_NO : TITRE

> MESSAGE
```

struct
```
[STRUCT] - Mantis : MANTIS_NO : TITRE

> MESSAGE
```

code review
```
[CODE_REVIEW] - TITRE

> MESSAGE
```

merge
```
[MERGE] rREVISION | AUTHOR | DATE

```
(e.g. `[MERGE] r222180 | lhubert | 2026-08-05 15:13:00 CEST`)

The most important part is the first line.
The complete commit message needs to be >= 25 characters.

- `MANTIS_NO` should be replaced by a positive integer that is the ticket reference on MantisBT
- `TITRE` should be replaced by the ticked title
- `MESSAGE` should be replaced by the message (the `> ` is optional but preferred)
- `REVISION` should be replaced by the revision number
- `AUTHOR` should be replaced by the login of the author
- `DATE` should be replaced by the date

When committing (either git or subversion), the commit prompt should have an option to select one of those as a template.

When a template is selected, the commit message is prefilled.

A non-blocking indicator should indicate whether the commit message is valid or not, alongside a char counter.

Users should be able to create new templates in the settings.
