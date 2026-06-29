# Writing for the successor — unambiguity rules

Any text aimed at another AI (a handoff doc, a successor launch prompt, a note left in
PLAN.md) is read COLD — by a session with zero memory of yours. Redundant clarity is
cheap; an unresolvable reference is expensive. Run this checklist before committing
such text.

## Checklist
- [ ] **Re-explain every ID / acronym / shorthand inline.** No bare references the
      reader cannot resolve (`the #14 fix`, `the bubble bug`, `DoR`). Write it out:
      `the overlay bubble dropping taps after rotation (app/.../bubble/BubbleService.kt)`.
- [ ] **Name the exact file + line + the contradiction/symptom**, not a vague area.
      "Save does nothing" -> "`onSave()` in PlayerActivity.kt:142 swallows the
      exception; the write never reaches TtsStore — repro: tap Save, no DB row". A
      fresh agent cannot fix what it cannot locate.
- [ ] **State a falsifiable / observable check for every "done" claim.** Not "TTS
      works" but "we know it's wrong if `assembleDebug` fails OR the phone reads
      nothing on PROCESS_TEXT". If you can't say how you'd know it's wrong, you
      haven't verified it.
- [ ] **One concrete behavior per instruction.** Each "next action" must answer "what
      observable thing changes when this is done?" in one sentence. If it can't,
      split it.
- [ ] **Quote + locate facts; mark the rest as assumption.** A fact = a literal value
      or quote + where it lives (`versionCode 7 in app/build.gradle.kts:12`). Anything
      you did not verify: label it "ASSUMED / not verified"; do not assert it.
- [ ] **Say "I don't know" instead of guessing.** If a state is unknown, write that it
      is unknown and how to find out — never invent a plausible value.
- [ ] **No unresolved cross-references.** Don't point at "the earlier discussion" or
      "the usual fix". Inline the content, or give an exact path the reader can open.

## Optional self-check (high-stakes text only)
After writing a high-stakes instruction (a successor launch prompt, a spec the next
session will build from), spawn a clean-context sub-agent, give it ONLY that text, and
ask it: (a) what is the single action required? (b) what is explicitly out of scope?
(c) is any sentence readable two ways? If its answers diverge from your intent, the
text is ambiguous — fix it.
