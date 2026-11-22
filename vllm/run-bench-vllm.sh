#!/bin/bash
# Бенч vllm-metal + MLX 8bit: сервер -> прогрев -> TTFT -> кривая нагрузки.
# usage: ./run-bench-vllm.sh
set -euo pipefail

MODEL=mlx-community/Qwen2.5-Coder-7B-Instruct-8bit
TOKENIZER=Qwen/Qwen2.5-Coder-7B-Instruct
PORT=1236
TAG=vllm
LOG=/tmp/vllm-mlx.log
RESDIR=bench-results/$TAG

echo "==> [$TAG] starting server: $MODEL (port $PORT)"
nohup vllm serve "$MODEL" \
  --host 127.0.0.1 --port $PORT \
  --max-model-len 4096 \
  --max-num-batched-tokens 512 \
  --disable-prefix-caching \
  --gpu-memory-utilization 0.9 \
  --seed 0 \
  --api-key api-key > "$LOG" 2>&1 &
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
   --endpoint /v1/chat/completions --model "$MODEL" --api-key api-key \
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
