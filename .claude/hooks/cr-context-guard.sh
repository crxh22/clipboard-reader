#!/usr/bin/env bash
# clipboard-reader context guard — Stop hook. Fires at each turn-end of the MAIN
# session (subagents fire SubagentStop, not this). Reads the EXACT input-side token
# count from the transcript `usage`; once over threshold, emits ONE non-blocking note
# suggesting a clean handoff to a FRESH session (via clipboard_canon.sh) before
# long-context quality degrades. Never blocks; one note per session.
#   Threshold override (env): CR_CTX_THRESHOLD_TOKENS (default 200000).
set -euo pipefail
INPUT=$(cat)
read -r SID TRANSCRIPT <<EOF
$(printf '%s' "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('session_id',''), d.get('transcript_path',''))" 2>/dev/null)
EOF
[ -n "${SID:-}" ] && [ -n "${TRANSCRIPT:-}" ] && [ -f "$TRANSCRIPT" ] || exit 0

BYTES=$(stat -c%s "$TRANSCRIPT" 2>/dev/null || echo 0)
EST=$(tail -c 1048576 "$TRANSCRIPT" 2>/dev/null | python3 -c "
import sys, json
last = 0
for line in sys.stdin:
    line = line.strip()
    if not line: continue
    try: o = json.loads(line)
    except Exception: continue
    m = o.get('message') if isinstance(o.get('message'), dict) else None
    u = (m or {}).get('usage') if m else o.get('usage')
    if isinstance(u, dict):
        c = (u.get('input_tokens',0) or 0) + (u.get('cache_read_input_tokens',0) or 0) + (u.get('cache_creation_input_tokens',0) or 0)
        if c: last = c
print(last)
" 2>/dev/null)
{ [ -n "$EST" ] && [ "$EST" -gt 0 ] 2>/dev/null; } || EST=$((BYTES * 10 / 41))

THRESHOLD="${CR_CTX_THRESHOLD_TOKENS:-200000}"
[ "$EST" -ge "$THRESHOLD" ] || exit 0

NOTICED="$HOME/.claude/cr-ctx-noticed-$SID"   # one note per session
[ -e "$NOTICED" ] && exit 0
: > "$NOTICED"

KTOK=$((EST / 1000)); PRAGK=$((THRESHOLD / 1000))
NOTE="Context is ~${KTOK}k tokens (threshold ${PRAGK}k). At the next natural break: note any in-progress state into PLAN.md, then continue in a FRESH session via ./clipboard_canon.sh (it re-injects the project docs). Keeps long-context quality high. Do not interrupt the current step for this."
python3 -c 'import json,sys; print(json.dumps({"hookSpecificOutput":{"hookEventName":"Stop","additionalContext":sys.argv[1]}}))' "$NOTE"
exit 0
