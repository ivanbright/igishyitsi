$ErrorActionPreference='Stop'
# Extract full servlet registration + mapping from web.xml, then the JSP each controller forwards to
$xml = [xml](Get-Content -Raw 'src\main\webapp\WEB-INF\web.xml')
$serv = @{}
foreach ($s in $xml.'web-app'.servlet) { $serv[$s.'servlet-name'] = $s.'servlet-class' }
foreach ($m in $xml.'web-app'.'servlet-mapping') {
    Write-Host ("{0,-24} {1,-65} {2}" -f $m.'servlet-name', $serv[$m.'servlet-name'], ($m.'url-pattern' -join ','))
}
