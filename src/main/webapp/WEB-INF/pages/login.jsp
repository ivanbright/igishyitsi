<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign in to Igishyitsi</title>
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
            --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
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

        .social-auth {
            display: flex;
            flex-direction: column;
            gap: 12px;
            margin-bottom: 24px;
        }

        .btn-social {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            width: 100%;
            padding: 10px 16px;
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            font-size: 14px;
            font-weight: 500;
            color: var(--text-primary);
            cursor: pointer;
            transition: all 0.2s;
            box-shadow: var(--shadow-sm);
        }

        .btn-social:hover {
            background: #F9FAFB;
            border-color: #D1D5DB;
        }

        .divider {
            display: flex;
            align-items: center;
            text-align: center;
            margin-bottom: 24px;
        }

        .divider::before, .divider::after {
            content: '';
            flex: 1;
            border-bottom: 1px solid var(--border-color);
        }

        .divider span {
            padding: 0 12px;
            font-size: 12px;
            color: var(--text-muted);
            text-transform: uppercase;
            font-weight: 500;
            letter-spacing: 0.05em;
        }

        .form-group {
            margin-bottom: 20px;
        }

        .form-label {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 6px;
            font-size: 14px;
            font-weight: 500;
            color: var(--text-primary);
        }

        .form-label a {
            font-size: 13px;
            color: var(--focus-ring);
            text-decoration: none;
            font-weight: 500;
        }

        .form-label a:hover {
            text-decoration: underline;
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

        .form-input::placeholder {
            color: var(--text-muted);
        }

        .form-input:focus {
            outline: none;
            border-color: var(--focus-ring);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }

        .captcha-wrapper {
            display: flex;
            gap: 12px;
            align-items: center;
            margin-top: 8px;
        }

        .captcha-img {
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            height: 42px;
            width: 120px;
            cursor: pointer;
            object-fit: cover;
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

        .btn-submit:focus {
            outline: none;
            box-shadow: 0 0 0 3px var(--ring-color);
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
            <h1 class="brand-title">Welcome back</h1>
            <p class="brand-subtitle">Sign in to your Igishyitsi account</p>
        </div>

        <div class="auth-card">
            <form method="post" action="${pageContext.request.contextPath}/login">
                <div class="form-group">
                    <label class="form-label" for="email">Email address</label>
                    <input type="email" id="email" name="email" class="form-input" placeholder="you@example.com" required autofocus>
                </div>

                <div class="form-group">
                    <div class="form-label">
                        <label for="password">Password</label>
                        <a href="${pageContext.request.contextPath}/forgotPassword">Forgot password?</a>
                    </div>
                    <div style="position: relative;">
                        <input type="password" id="password" name="password" class="form-input" placeholder="••••••••" required style="padding-right: 40px;">
                        <button type="button" id="togglePassword" style="position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: none; border: none; cursor: pointer; color: var(--text-muted); padding: 4px; display: flex;">
                            <svg id="eyeIcon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                        </button>
                    </div>
                </div>

                <div class="form-group">
                    <label class="form-label" for="captcha">Security check</label>
                    <div class="captcha-wrapper">
                        <input type="text" id="captcha" name="captcha" class="form-input" placeholder="Enter code" required>
                        <img src="${pageContext.request.contextPath}/captcha" class="captcha-img" title="Click to refresh" onclick="this.src='${pageContext.request.contextPath}/captcha?'+new Date().getTime()">
                    </div>
                </div>

                <button type="submit" class="btn-submit">Sign in</button>
            </form>
        </div>

        <div class="auth-footer">
            Don't have an account? <a href="${pageContext.request.contextPath}/signup">Sign up</a>
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
            
            const togglePassword = document.getElementById('togglePassword');
            if(togglePassword) {
                togglePassword.addEventListener('click', function () {
                    const password = document.getElementById('password');
                    const icon = document.getElementById('eyeIcon');
                    if (password.getAttribute('type') === 'password') {
                        password.setAttribute('type', 'text');
                        icon.innerHTML = '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line>';
                    } else {
                        password.setAttribute('type', 'password');
                        icon.innerHTML = '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle>';
                    }
                });
            }
        });
    </script>
</body>
</html>