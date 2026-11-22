#!/bin/bash
# Сравнение JSON-результатов vllm bench serve -> MD-отчёт (только bash + jq).
# usage:
#   ./make-report.sh <file-a.json> <file-b.json> [name-a] [name-b] [out.md]
#   ./make-report.sh <dir-a> <dir-b> [name-a] [name-b] [out.md]   # все пары r*.json
set -euo pipefail

A=${1:?usage: ./make-report.sh <file|dir-a> <file|dir-b> [name-a] [name-b] [out.md]}
B=${2:?второй аргумент обязателен}
NAME_A=${3:-$(basename "${A%/}")}
NAME_B=${4:-$(basename "${B%/}")}
OUT=${5:-bench-results/report-$(basename "${A%/}")-vs-$(basename "${B%/}").md}

METRICS="completed mean_ttft_ms median_ttft_ms p50_ttft_ms p90_ttft_ms p99_ttft_ms mean_itl_ms median_itl_ms p50_itl_ms p99_itl_ms mean_e2el_ms median_e2el_ms p50_e2el_ms p99_e2el_ms output_throughput total_token_throughput"

get() { jq -r ".$2 // empty" "$1" 2>/dev/null || true; }

pct_diff() {
  awk -v a="${1:-}" -v b="${2:-}" 'BEGIN {
    if (a == "" || b == "" || a == 0) { print "n/a"; exit }
    printf "%+.1f%%", (b - a) / a * 100
  }'
}

pairs() {
  if [ -f "$A" ] && [ -f "$B" ]; then
    echo "$A $B"
  elif [ -d "$A" ] && [ -d "$B" ]; then
    for f in "$A"/r*.json; do
      base=$(basename "$f")
      [ -f "$B/$base" ] && echo "$f $B/$base"
    done
  else
    echo "error: ожидаю два файла или два каталога" >&2
    exit 1
  fi
}

{
  echo "# Сравнение: $NAME_A vs $NAME_B"
  echo
  echo "- Дата: $(date '+%Y-%m-%d %H:%M')"
  echo "- A: $NAME_A ($A)"
  echo "- B: $NAME_B ($B)"
  echo

  SUMMARY="| rate | TTFT p50 A/B (ms) | TTFT p99 A/B (ms) | ITL p50 A/B (ms) | E2E p50 A/B (ms) | out tok/s A/B | Δ tok/s |"
  SUMMARY="$SUMMARY"$'\n'"|---|---|---|---|---|---|---|"

  while read -r FA FB; do
    RATE=$(basename "$FA" | sed -E 's/[^0-9]//g')
    echo "## rate = ${RATE:-?}"
    echo
    echo "| Метрика | $NAME_A | $NAME_B | Δ% |"
    echo "|---|---|---|---|"
    for m in $METRICS; do
      va=$(get "$FA" "$m")
      vb=$(get "$FB" "$m")
      echo "| $m | ${va:-n/a} | ${vb:-n/a} | $(pct_diff "$va" "$vb") |"
    done
    echo

    tt50a=$(get "$FA" p50_ttft_ms); tt50b=$(get "$FB" p50_ttft_ms)
    tt99a=$(get "$FA" p99_ttft_ms); tt99b=$(get "$FB" p99_ttft_ms)
    itl50a=$(get "$FA" p50_itl_ms); itl50b=$(get "$FB" p50_itl_ms)
    e50a=$(get "$FA" p50_e2el_ms); e50b=$(get "$FB" p50_e2el_ms)
    outa=$(get "$FA" output_throughput); outb=$(get "$FB" output_throughput)
    SUMMARY="$SUMMARY"$'\n'"| ${RATE:-?} | $tt50a / $tt50b | $tt99a / $tt99b | $itl50a / $itl50b | $e50a / $e50b | $outa / $outb | $(pct_diff "$outa" "$outb") |"
  done < <(pairs)

  echo "## Сводка"
  echo
  echo "$SUMMARY"
  echo
} > "$OUT"

echo "Отчёт: $OUT"
