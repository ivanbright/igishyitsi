$ErrorActionPreference = 'Stop'
$j = 'src\main\webapp\WEB-INF\pages\schoolAdmin.jsp'
$lines = Get-Content $j

Write-Host '=== every section DOM block in schoolAdmin.jsp: id + its first child heading + closing tag (disk-authoritative) ==='
$stack = New-Object System.Collections.Stack
for ($i = 0; $i -lt $lines.Count; $i++) {
    $l = $lines[$i]
    if ($l -match 'id="section-([A-Za-z]+)"') {
        $name = $Matches[1]
        $head = ''
        for ($k = $i; $k -lt [Math]::Min($i + 34, $lines.Count); $k++) {
            if ($lines[$k] -match '<h2>|<h3>|<h4>') {
                $head = $lines[$k].Trim()
                break
            }
        }
        Write-Host ("  [{0,3}] id=section-{1}  headingAt+{2}: {3}" -f ($i + 1), $name, ($k - $i), $head)
    }
}
Write-Host ''
Write-Host '=== the JS switchSection cases in the same file (which sections actually RENDER = which are not dead) ==='
rg -o "case '[A-Za-z]+':" "$j" | ForEach-Object { $_.Trim() } | Sort-Object -Unique
Write-Host ''
Write-Host '=== does AdminDash realData actually provide each key (what must be non-empty for the 4 missing sections) ==='
rg -o 'realData\.put\("[A-Za-z]+"' 'src\main\java\org\example\esubmission\controller\AdminDash.java' | ForEach-Object { $_.Trim() } | Sort-Object -Unique
