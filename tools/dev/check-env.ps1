<# Read-only environment report for Grand Line development. Prints versions and whether settings exist; never prints secret values. #>
function V($name, [scriptblock]$cmd) {
	try { $out = (& $cmd 2>&1 | Select-Object -First 1); Write-Host ("{0,-14} {1}" -f $name, $out) }
	catch { Write-Host ("{0,-14} NOT FOUND" -f $name) }
}
V 'PowerShell' { $PSVersionTable.PSVersion.ToString() }
V 'Claude Code' { claude --version }
V 'Git' { git --version }
V 'GitHub CLI' { gh --version }
V 'VS Code' { code --version }
V 'Python' { python --version }
V 'java (PATH)' { java -version }
Write-Host '  (Gradle/Loom 1.19 needs JDK 25+ to run; the mod targets Java 17)'
Write-Host ("{0,-14} {1}" -f 'JAVA_HOME', $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { '(not set)' }))
$jdkDirs = @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu", "$env:USERPROFILE\.jdks") |
	Where-Object { $_ -and (Test-Path $_) }
foreach ($d in $jdkDirs) { Get-ChildItem $d -Directory | Where-Object { $_.Name -match 'jdk|jre|openjdk' } | ForEach-Object { Write-Host "  JDK: $($_.FullName)" } }
foreach ($k in 'ANTHROPIC_API_KEY', 'ANTHROPIC_AUTH_TOKEN', 'ELEVENLABS_API_KEY', 'GH_TOKEN', 'GITHUB_TOKEN') {
	Write-Host ("{0,-22} {1}" -f $k, $(if ([Environment]::GetEnvironmentVariable($k)) { 'set (hidden)' } else { 'not set' }))
}
if ($env:ANTHROPIC_BASE_URL) { Write-Host "ANTHROPIC_BASE_URL     host $(([Uri]$env:ANTHROPIC_BASE_URL).Host) (requests are routed through it)" }
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Push-Location $repo
V 'Repo branch' { git rev-parse --abbrev-ref HEAD }
V 'Uncommitted' { "$(@(git status --short).Count) changed files" }
V 'Gradle wrap.' { (Select-String gradle/wrapper/gradle-wrapper.properties -Pattern 'distributionUrl').Line }
Pop-Location
Write-Host '--- Claude Code config (names only)'
$cfg = Join-Path $env:USERPROFILE '.claude'
foreach ($p in 'settings.json', 'skills', 'plugins', 'agents', 'commands') {
	$path = Join-Path $cfg $p
	if (Test-Path $path) { Write-Host "  ~/.claude/$p" ; if ((Get-Item $path).PSIsContainer) { Get-ChildItem $path -Name | Select-Object -First 30 | ForEach-Object { "    $_" } } }
}
Write-Host '--- MCP servers'
try { claude mcp list 2>&1 | Select-Object -First 30 } catch { Write-Host '  (claude not found)' }
