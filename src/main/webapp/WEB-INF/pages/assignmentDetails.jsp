<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Assignment Details - Igishyitsi</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: #3a3f47; min-height: 100vh; color: #f7fafc; }
        .nav { background: #2d3339; border-bottom: 1px solid rgba(255,255,255,0.08); padding: 16px 32px; display: flex; justify-content: space-between; align-items: center; }
        .brand { font-weight: 700; color: #3b82f6; letter-spacing: 2px; }
        .nav a { color: #a0aec0; text-decoration: none; font-size: 14px; }
        .nav a:hover { color: #3b82f6; }
        .container { max-width: 900px; margin: 40px auto; padding: 0 20px; }
        .card { background: #2d3339; border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 28px; margin-bottom: 20px; }
        h1 { font-size: 20px; margin-bottom: 6px; }
        .meta { display: flex; gap: 20px; color: #a0aec0; font-size: 13px; margin-bottom: 16px; flex-wrap: wrap; }
        .content { background: rgba(26,32,44,0.6); border-radius: 12px; padding: 18px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
        h2 { font-size: 14px; color: #a0aec0; text-transform: uppercase; letter-spacing: 1px; margin: 20px 0 10px; font-weight: 600; }
        table { width: 100%; border-collapse: collapse; font-size: 14px; }
        th, td { text-align: left; padding: 12px; border-bottom: 1px solid rgba(255,255,255,0.06); }
        th { color: #a0aec0; font-weight: 500; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; }
        .badge { display: inline-block; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600; }
        .badge.graded { background: rgba(16,185,129,0.15); color: #10b981; }
        .badge.pending { background: rgba(251,191,36,0.15); color: #fbbf24; }
        .empty { color: #718096; font-size: 14px; padding: 12px 0; }
    </style>
</head>
<body>
    <nav class="nav">
        <div class="brand">IGISHYITSI</div>
        <a href="${pageContext.request.contextPath}/teacher/dashboard">Back to Dashboard</a>
    </nav>
    <div class="container">
        <div class="card">
            <h1>${assignment.title}</h1>
            <div class="meta">
                <span>Class: ${assignment.className}</span>
                <span>Points: ${assignment.points}</span>
                <span>Deadline: ${assignment.formattedDeadline}</span>
            </div>
            <c:if test="${not empty assignment.description}">
                <h2>Instructions</h2>
                <div class="content">${assignment.description}</div>
            </c:if>
            <c:if test="${not empty assignment.googleFormUrl}">
                <h2>Google Form</h2>
                <div class="content"><a href="${assignment.googleFormUrl}" target="_blank" rel="noopener" style="color:#3b82f6;">${assignment.googleFormUrl}</a></div>
            </c:if>
        </div>

        <div class="card">
            <h2>Submissions (${submissions.size()})</h2>
            <c:choose>
                <c:when test="${empty submissions}">
                    <div class="empty">No submissions yet.</div>
                </c:when>
                <c:otherwise>
                    <table>
                        <thead>
                            <tr><th>Student</th><th>Submitted</th><th>Status</th><th>Grade</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="s" items="${submissions}">
                                <tr>
                                    <td>${s.student.name}</td>
                                    <td>${s.formattedSubmittedAt}</td>
                                    <td>
                                        <span class="badge ${s.grade != null ? 'graded' : 'pending'}">${s.grade != null ? 'Graded' : 'Pending'}</span>
                                    </td>
                                    <td><c:choose><c:when test="${s.grade != null}">${s.grade}/${assignment.points}</c:when><c:otherwise>--</c:otherwise></c:choose></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>

        <div class="card">
            <h2>Not Yet Submitted (${missingStudents.size()})</h2>
            <c:choose>
                <c:when test="${empty missingStudents}">
                    <div class="empty">All students have submitted.</div>
                </c:when>
                <c:otherwise>
                    <table>
                        <thead><tr><th>Student</th><th>Email</th></tr></thead>
                        <tbody>
                            <c:forEach var="m" items="${missingStudents}">
                                <tr><td>${m.name}</td><td>${m.email}</td></tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>