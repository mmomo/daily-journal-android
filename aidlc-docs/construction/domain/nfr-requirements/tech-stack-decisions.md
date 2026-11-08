# Tech Stack Decisions: Unit 1 Domain (and project setup)

## Version Policy (Q1, clarification C2: "use latest")

- Use the **latest stable** version of every tool and library at the start of the Domain unit's Code Generation, checked against the official release pages at that time, adjusted only where needed for compatibility (Kotlin ↔ Compose compiler plugin, Android Gradle Plugin ↔ Gradle, Kotest ↔ Kotlin).
- Every version pinned exactly in `gradle/libs.versions.toml` (no `+`, ranges or `latest`).
- **Java 17** as the compile target (`jvmToolchain(17)`), the current Android standard. Your installed Java 21 can run the build; CI uses Java 17.
- No pre-releases (alpha, beta, RC, EAP).
- The exact versions chosen are recorded in the Code Generation summary and in the version catalog.

## Stack

| Area | Choice | Scope | Rationale |
|---|---|---|---|
| Language | Kotlin (latest stable 2.x) | All modules | Requirements NFR-1.2 |
| Build | Gradle (latest stable) with Kotlin DSL, wrapper with distribution checksum | Project | NFR-1.2, DOM-SEC-6 |
| Version catalog | `gradle/libs.versions.toml` | Project | Single place for pinned versions (DOM-SEC-4) |
| Android build | Android Gradle Plugin (latest stable compatible with the chosen Gradle) | `:data`, `:app`, `:widget` skeletons | Needed for project setup in this unit |
| Android SDK levels | `minSdk` 29 (NFR-1.1); `compileSdk` and `targetSdk` = latest stable API level | Android modules | Latest target behavior and security fixes |
| Domain module type | Kotlin JVM library (no Android plugin) | `:domain` | Keeps the domain free of Android (application design Q3) |
| Coroutines | kotlinx-coroutines-core | `:domain` | `suspend` use cases and `Flow` change signals (application design Q5) |
| Date and time | kotlinx-datetime | `:domain` | `Instant`, `TimeZone`, calendar arithmetic with the time-zone database |
| Unicode normalization | `java.text.Normalizer` (JDK) | `:domain` | Accent-insensitive search (BR-Q2) without a library; available on JVM and Android |
| Test framework | **Kotest** (runner on JUnit Platform, assertions, property module) | Tests only | Q2:A; PBT-09: custom generators (`Arb`), shrinking, seeds |
| Coroutine testing | kotlinx-coroutines-test | Tests only | Deterministic tests of suspend functions |
| Coverage | **Kover** (JetBrains), report only | Project | Q8:B |
| Static checks | Kotlin compiler with warnings as errors | All modules | Q6:A |
| CI | GitHub Actions, workflow `.github/workflows/ci.yml` | Project | R3:A |
| Vulnerability scanning | Gradle dependency submission action → GitHub dependency graph → Dependabot alerts and dependency review | CI and repository | Q3:A |
| SBOM | Exported on demand from GitHub's dependency graph | Repository | C1:B (NFR-6.5 amended) |
| Download integrity | Gradle dependency verification (`gradle/verification-metadata.xml`, SHA-256) | Project | Q5:A |

All shipped libraries are official Kotlin or Google/AndroidX libraries (application design Q8:A). Kotest and Kover are test and build tools only; they don't ship in the app.

## PBT Framework (PBT-09)

| Need | Kotest property testing |
|---|---|
| Custom generators for domain types | `Arb` builders for days, zones, instants, category sets, texts, entry-day sets, queries (business logic model §10.2) |
| Shrinking | Built in, left enabled |
| Seed reproducibility | Random seed per run; printed on failure; replay by setting the seed (PBT-08) |
| Test runner integration | Runs on the JUnit Platform with Gradle and in CI, alongside example-based Kotest specs |

## CI Design (`.github/workflows/ci.yml`)

| Aspect | Decision |
|---|---|
| Triggers | Every push and every pull request (Q9:B) |
| Runner | Fixed Ubuntu image version (not `ubuntu-latest`); it includes the Android SDK |
| Actions | Official actions (checkout, setup-java, Gradle setup, dependency submission, upload-artifact), each pinned to a full commit SHA |
| JDK | Temurin 17 |
| Steps | Validate the Gradle wrapper → build all modules → run all tests (example, property and performance smoke tests) → Kover report → upload test and coverage reports → submit dependencies to GitHub |
| Permissions | `contents: read` by default; `contents: write` only on the dependency submission job |
| Secrets | None needed |

## One-time Repository Setting

Turn on **Dependabot alerts** (and optionally Dependabot security updates) in GitHub → repository Settings → Code security. Included in the build instructions.

## Compliance

| Extension | Result |
|---|---|
| Security Baseline | SECURITY-10 compliant (pinned versions, verification, scanning, SBOM export, pinned CI tools, no unused dependencies); SECURITY-13 compliant (checksums, wrapper validation, CI permissions); SECURITY-03 and SECURITY-15 compliant for the domain (no logging, no journal text in errors); server-only rules N/A |
| Property-Based Testing | PBT-09 compliant (Kotest property testing selected and documented); PBT-08 compliant at design level (random seed printed on failure, CI keeps reports, shrinking on) |
