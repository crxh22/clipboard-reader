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

RC_NAME="${CR_RC_NAME:-$TMUX_SESSION}"
RC_ARGS=()
[ -z "${CR_NO_RC:-}" ] && RC_ARGS=( --remote-control "$RC_NAME" )

# Inside tmux already (human in a pane) and not detaching, or opted out -> exec directly, no nesting.
if [ -n "${CR_NO_TMUX:-}" ] || { [ -n "${TMUX:-}" ] && [ -z "${CR_DETACH:-}" ]; }; then
  exec "$CLAUDE_BIN" --append-system-prompt-file "$CTXFILE" --model "$MODEL" --effort "$EFFORT" "${RC_ARGS[@]}" "$@"
fi

command -v tmux >/dev/null 2>&1 || { echo "clipboard_canon: tmux not found (set CR_NO_TMUX=1 to bypass)." >&2; exit 1; }
CMD="$(printf '%q ' "$CLAUDE_BIN" --append-system-prompt-file "$CTXFILE" --model "$MODEL" --effort "$EFFORT" "${RC_ARGS[@]}" "$@")"

# CR_DETACH=1 -> create a detached session and return (used to launch a successor, e.g. MOB-NN).
if [ -n "${CR_DETACH:-}" ]; then
  tmux new-session -d -s "$TMUX_SESSION" -c "$DIR" "$CMD"
  echo "clipboard_canon: launched DETACHED tmux '$TMUX_SESSION' (RC '$RC_NAME'). Attach: tmux attach -t $TMUX_SESSION" >&2
  exit 0
fi
echo "clipboard_canon: tmux '$TMUX_SESSION' (RC '$RC_NAME') — detach Ctrl-b d, re-attach by rerunning this script." >&2
exec tmux new-session -A -s "$TMUX_SESSION" -c "$DIR" "$CMD"
