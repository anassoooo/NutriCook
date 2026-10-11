# Rule-to-test traceability (Constitution V). Windows twin of check-br-coverage.sh.
# Usage: scripts\check-br-coverage.ps1 [-Enforce]
param([switch]$Enforce)
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$catalogue = Join-Path $root 'docs/domain/business-rules.md'
$ids = Get-Content $catalogue -Encoding UTF8 |
  Where-Object { $_ -match '^\| (BR-[A-Z]+-[0-9]+) \|' -and $_ -notmatch 'RETIRED' } |
  ForEach-Object { [regex]::Match($_, 'BR-[A-Z]+-[0-9]+').Value } |
  Sort-Object -Unique

$files = @('backend/src/test', 'frontend/src') |
  ForEach-Object { Join-Path $root $_ } | Where-Object { Test-Path $_ } |
  ForEach-Object { Get-ChildItem $_ -Recurse -File -Include *.java, *.ts, *.tsx }

$missing = @()
$rows = foreach ($id in $ids) {
  $token = $id -replace '-', '_'
  $hits = $files | Select-String -Pattern "$token([^0-9]|$)" -List | ForEach-Object { $_.Filename } | Sort-Object -Unique
  if ($hits) { [pscustomobject]@{ Rule = $id; Tests = ($hits -join ', ') } }
  else { $missing += $id; [pscustomobject]@{ Rule = $id; Tests = 'MISSING' } }
}
$rows | Format-Table -AutoSize | Out-String -Width 200 | Write-Output
Write-Output ("BR coverage: {0}/{1} rules have a test ({2} missing)" -f ($ids.Count - $missing.Count), $ids.Count, $missing.Count)
if ($Enforce -and $missing.Count -gt 0) { Write-Error ("Missing: " + ($missing -join ' ')); exit 1 }
