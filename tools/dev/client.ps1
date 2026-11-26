<# Launches the development client (or -Server for a dev server). Logs go to run/logs/latest.log. #>
param([switch]$Server)
. "$PSScriptRoot/_common.ps1"
& $Gradle ($(if ($Server) { 'runServer' } else { 'runClient' })) --console=plain
