$ErrorActionPreference = 'SilentlyContinue'
Write-Host '=== each Admin*Servlet: real package + class name + forward target (all disk-authoritative) ==='
Get-ChildItem 'src\main\java\org\example\esubmission\controller\Admin*Servlet.java' | Sort-Object Name | ForEach-Object {
    $pkg = (Select-String -Path $_.FullName -Pattern '^package ([\w.]+);' | Select-Object -First 1)
    $cls = (Select-String -Path $_.FullName -Pattern '^public class (\w+)' | Select-Object -First 1)
    $fwd = (Select-String -Path $_.FullName -Pattern 'getRequestDispatcher\("([^"]+)"\)' | Select-Object -First 1)
    $p = if ($pkg) { $pkg.Matches[0].Groups[1].Value } else { '?' }
    $c = if ($cls) { $cls.Matches[0].Groups[1].Value } else { '?' }
    $f = if ($fwd) { $fwd.Matches[0].Groups[1].Value } else { '(none - writes directly/JSON)' }
    Write-Host ('  {0,-38} class={1}  fwd={2}' -f $_.Name, "$p.$c", $f)
}
Write-Host ''
Write-Host '=== the JSPs the module servlets forward to — do they EXIST on disk? (registering a route to a missing JSP = 404 moved, not fixed) ==='
$targets = Get-ChildItem 'src\main\java\org\example\esubmission\controller\Admin*Servlet.java' | ForEach-Object {
    $m = (Select-String -Path $_.FullName -Pattern 'getRequestDispatcher\("([^"]+)"\)' | Select-Object -First 1)
    if ($m) { $m.Matches[0].Groups[1].Value }
} | Sort-Object -Unique
foreach ($t in $targets) {
    $rel = $t -replace '^/', ''
    $full = Join-Path 'src\main\webapp' $rel
    $exists = Test-Path $full
    $flag = if ($exists) { 'OK ' } else { 'MISSING!' }
    Write-Host ("  [{0}] {1}" -f $flag, $t)
}
Write-Host ''
Write-Host '=== also confirm the Spring-less plain-webapp layout: which JSPs does layout.tag/sidebar actually link to via contextPath? ==='
if (Test-Path 'src\main\webapp\WEB-INF\tags\layout.tag') {
    $links = Select-String -Path 'src\main\webapp\WEB-INF\tags\layout.tag' -Pattern "contextPath'?\s*\+\s*'(/[^']*admin[^']*)'"
    foreach ($l in $links) { Write-Host ('  sidebar link -> {0}' -f $l.Matches[0].Groups[1].Value) }
} else {
    Write-Host '  (no layout.tag on disk — checked)'
}
