# Build + deploy + run Igishyitsi on Apache Tomcat
# Usage:  powershell -ExecutionPolicy Bypass -File .\run.ps1
$ErrorActionPreference = 'Stop'

$tomcat = 'C:\apache-tomcat-10.1.50'
$war    = Join-Path $PSScriptRoot 'target\igishyitsi.war'

if (-not (Test-Path $tomcat)) { Write-Error "Tomcat not found at: $tomcat" }

Write-Host '[1/3] Building WAR...'
Push-Location $PSScriptRoot
try {
    .\mvnw.cmd clean package
} finally {
    Pop-Location
}

if (-not (Test-Path $war)) { Write-Error 'WAR build failed - check Maven output above.' }

Write-Host '[2/3] Deploying WAR to Tomcat...'
Copy-Item $war (Join-Path $tomcat 'webapps\igishyitsi.war') -Force

Write-Host '[3/3] Starting Tomcat...'
Write-Host 'App:  http://localhost:8080/igishyitsi/'
Write-Host 'Logs: C:\apache-tomcat-10.1.50\logs\catalina.<date>.log'
Write-Host 'Stop: Ctrl+C (and close this window)'
$env:CATALINA_HOME = $tomcat
$env:CATALINA_BASE = $tomcat
Push-Location $tomcat
try {
    & (Join-Path $tomcat 'bin\catalina.bat') run
} finally {
    Pop-Location
}