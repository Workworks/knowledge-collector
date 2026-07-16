#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$ROOT_DIR"

JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if ! JAVA_VERSION=$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ {print $2; exit}'); then
  echo "[ERROR] Java was not found. Install JDK 17 or set JAVA_HOME." >&2
  exit 1
fi

JAVA_MAJOR=$(printf '%s' "$JAVA_VERSION" | awk -F. '{if ($1 == 1) print $2; else print $1}')
if [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "[ERROR] Java 17 or later is required. Current version: $JAVA_VERSION" >&2
  exit 1
fi

export KNOWLEDGE_COLLECTOR_DATA_DIR="${KNOWLEDGE_COLLECTOR_DATA_DIR:-$ROOT_DIR/data}"
export KNOWLEDGE_COLLECTOR_SERVER_ADDRESS="${KNOWLEDGE_COLLECTOR_SERVER_ADDRESS:-127.0.0.1}"
export KNOWLEDGE_COLLECTOR_SERVER_PORT="${KNOWLEDGE_COLLECTOR_SERVER_PORT:-8080}"

for directory in database article-content snapshots exports logs; do
  mkdir -p "$KNOWLEDGE_COLLECTOR_DATA_DIR/$directory"
done

JAR="$ROOT_DIR/knowledge-collector-boot/target/knowledge-collector.jar"
if [ ! -f "$JAR" ]; then
  echo "[ERROR] Executable JAR was not found: $JAR" >&2
  echo "        Run 'mvn clean package' with JDK 17 first." >&2
  exit 1
fi

echo "Knowledge Collector is starting..."
echo "URL:  http://$KNOWLEDGE_COLLECTOR_SERVER_ADDRESS:$KNOWLEDGE_COLLECTOR_SERVER_PORT"
echo "Data: $KNOWLEDGE_COLLECTOR_DATA_DIR"
echo "Logs: $KNOWLEDGE_COLLECTOR_DATA_DIR/logs"
echo "Stop: Press Ctrl+C in this terminal."

exec "$JAVA_CMD" -jar "$JAR"
