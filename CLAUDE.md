# CLAUDE.md

## Project Overview

**proxyagent** is a Java agent (JAR) that provides proxy authentication support for Java applications. It works around the JVM's limitation of ignoring `http.proxyUser` and `http.proxyPassword` system properties by installing a custom `Authenticator` at JVM startup via the Java Instrumentation API.

Usage: `java -javaagent:proxyagent.jar -Dhttp.proxyHost=... -Dhttp.proxyPort=... -Dhttp.proxyUser=... -Dhttp.proxyPassword=... -jar app.jar`

## Repository Structure

```
proxyagent/
  src/main/java/net/anzix/proxyagent/
    Agent.java          # Sole source file — Java agent entry point
  build.gradle          # Gradle build config (legacy, not used by CI)
  .github/workflows/
    release.yml         # GitHub Actions: build JAR and publish release on tag push
  .travis.yml           # Legacy Travis CI config (unused)
  README.md
```

This is a single-file, zero-dependency Java project. It uses only `java.lang.instrument`, `java.net`, and `java.security` from the standard library.

## Build & Release

### Building locally

The CI workflow compiles directly with `javac` (no Gradle). To build locally:

```sh
mkdir -p build/classes build/tmp
printf 'Manifest-Version: 1.0\nPremain-Class: net.anzix.proxyagent.Agent\n\n' > build/tmp/MANIFEST.MF
javac -d build/classes src/main/java/net/anzix/proxyagent/Agent.java
jar cfm proxyagent.jar build/tmp/MANIFEST.MF -C build/classes .
```

Requires Java 8+.

### Releasing

Push a tag matching `v*` (e.g., `v1.0.0`) to trigger the GitHub Actions release workflow, which builds the JAR and attaches it to a GitHub Release via `softprops/action-gh-release@v2`.

## Code Conventions

- **Language:** Java 8 (no newer language features)
- **Package:** `net.anzix.proxyagent`
- **Style:** 4-space indentation, camelCase methods/variables, JavaDoc on public API
- **No external dependencies** — only Java standard library
- **No tests** — the project has no test suite
- **JAR manifest must declare** `Premain-Class: net.anzix.proxyagent.Agent`

## Key Technical Details

- `Agent.premain()` is the instrumentation entry point invoked by `-javaagent`
- Disables SSL tunneling auth restrictions: `Security.setProperty("jdk.http.auth.tunneling.disabledSchemes", "")`
- Property lookup uses protocol prefix with fallback: e.g., `http.proxyHost` falls back to `proxyHost`
- The custom `Authenticator` only responds to `RequestorType.PROXY` requests and validates the requesting host/port against configured properties before returning credentials
