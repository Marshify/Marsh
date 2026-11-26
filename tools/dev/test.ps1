<# Runs game tests (default) and/or unit tests, then summarises failures from the reports. #>
param([switch]$Unit, [switch]$All)
. "$PSScriptRoot/_common.ps1"
$code = 0
if ($Unit -or $All) { $code = Invoke-Gradle 'unit-test' @('test') }
if (-not $Unit -or $All) {
	$c = Invoke-Gradle 'gametest' @('runGameTest'); if ($c -ne 0) { $code = $c }
	$report = Join-Path $Root 'build/reports/gametest/server-gametests.xml'
	if (Test-Path $report) {
		[xml]$x = Get-Content $report
		$cases = @($x.SelectNodes('//testcase'))
		$failed = @($cases | Where-Object { $_.failure -or $_.error })
		Write-Host ("Game tests: {0} run, {1} failed" -f $cases.Count, $failed.Count)
		$failed | Select-Object -First 20 | ForEach-Object {
			$msg = if ($_.failure) { $_.failure.message } else { $_.error.message }
			Write-Host " FAIL $($_.name): $msg"
		}
	}
}
exit $code
