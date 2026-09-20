<%@ tag description="Dashboard Layout Template" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="role" required="true" type="java.lang.String" %>
<%@ attribute name="pageTitle" required="true" type="java.lang.String" %>
<%@ attribute name="activeSection" required="true" type="java.lang.String" %>
<%@ attribute name="topBarActions" fragment="true" %>
<%@ attribute name="extraScripts" fragment="true" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle} - Igishyitsi</title>
    <meta name="description" content="Igishyitsi e-submission platform dashboard">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css?v=3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/toast.css">
</head>
<body>
    <div id="toastContainer" class="toast-container"></div>

    <div class="app-layout">
        <!-- Sidebar Navigation -->
        <aside class="sidebar" id="sidebar">
            <div class="sidebar-header">
                <div class="logo">
                    <span class="logo-text">Igishyitsi</span>
                </div>
                <button class="sidebar-toggle" id="sidebarToggle" aria-label="Toggle sidebar">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
                </button>
            </div>
            
            <nav class="sidebar-nav">
                <c:choose>
                    <c:when test="${role == 'STUDENT'}">
                        <a href="${pageContext.request.contextPath}/studentDash" class="nav-item ${activeSection == 'overview' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
                            <span>Dashboard</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/studentDash?section=assignments" class="nav-item ${activeSection == 'assignments' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                            <span>Assignments</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/studentDash?section=submissions" class="nav-item ${activeSection == 'submissions' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                            <span>My Submissions</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/studentDash?section=grades" class="nav-item ${activeSection == 'grades' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
                            <span>Grades</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/studentDash?section=classes" class="nav-item ${activeSection == 'classes' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                            <span>My Classes</span>
                        </a>
                    </c:when>
                    <c:when test="${role == 'TEACHER'}">
                        <a href="${pageContext.request.contextPath}/teacher/dashboard" class="nav-item ${activeSection == 'overview' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
                            <span>Dashboard</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/teacher/dashboard?section=classes" class="nav-item ${activeSection == 'classes' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                            <span>My Classes</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/teacher/dashboard?section=assignments" class="nav-item ${activeSection == 'assignments' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                            <span>Assignments</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/teacher/dashboard?section=submissions" class="nav-item ${activeSection == 'submissions' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                            <span>Submissions</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/teacher/dashboard?section=roster" class="nav-item ${activeSection == 'roster' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                            <span>Class Roster</span>
                        </a>
                    </c:when>
                    <c:when test="${role == 'ADMIN'}">
                        <a href="${pageContext.request.contextPath}/admin/dashboard" class="nav-item ${activeSection == 'overview' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
                            <span>Dashboard</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/dashboard?section=users" class="nav-item ${activeSection == 'users' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                            <span>Users</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/dashboard?section=teachers" onclick="goAdminSection('teachers'); return false;" class="nav-item ${activeSection == 'teachers' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
                            <span>Teachers</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/dashboard?section=students" class="nav-item ${activeSection == 'students' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 9L12 4 2 9l10 5 10-5z"/><path d="M6 11v5c0 1.5 3 3 6 3s6-1.5 6-3v-5"/></svg>
                            <span>Students</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/dashboard?section=parents" class="nav-item ${activeSection == 'parents' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                            <span>Parents</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/dashboard?section=classes" class="nav-item ${activeSection == 'classes' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
                            <span>Classes</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/dashboard?section=subjects" class="nav-item ${activeSection == 'subjects' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
                            <span>Subjects</span>
                        </a>
                        <a href="${pageContext.request.contextPath}/admin/dashboard?section=announcements" class="nav-item ${activeSection == 'announcements' ? 'active' : ''}">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 17H2a3 3 0 0 0 3-3V9a7 7 0 0 1 14 0v5a3 3 0 0 0 3 3zm-8.27 4a2 2 0 0 1-3.46 0"/></svg>
                            <span>Announcements</span>
                        </a>
                    </c:when>
                </c:choose>
            </nav>
            <div class="sidebar-footer">
                <a href="${pageContext.request.contextPath}/logout" class="nav-item logout-btn" id="logoutBtn">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                    <span>Logout</span>
                </a>
            </div>
        </aside>

        <!-- Main Content -->
        <main class="main-content">
            <!-- Top Bar -->
            <header class="top-bar">
                <div class="top-bar-left">
                    <button class="mobile-menu-btn" id="mobileMenuBtn" aria-label="Open menu">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
                    </button>
                </div>
                <div class="top-bar-actions">
                    <jsp:invoke fragment="topBarActions"/>
                    <div class="notification-bell" id="notificationBell">
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                        <span class="notification-badge" id="notifBadge" style="display:none;">0</span>
                    </div>
                    <div class="user-profile-top">
                        <div class="user-avatar-top" id="userAvatarTop"></div>
                        <div class="user-info-top">
                            <span class="user-name-top" id="userNameTop"></span>
                            <span class="user-role-top">${role eq 'STUDENT' ? 'Student' : (role eq 'TEACHER' ? 'Teacher' : 'Administrator')}</span>
                        </div>
                    </div>
                </div>
            </header>

            <!-- Notification Panel -->
            <div class="notification-panel" id="notificationPanel" style="display:none;">
                <div class="notif-panel-header">
                    <h3>Notifications</h3>
                    <button class="notif-mark-read" id="markAllReadBtn">Mark all read</button>
                </div>
                <div class="notif-list" id="notifList">
                    <div class="empty-state-small">No notifications</div>
                </div>
            </div>

            <!-- Page Specific Content -->
            <div id="pageContentWrapper" style="display: flex; flex-direction: column; flex: 1; overflow-y: auto;">
                <jsp:doBody/>
            </div>
            
        </main>
    </div>

    <!-- Global Scripts -->
    <script src="${pageContext.request.contextPath}/js/toast.js"></script>
    <script>
        // Shared layout scripts for Notifications, WebSockets, and User profile
        document.addEventListener('DOMContentLoaded', function() {
            // Setup User Info
            if (window.serverData && window.serverData.currentUser) {
                const name = window.serverData.currentUser.name || '${role}';
                const avatarTop = document.getElementById('userAvatarTop');
                if (avatarTop) {
                    avatarTop.textContent = name.charAt(0).toUpperCase();
                    avatarTop.style.background = getAvatarColor(name);
                }
                const userNameTop = document.getElementById('userNameTop');
                if (userNameTop) {
                    userNameTop.textContent = name;
                }
            }

            // Mobile Menu
            const mobileBtn = document.getElementById('mobileMenuBtn');
            const sidebar = document.getElementById('sidebar');
            if (mobileBtn && sidebar) {
                mobileBtn.addEventListener('click', () => {
                    sidebar.classList.toggle('open');
                });
            }

            setupSharedNotifications();
            setupSharedWebSocket();
        });

        function getAvatarColor(str) {
            let hash = 0;
            for (let i = 0; i < str.length; i++) {
                hash = str.charCodeAt(i) + ((hash << 5) - hash);
            }
            const h = hash % 360;
            return 'hsl(' + h + ', 70%, 50%)';
        }

        function setupSharedNotifications() {
            const bell = document.getElementById('notificationBell');
            const panel = document.getElementById('notificationPanel');
            const contextPath = '${pageContext.request.contextPath}';
            
            if (!bell || !panel) return;
            
            bell.addEventListener('click', function(e) {
                e.stopPropagation();
                panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
                if (panel.style.display === 'block') fetchNotifications(contextPath);
            });

            document.addEventListener('click', function(e) {
                if (!panel.contains(e.target) && !bell.contains(e.target)) {
                    panel.style.display = 'none';
                }
            });

            const markAllBtn = document.getElementById('markAllReadBtn');
            if (markAllBtn) {
                markAllBtn.addEventListener('click', function() {
                    fetch(contextPath + '/api/notifications/read', { method: 'POST' })
                        .then(() => {
                            document.getElementById('notifBadge').style.display = 'none';
                            fetchNotifications(contextPath);
                        });
                });
            }

            // Initial badge count
            fetch(contextPath + '/api/notifications')
                .then(r => r.json())
                .then(data => {
                    if (data.unreadCount > 0) {
                        const badge = document.getElementById('notifBadge');
                        badge.textContent = data.unreadCount;
                        badge.style.display = 'flex';
                    }
                }).catch(() => {});
        }

        function fetchNotifications(contextPath) {
            fetch(contextPath + '/api/notifications')
                .then(r => r.json())
                .then(data => {
                    const list = document.getElementById('notifList');
                    if (!data.notifications || data.notifications.length === 0) {
                        list.innerHTML = '<div class="empty-state-small">No notifications</div>';
                        return;
                    }
                    let html = '';
                    data.notifications.forEach(n => {
                        html += '<div class="notif-item ' + (n.read ? '' : 'unread') + '">' +
                            '<div class="notif-content">' +
                                '<strong>' + escapeHtmlShared(n.title) + '</strong>' +
                                '<p>' + escapeHtmlShared(n.message) + '</p>' +
                                '<span class="notif-time">' + escapeHtmlShared(n.createdAt) + '</span>' +
                            '</div>' +
                            (n.link ? '<a href="' + contextPath + n.link + '" class="notif-link">View</a>' : '') +
                        '</div>';
                    });
                    list.innerHTML = html;

                    const badge = document.getElementById('notifBadge');
                    if (data.unreadCount > 0) {
                        badge.textContent = data.unreadCount;
                        badge.style.display = 'flex';
                    } else {
                        badge.style.display = 'none';
                    }
                }).catch(() => {});
        }

        function setupSharedWebSocket() {
            if (!window.serverData || !window.serverData.currentUser || !window.serverData.currentUser.id) return;
            try {
                const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                const contextPath = '${pageContext.request.contextPath}';
                const wsUrl = protocol + '//' + window.location.host + contextPath + '/ws/assignments/' + window.serverData.currentUser.id;
                const ws = new WebSocket(wsUrl);
                ws.onmessage = function(event) {
                    try {
                        const msg = JSON.parse(event.data);
                        if (msg.type === 'NEW_ASSIGNMENT') {
                            showToast('New Assignment: ' + (msg.title || 'Check dashboard'), 'info');
                            fetchNotifications(contextPath);
                        } else if (msg.type === 'GRADE_POSTED') {
                            showToast('Grade Posted: ' + (msg.title || '') + ' (' + msg.grade + '/' + msg.maxPoints + ')', 'success');
                            fetchNotifications(contextPath);
                        } else if (msg.type === 'NEW_ANNOUNCEMENT') {
                            showToast('Announcement: ' + (msg.title || ''), 'info');
                            fetchNotifications(contextPath);
                        } else if (msg.type === 'NEW_SUBMISSION') {
                            showToast('New Submission: ' + (msg.studentName || 'A student') + ' - ' + (msg.assignmentTitle || ''), 'info');
                            fetchNotifications(contextPath);
                        }
                    } catch (e) {
                        console.log('WS message:', event.data);
                    }
                };
            } catch (e) {
                console.warn('WebSocket init error:', e);
            }
        }

        function escapeHtmlShared(unsafe) {
            if (!unsafe) return '';
            return unsafe.toString().replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
        }
    </script>
    
    <!-- Page Specific Scripts -->
    <jsp:invoke fragment="extraScripts"/>
</body>
</html>
