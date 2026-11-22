#!/usr/bin/env python3
"""Сравнение JSON-результатов vllm bench serve (gguf vs mlx) -> bench-results/comparison.csv"""
import json
import sys
from pathlib import Path

BASE = Path(__file__).parent / "bench-results"
TAGS = ["llamacpp", "vllm"]
RATES = [1, 2, 4, 8]

METRICS = [
    "mean_ttft_ms", "median_ttft_ms", "mean_itl_ms", "median_itl_ms",
    "mean_e2el_ms", "median_e2el_ms",
    "request_throughput", "output_throughput", "total_token_throughput",
]
PERCENTILE_KEYS = [
    "p50_ttft_ms", "p90_ttft_ms", "p99_ttft_ms",
    "p50_itl_ms", "p99_itl_ms",
    "p50_e2el_ms", "p99_e2el_ms",
]


def load(tag: str, rate: int) -> dict | None:
    p = BASE / tag / f"r{rate}.json"
    if not p.exists():
        return None
    return json.loads(p.read_text())


def fmt(v):
    if isinstance(v, float):
        return f"{v:.1f}"
    return str(v)


def main() -> None:
    header = ["format", "rate", "completed"] + METRICS + PERCENTILE_KEYS
    rows = []
    for tag in TAGS:
        for rate in RATES:
            d = load(tag, rate)
            if d is None:
                print(f"! нет файла {BASE / tag / f'r{rate}.json'}", file=sys.stderr)
                continue
            row = [tag, rate, d.get("completed", "")]
            row += [fmt(d.get(m, "")) for m in METRICS]
            row += [fmt(d.get(k, "")) for k in PERCENTILE_KEYS]
            rows.append(row)

    out = BASE / "comparison.csv"
    with out.open("w", encoding="utf-8") as f:
        f.write(",".join(header) + "\n")
        for row in rows:
            f.write(",".join(str(x) for x in row) + "\n")
    print(f"CSV: {out}")

    widths = [max(len(str(x)) for x in col) for col in zip(*([header] + rows))]
    for line in [header] + rows:
        print("  ".join(str(x).ljust(w) for x, w in zip(line, widths)))


if __name__ == "__main__":
    main()
