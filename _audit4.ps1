$ErrorActionPreference='Stop'
Write-Host '=== css referenced in schoolAdmin.jsp <head> (pre-tag style) ==='
$c = Get-Content src\main\webapp\WEB-INF\pages\schoolAdmin.jsp -Raw
[regex]::Matches($c, 'href="[^"]*\.css"') | ForEach-Object { $_.Value } | Sort-Object -Unique
Write-Host
Write-Host '=== but layout.tag layout pulls which css? ==='
[regex]::Matches((Get-Content src\main\webapp\WEB-INF\tags\layout.tag -Raw), '\.css\?|\.css"|href="[^"]*css[^"]*"') | ForEach-Object { $_.Value } | Sort-Object -Unique
Write-Host
Write-Host '=== does css/schoolAdmin.css exist now? ==='
Test-Path src\main\webapp\css\schoolAdmin.css
Write-Host
Write-Host '=== schoolAdmin.jsp final signed <script> summary vars used after first data-load (contract) ==='
[regex]::Matches($c, 'const ([A-Za-z]+) = serverData\.([A-Za-z]+)') | ForEach-Object { "{0} = serverData.{1}" -f $_.Groups[1].Value, $_.Groups[2].Value } | Sort-Object -Unique
