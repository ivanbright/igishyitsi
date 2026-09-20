$ErrorActionPreference='Stop'
$root = 'E:\igishyitsi'
Set-Location $root
$j = Join-Path $root 'src\main\webapp\WEB-INF\pages\schoolAdmin.jsp'
if (-not (Test-Path $j)) { Write-Host "MISSING: $j"; exit 1 }
$js = Get-Content $j -Raw
Write-Host ('schoolAdmin.jsp exists at: ' + $j)
Write-Host ''
Write-Host '=== sidebar items the layout emits (disk-authoritative hrefs) vs sections schoolAdmin actually renders ==='
$lt = Join-Path $root 'src\main\webapp\WEB-INF\tags\layout.tag'
$rawt = Get-Content $lt -Raw
$links = [regex]::Matches($rawt,'/admin/(dashboard|users|teachers|students|parents|classes|subjects)(?:\?section=([a-z]+))?') | ForEach-Object {
  if ($_.Groups[2].Value) { $_.Groups[2].Value } else { $_.Groups[1].Value }
}
$renders = [regex]::Matches($js,'id="section-([a-z]+)"') | ForEach-Object { $_.Groups[1].Value }
foreach ($l in ($links | Select-Object -Unique)) {
  $st = if ($l -in $renders) { 'RENDERED' } else { 'DEAD' }
  Write-Host ('  section={0,-12} {1}' -f $l, $st)
}
Write-Host ''
Write-Host '=== what the four DEAD modules would need to forward to, if kept as their own routes (the SERVLET forward targets) vs what exists on disk ==='
