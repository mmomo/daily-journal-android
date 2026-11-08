# Daily Journal for Android

The Android version of Daily Journal: one journaling prompt a day, a quick text entry, a streak counter, and a Home Screen widget.

This project is being built with the AI-DLC workflow (see [`CLAUDE.md`](CLAUDE.md)); planning and design documents live in [`aidlc-docs/`](aidlc-docs/). Done so far: project setup, the **domain** module (all business rules, ported from the iOS app and checked against it) and the **data** module (storage, prompts, reminders, clock). Screens and the widget come next.

## Layout

| Module | Contents |
|---|---|
| `domain/` | Pure Kotlin business rules: prompt selection, entries, streak, search, export, reminders, use cases |
| `data/` | Room database for entries, DataStore for settings and prompt state (same JSON as iOS), the 600-prompt library, reminders and notifications, the clock |
| `app/` | Screens and the Android app (skeleton for now) |
| `widget/` | Home Screen widget (skeleton for now) |

## Prerequisites

- JDK 17 or newer
- Android SDK with platform API 37 (installed with Android Studio). Gradle finds it through `ANDROID_HOME` or a git-ignored `local.properties` with `sdk.dir=...`

No separate Gradle install is needed; use the wrapper.

## Build and test

```sh
./gradlew build                      # compile all modules (warnings are errors) and run all tests
./gradlew :domain:test               # domain tests only: example-based, property-based, performance smoke
./gradlew :data:testDebugUnitTest    # data tests (Robolectric: Room, DataStore, notifications on the JVM)
./gradlew koverHtmlReport            # coverage report in build/reports/kover/html
```

Property-based tests use a new random seed on every run. If one fails, the output shows the smallest failing input and a line like `Repeat this test by using seed 123`; set that seed in the test's `PropTestConfig` to replay it.

## Dependencies and supply chain

- Every version is pinned in `gradle/libs.versions.toml`, and only Google Maven and Maven Central are used.
- `gradle/verification-metadata.xml` holds a SHA-256 checksum for every downloaded file; the build fails if a file doesn't match. After changing a dependency, regenerate it:

  ```sh
  ./gradlew --write-verification-metadata sha256 --refresh-dependencies --no-configuration-cache build koverXmlReport koverHtmlReport
  ```

  and review the changes before committing. `--refresh-dependencies` makes Gradle fetch every file again, so metadata cached by earlier builds (such as parent POMs) is recorded too.
- CI (`.github/workflows/ci.yml`) builds and tests every push and pull request, and submits the dependency graph to GitHub from `main`.
- **One-time setting**: turn on **Dependabot alerts** in the repository's Settings → Code security, so GitHub warns about known vulnerabilities. An SBOM can be exported from Insights → Dependency graph → Export SBOM.
