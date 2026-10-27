# P0 — CSRF header: web.xml + JS wiring

Requirement (`PRODUCT_RECONSTRUCTION.md`): every mutating admin/XHR request must carry a
CSRF token. This is the currently-incomplete surface.

## Source of truth

`SecurityUtil.csrfValid(request)` (util/SecurityUtil.java) validates `X-CSRF-Token`
header OR `csrfToken` request param against `session.csrfToken`.

## Current gaps (from live grep)

| Surface | Posting JS | Sends header? | Verdict |
| ------- | ---------- | ------------- | ------- |
| `/admin/dashboard` (AdminDash) | admin JS `fetch` | **No** | GAP |
| `/api/admin/*` (AdminApi + AdminUsers + AdminClasses + AdminEditUser + AdminCreateAnnouncement + AllocateStudent) | schoolAdmin.jsp + admin JS | **No** | GAP |
| `/super/dashboard` + `/api/super/schools/*` (SchoolApiServlet) | superAdmin.jsp JS | **Yes** (`X-CSRF-Token` header) | OK |
| `/api/admin/notifications` markRead | notification JS | **No** | GAP |

## Fix template (schoolAdmin.jsp + admin JS + notification JS)

```js
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content
    || sessionStorage.getItem('csrfToken')
    || (window.__csrf ? window.__csrf : '');

fetch(url, {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'X-CSRF-Token': csrfToken,
        ...(extraHeaders || {})
    },
    body: JSON.stringify(payload)
});
```

The `X-CSRF-Token` must be read from the **page/session** — never hardcoded. The login/signup
response should set it; CSRF tokens are session-bound and rotate on login.
