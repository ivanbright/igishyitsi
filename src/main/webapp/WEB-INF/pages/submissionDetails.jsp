<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Submission Details - Igishyitsi</title>
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
        h1 { font-size: 20px; margin-bottom: 6px; }
        .meta { display: flex; gap: 20px; color: #a0aec0; font-size: 13px; margin-bottom: 20px; flex-wrap: wrap; }
        .badge { display: inline-block; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600; }
        .badge.graded { background: rgba(16,185,129,0.15); color: #10b981; }
        .badge.pending { background: rgba(251,191,36,0.15); color: #fbbf24; }
        .badge.late { background: rgba(239,68,68,0.15); color: #ef4444; }
        h2 { font-size: 14px; color: #a0aec0; text-transform: uppercase; letter-spacing: 1px; margin: 20px 0 10px; font-weight: 600; }
        .content { background: rgba(26,32,44,0.6); border-radius: 12px; padding: 18px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
        .grade-box { background: rgba(59,130,246,0.12); border: 1px solid rgba(59,130,246,0.3); border-radius: 12px; padding: 18px; }
        .grade-box .num { font-size: 32px; font-weight: 700; color: #3b82f6; }
        .grade-box .sub { color: #a0aec0; font-size: 13px; }
        .btn { background: #3b82f6; border: none; color: white; border-radius: 10px; padding: 12px 24px; font-size: 14px; font-weight: 600; cursor: pointer; text-decoration: none; display: inline-block; }
        .btn:hover { background: #2563eb; }
        a { color: #3b82f6; }
    </style>
</head>
<body>
    <nav class="nav">
        <div class="brand">IGISHYITSI</div>
        <a href="${pageContext.request.contextPath}${backLink != null ? backLink : '/mySubmissions'}">Back</a>
    </nav>
    <div class="container">
        <div class="card">
            <h1>${submission.assignment.title}</h1>
            <div class="meta">
                <span>Submitted: ${submission.formattedSubmittedAt}</span>
                <span>Class: ${submission.assignment.className}</span>
                <span class="badge ${submission.status == 'LATE' ? 'late' : ''}">${submission.status}</span>
            </div>

            <h2>Your Answer</h2>
            <c:choose>
                <c:when test="${not empty submission.content}">
                    <div class="content">${submission.content}</div>
                </c:when>
                <c:otherwise>
                    <div class="content" style="color:#718096;">No text answer provided.</div>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty submission.fileUrl}">
                <h2>Attachment</h2>
                <form action="${pageContext.request.contextPath}/submissionDetails" method="POST">
                    <input type="hidden" name="action" value="download">
                    <input type="hidden" name="submissionId" value="${submission.id}">
                    <button type="submit" class="btn">Download File</button>
                </form>
            </c:if>

            <c:if test="${not empty submission.formResponseUrl}">
                <h2>Form Response</h2>
                <div class="content"><a href="${submission.formResponseUrl}" target="_blank" rel="noopener">${submission.formResponseUrl}</a></div>
            </c:if>
        </div>

        <div class="card">
            <c:choose>
                <c:when test="${submission.grade != null}">
                    <h2>Grade &amp; Feedback</h2>
                    <div class="grade-box">
                        <div class="num">${submission.grade} <span style="font-size:16px; font-weight:400;">/ ${submission.assignment.points}</span></div>
                        <c:if test="${submission.percentage != null}">
                            <div class="sub">${submission.percentage.intValue()}%</div>
                        </c:if>
                    </div>
                    <c:if test="${not empty submission.feedback}">
                        <h2>Feedback</h2>
                        <div class="content">${submission.feedback}</div>
                    </c:if>
                </c:when>
                <c:otherwise>
                    <h2>Grade &amp; Feedback</h2>
                    <div class="content" style="color:#718096;">Not graded yet. Check back soon.</div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>