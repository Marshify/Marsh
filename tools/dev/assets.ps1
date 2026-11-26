<# Asset pipelines. -VfxCheck validates tools/vfx/incoming without writing; -Vfx imports; -Sounds syncs tools/sounds/incoming. #>
param([switch]$VfxCheck, [switch]$Vfx, [switch]$Sounds)
. "$PSScriptRoot/_common.ps1"
$py = (Get-Command python -ErrorAction SilentlyContinue) ?? (Get-Command python3 -ErrorAction SilentlyContinue)
if (-not $py) { Write-Error 'Python 3 not found (needed for the asset tools).' }
if ($VfxCheck) { & $py.Source tools/vfx/import_vfx.py --check }
if ($Vfx) { & $py.Source tools/vfx/import_vfx.py }
if ($Sounds) { & $py.Source tools/sounds/sync_sounds.py }
if (-not ($VfxCheck -or $Vfx -or $Sounds)) { Get-Help $PSCommandPath }
