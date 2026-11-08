# Requirements Clarification Questions

Thanks for your answers. A few of them pull in different directions, and the Resiliency extension you enabled requires some decisions that it does not allow me to make for you. Please answer the questions below the same way: write the letter after each `[Answer]:` tag, or choose the last option (Other) and describe what you want.

## Background: the extensions were written for cloud services

You enabled all three extensions (Security, Resiliency and Property-Based Testing). Property-based testing fits this app well. Many Security and Resiliency rules, however, assume a server running in the cloud: load balancers, central log services, alerting dashboards, multi-zone deployment, auto-scaling. This app has no server. It runs entirely on the phone, and (from Question 7) your entries never leave it.

Every rule that is enforced blocks a stage until it's satisfied, so it matters which rules apply.

---

## Clarification 1: How should the Security and Resiliency extensions apply?

### Question C1
How should the cloud-oriented Security and Resiliency rules be applied to an on-device app?

A) Keep both extensions. Enforce the rules that make sense on a phone (for example encryption of stored data, safe error handling, pinned and scanned dependencies, backups, release process). Mark server-only rules (load balancers, central logging, alerting dashboards, multi-zone, auto-scaling, health endpoints) as N/A with a written reason

B) Keep Security as in A, but turn Resiliency off (it is almost entirely about cloud availability)

C) Turn both Security and Resiliency off, like the iOS project, and keep only Property-Based Testing

D) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Clarification 2: Backups vs. "no cloud backup"

In Question 7 you chose **B: entries stay on the device with no cloud backup**. The Resiliency extension (rule RESILIENCY-12) requires automated backups of stored data. If the phone is lost, broken or reset, the journal is gone unless the user exported it by hand.

### Question C2
How should the journal be protected against losing the phone?

A) Keep it as answered: no automatic backup. The only recovery is the manual Markdown or plain-text export, and this is documented as an accepted risk (only possible if Resiliency is off, or its backup rule is formally waived)

B) Use Android's built-in automatic backup to the user's Google account. It is end-to-end encrypted with the phone's screen lock on Android 9 and later, and restored when setting up a new phone

C) Automatic local backup file: the app saves an encrypted backup on a schedule to a folder the user chooses once (for example a folder on the phone or in a cloud drive app). Nothing is sent anywhere by the app itself

D) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Clarification 3: Sync between devices

In Question 1 you chose **full feature parity** with iOS, and the iOS summary included iCloud sync between devices. In Question 7 you chose no cloud backup or sync.

### Question C3
Should entries sync between Android devices?

A) No sync. Each device has its own journal (matches Question 7)

B) Yes, sync between the user's Android devices (needs a cloud service, which conflicts with Question 7)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Clarification 4: Encrypting the journal on the phone

Security rule SECURITY-01 requires stored data to be encrypted at rest.

### Question C4
How should the journal database be encrypted on the phone?

A) Rely on Android's built-in storage encryption. Every phone that launched with Android 10 or later encrypts app data with hardware-backed keys tied to the screen lock. No extra code or libraries

B) Add a second layer: encrypt the database itself (SQLCipher) with a key stored in the Android Keystore. Safer if the phone is rooted or compromised, but adds a library and some startup cost

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Resiliency decisions (only needed if you keep Resiliency in Question C1)

The Resiliency extension requires you to make these choices; it doesn't let me assume them. If you chose B or C in Question C1, you can skip this section.

### Question R1: How much data loss and downtime is acceptable?
For this app, "recovery" means getting a working journal back after a lost or broken phone, or after a bad app update.

A) Hours to days, and losing entries since the last backup is acceptable (Backup and Restore). Fits a personal journal

B) Minutes, with no lost entries (would require live sync to a server)

C) N/A: no recovery target; losing the phone means losing the journal

D) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question R2: How should changes to the app be governed?

A) No formal process: it's a personal project. Document this as an exemption

B) Propose a lightweight process: every change goes through a pull request with a short description and a note on how to undo it

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question R3: Continuous integration (automatic build and test on GitHub)

A) Set up GitHub Actions to build the app and run all tests (including dependency vulnerability scanning) on every push. Free for public repositories

B) No CI: builds and tests run on my Mac only

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question R4: Rolling back a bad update
If a new version of the app has a bug, how do you get back to the previous one?

A) Reinstall the previous version from its Git tag. Database changes between versions must be designed so that older data still opens (no destructive migrations)

B) No rollback plan needed

C) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question R5: When something goes wrong (incident response)

A) No formal process: I fix issues as I find them. Document this as an exemption

B) Propose a lightweight process: report issues as GitHub Issues, fix them, and add a regression test for each one

C) Other (please describe after [Answer]: tag below)

[Answer]: B
