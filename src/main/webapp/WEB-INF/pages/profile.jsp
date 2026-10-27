<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Profile - Igishyitsi</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: #3a3f47; min-height: 100vh; color: #f7fafc; }
        .nav { background: #2d3339; border-bottom: 1px solid rgba(255,255,255,0.08); padding: 16px 32px; display: flex; justify-content: space-between; align-items: center; }
        .brand { font-weight: 700; color: #3b82f6; letter-spacing: 2px; }
        .nav a { color: #a0aec0; text-decoration: none; font-size: 14px; }
        .nav a:hover { color: #3b82f6; }
        .container { max-width: 800px; margin: 40px auto; padding: 0 20px; }
        .card { background: #2d3339; border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 28px; margin-bottom: 20px; }
        h1 { font-size: 22px; margin-bottom: 4px; }
        .sub { color: #a0aec0; font-size: 14px; margin-bottom: 24px; }
        .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
        .stat { background: rgba(26,32,44,0.6); border-radius: 12px; padding: 20px; text-align: center; }
        .stat .num { font-size: 28px; font-weight: 700; color: #3b82f6; }
        .stat .label { color: #a0aec0; font-size: 13px; margin-top: 4px; }
        .field { margin-bottom: 16px; }
        .field label { display: block; color: #a0aec0; font-size: 13px; margin-bottom: 6px; }
        .field input { width: 100%; height: 44px; background: rgba(26,32,44,0.8); border: 1px solid rgba(255,255,255,0.1); border-radius: 10px; padding: 0 14px; color: #f7fafc; font-size: 14px; }
        .field input:focus { outline: none; border-color: #3b82f6; }
        .btn { background: #3b82f6; border: none; color: white; border-radius: 10px; padding: 12px 24px; font-size: 14px; font-weight: 600; cursor: pointer; }
        .btn:hover { background: #2563eb; }
        .msg { padding: 12px 16px; border-radius: 10px; margin-bottom: 20px; font-size: 14px; }
        .msg.success { background: rgba(16,185,129,0.15); color: #10b981; }
        .msg.error { background: rgba(239,68,68,0.15); color: #ef4444; }
        table { width: 100%; border-collapse: collapse; font-size: 14px; }
        th, td { text-align: left; padding: 12px; border-bottom: 1px solid rgba(255,255,255,0.06); }
        th { color: #a0aec0; font-weight: 500; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; }
    </style>
</head>
<body>
    <nav class="nav">
        <div class="brand">IGISHYITSI</div>
        <c:choose>
            <c:when test="${user.role == 'SUPER_ADMIN'}"><a href="${pageContext.request.contextPath}/super/dashboard">Back to Dashboard</a></c:when>
            <c:when test="${user.role == 'ADMIN'}"><a href="${pageContext.request.contextPath}/admin/dashboard">Back to Dashboard</a></c:when>
            <c:when test="${user.role == 'TEACHER'}"><a href="${pageContext.request.contextPath}/teacher/dashboard">Back to Dashboard</a></c:when>
            <c:when test="${user.role == 'STUDENT'}"><a href="${pageContext.request.contextPath}/student/dashboard">Back to Dashboard</a></c:when>
            <c:otherwise><a href="${pageContext.request.contextPath}/logout">Sign Out</a></c:otherwise>
        </c:choose>
    </nav>
    <div class="container">
        <c:if test="${not empty sessionScope.error}">
            <div class="msg error">${sessionScope.error}</div>
            <% session.removeAttribute("error"); %>
        </c:if>
        <c:if test="${not empty sessionScope.message}">
            <div class="msg success">${sessionScope.message}</div>
            <% session.removeAttribute("message"); %>
        </c:if>
        <h1>${user.name}</h1>
        <div class="sub">${user.email} &middot; ${user.role}<c:if test="${not empty schoolName}"> &middot; ${schoolName}</c:if></div>

        <c:if test="${not empty schoolName}">
        <div class="grid">
            <div class="stat"><div class="num">${totalSubmissions}</div><div class="label">Total Submissions</div></div>
            <div class="stat"><div class="num">${gradedSubmissions}</div><div class="label">Graded</div></div>
            <div class="stat"><div class="num">${averageGrade eq 0 ? '--' : averageGrade}</div><div class="label">Average Grade</div></div>
        </div>
        </c:if>

        <div class="card">
            <h2 style="font-size:16px; margin-bottom:20px;">Edit Profile</h2>
              <form action="${pageContext.request.contextPath}/profile" method="POST">
                  <input type="hidden" name="csrfToken" value="${csrfToken}">
                  <input type="hidden" name="action" value="updateProfile">
                <div class="field">
                    <label>Full Name</label>
                    <input type="text" name="name" value="${user.name}" required>
                </div>
                <div class="field">
                    <label>Phone</label>
                    <input type="tel" name="phone" value="${user.phone}">
                </div>
                <button type="submit" class="btn">Save Changes</button>
            </form>
        </div>

        <div class="card">
            <h2 style="font-size:16px; margin-bottom:6px;">Change Password</h2>
            <p style="font-size:13px; color:rgba(255,255,255,0.5); margin-bottom:20px;">
                Choose a password of at least 8 characters that you do not use anywhere else.
            </p>
            <form action="${pageContext.request.contextPath}/profile" method="POST">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="changePassword">
                <div class="field">
                    <label>Current Password</label>
                    <input type="password" name="currentPassword" required autocomplete="current-password">
                </div>
                <div class="field">
                    <label>New Password</label>
                    <input type="password" name="newPassword" required minlength="8" autocomplete="new-password">
                </div>
                <div class="field">
                    <label>Confirm New Password</label>
                    <input type="password" name="confirmPassword" required minlength="8" autocomplete="new-password">
                </div>
                <button type="submit" class="btn">Change Password</button>
            </form>
        </div>

        <c:if test="${not empty submissions}">
            <div class="card">
                <h2 style="font-size:16px; margin-bottom:16px;">Recent Submissions</h2>
                <table>
                    <thead>
                        <tr><th>Assignment</th><th>Submitted</th><th>Status</th><th>Grade</th></tr>
                    </thead>
                    <tbody>
                        <c:forEach var="s" items="${submissions}">
                            <tr>
                                <td>${s.assignment.title}</td>
                                <td>${s.formattedSubmittedAt}</td>
                                <td>${s.status}</td>
                                <td><c:choose><c:when test="${s.grade != null}">${s.grade}/${s.assignment.points}</c:when><c:otherwise>--</c:otherwise></c:choose></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>
    </div>
</body>
</html>