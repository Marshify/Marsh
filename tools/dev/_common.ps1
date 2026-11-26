# Shared helpers for the Grand Line dev scripts. Read-only except where a script says otherwise.
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $Root
$Gradle = if ($IsWindows -or $env:OS -eq 'Windows_NT') { Join-Path $Root 'gradlew.bat' } else { Join-Path $Root 'gradlew' }
function Get-ModVersion { (Select-String -Path (Join-Path $Root 'gradle.properties') -Pattern '^mod_version=(.+)$').Matches[0].Groups[1].Value.Trim() }
# Runs Gradle, keeps the full log in build/dev-logs/<name>.log and prints only errors, warnings from our code and the result.
function Invoke-Gradle([string]$Name, [string[]]$GradleArgs) {
	$logDir = Join-Path $Root 'build/dev-logs'
	New-Item -ItemType Directory -Force -Path $logDir | Out-Null
	$log = Join-Path $logDir "$Name.log"
	& $Gradle @GradleArgs --console=plain *> $log
	$code = $LASTEXITCODE
	Select-String -Path $log -Pattern 'error:|\.java:\d+: |FAILURE|BUILD (SUCCESSFUL|FAILED)|What went wrong|> Task .* FAILED|tests completed|failed' |
		Select-Object -First 40 | ForEach-Object { Write-Host $_.Line }
	Write-Host "Full log: $log"
	return $code
}
