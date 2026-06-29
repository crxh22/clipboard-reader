# Session handoff runbook — MOB-NN succession

How one MOB session hands the project to the next. This is a solo project: only ONE
MOB session works at a time, so there is no concurrent-writer problem and no marker
file. Succession is just "write a good handoff, launch the successor, leave the old
session alone".

## When to hand off
Hand off at the next **natural break** (never mid-change — a successor inheriting a
half-finished edit re-derives context expensively and errs) when ANY of these is true:
- The context-guard Stop hook (`.claude/hooks/cr-context-guard.sh`) injects its
  one-time note. It fires at ~200k input tokens (`CR_CTX_THRESHOLD_TOKENS`, default
  200000) and is non-blocking — finish the current step first.
- The owner asks you to hand off / start fresh.
- You are at a clean boundary and the window is already large (long-context quality
  drops as the window fills).

## How to hand off (in order)
1. **Finish the current work unit.** Do not split one change across two sessions.
2. **Bring the record current**, so the successor reads truth, not a stale plan:
   - Update `PLAN.md` (open items / current state).
   - If you established a durable fact (a constraint, an owner decision, a gotcha),
     record it where it lives (`PLAN.md`, `docs/agent-context.md`, or a commit message).
3. **Write the handoff** to `docs/handoff/MOB-<next>.md` (named for the SUCCESSOR —
   the session that will read it) using `docs/runbooks/handoff-template.md`: copy the
   skeleton, fill EVERY section. Write it for an AI with zero memory of your session —
   follow `docs/runbooks/writing-for-the-successor.md`.
4. **Validate it:** `python3 scripts/check_handoff.py docs/handoff/MOB-<next>.md` — it
   MUST exit 0 (it fails, naming the offenders, on any missing or empty section).
5. **Commit** the handoff and the doc updates from step 2.
6. **Launch the successor** (next section) and **verify it is live** before going silent.
7. **Announce** to the owner in one line where the successor lives, then **stop
   working** — never keep editing after the successor exists.

## The successor launch command (run from YOUR Bash)
`NN` = your number + 1, zero-padded (`07` -> `08`). The tmux slug uses a dash
(`mob-08`); the phone/RC label uses spaces (`MOB - 08`). The handoff file you wrote
is named for the successor, so it reads the file matching its own number.

```bash
CR_TMUX_SESSION=mob-NN CR_RC_NAME='MOB - NN' CR_DETACH=1 \
  /home/artur/projects/clipboard-reader/clipboard_canon.sh \
  "You are MOB - NN, the successor session for the clipboard-reader (CitesteMi) Android app. Read docs/handoff/MOB-NN.md IN FULL, then continue. The project docs are already injected into your context. ABSOLUTE RULES: never pkill -f / pgrep -f a pattern that could match a session prompt; stop a task only by exact PID or exact tmux session name; never kill or exit a predecessor mob-* session — leave it idle, the owner retires it. <one line: the action in progress and its exact state>"
```

`CR_DETACH=1` makes `clipboard_canon.sh` create a DETACHED, RC-registered tmux
session and return — the correct successor-launch path (the script wires up
tmux/RC/model/effort itself; do NOT wrap it in your own `tmux new-session`). RC is on
by default, so the successor appears on the owner's phone as `MOB - NN` automatically.

## Verify before going silent
```bash
tmux has-session -t mob-NN                                  # must succeed
ps -eww -o pid,args | grep -F -- '--remote-control MOB - NN' | grep -v grep
```
Both must show the successor, AND the owner should confirm it on his phone. Always
pick a FRESH, never-used `NN` — a reused tmux name makes the launch silently fail
(it re-attaches with your args ignored), so a name collision = a failed launch.

## Forbidden actions
- **NEVER `pkill -f` / `pgrep -f`** (or any broad pattern kill) with a string that
  can appear in a session's launch prompt (e.g. `clipboard`, `reader`, `MOB`,
  `canon`). It matches the full command line and can kill the wrong `claude` process.
  Stop a task ONLY by **exact PID** (check `/proc/<pid>/cmdline` first, then
  `kill <pid>`) or **exact tmux session name** (`tmux kill-session -t <name>`).
- **NEVER kill or exit a PREDECESSOR `mob-*` session** — no `tmux kill-session`, no
  `kill`, no `/exit`. Leave it attached and idle. Killing it drops its claude.ai/code
  dashboard history (the on-disk transcript survives, but the owner reads history on
  the dashboard). The **owner** retires old sessions himself, his own way.
- **Never run two sessions working at once.** After the successor is live, the
  predecessor stops working.

## How the MOB-NN series increments
Sequential integers, zero-padded, starting at `01`: `MOB - 01` -> `MOB - 02` ->
`MOB - 03` ... Each successor's `NN` is the predecessor's `NN` + 1. The handoff file
is `docs/handoff/MOB-<NN>.md`, where `<NN>` is the **successor's** number (the session
that will read it).
