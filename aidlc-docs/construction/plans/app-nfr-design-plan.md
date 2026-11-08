# NFR Design Plan: App Unit

**Inputs**: `construction/app/nfr-requirements/` (approved), application design (components and services S1–S12), Data unit factory and environment.

**Settled by earlier decisions** (not asked): ViewModels built through `viewModelFactory` from the App container (manual injection); autosave after a short pause like iOS (about 0.8 s), with the edit time sent along so the 5-second midnight grace applies; the editor flushes pending text when the midnight signal arrives; receivers do their work with `goAsync()` and a time limit; StrictMode in debug builds; R8 rules strip `Log.v/d/i` in release; generic, user-facing error wording owned by the App.

**Categories considered**:
- **Performance and resilience patterns**: Question 1 (where and when the environment is created)
- **Logical components**: Question 2 (where screens get the environment)
- **Security patterns**: already set in NFR Requirements (APP-SEC-1 to 8); applied in the design document
- **Scalability patterns**: N/A (one user, on-device)

---

## Questions

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

### Question 1
When and on which thread should the app create its environment (open storage, load and validate the 600 prompts)? It must also exist when the app is started only for a reminder, a phone restart or the widget, with no screen.

A) Start creating it on a background thread as soon as the app process starts; anything that needs it (screens, receivers, widget) waits for it to finish. Nothing heavy ever runs on the main thread; screens show the loading frame for the few milliseconds it takes

B) Create it synchronously on the main thread when the app process starts (simplest; roughly 20–50 ms of main-thread work at each cold start)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2
How should the startup state reach the screens?

A) One app-level state with three cases (loading, ready with the environment, failed with a "Try again" action) that `MainActivity` observes; navigation starts only when ready, and onboarding or Today is chosen from the settings

B) Each ViewModel waits for the environment itself and shows its own loading and error states

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Checklist

These steps run after the questions are answered and any follow-ups are resolved. Each box is ticked as soon as its step is done.

- [x] 1. Write `aidlc-docs/construction/app/nfr-design/nfr-design-patterns.md`: startup and environment creation, refresh signals in ViewModels, autosave and midnight flush, saved state, Back handling, permission flow, deep-link routing, receivers, release hardening and logging, error wording, test patterns
- [x] 2. Write `aidlc-docs/construction/app/nfr-design/logical-components.md`: Application, container, startup state, activity, navigation, ViewModels, receivers, theme
- [x] 3. Security and PBT compliance review
- [x] 4. Check consistency with the NFR requirements, stories and application design
