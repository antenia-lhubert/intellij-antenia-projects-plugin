# Env var injection

On run configuration start, the project configuration root folder path environment variables should be injected (c.f. projects.md)

`JAVA_TOOL_OPTIONS=-Djava.rmi.server.hostname=127.0.0.1` should also be injected.