# План: установка и запуск vLLM-Metal на Mac M5 Pro 64GB

Повторяется на каждом Mac'е, идентично. Всё из официальной документации vllm-project/vllm-metal (v0.29–0.30, macOS Tahoe на M5 — поддерживается).

## 1. Требования

- Apple Silicon, macOS 15 (Sequoia) или новее — M5 Pro идёт с Tahoe (26.x), подходит
- Homebrew установлен (`brew --version`)

## 2. Установка (путь через Homebrew — рекомендованный)

```bash
brew tap vllm-project/vllm-metal https://github.com/vllm-project/vllm-metal
brew install vllm-project/vllm-metal/vllm-metal
vllm --version
```

Формула привозит всё самодостаточно: свой Python 3.12, vLLM, MLX и предкомпилированные Metal-ядра. Ничего не компилируется, venv активировать не нужно — команда `vllm` готова сразу.

Альтернатива (официальный install.sh, если brew нежелателен) — создаёт venv:

```bash
curl -LsSf https://raw.githubusercontent.com/vllm-project/vllm-metal/main/install.sh | bash
source ~/.venv-vllm-metal/bin/activate   # в каждом новом терминале
```

## 3. Что и где лежит

| Объект | Путь |
|---|---|
| Формула/пакет | `/opt/homebrew/Cellar/vllm-metal/<version>/` (~1.9 ГБ) |
| Бинарник `vllm` | `/opt/homebrew/bin/vllm` (в PATH) |
| Кэш моделей (после первого запуска) | `~/.cache/huggingface/hub/models--mlx-community--Qwen3-Coder-30B-A3B-Instruct-4bit/` (17.2 ГБ) |
| Логи | stdout сервера → в launchd-лог (`/tmp/vllm-metal.log` или системный журнал) |
| launchd plist (автозапуск) | `/Library/LaunchDaemons/com.team.vllm-metal.plist` |

Модель качается один раз на первом запуске; для остальных Mac'ов можно скопировать папку `~/.cache/huggingface` целиком (rsync) — сэкономит трафик.

## 4. Сетевые доступы

Исходящие (наружу), нужны только при установке/первом запуске:

- `github.com` (443) — brew tap
- `ghcr.io` (443) — бутылки Homebrew
- `huggingface.co` + `cdn-lfs.huggingface.co` (443) — скачивание весов модели
- `formulae.brew.sh` (443) — метаданные формул

Входящие (в LAN):

- Порт **1234** TCP на каждом Mac — OpenAI API (`/v1/*`), `/health`, `/metrics` — для LiteLLM и Prometheus
- Порт **1235** TCP — второй сервер (7B)
- Порт **4000** TCP на infra-Mac'е — API LiteLLM (единая точка входа для разработчиков)
- Порт **9090** TCP на infra-Mac'е — веб-интерфейс Prometheus
- Порт **3000** TCP на infra-Mac'е — Grafana
- SSH (22) — если модели будете раздавать rsync/scp

В рантайме (после кэширования модели) внешний интернет не нужен — сервер полностью офлайн.

## 5. Запуск сервера (30B для чата/агентов)

```bash
vllm serve mlx-community/Qwen3-Coder-30B-A3B-Instruct-4bit \
  --host 0.0.0.0 --port 1234 \
  --max-model-len 32768 \
  --enable-auto-tool-choice --tool-call-parser qwen3_coder \
  --reasoning-parser qwen3 \
  --api-key <secret-key>
```

Опционально `--gpu-memory-utilization 0.9` (default 0.92 — на 64 ГБ можно оставить).

Второй сервер (7B, порт 1235):

```bash
vllm serve mlx-community/Qwen3-Coder-7B-Instruct-4bit \
  --host 0.0.0.0 --port 1235 \
  --max-model-len 8192 \
  --api-key <secret-key>
```

## 6. Проверка

```bash
curl -s http://localhost:1234/health
curl -s http://localhost:1234/v1/models -H "Authorization: Bearer <secret-key>"
curl -s http://localhost:1234/metrics | grep -E "vllm:(kv_cache_usage_perc|ttft|num_requests_running)"
```

Тестовый запрос:

```bash
curl http://localhost:1234/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <secret-key>" \
  -d '{"model":"mlx-community/Qwen3-Coder-30B-A3B-Instruct-4bit",
       "messages":[{"role":"user","content":"Напиши функцию quicksort на Python"}]}'
```

## 7. DevoxxGenie у разработчиков

Settings → Tools → DevoxxGenie → LLM Providers → **Custom OpenAI**:

- URL: `http://<lb-ip>:4000/v1` (где `<lb-ip>` — адрес infra-Mac'а с LiteLLM)
- Model: `mlx-community/Qwen3-Coder-30B-A3B-Instruct-4bit`
- API Key: виртуальный ключ LiteLLM (из config.yaml шлюза)
- Timeout: 300 с

## 8. Автозапуск (launchd + запрет сна)

`/Library/LaunchDaemons/com.team.vllm-metal.plist` (в плане исполнения создадим): KeepAlive, запуск при загрузке, вывод лога в файл. Плюс на серверных Mac'ах:

```bash
sudo pmset -a sleep 0 disksleep 0
```

или запуск сервера внутри `caffeinate -dimsu`.

## 9. Последовательность работ (при выполнении)

1. На одном Mac: brew tap + install → `vllm serve` → дождаться загрузки модели → проверить `/health` и `/metrics`
2. Скопировать `~/.cache/huggingface` на остальные Mac'ы (rsync по SSH)
3. Повторить установку и запуск на каждом; проверить доступность с одной из машин (`curl http://<mac-ip>:1234/health`)
4. Создать launchd plist'ы на всех узлах
5. На infra-Mac'е: Prometheus — scrape job на все узлы (порты 1234 и 1235, `/metrics`); Grafana-дашборд vLLM
6. На infra-Mac'е: LiteLLM (`pip install "litellm[proxy]"`), конфиг `config.yaml` с бэкендами — все узлы 1234 (chat) и 1235 (7B), виртуальные ключи на разработчиков, fallback между узлами; запуск `litellm --config config.yaml --port 4000` → команда ходит на один адрес `http://<infra-ip>:4000/v1`
