# audit.ps1
$ErrorActionPreference='Stop'
Write-Host '=== 1. realData keys forwarded by AdminApiServlet.doGet ==='
rg -n 'realData\.put\(' src\main\java\org\example\esubmission\controller\AdminApiServlet.java
Write-Host
Write-Host '=== 2. sidebar ids referenced in schoolAdmin.jsp (via switchSection) ==='
rg -o "switchSection\('[a-z-]+'\)" src\main\webapp\WEB-INF\pages\schoolAdmin.jsp | Sort-Object -Unique
Write-Host
Write-Host '=== 3. content-section ids in schoolAdmin.jsp ==='
rg -o 'id="section-[a-z-]+"' src\main\webapp\WEB-INF\pages\schoolAdmin.jsp | Sort-Object -Unique
Write-Host
Write-Host '=== 4. AdminApiServlet doGet forwards to which JSP ==='
rg -n 'RequestDispatcher|\.jsp' src\main\java\org\example\esubmission\controller\AdminApiServlet.java | rg -v '^\s*//'
Write-Host
Write-Host '=== 5. new Admin* module servlet classes registered in web.xml? ==='
rg -c 'AdminTeachersServlet|AdminStudentsServlet|AdminParentsServlet|AdminSubjectsServlet|AdminUsersServlet|AdminAddUserServlet|AdminEditUserServlet|AdminCreateAnnouncementServlet|NotificationApiServlet' src\main\webapp\WEB-INF\web.xml
