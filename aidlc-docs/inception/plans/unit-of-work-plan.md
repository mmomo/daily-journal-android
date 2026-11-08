# Unit of Work Plan: Daily Journal for Android

**Inputs**: approved requirements, stories and application design (`aidlc-docs/inception/application-design/`).

**Starting point**: the application design already defines four Gradle modules (`:domain`, `:data`, `:app`, `:widget`) with inward dependencies. On iOS each module became one unit, built in order (Domain → Data → App → Widget), each fully designed, coded and approved before the next.

**Categories considered**:
- Story grouping, dependencies, technical considerations, business domain and code organization: Questions 1–6
- **Team alignment**: N/A. Single developer, personal project (requirements Q13:A, R2:A); no ownership boundaries to plan around
- **Deployment and scaling per unit**: N/A. All units ship together in one app installed on one phone; there are no separately deployed services

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
How should the work be split into units?

A) Four units, one per module, like iOS: Domain, Data, App, Widget. Project setup (Gradle wrapper, version catalog, module skeletons) and the GitHub Actions workflow are done as the first steps of the Domain unit

B) Five units: a separate first "Project Setup" unit (Gradle, version catalog, empty modules, CI workflow, dependency scanning, SBOM), then Domain, Data, App, Widget

C) Three units: Domain, Data, and one "App and Widget" unit (both are presentation and share the design system)

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
In what order should the units be built? Each unit is fully designed, coded, tested and approved before the next starts.

A) Domain → Data → App → Widget (same as iOS; the widget comes last because it reuses the app's design system and container)

B) Domain → Data → Widget → App (widget earlier because it has no navigation or screens)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3
How should the code be laid out in the repository?

A) Standard Gradle multi-module layout: one folder per module at the root (`domain/`, `data/`, `app/`, `widget/`), each with `src/main/kotlin` and `src/test/kotlin`; build files at the root (`settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, Gradle wrapper); CI in `.github/workflows/`

B) Modules grouped under a `modules/` folder (`modules/domain/`, …), with the same internal layout

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4
Which application id and Kotlin package name should be used? The application id identifies the app on the phone and can't change later without losing installed data.

A) `com.mmomo.dailyjournal`, the same as the iOS bundle id, with packages `com.mmomo.dailyjournal.domain`, `.data`, `.app`, `.widget`

B) A different id (please write it after the [Answer]: tag)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5
How should the 600 prompts be shared with the iOS app?

A) Copy the iOS `prompts.json` into the Data module's assets. Future prompt changes are made in both repositories, and a test checks the Android copy has 600 valid prompts with the same ids

B) Keep one shared copy (for example a Git submodule or a separate repository) used by both apps

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6
Which units need a Functional Design stage before their code? The execution plan said "units with business logic or data transformations", which the PBT extension also requires (PBT-01).

A) Domain and Data (business rules; storage model, migrations and file formats). App and Widget skip it: their rules come from Domain, and their behavior is set by the stories and application design

B) All units, including App and Widget (screen states and navigation rules designed in detail first)

C) Domain only

D) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Generation Checklist

These steps run after the questions are answered, any follow-ups are resolved and the plan is approved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/inception/application-design/unit-of-work.md`: each unit's purpose, responsibilities, components (from `components.md`), exit criteria and which construction stages run for it (Question 6)
- [x] 2. Document the code organization strategy in `unit-of-work.md` (Questions 3–5): directory tree, application id, packages, where prompts and build files live (greenfield requirement)
- [x] 3. Write `aidlc-docs/inception/application-design/unit-of-work-dependency.md`: dependency matrix, build order (Question 2), interfaces each unit provides to the next, and what can be tested at the end of each unit
- [x] 4. Write `aidlc-docs/inception/application-design/unit-of-work-story-map.md`: every story (26) assigned to the unit(s) that implement it, plus requirement coverage for technical-only NFRs (CI, supply chain, logging)
- [x] 5. Validate unit boundaries and dependencies (no cycles; dependencies match `component-dependency.md`; each unit can be built and tested before the next starts)
- [x] 6. Check that every story and every requirement is assigned to a unit
