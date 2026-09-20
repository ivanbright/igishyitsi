$ErrorActionPreference='Stop'
$cs = Get-ChildItem src\main\java\org\example\esubmission\controller -Filter 'Admin*.java'
Write-Host '=== Admin*Servlet (non-Api) → forward JSP ==='
foreach ($f in $cs) {
    if ($f.Name -match 'AdminApi|AdminDash') { continue }
    $raw = Get-Content $f.FullName -Raw
    $fwd = @([regex]::Matches($raw, 'getRequestDispatcher\("([^"]+)"\)') | ForEach-Object { $_.Groups[1].Value }) | Sort-Object -Unique
    Write-Host ("{0,-30} -> {1}" -f $f.BaseName, ($fwd -join ' | ') )
}
Write-Host ''
Write-Host '=== JSPs actually on disk ==='
Get-ChildItem src\main\webapp\WEB-INF\pages -Filter *.jsp | Where-Object Name -in @('adminUsers.jsp','adminTeachers.jsp','adminStudents.jsp','adminParents.jsp','adminSubjects.jsp','adminClasses.jsp','schoolAdmin.jsp','adminDashboard.jsp','adminAnnouncements.jsp') | ForEach-Object Name
Write-Host ''
Write-Host '=== web.xml admin servlet-registrations (name:class + pattern) ==='
$xml = [xml](Get-Content -Raw 'src\main\webapp\WEB-INF\web.xml')
$serv = @{}; foreach($s in $xml.'web-app'.servlet){ $serv[$s.'servlet-name']=$s.'servlet-class' }
foreach($m in $xml.'web-app'.'servlet-mapping'){ if($m.'url-pattern' -match '/admin'){ Write-Host ("{0,-22} {1}  ->  {2}" -f $m.'servlet-name', $serv[$m.'servlet-name'], ($m.'url-pattern' -join ',')) } }
