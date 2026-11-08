# Domain Functional Design: Clarification Questions

Thanks for your answers. Q1, Q4, Q5, Q7 and Q8 are clear. Three answers need a follow-up before I write the design: two conflict with things already approved, and one leaves a detail open.

Answer each question by writing the letter after its `[Answer]:` tag. If no option fits, choose the last one (Other) and describe what you want.

**Note on Q8 (no question needed)**: with hand-copied iOS examples, the cross-platform check is example-based. The property-based "oracle" rule (PBT-05) is marked N/A for the iOS comparison, with the reason that the reference is Swift code in another repository and can't run inside Kotlin tests. PBT-05 still applies to the streak, where a simple day-by-day count can be written as a reference.

---

## Contradiction 1: Saving across midnight (Q2:B vs. requirement FR-8.1 and story US-2.2)

You chose **Q2:B**: keep the iOS rule that rejects saves after midnight, save immediately in the last seconds, and accept that a word typed in the final moment might be lost.

The approved requirement **FR-8.1** and story **US-2.2** say: "text typed before midnight is kept". With Q2:B, a word typed in the final moment before 00:00 could be lost, so the design would not meet the approved requirement.

### Clarification Question 1
How should this be resolved?

A) Switch to Q2 option A: each save carries the instant the text was last edited, and a save for yesterday's entry is accepted for a few seconds after midnight if the edit happened before midnight. This meets FR-8.1 as written

B) Keep Q2:B, and change FR-8.1 and US-2.2 to say "text typed before about the last second before midnight is kept; a word typed at the very last moment may be lost" (an accepted, documented limitation)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Contradiction 2: Narrow test inputs (Q6:B vs. the enforced Property-Based Testing rules)

You chose **Q6:B**: dates within a few years of today, a handful of time zones, plain text.

The Property-Based Testing extension is enforced (blocking). Its rule **PBT-03** requires invariant tests to "generate a wide range of inputs including boundary values". Plain text only would leave out the boundaries that matter for these rules: empty and whitespace-only text (which must be rejected), accented text (search ignores accents) and emoji. That would be a blocking finding later in Code Generation.

### Clarification Question 2
Which input range should the property-based tests use?

A) Narrow ranges, but with the boundary values included: dates from 2024 to 2032 (covers leap years 2024 and 2028 and many daylight-saving changes), about 8 representative time zones (including New York and Mexico City, a half-hour zone, UTC−12 and UTC+14), and text that includes empty, whitespace-only, accented, emoji and long strings

B) Switch to Q6 option A: the wide ranges (1970–2100, every time zone, all text kinds)

C) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Ambiguity 1: The 20,000-character limit (Q3:B)

You chose **Q3:B**: a maximum of 20,000 characters, with a notice near the limit and no text accepted past it. Two details are open.

### Clarification Question 3
When should the notice appear?

A) From 19,000 characters: a small "1,000 characters left" style counter that counts down to the limit

B) Only when the limit is reached: "You've reached the maximum length for one entry"

C) Other (please describe after [Answer]: tag below)

[Answer]: B

### Clarification Question 4
What happens when pasted text would go past 20,000 characters?

A) Paste as much as fits, cut off at the limit, and show the "maximum length" message

B) Reject the whole paste and show the "maximum length" message, so nothing is cut off silently

C) Other (please describe after [Answer]: tag below)

[Answer]: B
