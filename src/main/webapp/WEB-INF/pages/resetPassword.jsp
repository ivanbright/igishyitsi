<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Set New Password - Igishyitsi</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/toast.css">
    <style>
        :root {
            --bg-body: #F9FAFB;
            --bg-card: #FFFFFF;
            --text-primary: #111827;
            --text-secondary: #4B5563;
            --text-muted: #9CA3AF;
            --border-color: #E5E7EB;
            --input-bg: #FFFFFF;
            --primary-color: #0F172A;
            --primary-hover: #1E293B;
            --ring-color: rgba(15, 23, 42, 0.1);
            --focus-ring: #2563EB;
            --radius-md: 8px;
            --radius-lg: 12px;
            --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
            --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -2px rgba(0, 0, 0, 0.025);
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--bg-body);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 20px;
            background-image: radial-gradient(#E5E7EB 1px, transparent 1px);
            background-size: 20px 20px;
        }

        .auth-wrapper {
            width: 100%;
            max-width: 400px;
            margin: 0 auto;
        }

        .brand-header {
            text-align: center;
            margin-bottom: 32px;
        }

        .brand-logo {
            width: 48px;
            height: 48px;
            background: var(--primary-color);
            border-radius: 12px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 24px;
            font-weight: 700;
            margin-bottom: 16px;
            box-shadow: var(--shadow-sm);
        }

        .brand-title {
            font-size: 24px;
            font-weight: 700;
            letter-spacing: -0.025em;
            margin-bottom: 8px;
        }

        .brand-subtitle {
            font-size: 14px;
            color: var(--text-secondary);
        }

        .auth-card {
            background: var(--bg-card);
            border-radius: var(--radius-lg);
            box-shadow: var(--shadow-lg);
            border: 1px solid var(--border-color);
            padding: 32px;
        }

        .form-group {
            margin-bottom: 20px;
        }

        .form-label {
            display: block;
            margin-bottom: 6px;
            font-size: 14px;
            font-weight: 500;
            color: var(--text-primary);
        }

        .form-input {
            width: 100%;
            padding: 10px 14px;
            background: var(--input-bg);
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            font-size: 14px;
            color: var(--text-primary);
            transition: all 0.2s;
            font-family: inherit;
            box-shadow: var(--shadow-sm);
        }

        .form-input:focus {
            outline: none;
            border-color: var(--focus-ring);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }

        .btn-submit {
            width: 100%;
            padding: 10px 16px;
            background: var(--primary-color);
            color: white;
            border: none;
            border-radius: var(--radius-md);
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s;
            margin-top: 8px;
            box-shadow: var(--shadow-sm);
        }

        .btn-submit:hover {
            background: var(--primary-hover);
        }

        .auth-footer {
            text-align: center;
            margin-top: 24px;
            font-size: 14px;
            color: var(--text-secondary);
        }

        .auth-footer a {
            color: var(--text-primary);
            font-weight: 600;
            text-decoration: none;
        }

        .auth-footer a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="auth-wrapper">
        <div class="brand-header">
            <div class="brand-logo">Ig</div>
            <h1 class="brand-title">Set new password</h1>
            <p class="brand-subtitle">Please enter your new password below.</p>
        </div>

        <div class="auth-card">
            <form method="post" action="${pageContext.request.contextPath}/resetPassword">
                
                <div class="form-group">
                    <label class="form-label" for="password">New Password</label>
                    <input type="password" id="password" name="password" class="form-input" placeholder="••••••••" required autofocus>
                </div>
                
                <div class="form-group">
                    <label class="form-label" for="confirmPassword">Confirm Password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" class="form-input" placeholder="••••••••" required>
                </div>

                <button type="submit" class="btn-submit">Update password</button>
            </form>
        </div>

        <div class="auth-footer">
            <a href="${pageContext.request.contextPath}/login">Back to sign in</a>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/js/toast.js"></script>
    <script>
        document.addEventListener('DOMContentLoaded', function () {
            const errorMsg = "${error}";
            if (errorMsg && errorMsg.trim() !== "" && errorMsg !== "null") {
                showMessage('error', errorMsg);
            }
            const successMsg = "${success}";
            if (successMsg && successMsg.trim() !== "" && successMsg !== "null") {
                showMessage('success', successMsg);
            }
        });
    </script>
</body>
</html>