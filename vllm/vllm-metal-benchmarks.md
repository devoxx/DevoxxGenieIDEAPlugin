# План бенчмарков vllm-metal

Инструмент уже есть на infra-Mac'е — вместе с vllm-metal поставился `vllm bench serve` (официальный инструмент команды vLLM). Он гоняет запросы к работающему серверу и отдаёт TTFT/ITL/E2E/throughput в JSON.

## Что меряем

- TTFT (time-to-first-token) — главный показатель для IDE-сценария
- Throughput (ток/с суммарно) при разной нагрузке
- ITL/E2E — межтоковая и полная латентность
- Процентили p50/p90/p99

## Сценарии

### A. Базовый прогон на один сервер (каждый MODEL-MAC отдельно)

```bash
vllm bench serve \
  --backend openai \
  --base-url http://<mac-ip>:1234/v1 \
  --model mlx-community/Qwen3-Coder-30B-A3B-Instruct-4bit \
  --api-key <secret> \
  --endpoint /v1/chat/completions \
  --dataset-name sonnet \
  --num-prompts 100 \
  --request-rate 2 \
  --save-result bench-30b-r2.json
```

### B. Кривая нагрузка→производительность

Повторять с `--request-rate` 1, 2, 4, 8, 16, 32 (или `--max-concurrency`). Так находим точку насыщения каждого узла.

### C. Через LiteLLM (вся связка)

Тот же прогон, но `--base-url http://<infra-ip>:4000/v1` и `--api-key <virtual-key>`, model `qwen3-coder-30b` — покажет агрегатную мощность N узлов и накладные расходы шлюза.

### D. Сравнение 30B vs 7B

Прогон B для порта 1235.

### E. Движки: llama.cpp+GGUF vs vllm-metal+MLX

Сравнение связок целиком (движок + формат). Та же модель Qwen2.5-Coder-7B, тот же уровень квантизации (int8). Разница в результатах — это движок (планировщик, ядра, батчинг), а не только формат весов.

| Связка | Модель | Запуск |
|---|---|---|
| llama.cpp + GGUF Q8_0 | `qwen2.5-coder-7b-q8_0.gguf` (из кэша HF) | `./run-bench-llamacpp.sh` |
| vllm-metal + MLX 8bit | `mlx-community/Qwen2.5-Coder-7B-Instruct-8bit` | `./run-bench-vllm.sh` |

MLX-модель скачается автоматически при первом запуске (~7.6 ГБ). Затем отчёты:

```bash
./make-report.sh bench-results/llamacpp bench-results/vllm "GGUF+llama.cpp" "MLX+vllm-metal" bench-results/report.md
python3 compare-bench.py   # вариант CSV: bench-results/comparison.csv
```

Параметры выровнены между движками:

| Параметр | llama.cpp | vllm-metal |
|---|---|---|
| Контекст | `--ctx-size 4096` | `--max-model-len 4096` |
| Префилл-чанк | `--ubatch-size 512` | `--max-num-batched-tokens 512` |
| Декод-батч | `--batch-size 2048` | дефолт (max 1024 seqs) |
| Параллелизм | `--parallel 8` | KV-кэш; нагрузка та же 1/2/4/8 |
| Слои на GPU | `--n-gpu-layers 999` | всегда все |
| Seed / greedy | `--seed 0` + `--temperature 0` в клиенте | `--seed 0` + то же |

Сценарии одинаковы в обоих скриптах: прогрев 5 промптов → TTFT-фокус `--num-prompts 50 --request-rate 1` → кривая нагрузки `--request-rate 2/4/8 --num-prompts 100`, `--input-len 512 --output-len 128`, клиент `vllm bench serve` (openai, `/v1/chat/completions`, `--tokenizer Qwen/Qwen2.5-Coder-7B-Instruct`, `--temperature 0 --seed 0`). Порт 1236, prefix caching выключен, свежий запуск на каждый прогон. Результаты JSON: `bench-results/{llamacpp,vllm}/r{1,2,4,8}.json` — ключи метрик совпадают (TTFT/ITL/E2E считаются клиентом из стримов).

Неустранимые различия (это и есть предмет сравнения): attention-ядра Metal (llama.cpp) vs paged attention (vllm-metal), dtype KV-кэша (f16 vs auto), планировщик (статические слоты vs непрерывный батчинг), C++ vs Python-обвязка.

## Важные настройки честного сравнения

- Отключать prefix caching (`--no-enable-prefix-caching` на сервере) для повторяемых прогонов — иначе кэш искажает TTFT
- Свежий запуск сервера перед каждым уровнем нагрузки (методика из официального блога vLLM)
- Фиксировать `--gpu-memory-utilization` одинаковым на всех узлах

## Анализ

- JSON-результаты (`--save-result`) — скормить скрипту сравнения или собрать в таблицу CSV
- Параллельно смотреть Grafana (`vllm:kv_cache_usage_perc`, `vllm:num_requests_running`) во время прогонов — видно, где упираемся
- Бонус: `ping`/iperf3 между infra и узлами — убедиться, что сеть не вносит искажений

## Ожидаемые ориентиры (для сверки)

- 30B-A3B 4-bit на M5 Pro: ~100–150 ток/с один стрим, TTFT на 1K-промпт — единицы секунд
- Плато агрегатной пропускной способности обычно наступает при 4–8 конкурентных стримах (MoE упирается в память)
