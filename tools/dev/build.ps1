<# Compile and build the mod jar (skips tests). -Dist copies the jar to dist/ (overwrites only that version's jar). #>
param([switch]$Dist, [switch]$Clean)
. "$PSScriptRoot/_common.ps1"
$tasks = @()
if ($Clean) { $tasks += 'clean' }
$tasks += @('build', '-x', 'test', '-x', 'runGameTest')
$code = Invoke-Gradle 'build' $tasks
if ($code -ne 0) { exit $code }
if ($Dist) {
	$ver = Get-ModVersion
	$jar = Join-Path $Root "build/libs/grandline-$ver.jar"
	New-Item -ItemType Directory -Force -Path (Join-Path $Root 'dist') | Out-Null
	Copy-Item $jar (Join-Path $Root "dist/grandline-$ver.jar") -Force
	Write-Host "Copied to dist/grandline-$ver.jar"
}
