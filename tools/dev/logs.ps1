<# Shows the interesting lines of the newest dev log, or the head of the newest crash report (-Crash). Read-only. #>
param([switch]$Crash, [switch]$Server, [int]$Lines = 60)
. "$PSScriptRoot/_common.ps1"
$dir = if ($Server) { 'run-server' } else { 'run' }
if ($Crash) {
	$f = Get-ChildItem (Join-Path $Root "$dir/crash-reports") -Filter *.txt -ErrorAction SilentlyContinue | Sort-Object LastWriteTime | Select-Object -Last 1
	if (-not $f) { Write-Host 'No crash reports.'; exit 0 }
	Write-Host "== $($f.FullName)"
	Get-Content $f.FullName -TotalCount $Lines
	exit 0
}
$log = Join-Path $Root "$dir/logs/latest.log"
if (-not (Test-Path $log)) { Write-Host "No log at $log"; exit 0 }
Select-String -Path $log -Pattern 'ERROR|Exception|Caused by|GrandLine.*(WARN|fail|problem)|\(GrandLine\)' |
	Where-Object { $_.Line -notmatch 'sessionserver|ALSA' } | Select-Object -Last $Lines | ForEach-Object { $_.Line }
