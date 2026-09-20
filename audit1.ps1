$ErrorActionPreference = 'Stop'
$root = 'E:\igishyitsi\src\main\webapp\WEB-INF\pages\schoolAdmin.jsp'

Write-Host '=== sidebar nav: switchSection targets ==='
$nav = rg -o "switchSection\('[a-z-]+'\)" $root | Sort-Object -Unique
$nav

Write-Host ''
Write-Host '=== content sections present ==='
rg -o 'id="section-[a-z-]+"' $root | Sort-Object -Unique

Write-Host ''
Write-Host '=== switchSection handler (JS) ==='
rg -n 'window.switchSection' $root

Write-Host ''
Write-Host '=== who forwards to schoolAdmin.jsp ==='
rg -l 'schoolAdmin\.jsp' E:\igishyitsi\src\main\java

Write-Host ''
Write-Host '=== AdminApiServlet.doGet: which realData keys are built / forwarded ==='
Select-String -Path E:\igishyitsi\src\main\java\org\example\esubmission\controller\AdminApiServlet.java -Pattern 'realData\.put\(|"users"|"students"|"teachers"|"classes"|"announcements"|"logs"|"stats"' | ForEach-Object { $_.Line.Trim() }
