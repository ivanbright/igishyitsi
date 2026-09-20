<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Onboarding - Igishyitsi</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: #3a3f47; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
        .card { width: 100%; max-width: 460px; background: #2d3339; border: 1px solid rgba(255,255,255,0.08); border-radius: 24px; padding: 40px; }
        .brand { font-weight: 700; color: #3b82f6; text-align: center; margin-bottom: 20px; font-size: 1.2rem; letter-spacing: 2px; }
        h1 { color: #f7fafc; font-size: 22px; text-align: center; margin-bottom: 6px; }
        p.sub { color: #a0aec0; font-size: 14px; text-align: center; margin-bottom: 28px; line-height: 1.5; }
        .field { margin-bottom: 18px; }
        .field label { display: block; color: #a0aec0; font-size: 13px; margin-bottom: 6px; }
        .field input { width: 100%; height: 44px; background: rgba(26,32,44,0.8); border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 0 14px; color: #f7fafc; font-size: 14px; }
        .field input:focus { outline: none; border-color: #3b82f6; box-shadow: 0 0 12px rgba(59,130,246,0.25); }
        .btn { width: 100%; height: 48px; background: #3b82f6; border: none; border-radius: 12px; color: white; font-size: 16px; font-weight: 600; cursor: pointer; }
        .btn:hover { background: #2563eb; }
        .msg { padding: 12px 16px; border-radius: 10px; margin-bottom: 20px; font-size: 14px; text-align: center; }
        .msg.error { background: rgba(239,68,68,0.15); color: #ef4444; }
    </style>
</head>
<body>
    <div class="card">
        <div class="brand">IGISHYITSI</div>
        <h1>Welcome, ${user.name}</h1>
        <p class="sub">One quick step to finish setting up your account.</p>

        <c:if test="${not empty error}">
            <div class="msg error">${error}</div>
        </c:if>

        <form action="${pageContext.request.contextPath}/onboarding" method="POST">
            <input type="hidden" name="action" value="updateProfile">
            <div class="field">
                <label>Full Name</label>
                <input type="text" name="name" value="${user.name}" required>
            </div>
            <div class="field">
                <label>Phone</label>
                <input type="tel" name="phone" value="${user.phone}">
            </div>
            <button type="submit" class="btn">Continue to Dashboard</button>
        </form>
    </div>
</body>
</html>