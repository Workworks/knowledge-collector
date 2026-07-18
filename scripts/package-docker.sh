#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"
MODEL="${OLLAMA_MODEL:-deepseek-r1:14b}"
FILES="-f compose.yaml"
if [ "${1:-}" = "--gpu" ]; then
  FILES="$FILES -f compose.gpu.yaml"
fi
export OLLAMA_MODEL="$MODEL"
docker compose $FILES build
docker compose $FILES up -d
echo "Knowledge Collector: http://127.0.0.1:8080"
echo "Ollama model: $MODEL"
