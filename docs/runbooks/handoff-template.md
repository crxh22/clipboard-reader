# Handoff template — MOB-NN (required sections)

Copy everything under the `--- 8< ---` line into `docs/handoff/MOB-<next>.md` (named
for the SUCCESSOR — the session that will read it). Keep every `##` header
**verbatim** (the checker matches them by text), replace every `<...>` placeholder
with real content, and fill EVERY section.

A handoff is NOT done until `python3 scripts/check_handoff.py docs/handoff/MOB-<next>.md`
exits 0. The checker fails (exit 1, names the offenders) on any missing or empty
section, and reads a section left as the bare `<!-- ... -->` placeholder as empty. It
enforces the SLOTS, not the QUALITY — a section can be present and still shallow, so
re-read the whole session before writing. Write each section for an AI with zero
memory of this session: see `docs/runbooks/writing-for-the-successor.md`.

--- 8< --- copy below this line --- 8< ---

# Handoff — MOB-<N> -> MOB-<N+1> (DD-MM-YYYY)

<!-- One line: how full this session's context got (e.g. "~210k tokens") and why you
     are handing off (threshold / owner-directed / clean boundary). -->

## State now
<!-- The snapshot the successor needs first: what branch/working state the repo is in,
     what is built and working, what is broken, and the 1-3 pointers that matter
     (files, PLAN.md items). Reality as it IS right now. -->

## Done this session
<!-- Each thing you finished + its commit hash (or "uncommitted: <files>"). If you
     shipped a release: the version + APK link. -->

## In progress
<!-- The ONE action mid-flight and its EXACT state (file, function, what is written vs
     not), or "NONE — clean boundary". The successor must be able to resume it without
     guessing. -->

## Open decisions
<!-- Each unresolved decision: the options, who decides (owner vs you), and what
     triggers it. If none: write "NONE" explicitly. -->

## Gotchas / constraints
<!-- What the real code/device/toolchain forced that the docs did not say, plus any
     dead ends already tried (so the successor does not re-explore them). Name the
     exact file + line where it bit. If none: "NONE". -->

## Next action
<!-- The single next thing to do, named so precisely the successor needs zero
     interpretation: the exact file/command and the observable result that means it
     worked. -->

## Verification status
<!-- What is machine-verified (build ran / unit tests passed — say which) vs what is
     only verifiable on the owner's phone (voice, overlay bubble, QS tile) and is
     therefore ASSUMED. The successor trusts the first and re-checks the second. -->
