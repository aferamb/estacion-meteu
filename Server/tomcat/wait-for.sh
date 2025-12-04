#!/usr/bin/env bash
# wait-for.sh host port cmd...
set -euo pipefail

HOST="$1"
PORT="$2"
shift 2

if [ -z "$HOST" ] || [ -z "$PORT" ]; then
  echo "Usage: $0 host port [cmd]" >&2
  exit 1
fi

echo "Waiting for $HOST:$PORT..."

# If MYSQL_ROOT_PASSWORD is set, do an authenticated ping using mysqladmin (better readiness)
if [ -n "${MYSQL_ROOT_PASSWORD:-}" ] && command -v mysqladmin >/dev/null 2>&1; then
  echo "MYSQL_ROOT_PASSWORD present — using mysqladmin ping"
  until mysqladmin ping -h "$HOST" -u root -p"$MYSQL_ROOT_PASSWORD" --silent; do
    sleep 1
  done
  echo "MySQL at $HOST is available (authenticated)"
else
  # Fallback: Wait until the TCP port is open
  while true; do
    if command -v timeout >/dev/null 2>&1; then
      if timeout 1 bash -c "cat < /dev/tcp/$HOST/$PORT" >/dev/null 2>&1; then
        echo "$HOST:$PORT is available (TCP)"
        break
      fi
    else
      if bash -c "cat < /dev/tcp/$HOST/$PORT" >/dev/null 2>&1; then
        echo "$HOST:$PORT is available (TCP)"
        break
      fi
    fi
    sleep 1
  done
fi

# Execute the provided command
exec "$@"
