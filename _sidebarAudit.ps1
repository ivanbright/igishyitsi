$ErrorActionPreference = 'SilentlyContinue'
Write-Host '=== ADMIN sidebar items now emitted by layout.tag (disk: every href whose URL contains /admin/) ==='
$raw = Get-Content 'src\main\webapp\WEB-INF\tags\layout.tag' -Raw
$hrefs = [regex]::Matches($raw, 'href="\$\{pageContext\.request\.contextPath\}/admin/([^"]*)"') | ForEach-Object { $_.Groups[1].Value }
$hrefs | Sort-Object -Unique | ForEach-Object { Write-Host ('  /admin/' + $_) }

Write-Host ''
Write-Host '=== real renderable sections in schoolAdmin.jsp per its inline JS switchSection (disk) ==='
$js = Get-Content 'src\main\webapp\WEB-INF\pages\schoolAdmin.jsp' -Raw
$cases = [regex]::Matches($js, "case\s+'([a-zA-Z]+)':") | ForEach-Object { $_.Groups[1].Value }
$cases | Sort-Object -Unique | ForEach-Object { Write-Host ('  case: ' + $_) }

Write-Host ''
Write-Host '=== does schoolAdmin.jsp have render() content (non-empty table html) per that case, i.e. is each sidebar item backed? ==='
foreach ($c in ($cases | Sort-Object -Unique)) {
    $has = [regex]::Matches($js, "id=\"section-" + $c + "\"").Count
    Write-Host ('  section ' + $c + ': DOM block present=' + $has)
}
