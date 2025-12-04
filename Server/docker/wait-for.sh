#!/bin/bash
# wait-for.sh host port cmd...
set -e
HOST="$1"
PORT="$2"
shift 2

if [ -z "$HOST" ] || [ -z "$PORT" ]; then
  echo "Usage: $0 host port [cmd]"
  exit 1
fi

echo "Waiting for $HOST:$PORT..."
# proceed to check readiness
if [ -n "$MYSQL_ROOT_PASSWORD" ]; then
  # If MYSQL_ROOT_PASSWORD is available, try a proper MySQL ping (stronger readiness check)
  echo "MYSQL_ROOT_PASSWORD present — using mysqladmin ping"
  while ! mysqladmin ping -h "$HOST" -u root -p"$MYSQL_ROOT_PASSWORD" --silent; do
    sleep 1
  done
  echo "MySQL at $HOST is available"
else
  # Fallback: Wait until the port is open (TCP)
  while ! timeout 1 bash -c "cat < /dev/tcp/$HOST/$PORT" >/dev/null 2>&1; do
    sleep 1
  done
  echo "$HOST:$PORT is available (TCP)"
fi

# Execute the provided command
exec "$@"
