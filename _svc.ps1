$ErrorActionPreference='Stop'
$j = 'src\main\webapp\WEB-INF\pages\schoolAdmin.jsp'
$raw = Get-Content $j -Raw
Write-Host '=== sidebar switchSection targets in schoolAdmin.jsp sidebar ==='
[regex]::Matches($raw,'switchSection\(''([a-z-]+)''\)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique | ForEach-Object { Write-Host ("  sidebar -> {0}" -f $_) }
Write-Host
Write-Host '=== renderers/functions present at top level ==='
[regex]::Matches($raw,'function render([A-Za-z]+)\(') | ForEach-Object { "  render" + $_.Groups[1].Value } | Sort-Object -Unique
Write-Host
Write-Host '=== section ids that exist in markup ==='
[regex]::Matches($raw,'id="section-([a-z-]+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique | ForEach-Object { Write-Host ("  section-{0}" -f $_) }
Write-Host
Write-Host '=== which serverData keys the JS reads (contract) ==='
[regex]::Matches($raw,'serverData\.([a-zA-Z_]+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique | ForEach-Object { Write-Host ("  serverData.{0}" -f $_) }
Write-Host
Write-Host '=== occurrences of escapeHtml/escapeJs helpers + csrf fetch wrappers ==='
'r-i === /admin/<section> routes: what does layout.tag sidebar actually LINK, and what near-empty handlers do the JS fetch wrappers mention?' | Out-Null
Write-Host ('  ctxPattern count: ' + ([regex]::Matches($raw,"href=\"?\$\{pageContext.request.contextPath\}/admin/[a-zA-Z]+").Count))
