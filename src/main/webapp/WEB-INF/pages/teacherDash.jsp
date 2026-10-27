<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="activeSection" value="${param.section != null ? param.section : 'overview'}"/>

<t:layout role="TEACHER" pageTitle="Teacher Dashboard" activeSection="${activeSection}">
    <jsp:attribute name="topBarActions">
        <button class="btn btn-primary" id="createAssignmentBtn" onclick="openCreateModal()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            New Assignment
        </button>
    </jsp:attribute>

    <jsp:attribute name="extraScripts">
    <script>
    (function() {
        'use strict';

        let serverData = {};
        try {
            const el = document.getElementById('server-data');
            serverData = el ? JSON.parse(el.textContent) : (window.serverData || {});
        } catch (e) {
            serverData = window.serverData || {};
            console.error('Failed to parse serverData', e);
        }
        const metaCsrf = document.querySelector('meta[name="csrf-token"]');
        window.__csrf = (metaCsrf && metaCsrf.content) || serverData.csrfToken || '';

        const csrfToken = window.__csrf || '';
        const user = serverData.currentUser || {};
        const assignments = serverData.assignments || [];
        const classes = serverData.classes || [];
        const enrollments = serverData.enrollments || [];
        const recentSubmissions = serverData.recentSubmissions || [];
        const stats = serverData.stats || {};
        const contextPath = '${pageContext.request.contextPath}';

        function init() {
            renderStats();
            renderRecentSubmissions();
            renderNeedsGrading();
            renderAssignments();
            renderAllSubmissions();
            renderStudents();
            renderClasses();
            populateClassSelect();
            setupNavigation();
            setupGradeForm();
            setupSubmissionFilter();
            checkSessionMessages();
        }

        // ===== Stats =====
        function renderStats() {
            document.getElementById('statStudents').textContent = stats.studentCount != null ? stats.studentCount : 0;
            document.getElementById('statAssignments').textContent = stats.assignmentCount != null ? stats.assignmentCount : assignments.length;
            document.getElementById('statSubmissions').textContent = stats.submissionCount != null ? stats.submissionCount : recentSubmissions.length;
            document.getElementById('statClasses').textContent = classes.length || 0;
            document.getElementById('trendStudents').textContent = enrollments.length + ' enrollments';
            document.getElementById('trendAssignments').textContent = recentSubmissions.filter(s => s.grade == null).length + ' awaiting grading';
            document.getElementById('trendSubmissions').textContent = 'from ' + classes.length + ' classes';
            document.getElementById('trendClasses').textContent = 'your assigned classes';
        }

        // ===== Recent Submissions (Overview) =====
        function renderRecentSubmissions() {
            const container = document.getElementById('recentSubmissionsList');
            const recent = recentSubmissions.slice(0, 8);
            if (recent.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>No submissions received yet.</p></div>';
                return;
            }
            let html = '';
            recent.forEach(s => {
                const a = s.assignment || {};
                html += '<div class="submission-item">' +
                    '<div class="submission-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/></svg></div>' +
                    '<div class="submission-details">' +
                        '<span class="submission-title">' + escapeHtml(s.student ? s.student.name : 'Student') + '</span>' +
                        '<span class="submission-meta">' + escapeHtml(a.title || '') + ' &middot; ' + formatDateTime(s.submittedAt) + '</span>' +
                    '</div>' +
                    '<div class="submission-grade">' +
                        (s.grade != null ?
                            '<span class="grade-display">' + s.grade + '/' + (a.points || 100) + '</span>' :
                            '<button class="btn btn-sm btn-primary" onclick="openGradeModal(' + s.id + ')">Grade</button>'
                        ) +
                    '</div>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        // ===== Needs Grading =====
        function renderNeedsGrading() {
            const container = document.getElementById('needsGradingList');
            const ungraded = recentSubmissions.filter(s => s.grade == null);
            if (ungraded.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>All submissions have been graded!</p></div>';
                return;
            }
            let html = '';
            ungraded.slice(0, 10).forEach(s => {
                const a = s.assignment || {};
                html += '<div class="submission-item">' +
                    '<div class="submission-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg></div>' +
                    '<div class="submission-details">' +
                        '<span class="submission-title">' + escapeHtml(s.student ? s.student.name : 'Student') + '</span>' +
                        '<span class="submission-meta">' + escapeHtml(a.title || '') + ' &middot; ' + escapeHtml(a.className || '') + '</span>' +
                    '</div>' +
                    '<button class="btn btn-sm btn-primary" onclick="openGradeModal(' + s.id + ')">Grade</button>' +
                '</div>';
            });
            if (ungraded.length > 10) {
                html += '<div style="text-align:center; padding:12px;"><a href="#" onclick="switchSection(\'submissions\'); return false;" style="color:#4379EE; font-size:13px; font-weight:600;">View all ' + ungraded.length + ' ungraded submissions &rarr;</a></div>';
            }
            container.innerHTML = html;
        }

        // ===== Assignments =====
        function renderAssignments() {
            const container = document.getElementById('assignmentsList');
            if (assignments.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>No assignments created yet.</p><button class="btn btn-primary" style="margin-top:16px;" onclick="openCreateModal()">Create your first assignment</button></div>';
                return;
            }
            let html = '';
            assignments.forEach(a => {
                const deadline = parseDate(a.deadline);
                const isPast = deadline && deadline < new Date();
                const subsForA = recentSubmissions.filter(s => (s.assignment ? s.assignment.id : null) === a.id);
                const gradedCount = subsForA.filter(s => s.grade != null).length;

                html += '<div class="assignment-card">' +
                    '<div class="assignment-card-top">' +
                        '<div class="assignment-meta">' +
                            '<span class="assignment-class">' + escapeHtml(a.className || '') + '</span>' +
                            (isPast ? '<span class="badge badge-red">Closed</span>' : '<span class="badge badge-green">Active</span>') +
                        '</div>' +
                        '<h3 class="assignment-title">' + escapeHtml(a.title) + '</h3>' +
                        '<p class="assignment-desc">' + escapeHtml(truncate(a.description || '', 100)) + '</p>' +
                    '</div>' +
                    '<div class="assignment-card-bottom">' +
                        '<div class="assignment-info">' +
                            '<span class="assignment-deadline"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg> ' + (deadline ? formatDate(deadline) : 'No deadline') + '</span>' +
                            '<span class="assignment-points">' + (a.points || 100) + ' pts</span>' +
                        '</div>' +
                        '<span class="progress-text" style="font-size:13px; color:#718096;">' + subsForA.length + ' submitted &middot; ' + gradedCount + ' graded</span>' +
                    '</div>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        // ===== All Submissions (Tab) =====
        function renderAllSubmissions(filter) {
            if (filter === undefined) filter = 'all';
            const container = document.getElementById('allSubmissionsList');
            let filtered = recentSubmissions;
            if (filter === 'graded') filtered = filtered.filter(s => s.grade != null);
            else if (filter === 'pending') filtered = filtered.filter(s => s.grade == null);

            if (filtered.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>No submissions found for the selected filter.</p></div>';
                return;
            }

            let html = '';
            filtered.forEach(s => {
                const a = s.assignment || {};
                const isGraded = s.grade != null;
                html += '<div class="submission-item">' +
                    '<div class="submission-icon">' +
                        (isGraded ? '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>' : '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>') +
                    '</div>' +
                    '<div class="submission-details">' +
                        '<span class="submission-title">' + escapeHtml(s.student ? s.student.name : 'Student') + ' - ' + escapeHtml(a.title || '') + '</span>' +
                        '<span class="submission-meta">' + escapeHtml(a.className || '') + ' &middot; ' + formatDateTime(s.submittedAt) + ' &middot; ' + escapeHtml(s.status || '') + '</span>' +
                    '</div>' +
                    '<div class="submission-grade">' +
                        (isGraded ?
                            '<span class="grade-display">' + s.grade + '/' + (a.points || 100) + '</span>' :
                            '<button class="btn btn-sm btn-primary" onclick="openGradeModal(' + s.id + ')">Grade</button>'
                        ) +
                        (s.fileUrl ? '<a href="' + contextPath + '/submissionDetails?id=' + s.id + '" target="_blank" class="btn btn-sm btn-outline" style="margin-left:8px;">View Submission</a>' : '') +
                    '</div>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        // ===== Students (Roster) =====
        function renderStudents() {
            const container = document.getElementById('studentsList');
            const uniqueStudents = new Map();
            enrollments.forEach(e => {
                if (!e.student) return;
                if (!uniqueStudents.has(e.student.id)) {
                    uniqueStudents.set(e.student.id, Object.assign({}, e.student, { classes: [e.className] }));
                } else {
                    uniqueStudents.get(e.student.id).classes.push(e.className);
                }
            });

            if (uniqueStudents.size === 0) {
                container.innerHTML = '<div class="empty-state"><p>No students enrolled in your classes yet.</p></div>';
                return;
            }

            let html = '';
            uniqueStudents.forEach(student => {
                html += '<div class="assignment-card">' +
                    '<div class="assignment-meta"><span class="badge badge-green">Enrolled</span></div>' +
                    '<h3 class="assignment-title">' + escapeHtml(student.name) + '</h3>' +
                    '<p class="assignment-desc">' + escapeHtml(student.email) + '</p>' +
                    '<div class="assignment-card-bottom">' +
                        '<div class="assignment-info"><span>' + escapeHtml(student.classes.join(', ')) + '</span></div>' +
                    '</div>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        // ===== Classes =====
        function renderClasses() {
            const container = document.getElementById('classesList');
            if (classes.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>You are not assigned to any classes yet.</p></div>';
                return;
            }
            let html = '';
            classes.forEach(c => {
                const count = enrollments.filter(e => e.className === c).length;
                html += '<div class="class-card">' +
                    '<div class="class-avatar" style="background:' + stringToColor(c) + '">' + escapeHtml(c.charAt(0)) + '</div>' +
                    '<div class="class-info"><h3>' + escapeHtml(c) + '</h3><span class="class-teacher">You teach this class</span></div>' +
                    '<div class="class-stats-row"><span>' + count + ' students</span><span>' + assignments.filter(a => a.className === c).length + ' assignments</span></div>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        // ===== Filters & Handlers =====
        function populateClassSelect() {
            const select = document.getElementById('assignmentClass');
            if (!select) return;
            let opts = '<option value="" disabled selected>Select a class</option>';
            classes.forEach(c => { opts += '<option value="' + escapeHtml(c) + '">' + escapeHtml(c) + '</option>'; });
            select.innerHTML = opts;
        }

        function setupSubmissionFilter() {
            const filter = document.getElementById('submissionFilter');
            if (filter) {
                filter.addEventListener('change', (e) => renderAllSubmissions(e.target.value));
            }
        }

        function setupGradeForm() {
            const form = document.getElementById('gradeForm');
            if (!form) return;
            form.addEventListener('submit', function(e) {
                e.preventDefault();
                const subId = document.getElementById('gradeSubmissionId').value;
                const grade = document.getElementById('gradeInput').value;
                const feedback = document.getElementById('feedbackInput').value;
                if (!grade || grade === '') {
                    showToast('Please enter a grade.', 'error');
                    return;
                }

                const formData = new URLSearchParams();
                formData.append('submissionId', subId);
                formData.append('grade', grade);
                formData.append('feedback', feedback);

                fetch(contextPath + '/teacher/grade', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData
                })
                .then(response => {
                    if (response.ok) {
                        return response.json();
                    }
                    return response.text().then(text => {
                        let msg = text;
                        try { msg = JSON.parse(text).message || text; } catch (e) { /* not json */ }
                        throw new Error(msg);
                    });
                })
                .then(json => {
                    showToast(json.message || 'Grade submitted successfully.', 'success');
                    closeGradeModal();
                    setTimeout(() => window.location.reload(), 700);
                })
                .catch(err => {
                    console.error(err);
                    showToast(err.message && err.message.indexOf('Failed') >= 0 ? err.message : ('Error: ' + err.message), 'error');
                });
            });
        }

        // ===== Navigation =====
        window.switchSection = function(sectionId) {
            document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
            document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

            const target = document.getElementById('section-' + sectionId);
            if (!target) return;
            target.classList.add('active');

            document.querySelectorAll('.nav-item').forEach(link => {
                const href = link.getAttribute('href') || '';
                if (href.indexOf('section=' + sectionId) >= 0 || (sectionId === 'overview' && href.indexOf('/dashboard') >= 0 && href.indexOf('section=') < 0)) {
                    link.classList.add('active');
                }
            });

            const url = new URL(window.location);
            if (sectionId === 'overview') url.searchParams.delete('section');
            else url.searchParams.set('section', sectionId);
            window.history.pushState({}, '', url);
        };

        function setupNavigation() {
            const urlParams = new URLSearchParams(window.location.search);
            const section = urlParams.get('section') || 'overview';
            window.switchSection(section);
        }

        // ===== Modals =====
        window.openModal = function(id) {
            const el = document.getElementById(id);
            if (el) { el.style.display = 'flex'; document.body.style.overflow = 'hidden'; }
        };
        window.closeModal = function(id) {
            const el = document.getElementById(id);
            if (el) { el.style.display = 'none'; document.body.style.overflow = ''; }
        };

        window.openCreateModal = function() {
            openModal('createAssignmentModal');
        };
        window.closeCreateModal = function() {
            closeModal('createAssignmentModal');
            const form = document.getElementById('createAssignmentForm');
            if (form) form.reset();
        };

        window.openGradeModal = function(subId) {
            const sub = recentSubmissions.find(s => s.id === subId);
            if (!sub) return;
            document.getElementById('gradeSubmissionId').value = sub.id;
            document.getElementById('gradeStudentName').textContent = sub.student ? sub.student.name : 'Student';
            document.getElementById('gradeAssignmentTitle').textContent = sub.assignment ? sub.assignment.title : 'Assignment';

            const fileLink = document.getElementById('gradeFileLink');
            if (sub.fileUrl) {
                fileLink.href = contextPath + '/submissionDetails?id=' + sub.id;
                fileLink.style.display = 'inline-block';
            } else {
                fileLink.style.display = 'none';
            }

            document.getElementById('gradeInput').value = sub.grade != null ? sub.grade : '';
            document.getElementById('feedbackInput').value = sub.feedback || '';
            openModal('gradeModal');
        };
        window.closeGradeModal = function() {
            closeModal('gradeModal');
            const form = document.getElementById('gradeForm');
            if (form) form.reset();
        };

        // ===== Session Messages =====
        function checkSessionMessages() {
            const msg = "${message}";
            if (msg && msg.trim() !== '' && msg !== 'null') showToast(msg, 'success');
            const err = "${error}";
            if (err && err.trim() !== '' && err !== 'null') showToast(err, 'error');
        }

        // ===== Utilities =====
        function parseDate(d) {
            if (!d) return null;
            if (d instanceof Date) return d;
            if (Array.isArray(d)) return new Date(d[0], (d[1] || 1) - 1, d[2] || 1, d[3] || 0, d[4] || 0);
            return new Date(d);
        }
        function formatDate(d) { return d ? d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : ''; }
        function formatDateTime(d) {
            const date = parseDate(d);
            if (!date || isNaN(date.getTime())) return '';
            return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ', ' + date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        }
        function stringToColor(str) {
            let hash = 0;
            for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
            const h = hash % 360;
            return 'hsl(' + (h < 0 ? h + 360 : h) + ', 65%, 55%)';
        }
        function escapeHtml(s) { return s ? String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;') : ''; }
        function truncate(s, l) { return (!s || s.length <= l) ? (s || '') : s.substring(0, l) + '...'; }
        function showToast(msg, type) {
            if (typeof window.showToast === 'function') window.showToast(msg, type);
            else alert(msg);
        }

        document.addEventListener('DOMContentLoaded', init);
    })();
    </script>
    </jsp:attribute>

    <jsp:body>
    <!-- Server Data Injection -->
    <script id="server-data" type="application/json">${realData != null && realData != '' ? realData : "{}"}</script>
    <script>
        try { window.serverData = JSON.parse(document.getElementById('server-data').textContent); }
        catch (e) { window.serverData = {}; console.error('Parse error', e); }
    </script>

    <!-- Section: Overview -->
    <section class="content-section active" id="section-overview">
        <div class="section-header-row" style="margin-bottom:24px; border-radius:10px;">
            <h2 class="section-title">Dashboard Overview</h2>
        </div>

        <div class="stats-grid">
            <div class="stat-card stat-purple">
                <div class="stat-header">
                    <span class="stat-title">Total Students</span>
                    <div class="stat-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                    </div>
                </div>
                <span class="stat-value" id="statStudents">0</span>
                <div class="stat-trend trend-neutral"><span class="trend-text" id="trendStudents">-</span></div>
            </div>

            <div class="stat-card stat-yellow">
                <div class="stat-header">
                    <span class="stat-title">Assignments Created</span>
                    <div class="stat-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                    </div>
                </div>
                <span class="stat-value" id="statAssignments">0</span>
                <div class="stat-trend trend-neutral"><span class="trend-text" id="trendAssignments">-</span></div>
            </div>

            <div class="stat-card stat-green">
                <div class="stat-header">
                    <span class="stat-title">Total Submissions</span>
                    <div class="stat-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                    </div>
                </div>
                <span class="stat-value" id="statSubmissions">0</span>
                <div class="stat-trend trend-neutral"><span class="trend-text" id="trendSubmissions">-</span></div>
            </div>

            <div class="stat-card stat-orange">
                <div class="stat-header">
                    <span class="stat-title">Classes</span>
                    <div class="stat-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
                    </div>
                </div>
                <span class="stat-value" id="statClasses">0</span>
                <div class="stat-trend trend-neutral"><span class="trend-text" id="trendClasses">-</span></div>
            </div>
        </div>

        <div class="section-block">
            <div class="section-header-row">
                <h2 class="section-title">Recent Submissions</h2>
            </div>
            <div class="card-container">
                <div id="recentSubmissionsList" class="submission-list"></div>
            </div>
        </div>

        <div class="section-block">
            <div class="section-header-row">
                <h2 class="section-title">Needs Grading</h2>
            </div>
            <div class="card-container">
                <div id="needsGradingList" class="submission-list"></div>
            </div>
        </div>
    </section>

    <!-- Section: Assignments -->
    <section class="content-section" id="section-assignments">
        <div class="section-header-row" style="margin-bottom:24px; border-radius:10px;">
            <h2 class="section-title">My Assignments</h2>
            <button class="btn btn-primary" onclick="openCreateModal()">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                New Assignment
            </button>
        </div>
        <div id="assignmentsList" class="assignment-list"></div>
    </section>

    <!-- Section: Submissions -->
    <section class="content-section" id="section-submissions">
        <div class="section-header-row" style="margin-bottom:24px; border-radius:10px;">
            <h2 class="section-title">All Submissions</h2>
            <select id="submissionFilter" class="filter-select">
                <option value="all">All</option>
                <option value="pending">Needs Grading</option>
                <option value="graded">Graded</option>
            </select>
        </div>
        <div id="allSubmissionsList" class="submission-list"></div>
    </section>

    <!-- Section: Roster -->
    <section class="content-section" id="section-roster">
        <div class="section-header-row" style="margin-bottom:24px; border-radius:10px;">
            <h2 class="section-title">Class Roster</h2>
        </div>
        <div id="studentsList" class="assignment-list"></div>
    </section>

    <!-- Section: Classes -->
    <section class="content-section" id="section-classes">
        <div class="section-header-row" style="margin-bottom:24px; border-radius:10px;">
            <h2 class="section-title">My Classes</h2>
        </div>
        <div id="classesList" class="classes-grid"></div>
    </section>

    <!-- Create Assignment Modal -->
    <div class="modal-overlay" id="createAssignmentModal" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2>Create New Assignment</h2>
                <button class="modal-close" type="button" onclick="closeModal('createAssignmentModal')">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
            </div>
            <form id="createAssignmentForm" action="${pageContext.request.contextPath}/teacher/dashboard" method="POST">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="createAssignment">
                <div class="form-group">
                    <label>Title</label>
                    <input type="text" name="title" required placeholder="E.g., Chapter 1 Quiz">
                </div>
                <div class="form-group">
                    <label>Description</label>
                    <textarea name="description" rows="3" required placeholder="Instructions for the assignment..."></textarea>
                </div>
                <div class="form-group">
                    <label>Select Class</label>
                    <select name="className" id="assignmentClass" required></select>
                </div>
                <div style="display:flex; gap:16px;">
                    <div class="form-group" style="flex:1;">
                        <label>Points</label>
                        <input type="number" name="points" value="100" required min="0">
                    </div>
                    <div class="form-group" style="flex:1;">
                        <label>Deadline</label>
                        <input type="datetime-local" name="deadline">
                    </div>
                </div>
                <div class="form-group">
                    <label>Google Form URL (Optional)</label>
                    <input type="url" name="googleFormUrl" placeholder="https://docs.google.com/forms/...">
                </div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="closeModal('createAssignmentModal')">Cancel</button>
                    <button type="submit" class="btn btn-primary">Create Assignment</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Grading Modal -->
    <div class="modal-overlay" id="gradeModal" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2>Grade Submission</h2>
                <button class="modal-close" type="button" onclick="closeModal('gradeModal')">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
            </div>
            <form id="gradeForm">
                <input type="hidden" name="submissionId" id="gradeSubmissionId">
                <div class="form-group">
                    <label>Student</label>
                    <div style="font-size:14px; font-weight:600; padding:8px 0;" id="gradeStudentName"></div>
                </div>
                <div class="form-group">
                    <label>Assignment</label>
                    <div style="font-size:14px; font-weight:600; padding:8px 0;" id="gradeAssignmentTitle"></div>
                </div>
                <div class="form-group">
                    <a id="gradeFileLink" href="#" target="_blank" class="btn btn-outline btn-sm" style="display:none;">View Submission File</a>
                </div>
                <div class="form-group">
                    <label>Grade (Points)</label>
                    <input type="number" name="grade" id="gradeInput" required min="0" placeholder="0">
                </div>
                <div class="form-group">
                    <label>Feedback / Comments</label>
                    <textarea name="feedback" id="feedbackInput" rows="4" placeholder="Great job on..."></textarea>
                </div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="closeModal('gradeModal')">Cancel</button>
                    <button type="submit" class="btn btn-primary">Publish Grade</button>
                </div>
            </form>
        </div>
    </div>
    </jsp:body>
</t:layout>