Write-Host '=== AdminDash.java : what JSP does it forward to? ==='
rg -n 'RequestDispatcher|\.jsp|realData|serverData|getRequestDispatcher' src\main\java\org\example\esubmission\controller\AdminDash.java
Write-Host ''
Write-Host '=== AdminApiServlet doGet -> doGetOverview realData & which JSP ==='
rg -n 'getRequestDispatcher|\.jsp|realData.put|"admin|forward' src\main\java\org\example\esubmission\controller\AdminApiServlet.java
Write-Host ''
Write-Host '=== Which JSPs have a sidebar/switchSection nav (the REAL admin shells)? ==='
Get-ChildItem src\main\webapp\WEB-INF\pages -Filter *.jsp | Where-Object { (Get-Content $_ -Raw) -match 'switchSection|schoolAdmin|admin/' } | ForEach-Object { $_.Name }
Write-Host ''
Write-Host '=== Listeners/DAOs actually registered for ClassGroup/TeacherClass/ParentChild/StudentClass/Subject correctly? ==='
Get-ChildItem src\main\java\org\example\esubmission\dao\*.java | ForEach-Object { $_.BaseName }
Write-Host ''
Write-Host '=== headers in schoolAdmin.jsp that map to sidebar == Found ==='
rg -o 'id="section-[a-z-]+"' src\main\webapp\WEB-INF\pages\schoolAdmin.jsp | Sort-Object -Unique
