<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verify Identity - Igishyitsi</title>
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
            max-width: 440px;
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
            line-height: 1.5;
        }

        .auth-card {
            background: var(--bg-card);
            border-radius: var(--radius-lg);
            box-shadow: var(--shadow-lg);
            border: 1px solid var(--border-color);
            padding: 32px;
        }

        .code-inputs {
            display: flex;
            gap: 12px;
            justify-content: space-between;
            margin-bottom: 32px;
        }

        .code-inputs input {
            width: 48px;
            height: 56px;
            background: var(--input-bg);
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            text-align: center;
            font-size: 24px;
            font-weight: 600;
            color: var(--text-primary);
            transition: all 0.2s;
            box-shadow: var(--shadow-sm);
        }

        .code-inputs input:focus {
            outline: none;
            border-color: var(--focus-ring);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
        }

        .btn-submit {
            width: 100%;
            padding: 12px 16px;
            background: var(--primary-color);
            color: white;
            border: none;
            border-radius: var(--radius-md);
            font-size: 15px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s;
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
            color: var(--focus-ring);
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
            <h1 class="brand-title">Two-step verification</h1>
            <p class="brand-subtitle">We've sent a 6-digit verification code to <strong>${sessionScope.pendingUser.email}</strong>. Please enter it below.</p>
        </div>

        <div class="auth-card">
            <form action="${pageContext.request.contextPath}/verify" method="POST">
                <div class="code-inputs" id="otp-inputs">
                    <input type="text" maxlength="1" pattern="\d*" required autofocus>
                    <input type="text" maxlength="1" pattern="\d*" required>
                    <input type="text" maxlength="1" pattern="\d*" required>
                    <input type="text" maxlength="1" pattern="\d*" required>
                    <input type="text" maxlength="1" pattern="\d*" required>
                    <input type="text" maxlength="1" pattern="\d*" required>
                </div>
                <input type="hidden" name="code" id="finalCode">
                <button type="submit" class="btn-submit">Verify & Continue</button>
            </form>
        </div>

        <div class="auth-footer">
            Didn't receive the code? <a href="#" onclick="resendCode()">Click to resend</a>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/js/toast.js"></script>
    <script>
        const inputs = document.querySelectorAll('#otp-inputs input');
        const finalCodeInput = document.getElementById('finalCode');
        const form = document.querySelector('form');

        inputs.forEach((input, index) => {
            // Handle Paste
            input.addEventListener('paste', (e) => {
                e.preventDefault();
                const pastedData = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, inputs.length);
                if (pastedData) {
                    for (let i = 0; i < pastedData.length; i++) {
                        inputs[i].value = pastedData[i];
                    }
                    updateFinalCode();
                    if (pastedData.length === inputs.length) {
                        inputs[inputs.length - 1].focus();
                        form.submit();
                    } else {
                        if (pastedData.length < inputs.length) {
                            inputs[pastedData.length].focus();
                        }
                    }
                }
            });

            input.addEventListener('input', (e) => {
                if (e.target.value.length > 1) {
                    e.target.value = e.target.value.slice(0, 1);
                }
                if (e.target.value && index < inputs.length - 1) {
                    inputs[index + 1].focus();
                }
                updateFinalCode();
                
                // Auto submit when all filled
                if (Array.from(inputs).every(inp => inp.value.length === 1)) {
                    form.submit();
                }
            });

            input.addEventListener('keydown', (e) => {
                if (e.key === 'Backspace' && !e.target.value && index > 0) {
                    inputs[index - 1].focus();
                }
            });
        });

        function updateFinalCode() {
            let code = "";
            inputs.forEach(input => code += input.value);
            finalCodeInput.value = code;
        }

        function resendCode() {
            window.location.href = '${pageContext.request.contextPath}/verify?action=resend';
        }

        document.addEventListener('DOMContentLoaded', function () {
            const errorMsg = "${error}";
            if (errorMsg && errorMsg.trim() !== "" && errorMsg !== "null") {
                showMessage('error', errorMsg);
            }
            const infoMsg = "${message}";
            if (infoMsg && infoMsg.trim() !== "" && infoMsg !== "null") {
                showMessage('success', infoMsg);
            }
        });
    </script>
</body>
</html>