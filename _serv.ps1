$ErrorActionPreference='Stop'
Get-ChildItem src\main\java\org\example\esubmission\controller -Filter 'Admin*Servlet.java' | Sort-Object Name | ForEach-Object {
    $c = Get-Content $_.FullName -Raw
    $fwd = [regex]::Match($c, 'getRequestDispatcher\("([^"]+)"\)').Groups[1].Value
    $class = [regex]::Match($c, 'class (\w+)').Groups[1].Value
    $fields = [regex]::Matches($c, 'private \w+DAO (\w+);') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
    $reqId = [regex]::Match($c, '\.php"([^"]+)"').Groups[1].Value
    Write-Host ("{0,-22} fwd-> {1,-32} DAOs: {2}" -f $class, ($fwd -replace '.*pages\\?\/?',''), ($fields -join ','))
}
