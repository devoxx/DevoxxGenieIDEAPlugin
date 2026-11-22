#!/bin/bash
# Бенч llama.cpp + GGUF Q8_0: сервер -> прогрев -> TTFT -> кривая нагрузки.
# usage: ./run-bench-llamacpp.sh
set -euo pipefail

GGUF=$(ls "$HOME"/.cache/huggingface/hub/models--ggml-org--Qwen2.5-Coder-7B-Q8_0-GGUF/snapshots/*/qwen2.5-coder-7b-q8_0.gguf 2>/dev/null | head -1)
: "${GGUF:?GGUF не найден в кэше HF: ggml-org/Qwen2.5-Coder-7B-Q8_0-GGUF}"

ALIAS=qwen2.5-coder-7b
TOKENIZER=Qwen/Qwen2.5-Coder-7B-Instruct
PORT=1236
TAG=llamacpp
LOG=/tmp/llama-server.log
RESDIR=bench-results/$TAG

echo "==> [$TAG] starting llama-server: $GGUF (port $PORT)"
nohup llama-server -m "$GGUF" \
  --host 127.0.0.1 --port $PORT \
  --ctx-size 4096 \
  --batch-size 2048 \
  --ubatch-size 512 \
  --parallel 8 \
  --n-gpu-layers 999 \
  --alias "$ALIAS" \
  --seed 0 \
  --metrics > "$LOG" 2>&1 &
SRV=$!
trap 'kill $SRV 2>/dev/null || true; sleep 2' EXIT

for i in $(seq 1 120); do
  if curl -sf http://127.0.0.1:$PORT/health >/dev/null 2>&1; then
    echo "==> [$TAG] server ready"
    break
  fi
  if ! kill -0 $SRV 2>/dev/null; then
    echo "==> [$TAG] server died"; tail -30 "$LOG"; exit 1
  fi
  sleep 5
done
curl -sf http://127.0.0.1:$PORT/health >/dev/null || { echo "==> [$TAG] health timeout"; tail -30 "$LOG"; exit 1; }

B=(--backend openai --base-url http://127.0.0.1:$PORT/v1 \
   --endpoint /v1/chat/completions --model "$ALIAS" --api-key api-key \
   --dataset-name random --tokenizer "$TOKENIZER" \
   --input-len 512 --output-len 128 --temperature 0 --seed 0)
mkdir -p "$RESDIR"

echo "==> [$TAG] warmup"
vllm bench serve "${B[@]}" --num-prompts 5 --request-rate 1 >/dev/null

echo "==> [$TAG] TTFT focus: rate 1"
vllm bench serve "${B[@]}" --num-prompts 50 --request-rate 1 \
  --save-result --result-dir "$RESDIR" --result-filename r1.json

for R in 2 4 8; do
  echo "==> [$TAG] load: rate $R"
  vllm bench serve "${B[@]}" --num-prompts 100 --request-rate "$R" \
    --save-result --result-dir "$RESDIR" --result-filename "r$R.json"
done

echo "==> [$TAG] done, results in $RESDIR/"
