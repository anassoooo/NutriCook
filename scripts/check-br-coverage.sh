#!/usr/bin/env bash
# Rule-to-test traceability (Constitution V).
#
# Lists every business rule in docs/domain/business-rules.md (rows marked RETIRED are skipped)
# and the test files that reference it by ID (BR-SAF-04 -> BR_SAF_04 in a test name).
#
# Usage: scripts/check-br-coverage.sh [--enforce]
#   --enforce   exit 1 if any active rule has no test
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
catalogue="$root/docs/domain/business-rules.md"
enforce=false
[[ "${1:-}" == "--enforce" ]] && enforce=true

mapfile -t ids < <(grep -E '^\| BR-[A-Z]+-[0-9]+ \|' "$catalogue" | grep -v 'RETIRED' \
  | grep -oE '^\| BR-[A-Z]+-[0-9]+' | cut -c3- | sort -u)

search_dirs=()
for d in "$root/backend/src/test" "$root/frontend/src"; do [[ -d "$d" ]] && search_dirs+=("$d"); done

covered=0
missing_ids=()
report="| Rule | Tests |"$'\n'"|---|---|"$'\n'
for id in "${ids[@]}"; do
  token="${id//-/_}"
  files=$(grep -rlE "${token}([^0-9]|$)" "${search_dirs[@]}" \
    --include='*.java' --include='*.ts' --include='*.tsx' 2>/dev/null | xargs -r -n1 basename | sort -u | paste -sd, - || true)
  if [[ -n "$files" ]]; then
    covered=$((covered + 1))
    report+="| $id | ✅ $files |"$'\n'
  else
    missing_ids+=("$id")
    report+="| $id | ❌ |"$'\n'
  fi
done

total=${#ids[@]}
summary="BR coverage: $covered/$total rules have a test (${#missing_ids[@]} missing)"
echo "$report"
echo "$summary"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  { echo "### $summary"; echo; echo "$report"; } >> "$GITHUB_STEP_SUMMARY"
fi

if $enforce && (( ${#missing_ids[@]} > 0 )); then
  echo "Missing: ${missing_ids[*]}" >&2
  exit 1
fi
