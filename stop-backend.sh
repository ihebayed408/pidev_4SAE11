#!/usr/bin/env bash
# ============================================================
# stop-backend.sh  –  Kill all backend services started by start-backend.sh
# ============================================================

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$ROOT/logs"
PIDS_FILE="$LOG_DIR/pids.txt"

GREEN='\033[0;32m'; RED='\033[0;31m'; NC='\033[0m'

if [ ! -f "$PIDS_FILE" ]; then
  echo -e "${RED}[ERROR]${NC} No PID file found at $PIDS_FILE"
  exit 1
fi

echo "Stopping all backend services…"
while read -r pid; do
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null
    echo -e "${GREEN}[KILLED]${NC} PID $pid"
  fi
done < "$PIDS_FILE"

rm -f "$PIDS_FILE"
echo "Done."
