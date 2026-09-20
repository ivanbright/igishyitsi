Write-Host '=== AdminDash.java: forward + realData keys (authoritative from disk) ==='
$p = 'src\main\java\org\example\esubmission\controller\AdminDash.java'
Select-String -Path $p -Pattern 'getRequestDispatcher|request.getDispatcher|\.jsp|realData.put|forward\(' | ForEach-Object { $_.Line.Trim() }
Write-Host ''
Write-Host '=== does the target JSP exist on disk? ==='
$all = Get-ChildItem src\main\webapp\WEB-INF\pages -Filter *.jsp | Select-Object -ExpandProperty Name
Write-Host ($all -join ', ')
