<# Read-only summary of the working tree before a commit: branch, changes, unpushed commits. Never modifies anything. #>
. "$PSScriptRoot/_common.ps1"
$branch = git rev-parse --abbrev-ref HEAD
Write-Host "Branch: $branch"
git status --short
Write-Host '--- diff stat (staged + unstaged)'
git diff HEAD --stat | Select-Object -Last 25
Write-Host '--- commits not on origin'
git log --oneline "origin/$branch..HEAD" 2>$null
