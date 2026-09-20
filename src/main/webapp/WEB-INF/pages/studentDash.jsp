<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="activeSection" value="${param.section != null ? param.section : 'overview'}"/>

<t:layout role="STUDENT" pageTitle="Student Dashboard" activeSection="${activeSection}">
    <jsp:attribute name="extraScripts">
    <script>
    (function() {
        'use strict';

        let serverData = {};
        try {
            serverData = JSON.parse(document.getElementById('server-data').textContent);
        } catch (e) {
            serverData = window.serverData || {};
            console.error('Failed to parse serverData', e);
        }

        const user = serverData.currentUser || {};
        const assignments = serverData.assignments || [];
        const submissions = serverData.recentSubmissions || [];
        const enrolledClasses = serverData.enrolledClasses || [];
        const stats = serverData.stats || {};
        const contextPath = '${pageContext.request.contextPath}';
        const submittedIds = new Set(submissions.map(s => (s.assignment ? s.assignment.id : s.assignmentId)));

        function init() {
            renderStats();
            renderDueSoon();
            renderRecentSubmissions();
            renderAssignments();
            renderSubmissions();
            renderGrades();
            renderClasses();
            populateClassFilter();
            setupNavigation();
            setupFileUpload();
            checkDeepLink();
            checkSessionMessages();
        }

        // ===== Stats =====
        function renderStats() {
            const totalAssignments = assignments.length;
            const submittedCount = submittedIds.size;
            const pendingCount = Math.max(0, totalAssignments - submittedCount);
            const overdueCount = assignments.filter(a => {
                if (submittedIds.has(a.id)) return false;
                const deadline = parseDate(a.deadline);
                return deadline && deadline < new Date();
            }).length;

            document.getElementById('statCourses').textContent = enrolledClasses.length;
            document.getElementById('statTotal').textContent = totalAssignments;
            document.getElementById('statSubmitted').textContent = submittedCount;
            document.getElementById('statPending').textContent = pendingCount;

            document.getElementById('trendCourses').textContent = enrolledClasses.length + ' enrolled';
            document.getElementById('trendAssignments').textContent = pendingCount + ' not submitted';
            document.getElementById('trendSubmitted').textContent = (totalAssignments - submittedCount) + ' remaining';
            document.getElementById('trendPending').textContent = overdueCount + ' overdue';
        }

        // ===== Due Soon =====
        function renderDueSoon() {
            const container = document.getElementById('dueSoonList');
            const now = new Date();
            const threeDays = new Date(now.getTime() + 3 * 24 * 60 * 60 * 1000);

            const overdue = assignments.filter(a => {
                if (submittedIds.has(a.id)) return false;
                const deadline = parseDate(a.deadline);
                return deadline && deadline < now;
            });
            const dueSoon = assignments.filter(a => {
                if (submittedIds.has(a.id)) return false;
                const deadline = parseDate(a.deadline);
                return deadline && deadline > now && deadline <= threeDays;
            }).sort((a, b) => parseDate(a.deadline) - parseDate(b.deadline));

            if (dueSoon.length === 0 && overdue.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>All caught up! No assignments due soon.</p></div>';
                return;
            }

            let html = '';
            overdue.forEach(a => html += renderAssignmentCard(a, 'overdue'));
            dueSoon.forEach(a => html += renderAssignmentCard(a, 'due-soon'));
            container.innerHTML = html;
        }

        // ===== Assignments =====
        function renderAssignments() {
            const container = document.getElementById('assignmentsList');
            if (assignments.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>No assignments yet. Check back later!</p></div>';
                return;
            }
            let html = '';
            assignments.forEach(a => html += renderAssignmentCard(a, getAssignmentStatus(a)));
            container.innerHTML = html;
        }

        function renderAssignmentCard(a, statusOverride) {
            const deadline = parseDate(a.deadline);
            const now = new Date();
            const status = statusOverride || getAssignmentStatus(a);
            const isSubmitted = submittedIds.has(a.id);
            const timeLeft = deadline ? getTimeRemaining(deadline) : '';
            const sub = submissions.find(s => (s.assignment ? s.assignment.id : s.assignmentId) === a.id);
            const finalStatus = (sub && sub.grade != null) ? 'graded' : status;

            const statusLabels = {
                'submitted': '<span class="badge badge-green">Submitted</span>',
                'overdue': '<span class="badge badge-red">Overdue</span>',
                'due-soon': '<span class="badge badge-amber">Due Soon</span>',
                'pending': '<span class="badge badge-blue">Pending</span>',
                'graded': '<span class="badge badge-purple">Graded</span>'
            };

            let action = '';
            if (!isSubmitted && deadline && deadline > now) {
                action = '<button class="btn btn-primary btn-sm" onclick="openSubmitModal(' + a.id + ')">Submit</button>';
            } else if (sub && sub.grade != null) {
                action = '<span class="grade-display">' + sub.grade + '/' + (a.points || 100) + '</span>';
            } else if (isSubmitted) {
                action = '<span class="submitted-check">&#10003; Submitted</span>';
            } else {
                action = '<span class="badge badge-red">Overdue</span>';
            }

            return '<div class="assignment-card" data-class="' + escapeHtml(a.className || '') + '" data-status="' + finalStatus + '">' +
                '<div class="assignment-meta">' +
                    '<span class="assignment-class">' + escapeHtml(a.className || 'Unknown Class') + '</span>' +
                    (statusLabels[finalStatus] || '') +
                '</div>' +
                '<h3 class="assignment-title">' + escapeHtml(a.title || '') + '</h3>' +
                '<p class="assignment-desc">' + escapeHtml(truncate(a.description || '', 120)) + '</p>' +
                '<div class="assignment-card-bottom">' +
                    '<div class="assignment-info">' +
                        '<span class="assignment-deadline"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg> ' + (deadline ? formatDate(deadline) : 'No deadline') + '</span>' +
                        (a.points ? '<span class="assignment-points">' + a.points + ' pts</span>' : '') +
                        (timeLeft && !isSubmitted ? '<span class="time-remaining">' + timeLeft + '</span>' : '') +
                    '</div>' +
                    (a.googleFormUrl && !isSubmitted ? '<a href="' + escapeHtml(a.googleFormUrl) + '" target="_blank" class="btn btn-outline btn-sm">Open Google Form</a>' : '') +
                    action +
                '</div>' +
            '</div>';
        }

        // ===== Submissions =====
        function renderSubmissions() {
            const container = document.getElementById('submissionsList');
            if (submissions.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>You haven\'t submitted any work yet.</p></div>';
                return;
            }

            let html = '<div class="table-header"><span>Assignment</span><span>Class</span><span>Submitted</span><span>Status</span><span>Grade</span></div>';
            submissions.forEach(s => {
                const a = s.assignment || {};
                const statusText = s.grade != null ? 'Graded' : (s.status || 'Submitted');
                const statusClass = s.grade != null ? 'badge-purple' : (s.status === 'LATE' ? 'badge-amber' : 'badge-green');
                html += '<div class="table-row">' +
                    '<span class="cell-primary">' + escapeHtml(a.title || 'Assignment') + '</span>' +
                    '<span>' + escapeHtml(a.className || '') + '</span>' +
                    '<span>' + formatDateTime(s.submittedAt) + '</span>' +
                    '<span><span class="badge ' + statusClass + '">' + statusText + '</span></span>' +
                    '<span>' + (s.grade != null ? s.grade + '/' + (a.points || 100) : '&mdash;') + '</span>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        function renderRecentSubmissions() {
            const container = document.getElementById('recentSubmissionsList');
            const recent = submissions.slice(0, 5);
            if (recent.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>No submissions yet.</p></div>';
                return;
            }
            let html = '';
            recent.forEach(s => {
                const a = s.assignment || {};
                html += '<div class="submission-item">' +
                    '<div class="submission-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/></svg></div>' +
                    '<div class="submission-details">' +
                        '<span class="submission-title">' + escapeHtml(a.title || 'Assignment') + '</span>' +
                        '<span class="submission-meta">' + escapeHtml(a.className || '') + ' &middot; ' + formatDateTime(s.submittedAt) + '</span>' +
                    '</div>' +
                    '<div class="submission-grade">' +
                        (s.grade != null ? '<span class="grade-display">' + s.grade + '/' + (a.points || 100) + '</span>' : '<span class="badge badge-blue">Pending</span>') +
                    '</div>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        // ===== Grades =====
        function renderGrades() {
            const graded = submissions.filter(s => s.grade != null);
            const summaryEl = document.getElementById('gradesSummary');
            const listEl = document.getElementById('gradesList');

            if (graded.length === 0) {
                summaryEl.innerHTML = '';
                listEl.innerHTML = '<div class="empty-state"><p>No grades available yet. Submit your assignments to see grades here.</p></div>';
                return;
            }

            const totalPoints = graded.reduce((sum, s) => sum + (s.grade || 0), 0);
            const maxPoints = graded.reduce((sum, s) => sum + ((s.assignment ? s.assignment.points : null) || 100), 0);
            const avg = maxPoints > 0 ? Math.round((totalPoints / maxPoints) * 100) : 0;
            const highest = Math.max.apply(null, graded.map(s => {
                const mp = (s.assignment ? s.assignment.points : null) || 100;
                return Math.round((s.grade / mp) * 100);
            }));

            summaryEl.innerHTML =
                '<div class="grade-summary-card"><span class="grade-summary-value">' + avg + '%</span><span class="grade-summary-label">Overall Average</span></div>' +
                '<div class="grade-summary-card"><span class="grade-summary-value">' + graded.length + '</span><span class="grade-summary-label">Graded Assignments</span></div>' +
                '<div class="grade-summary-card"><span class="grade-summary-value">' + highest + '%</span><span class="grade-summary-label">Highest Score</span></div>';

            let html = '';
            graded.forEach(s => {
                const a = s.assignment || {};
                const pct = ((s.grade || 0) / ((a.points) || 100)) * 100;
                const gradeClass = pct >= 80 ? 'grade-a' : pct >= 60 ? 'grade-b' : pct >= 40 ? 'grade-c' : 'grade-f';
                html += '<div class="grade-card">' +
                    '<div class="grade-card-info">' +
                        '<h4>' + escapeHtml(a.title || '') + '</h4>' +
                        '<span class="grade-class-name">' + escapeHtml(a.className || '') + '</span>' +
                        (s.feedback ? '<p class="grade-feedback">' + escapeHtml(s.feedback) + '</p>' : '') +
                    '</div>' +
                    '<div class="grade-card-score ' + gradeClass + '">' +
                        '<span class="score-value">' + s.grade + '/' + (a.points || 100) + '</span>' +
                        '<div class="score-bar"><div class="score-fill" style="width:' + Math.min(100, pct) + '%"></div></div>' +
                        '<span class="score-pct">' + Math.round(pct) + '%</span>' +
                    '</div>' +
                '</div>';
            });
            listEl.innerHTML = html;
        }

        // ===== Classes =====
        function renderClasses() {
            const container = document.getElementById('classesList');
            if (enrolledClasses.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>You are not enrolled in any classes yet.</p></div>';
                return;
            }
            let html = '';
            enrolledClasses.forEach(sc => {
                const className = sc.className || sc;
                const teacherName = sc.teacher ? sc.teacher.name : 'Teacher';
                const classAssignments = assignments.filter(a => a.className === className);
                const pendingCount = classAssignments.filter(a => !submittedIds.has(a.id)).length;
                html += '<div class="class-card">' +
                    '<div class="class-avatar" style="background:' + stringToColor(className) + '">' + escapeHtml(className.charAt(0)) + '</div>' +
                    '<div class="class-info"><h3>' + escapeHtml(className) + '</h3><span class="class-teacher">' + escapeHtml(teacherName) + '</span></div>' +
                    '<div class="class-stats-row"><span>' + classAssignments.length + ' assignments</span>' +
                        (pendingCount > 0 ? '<span class="badge badge-amber">' + pendingCount + ' pending</span>' : '<span class="badge badge-green">All done</span>') +
                    '</div>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        // ===== Filters =====
        function populateClassFilter() {
            const select = document.getElementById('classFilter');
            const classNames = [];
            enrolledClasses.forEach(sc => {
                const cn = sc.className || sc;
                if (classNames.indexOf(cn) < 0) classNames.push(cn);
            });
            classNames.forEach(cn => {
                const opt = document.createElement('option');
                opt.value = cn;
                opt.textContent = cn;
                select.appendChild(opt);
            });

            select.addEventListener('change', filterAssignments);
            document.getElementById('assignmentFilter').addEventListener('change', filterAssignments);
        }

        function filterAssignments() {
            const statusFilter = document.getElementById('assignmentFilter').value;
            const classFilterVal = document.getElementById('classFilter').value;
            document.querySelectorAll('#assignmentsList .assignment-card').forEach(card => {
                let show = true;
                if (classFilterVal !== 'all' && card.dataset.class !== classFilterVal) show = false;
                if (statusFilter !== 'all' && card.dataset.status !== statusFilter) show = false;
                card.style.display = show ? '' : 'none';
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
                if (href.indexOf('section=' + sectionId) >= 0 || (sectionId === 'overview' && href.indexOf('/studentDash') >= 0 && href.indexOf('section=') < 0)) {
                    link.classList.add('active');
                }
            });

            const titles = { overview: 'Dashboard', assignments: 'Assignments', submissions: 'My Submissions', grades: 'Grades', classes: 'My Classes' };
            const pageTitleEl = document.getElementById('pageTitle');
            if (pageTitleEl) pageTitleEl.textContent = titles[sectionId] || 'Dashboard';

            const url = new URL(window.location);
            if (sectionId === 'overview') url.searchParams.delete('section');
            else url.searchParams.set('section', sectionId);
            window.history.pushState({}, '', url);
        };

        function setupNavigation() {
            const urlParams = new URLSearchParams(window.location.search);
            window.switchSection(urlParams.get('section') || 'overview');
        }

        function checkDeepLink() {
            const urlParams = new URLSearchParams(window.location.search);
            const submitId = urlParams.get('submit');
            const errorParam = urlParams.get('error');
            if (errorParam === 'graded') {
                showToast('This assignment has already been graded and can no longer be resubmitted.', 'error');
                return;
            }
            if (!submitId) return;
            const assignment = assignments.find(a => a.id == submitId);
            if (assignment) {
                setTimeout(() => openSubmitModal(assignment.id), 300);
            }
        }

        // ===== Submit Modal =====
        window.openSubmitModal = function(assignmentId) {
            const assignment = assignments.find(a => a.id == assignmentId);
            if (!assignment) return;
            document.getElementById('submitAssignmentId').value = assignmentId;
            document.getElementById('submitModalTitle').textContent = 'Submit: ' + (assignment.title || 'Assignment');

            const detailsEl = document.getElementById('submitModalDetails');
            if (detailsEl) {
                if (assignment.description) {
                    document.getElementById('submitModalDescription').textContent = assignment.description;
                    const deadline = parseDate(assignment.deadline);
                    document.getElementById('submitModalDeadline').textContent = deadline ? 'Due: ' + formatDateTime(deadline) : 'No deadline';
                    document.getElementById('submitModalPoints').textContent = assignment.points ? assignment.points + ' pts' : '';
                    detailsEl.style.display = 'block';
                } else {
                    detailsEl.style.display = 'none';
                }
            }

            document.getElementById('commentsInput').value = '';

            const fileNameEl = document.getElementById('fileName');
            if (fileNameEl) fileNameEl.textContent = 'No file chosen';
            const fileInput = document.getElementById('submitFile');
            if (fileInput) fileInput.value = '';
            const area = document.getElementById('fileUploadArea');
            if (area) area.classList.remove('has-file');

            const gfGroup = document.getElementById('googleFormGroup');
            const googleFormUrl = assignment.googleFormUrl || '';
            if (googleFormUrl) {
                gfGroup.style.display = 'block';
                document.getElementById('googleFormLink').href = googleFormUrl;
            } else {
                gfGroup.style.display = 'none';
            }

            openModal('submitModal');
        };

        window.openModal = function(id) {
            const el = document.getElementById(id);
            if (el) { el.style.display = 'flex'; document.body.style.overflow = 'hidden'; }
        };
        window.closeModal = function(id) {
            const el = document.getElementById(id);
            if (el) { el.style.display = 'none'; document.body.style.overflow = ''; }
        };

        function setupFileUpload() {
            const area = document.getElementById('fileUploadArea');
            const input = document.getElementById('submitFile');
            const nameDisplay = document.getElementById('fileName');
            if (!area || !input) return;

            area.addEventListener('click', function(e) {
                if (e.target.id !== 'fileName') input.click();
            });
            area.addEventListener('dragover', function(e) { e.preventDefault(); area.classList.add('has-file'); });
            area.addEventListener('dragleave', function() { area.classList.remove('has-file'); });
            area.addEventListener('drop', function(e) {
                e.preventDefault();
                area.classList.remove('has-file');
                if (e.dataTransfer.files && e.dataTransfer.files.length) {
                    input.files = e.dataTransfer.files;
                    nameDisplay.textContent = e.dataTransfer.files[0].name;
                }
            });
            input.addEventListener('change', function() {
                nameDisplay.textContent = (this.files && this.files[0]) ? this.files[0].name : 'No file chosen';
            });
        }

        // ===== Session Messages =====
        function checkSessionMessages() {
            const msg = "${message}";
            if (msg && msg.trim() !== '' && msg !== 'null') showToast(msg, 'success');
            const err = "${error}";
            if (err && err.trim() !== '' && err !== 'null') showToast(err, 'error');
        }

        // ===== Utilities =====
        function getAssignmentStatus(a) {
            if (submittedIds.has(a.id)) return 'submitted';
            const deadline = parseDate(a.deadline);
            if (deadline && deadline < new Date()) return 'overdue';
            const threeDays = new Date(new Date().getTime() + 3 * 24 * 60 * 60 * 1000);
            if (deadline && deadline <= threeDays) return 'due-soon';
            return 'pending';
        }
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
        function getTimeRemaining(deadline) {
            const diff = deadline - new Date();
            if (diff <= 0) return 'Overdue';
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            return days > 0 ? days + 'd ' + hours + 'h left' : hours + 'h left';
        }
        function stringToColor(str) {
            let hash = 0;
            for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
            const h = hash % 360;
            return 'hsl(' + (h < 0 ? h + 360 : h) + ', 65%, 55%)';
        }
        function escapeHtml(s) { return s ? String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;') : ''; }
        function escapeJs(s) { return s ? String(s).replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"') : ''; }
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
        catch (e) { window.serverData = {}; console.error('Failed to parse serverData', e); }
    </script>

    <!-- Section: Overview -->
    <section class="content-section active" id="section-overview">
        <h2 class="section-title" id="pageTitle" style="margin-bottom:20px;">Dashboard</h2>

        <div class="stats-grid">
            <div class="stat-card stat-purple">
                <div class="stat-header">
                    <span class="stat-title">Enrolled Classes</span>
                    <div class="stat-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                    </div>
                </div>
                <span class="stat-value" id="statCourses">0</span>
                <div class="stat-trend trend-neutral"><span class="trend-text" id="trendCourses">-</span></div>
            </div>

            <div class="stat-card stat-yellow">
                <div class="stat-header">
                    <span class="stat-title">Total Assignments</span>
                    <div class="stat-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                    </div>
                </div>
                <span class="stat-value" id="statTotal">0</span>
                <div class="stat-trend trend-neutral"><span class="trend-text" id="trendAssignments">-</span></div>
            </div>

            <div class="stat-card stat-green">
                <div class="stat-header">
                    <span class="stat-title">Submitted</span>
                    <div class="stat-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                    </div>
                </div>
                <span class="stat-value" id="statSubmitted">0</span>
                <div class="stat-trend trend-neutral"><span class="trend-text" id="trendSubmitted">-</span></div>
            </div>

            <div class="stat-card stat-orange">
                <div class="stat-header">
                    <span class="stat-title">Pending</span>
                    <div class="stat-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    </div>
                </div>
                <span class="stat-value" id="statPending">0</span>
                <div class="stat-trend trend-neutral"><span class="trend-text" id="trendPending">-</span></div>
            </div>
        </div>

        <div class="section-block">
            <div class="section-header-row">
                <h2 class="section-title">Due Soon</h2>
            </div>
            <div class="card-container">
                <div id="dueSoonList" class="assignment-list"></div>
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
    </section>

    <!-- Section: Assignments -->
    <section class="content-section" id="section-assignments">
        <div class="section-header-row" style="margin-bottom:24px; border-radius:10px;">
            <h2 class="section-title">All Assignments</h2>
            <div class="filter-bar">
                <select id="assignmentFilter" class="filter-select">
                    <option value="all">All</option>
                    <option value="pending">Pending</option>
                    <option value="submitted">Submitted</option>
                    <option value="overdue">Overdue</option>
                </select>
                <select id="classFilter" class="filter-select">
                    <option value="all">All Classes</option>
                </select>
            </div>
        </div>
        <div id="assignmentsList" class="assignment-list"></div>
    </section>

    <!-- Section: Submissions -->
    <section class="content-section" id="section-submissions">
        <div class="section-block">
            <div class="section-header-row">
                <h2 class="section-title">My Submissions</h2>
            </div>
            <div class="card-container">
                <div id="submissionsList"></div>
            </div>
        </div>
    </section>

    <!-- Section: Grades -->
    <section class="content-section" id="section-grades">
        <h2 class="section-title" style="margin-bottom:20px;">Grades &amp; Academic Progress</h2>
        <div class="grades-summary" id="gradesSummary"></div>
        <div class="section-block">
            <div class="section-header-row">
                <h2 class="section-title">Recent Grades</h2>
            </div>
            <div class="card-container">
                <div id="gradesList" class="grade-list" style="display:flex; flex-direction:column; gap:16px;"></div>
            </div>
        </div>
    </section>

    <!-- Section: Classes -->
    <section class="content-section" id="section-classes">
        <h2 class="section-title" style="margin-bottom:20px;">My Enrolled Classes</h2>
        <div id="classesList" class="classes-grid"></div>
    </section>

    <!-- Submit Assignment Modal -->
    <div class="modal-overlay" id="submitModal" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2 id="submitModalTitle">Submit Assignment</h2>
                <button class="modal-close" type="button" onclick="closeModal('submitModal')">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
            </div>
            <form id="submitForm" action="${pageContext.request.contextPath}/submitAssignment" method="POST" enctype="multipart/form-data">
                <input type="hidden" name="assignmentId" id="submitAssignmentId">
                <div class="form-group" id="submitModalDetails" style="display:none; border-bottom:1px solid rgba(255,255,255,0.08); padding-bottom:16px; margin-bottom:16px;">
                    <p class="assignment-desc" id="submitModalDescription" style="white-space:pre-wrap; margin-bottom:10px;"></p>
                    <div class="assignment-info" style="display:flex; gap:16px; flex-wrap:wrap; color:#a0aec0; font-size:13px;">
                        <span id="submitModalDeadline"></span>
                        <span id="submitModalPoints"></span>
                    </div>
                </div>
                <div class="form-group">
                    <label>Your Answer / Comments (Optional)</label>
                    <textarea name="content" id="commentsInput" rows="4" placeholder="Write your answer, notes, or link description here..."></textarea>
                </div>
                <div class="form-group">
                    <label>Upload File (Optional, max 10MB)</label>
                    <div class="file-upload-area" id="fileUploadArea">
                        Drag &amp; drop your file here, or <span class="file-browse">browse</span>
                        <span class="file-name" id="fileName">No file chosen</span>
                    </div>
                    <input type="file" name="file" id="submitFile" style="display:none;">
                </div>
                <div class="form-group" id="googleFormGroup" style="display:none;">
                    <label>Google Form Link</label>
                    <a id="googleFormLink" href="#" target="_blank">Open Google Form</a>
                    <input type="url" name="formResponseUrl" placeholder="Paste your Google Form response URL after completing it (optional)">
                </div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-outline" onclick="closeModal('submitModal')">Cancel</button>
                    <button type="submit" class="btn btn-primary">Submit Work</button>
                </div>
            </form>
        </div>
    </div>
    </jsp:body>
</t:layout>