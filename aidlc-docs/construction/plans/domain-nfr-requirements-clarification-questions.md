# Domain NFR Requirements: Clarification Questions

Thanks for your answers. Q2, Q3, Q5, Q6, Q7, Q8 and Q9 are clear. Two answers need a follow-up.

**Note on Q3 with Q9 (no question needed)**: with GitHub's Dependabot alerts (Q3:A), GitHub checks the stored dependency list against newly published vulnerabilities by itself, so alerts still arrive without the weekly CI run you left out in Q9:B. Dependabot alerts have to be switched on once in the repository's Settings → Code security; I'll include that step in the build instructions.

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

---

## Contradiction 1: SBOM (Q4:B vs. approved requirement NFR-6.5)

You chose **Q4:B**: use GitHub's dependency graph, which can export an SBOM on demand from the repository page.

The approved requirement **NFR-6.5** says "CI produces a Software Bill of Materials". With Q4:B nothing in CI produces one, so the design would not meet the approved requirement.

### Clarification Question 1
How should this be resolved?

A) Switch to Q4 option A: the CycloneDX Gradle plugin produces an SBOM file in every CI run, saved as a build artifact. NFR-6.5 is met as written

B) Keep Q4:B, and change NFR-6.5 to "an SBOM can be exported on demand from GitHub's dependency graph" (the dependency list CI uploads for scanning in Q3:A is what GitHub exports)

C) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Ambiguity 1: "One release behind the latest" (Q1:B)

"One release behind" can mean different things, and the build tools must also work together (for example, the Kotlin version decides which Compose compiler can be used, and the Android Gradle Plugin requires certain Gradle versions).

### Clarification Question 2
What should "one release behind" mean?

A) For each tool and library, the newest patch of the previous minor version line (for example, if Kotlin's latest is 2.N.x, use the newest 2.(N−1).x), adjusted where needed so the whole set is compatible; Java 17 as the compile target

B) For each tool and library, the version before the newest patch (for example 2.N.1 when 2.N.2 is the latest); Java 17 as the compile target

C) Any stable version that has been out for at least three months, chosen as a compatible set; Java 17 as the compile target

D) Other (please describe after [Answer]: tag below)

[Answer]: D: use latest
