# NFR Requirements Plan: Domain Unit

**Unit**: Domain (`:domain`, pure Kotlin) **plus project setup and CI** (`unit-of-work.md`). So this stage decides both the domain's NFRs and the project-wide build, test and supply-chain tooling. Storage, reminders and UI technology are decided in the Data and App units.

**Already decided** (not asked again): Kotlin; Gradle with Kotlin DSL and a version catalog; minimum Android 10 (API 29); Kotest property testing (NFR-4.3); GitHub Actions CI (R3:A); official Kotlin, AndroidX and Google libraries only in shipped code (application design Q8:A); kotlinx-coroutines and kotlinx-datetime in the domain; random PBT seed printed on failure.

**Categories considered**:
- **Scalability**: N/A. A single user on one phone; data grows by one entry a day (about 3,650 in ten years), covered by the performance question
- **Availability**: N/A for the domain. It runs on the phone with no server; data protection is in requirements NFR-2 (accepted risk) and the Data unit
- **Usability**: N/A for the domain (no UI); handled in the App unit
- Performance, security (supply chain), tech stack, reliability and maintainability: Questions 1–9

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
Which versions should the project use?

A) The latest stable versions at the start of Code Generation (Kotlin, Android Gradle Plugin, Gradle, Compose, libraries), each pinned to an exact version in the version catalog, with Java 17 as the compile target (the current Android standard; your Java 21 can build it)

B) Slightly older, widely used versions (one release behind the latest) for more stability

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 2
Which test framework should the example-based tests use? (Property-based tests use Kotest either way.)

A) Kotest for everything: example-based and property-based tests in the same style and runner (on the JUnit Platform)

B) JUnit 5 for example-based tests, plus Kotest only for property-based tests

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
How should dependencies be scanned for known vulnerabilities (SECURITY-10)?

A) GitHub's built-in tools: CI uploads the Gradle dependency list to GitHub, which then raises Dependabot alerts for known vulnerabilities and checks new dependencies on pull requests. No accounts or keys needed; free for public repos

B) OWASP Dependency-Check in CI: fails the build on known vulnerabilities, but needs a free NVD API key stored as a GitHub secret and adds several minutes to each run

C) Both A and B

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4
How should the Software Bill of Materials (SBOM) be produced (SECURITY-10)?

A) The CycloneDX Gradle plugin generates an SBOM file in every CI run, saved as a build artifact

B) Only GitHub's dependency graph, which can export an SPDX SBOM on demand from the repository page

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 5
How strictly should dependency downloads be checked (SECURITY-10, SECURITY-13)?

A) Exact versions in the version catalog plus Gradle dependency verification: every downloaded file's checksum is recorded in `gradle/verification-metadata.xml` and checked on every build, so a tampered file fails the build. Adds a step whenever a dependency changes

B) Exact versions in the version catalog plus Gradle dependency locking (locks transitive versions, no checksums)

C) Exact versions in the version catalog only

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
Should the build include code style and static analysis tools? (Build-time only; nothing ships in the app.)

A) None beyond the Kotlin compiler with warnings treated as errors

B) ktlint (formatting) and detekt (static analysis) run in CI and fail the build on issues

C) Kotlin compiler warnings as errors now, plus Android Lint when the Android units arrive

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7
What speed should the domain guarantee (supports NFR-5.1 and NFR-5.2)?

A) Explicit targets checked by tests: with 3,650 entries, computing the streak under 50 ms and picking a prompt under 10 ms on the CI machine (the same kind of smoke test the iOS project has)

B) No explicit targets for the domain; speed is checked later on a device

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8
Should test coverage be measured?

A) Yes, with Kover (JetBrains' coverage tool): a report in every CI run and a minimum of 90% line coverage for the domain, failing the build below it

B) Yes, report only, without a minimum

C) No coverage measurement

D) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 9
When should CI run?

A) On every push and pull request, plus a weekly scheduled run so newly published vulnerabilities in existing dependencies are caught even when the code doesn't change

B) On every push and pull request only

C) Only on pushes to `main`

D) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/construction/domain/nfr-requirements/nfr-requirements.md`: performance, reliability (error handling from the functional design), security (supply chain, logging, no journal text in errors), maintainability (tests, coverage, style), and N/A categories with reasons
- [x] 2. Write `aidlc-docs/construction/domain/nfr-requirements/tech-stack-decisions.md`: languages, versions policy, build tooling, domain libraries, test frameworks (PBT-09), CI design, scanning, SBOM, verification, with rationale
- [x] 3. Security compliance review (SECURITY-10 in particular) and PBT compliance (PBT-08 seed logging in CI, PBT-09 framework)
- [x] 4. Check consistency with requirements, application design and the domain functional design
