<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Class Students - Igishyitsi</title>
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
        .card { background: #2d3339; border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; padding: 28px; }
        h1 { font-size: 20px; margin-bottom: 4px; }
        .sub { color: #a0aec0; font-size: 14px; margin-bottom: 20px; }
        table { width: 100%; border-collapse: collapse; font-size: 14px; }
        th, td { text-align: left; padding: 12px; border-bottom: 1px solid rgba(255,255,255,0.06); }
        th { color: #a0aec0; font-weight: 500; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; }
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
            <h1>${className}</h1>
            <div class="sub">${students.size()} student(s) enrolled</div>
            <c:choose>
                <c:when test="${empty students}">
                    <div class="empty">No students are enrolled in this class yet.</div>
                </c:when>
                <c:otherwise>
                    <table>
                        <thead><tr><th>Name</th><th>Email</th></tr></thead>
                        <tbody>
                            <c:forEach var="s" items="${students}">
                                <tr><td>${s.name}</td><td>${s.email}</td></tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>