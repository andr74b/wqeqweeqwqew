# Desktop application

The desktop edition wraps the existing Spring Boot and Vaadin application in an
Electron window. The game, routes, state model and Vaadin UI are the same code
used by the web edition.

The initial route follows the operating-system language for English, Russian or
Spanish. The in-game language selector continues to work normally.

Each installer contains:

- the production `blackjack.jar`;
- a Java 25 runtime made with `jlink` for the target operating system and CPU;
- Electron's Chromium application window.

At launch, Electron starts Java on `127.0.0.1` with an operating-system-assigned
port, waits for the application to become ready and loads it in its own window.
There is no visible address bar and no external browser. Closing the window
stops Java. A Java-side parent monitor also stops the server if Electron crashes.

## Build after changing the game

Install these tools on the build computer:

- a full JDK 25, preferably Eclipse Temurin, with `JAVA_HOME` set;
- Node.js 22.12 or newer with npm;
- the normal platform build tools used by electron-builder.

Install the desktop dependencies once after cloning, and again whenever
`desktop/package-lock.json` changes:

```sh
cd desktop
npm ci
```

After any Java, Vaadin, CSS, resource or desktop-launcher change, run this from
the `desktop` directory:

```sh
npm run dist
```

That one command runs `mvnw clean verify`, creates a fresh trimmed Java runtime,
copies the new production JAR and builds installers for the current platform.
It will not package a stale JAR if compilation or a test fails.

Artifacts are written to `desktop/dist`:

- macOS: `Blackjack-<version>-mac-<arch>.dmg` and `.zip`;
- Windows: an NSIS `.exe` installer and an `.msi` installer.

Build macOS packages on macOS and Windows packages on Windows. The embedded JVM
is platform- and architecture-specific, so cross-compiling an installer with a
runtime from another platform is intentionally rejected. On Apple Silicon use:

```sh
npm run dist:mac -- --arch arm64
```

On an Intel Mac use `--arch x64`. On Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25"
npm run dist:win -- --arch x64
```

End users install only the resulting application. They do not need Java, Node,
Maven or a browser.

## Fast development launch

To exercise the Electron wrapper without producing an installer, first build
the backend and then launch Electron:

```sh
./mvnw clean verify
cd desktop
npm start
```

This development command uses `JAVA_HOME/bin/java` and
`target/blackjack.jar`. The distributable application always uses its bundled
runtime instead.

## Build every supported installer in GitHub Actions

The **Desktop installers** workflow builds Windows x64, macOS Apple Silicon and
macOS Intel artifacts on their native runners. Run it manually from the Actions
tab or push a tag such as `v1.0.1`.

Before publishing a release, update the `version` in `desktop/package.json` and
run `npm install --package-lock-only` in `desktop` so the lock file has the same
version. The workflow uploads each platform's installers as build artifacts.

## Signing releases

Unsigned development installers are usable for local testing, but macOS
Gatekeeper and Windows SmartScreen will warn users about unsigned downloads.
Public releases should be signed. Never commit signing certificates or secrets.

For macOS, add these repository secrets:

- `MAC_CSC_LINK`: base64-encoded Developer ID Application `.p12`;
- `MAC_CSC_KEY_PASSWORD`: certificate password;
- `APPLE_API_KEY`, `APPLE_API_KEY_ID`, `APPLE_API_ISSUER` and `APPLE_TEAM_ID`:
  App Store Connect notarization credentials.

For Windows, add `WIN_CSC_LINK` with the base64-encoded `.pfx` certificate and
`WIN_CSC_KEY_PASSWORD`. electron-builder detects these variables, signs the app
and installers, and notarizes signed macOS builds. The workflow already passes
the secrets to the appropriate platform job.

## Updates and maintenance

There is no update server configured. To release an update, bump the desktop
version, build signed installers and publish them. Users can install the new
version over the old one. This avoids tying the project to a release provider;
automatic updates can be added later once a permanent download host and signing
identity are chosen.

Keep Electron current because it supplies the embedded browser and its security
fixes. The renderer has no Node.js access, runs with context isolation and the
Chromium sandbox, rejects permission requests, blocks pop-up windows and cannot
navigate outside the private loopback origin.

Backend logs are written to the application's user-data directory:

- macOS: `~/Library/Application Support/blackjack/logs/backend.log`;
- Windows: `%APPDATA%\blackjack\logs\backend.log`.

If startup fails, the application shows this log location in its error dialog.
