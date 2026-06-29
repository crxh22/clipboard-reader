#!/usr/bin/env bash
# clipboard_canon.sh — launch `claude` for the clipboard-reader app with the project
# docs assembled into the system prompt, inside tmux (survives SSH disconnects).
# Mirrors SF-F5's claude_canon.sh, simplified for a solo project:
#   - concatenates README.md + SPEC.md + PLAN.md + docs/agent-context.md into
#     _assembled_context.md (gitignored, rebuilt each launch),
#   - launches: claude --append-system-prompt-file <that file> --model --effort
#     [--remote-control <session>]  so a FRESH session already "knows" the project.
# Overrides (env): CR_MODEL, CR_EFFORT, CR_TMUX_SESSION, CR_NO_TMUX=1, CR_NO_RC=1, CLAUDE_BIN.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLAUDE_BIN="${CLAUDE_BIN:-claude}"
TMUX_SESSION="${CR_TMUX_SESSION:-clipboard-reader}"
MODEL="${CR_MODEL:-opus}"
EFFORT="${CR_EFFORT:-high}"

CTX_FILES=( README.md SPEC.md PLAN.md docs/agent-context.md )   # explicit order — edit here
CTXFILE="$DIR/_assembled_context.md"
{
  printf '=== CLIPBOARD-READER CONTEXT === Assembled at launch from the project docs below.\n'
  for rel in "${CTX_FILES[@]}"; do
    f="$DIR/$rel"
    if [ ! -s "$f" ]; then
      echo "clipboard_canon: context file missing or empty: $rel — aborting launch." >&2
      exit 1
    fi
    printf '\n--- %s ---\n' "$rel"
    cat "$f"
  done
  printf '\n=== END CLIPBOARD-READER CONTEXT ===\n'
} > "$CTXFILE"

RC_ARGS=()
[ -z "${CR_NO_RC:-}" ] && RC_ARGS=( --remote-control "$TMUX_SESSION" )

# Already inside tmux (a human in a pane) or opted out -> exec claude directly, no nesting.
if [ -n "${CR_NO_TMUX:-}" ] || [ -n "${TMUX:-}" ]; then
  exec "$CLAUDE_BIN" --append-system-prompt-file "$CTXFILE" --model "$MODEL" --effort "$EFFORT" "${RC_ARGS[@]}" "$@"
fi

command -v tmux >/dev/null 2>&1 || { echo "clipboard_canon: tmux not found (set CR_NO_TMUX=1 to bypass)." >&2; exit 1; }
CMD="$(printf '%q ' "$CLAUDE_BIN" --append-system-prompt-file "$CTXFILE" --model "$MODEL" --effort "$EFFORT" "${RC_ARGS[@]}" "$@")"
echo "clipboard_canon: tmux '$TMUX_SESSION' — detach Ctrl-b d, re-attach by rerunning this script." >&2
exec tmux new-session -A -s "$TMUX_SESSION" -c "$DIR" "$CMD"
