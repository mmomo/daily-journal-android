# App NFR Requirements: Clarification Question

Thanks for your answers. Q1, Q2, Q3, Q5 and Q6 are clear. One answer conflicts with approved requirements and the enforced Security extension.

Answer by writing the letter after the `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

---

## Contradiction 1: Installing a debug build (Q4:B)

You chose **Q4:B**: install a debug build.

That conflicts with:
- **NFR-6.4** (approved): release builds are minified, contain no debug features, and show generic errors
- **NFR-6.2** (approved): release builds strip debug and verbose logs
- **SECURITY-09** (Security extension, enforced and blocking): installed components contain no debug features

A debug build is debuggable (another app on a connected computer can inspect it), keeps every log line, and isn't shrunk. Installing it on your phone would be a blocking security finding in Code Generation.

If the main reason for B was to avoid creating and keeping a signing keystore, option B below keeps that simplicity while staying compliant.

### Clarification Question 1
How should the build you install be produced?

A) Switch to Q4 option A: a hardened release build signed with your own keystore, kept outside the repository (its passwords in a git-ignored file)

B) A hardened release build (shrunk, logs stripped, not debuggable) signed with the debug signing key that Android Studio already created on this Mac. No keystore to create or manage. Fine for installing on your own phone; the catch is that if this Mac's debug key is lost, the next install needs an uninstall first (which deletes the journal on the phone, so export first)

C) Keep Q4:B (debug build) and change NFR-6.2 and NFR-6.4 to apply only to builds for other people, recording SECURITY-09 as a documented exception for this personal install

D) Other (please describe after [Answer]: tag below)

[Answer]: B
