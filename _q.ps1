$ErrorActionPreference='Stop'
Write-Host '=== who forwards to schoolAdmin.jsp (must set serverData) ==='
rg -n 'schoolAdmin\.jsp' src\main\java
Write-Host
Write-Host '=== does schoolAdmin.jsp read "realData" or "serverData"? first 40 lines after imports ==='
$raw = Get-Content 'src\main\webapp\WEB-INF\pages\schoolAdmin.jsp' -TotalCount 8
$raw
Write-Host
Write-Host '=== the inline <script id=server-data> block near top ==='
rg -n 'server-data|serverData' src\main\webapp\WEB-INF\pages\schoolAdmin.jsp | Select-Object -First 5
