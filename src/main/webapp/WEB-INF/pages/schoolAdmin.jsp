<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<c:set var="activeSection" value="${param.section != null ? param.section : 'overview'}"/>

<t:layout role="ADMIN" pageTitle="School Admin Dashboard" activeSection="${activeSection}">
    <jsp:attribute name="extraScripts">
    <style>
        .form-control {
            width: 100%; padding: 10px 14px; font-size: 14px; font-family: inherit;
            border: 1px solid #E2E4EB; border-radius: 8px; background: #fff; color: #16181D; outline: none;
        }
        .form-control:focus { border-color: #4379EE; box-shadow: 0 0 0 3px rgba(67,121,238,0.1); }
        .form-label { display: block; font-size: 13px; font-weight: 600; color: #16181D; margin-bottom: 6px; }
        .status-badge { display: inline-block; padding: 4px 10px; border-radius: 6px; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
        .status-badge.badge-green { background: rgba(0,182,155,0.1); color: #00B69B; }
        .status-badge.badge-red { background: rgba(249,60,101,0.1); color: #F93C65; }
        .status-badge.badge-blue { background: rgba(67,121,238,0.1); color: #4379EE; }
        .status-badge.badge-amber { background: rgba(254,197,61,0.1); color: #e6a000; }
        .data-table { width: 100%; border-collapse: collapse; text-align: left; }
        .announcement-card { border: 1px solid #E2E4EB; border-radius: 10px; padding: 18px 20px; margin-bottom: 14px; background: #fff; }
    </style>
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
        const csrfToken = (metaCsrf && metaCsrf.content) || serverData.csrfToken || (window.__csrf || '');
        const stats = serverData.stats || {};
        const users = serverData.users || [];
        const students = serverData.students || [];
        const teachers = serverData.teachers || [];
        const classes = serverData.classes || [];
        const parents = serverData.parents || [];
        const subjects = serverData.subjects || [];
        const announcements = serverData.announcements || [];
        const logs = serverData.logs || [];
        const contextPath = '${pageContext.request.contextPath}';

        function init() {
            renderStats();
            renderActivityFeed();
            renderUsersTable();
            renderTeachersTable();
            renderStudentsTable();
            renderParentsTable();
            renderSubjectsTable();
            renderClassesTable();
            renderAnnouncements();
            setupSearch();
            checkQueryMessages();
            setupNavigation();
        }

        function renderStats() {
            const el = function(id) { return document.getElementById(id); };
            if (el('statStudents')) el('statStudents').textContent = stats.students || 0;
            if (el('statTeachers')) el('statTeachers').textContent = stats.teachers || 0;
            if (el('statClasses')) el('statClasses').textContent = stats.classes || 0;
            if (el('statAssignments')) el('statAssignments').textContent = stats.assignments || 0;
            if (el('statSubjects')) el('statSubjects').textContent = stats.subjects || 0;
        }

        function renderActivityFeed() {
            const feed = document.getElementById('activityFeed');
            if (!feed) return;
            if (!logs || logs.length === 0) {
                feed.innerHTML = '<div class="empty-state" style="padding:1rem;">No recent activity found.</div>';
                return;
            }
            let html = '';
            logs.slice(0, 10).forEach(log => {
                let icon = '📝';
                const action = log.action || '';
                if (action.indexOf('Submission') >= 0) icon = '✅';
                else if (action.indexOf('Assignment') >= 0) icon = '📚';
                html += '<div style="padding:0.75rem 0;border-bottom:1px solid #E2E4EB;display:flex;align-items:center;gap:1rem;">' +
                    '<div style="background:#F3F4F8;color:#43506C;width:40px;height:40px;border-radius:50%;display:flex;align-items:center;justify-content:center;flex-shrink:0;font-size:18px;">' + icon + '</div>' +
                    '<div style="flex:1;">' +
                        '<span style="display:block;font-weight:600;font-size:14px;">' + escapeHtml(action) + '</span>' +
                        '<span style="display:block;font-size:12px;color:#718096;margin-top:2px;">' + escapeHtml(log.user || '') + ' &middot; ' + escapeHtml(log.details || '') + '</span>' +
                    '</div>' +
                    '<span class="status-badge badge-blue" style="font-size:11px;">' + escapeHtml(shortDate(log.time)) + '</span>' +
                '</div>';
            });
            feed.innerHTML = html;
        }

        function renderUsersTable() {
            const tbody = document.getElementById('userTableBodyRows');
            if (!tbody) return;
            if (users.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:1.5rem;">No users found.</td></tr>';
                return;
            }
            let html = '';
            users.forEach(u => {
                const roleBadge = u.role === 'ADMIN' ? 'badge-amber' : (u.role === 'TEACHER' ? 'badge-blue' : 'badge-green');
                const verifiedBadge = u.emailVerified ? '<span class="status-badge badge-green">Verified</span>' : '<span class="status-badge badge-red">Unverified</span>';
                html += '<tr>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-weight:500;">' + escapeHtml(u.name || '') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="color:#718096;font-size:14px;">' + escapeHtml(u.email || '') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' + verifiedBadge + '</td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><span class="status-badge ' + roleBadge + '">' + escapeHtml(u.role || '') + '</span></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' +
                        '<button class="btn btn-outline btn-sm" onclick="openUserDetails(' + u.id + ')">View</button> ' +
                        '<button class="btn btn-outline btn-sm" onclick="openEditUserModal(' + u.id + ', \'' + escapeJs(u.name) + '\', \'' + escapeJs(u.email) + '\', \'' + escapeJs(u.role) + '\', \'' + escapeJs(u.phone || '') + '\')">Edit</button>' +
                    '</td>' +
                '</tr>';
            });
            tbody.innerHTML = html;
        }

        window.openRecordDetails = function(user, sectionLabel) {
    const hdr = document.getElementById('userDetailsHeader');
    const body = document.getElementById('userDetailsBody');
    if (!hdr || !body) return;
    hdr.textContent = sectionLabel;
    let html = '<div style="display:grid;grid-template-columns:90px 1fr;gap:1rem;align-items:center;border-bottom:1px solid var(--border-color);padding-bottom:1.25rem;margin-bottom:1.25rem;">';
    html += '<div style="width:90px;height:90px;border-radius:50%;background:linear-gradient(135deg,#3b82f6,#2563eb);display:flex;align-items:center;justify-content:center;color:#fff;font-size:2rem;font-weight:700;">' + (user.name ? user.name.charAt(0).toUpperCase() : '?') + '</div>';
    html += '<div><div style="font-size:1.4rem;font-weight:700;color:#23304a;">' + escapeHtml(user.name || '') + '</div>';
    html += '<div style="color:var(--text-muted);font-size:0.9rem;margin-top:2px;">ROLE: ' + escapeHtml(user.role || '') + '</div></div></div>';
    const meta = [['Email', user.email], ['Phone', user.phone], ['Status', user.isDisabled ? 'Disabled' : 'Active'], ['Joined', user.createdAt ? String(user.createdAt).slice(0,10) : '']];
    for (const [k, v] of meta) { if (!v) continue; html += '<div style="display:flex;justify-content:space-between;padding:.45rem 0;border-bottom:1px solid var(--border-light);"><span style="color:var(--text-muted)">' + k + '</span><span style="font-weight:600;">' + escapeHtml(v) + '</span></div>'; }
    const rels = (user.associations && user.associations.length) ? user.associations : [];
    html += '<div style="margin-top:.75rem;font-weight:700;color:#23304a;">Related</div>';
    if (!rels.length) { html += '<div style="color:var(--text-muted);font-size:0.9rem;padding:.35rem 0;">None</div>'; }
    rels.forEach(function(r){
        html += '<div style="display:flex;justify-content:space-between;align-items:center;padding:.6rem 0;border-bottom:1px solid var(--border-light);"><div><div style="font-weight:600;">' + escapeHtml(r.name || r.fullName || '') + '</div><div style="color:var(--text-muted);font-size:0.85rem;">' + escapeHtml(r.type || '') + ' &middot; ' + escapeHtml(r.grade || r.className || r.subjectName || '') + '</div></div><span style="border:1px solid var(--border-color);border-radius:999px;padding:2px 10px;font-size:0.75rem;color:var(--text-muted)">' + escapeHtml(r.role || '') + '</span></div>';
    });
    body.innerHTML = html;
    body.innerHTML = html;
    window.switchSection('user-details');
};

window.openTeacherDetails = function(id) { const r = teachers.find(function(t){ return t.id === id; }); if (!r) return; openRecordDetails(r.user || r, 'Teacher Details'); };
window.openStudentDetails = function(id) { const s = students.find(function(x){ return x.id === id; }); if (!s) return; openRecordDetails(s.user || s, 'Student Details'); };
window.openParentDetails = function(id) { const pa = parents.find(function(x){ return x.id === id; }); if (!pa) return; openRecordDetails(pa.user || pa, 'Parent Details'); };
window.openSubjectDetails = function(id) { const su = subjects.find(function(x){ return x.id === id; }); if (!su) return; openRecordDetails({ name: su.name, extra: su.code, role: su.teacherName ? ('Teacher: ' + su.teacherName) : 'Subject', associations: [{ name: su.code, type: 'Code', grade: su.className || '' }] }, 'Subject Details'); };
window.openClassDetails = function(id) { const cl = classes.find(function(x){ return x.id === id; }); if (!cl) return; openRecordDetails({ name: cl.name, role: 'Class', associations: [{ name: cl.name, type: 'Class', grade: cl.teacherName || '' }] }, 'Class Details'); };

window.openUserDetails = function(userId) {
            const u = users.find(x => x.id === userId);
            if (!u) return;
            const detailsBody = document.getElementById('userDetailsBody');
            if (!detailsBody) return;
            const roleCard = (u.role === 'ADMIN')
                ? '<div style="padding:1rem;border:1px solid #E2E4EB;border-radius:8px;margin-top:1rem;"><div style="font-weight:600;color:#43506C;">Administrator</div><div style="font-size:14px;color:#718096;margin-top:0.25rem;">Full platform access. Manage users, teachers, students, parents, classes and subjects.</div></div>'
                : (u.role === 'TEACHER')
                    ? '<div style="padding:1rem;border:1px solid #E2E4EB;border-radius:8px;margin-top:1rem;">' +
                        '<div style="font-weight:600;color:#43506C;">Assigned Classes</div>' +
                        (teachersClasses(u.id).length ? teachersClasses(u.id).map(c => '<a href="#" onclick="openClassDetails(' + c.id + '); return false;" style="display:block;color:#4379EE;text-decoration:none;font-size:14px;margin-top:0.5rem;">' + escapeHtml(c.name || '') + ' &rarr;</a>').join('') : '<div style="font-size:14px;color:#718096;margin-top:0.5rem;">No classes assigned yet.</div>') +
                        '<div style="font-weight:600;color:#43506C;margin-top:1rem;">Subjects Taught</div>' +
                        (teachersSubjects(u.id).length ? teachersSubjects(u.id).map(s => '<div style="font-size:14px;color:#43506C;margin-top:0.5rem;">' + escapeHtml(s.name || '') + '</div>').join('') : '<div style="font-size:14px;color:#718096;margin-top:0.5rem;">No subjects assigned yet.</div>') +
                    '</div>'
                : (u.role === 'STUDENT')
                    ? '<div style="padding:1rem;border:1px solid #E2E4EB;border-radius:8px;margin-top:1rem;">' +
                        '<div style="font-weight:600;color:#43506C;">Class</div>' +
                        (studentsClass(u.id) ? '<a href="#" onclick="openClassDetails(' + studentsClass(u.id).id + '); return false;" style="display:block;color:#4379EE;text-decoration:none;font-size:14px;margin-top:0.5rem;">' + escapeHtml(studentsClass(u.id).name || '') + ' &rarr;</a>' : '<div style="font-size:14px;color:#718096;margin-top:0.5rem;">Not assigned to a class yet.</div>') +
                        '<div style="font-weight:600;color:#43506C;margin-top:1rem;">Linked Parent</div>' +
                        (linkedParentOf(u.id) ? '<a href="#" onclick="openParentDetails(' + linkedParentOf(u.id).id + '); return false;" style="display:block;color:#4379EE;text-decoration:none;font-size:14px;margin-top:0.5rem;">' + escapeHtml(linkedParentOf(u.id).name || '') + ' &rarr;</a>' : '<div style="font-size:14px;color:#718096;margin-top:0.5rem;">No parent linked yet.</div>') +
                    '</div>'
                : '<div style="padding:1rem;border:1px solid #E2E4EB;border-radius:8px;margin-top:1rem;">' +
                        '<div style="font-weight:600;color:#43506C;">Linked Children</div>' +
                        (childrenOf(u.id).length ? childrenOf(u.id).map(c => '<a href="#" onclick="openStudentDetails(' + c.id + '); return false;" style="display:block;color:#4379EE;text-decoration:none;font-size:14px;margin-top:0.5rem;">' + escapeHtml(c.name || '') + ' &rarr;</a>').join('') : '<div style="font-size:14px;color:#718096;margin-top:0.5rem;">No children linked yet.</div>') +
                    '</div>';

            const verifiedBadge = u.emailVerified ? '<span class="status-badge badge-green">Verified</span>' : '<span class="status-badge badge-red">Unverified</span>';
            const accountBadge = u.disabled ? '<span class="status-badge badge-red">Disabled</span>' : '<span class="status-badge badge-green">Active</span>';
            detailsBody.innerHTML =
                '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">' +
                    '<h3 style="margin:0;font-size:1.25rem;">' + escapeHtml(u.name || '') + '</h3>' +
                '</div>' +
                '<div style="display:grid;grid-template-columns:1fr 1fr;gap:1rem;">' +
                    '<div><div style="font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Email</div><div style="font-weight:500;margin-top:0.25rem;">' + escapeHtml(u.email || '') + '</div></div>' +
                    '<div><div style="font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Phone</div><div style="font-weight:500;margin-top:0.25rem;">' + escapeHtml(u.phone || '—') + '</div></div>' +
                    '<div><div style="font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Role</div><div style="font-weight:500;margin-top:0.25rem;">' + escapeHtml(u.role || '') + '</div></div>' +
                    '<div><div style="font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Email Status</div><div style="margin-top:0.25rem;">' + verifiedBadge + '</div></div>' +
                    '<div><div style="font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Account Status</div><div style="margin-top:0.25rem;">' + accountBadge + '</div></div>' +
                '</div>' +
                roleCard +
                '<div style="display:flex;gap:0.75rem;margin-top:1.5rem;">' +
                    '<button class="btn btn-primary" onclick="openEditUserModal(' + u.id + ', \'' + escapeJs(u.name) + '\', \'' + escapeJs(u.email) + '\', \'' + escapeJs(u.role) + '\', \'' + escapeJs(u.phone || '') + '\'); closeUserDetails();">Edit User</button>' +
                '</div>';

            window.switchSection('user-details');
        };

        window.closeUserDetails = function() {
            window.switchSection(window.previousSection || 'users');
        };

        function teachersClasses(userId) {
            return classes.filter(c => teachers.find(t => t.id === userId && (t.classIds || []).indexOf(c.id) >= 0));
        }
        function teachersSubjects(userId) {
            return subjects.filter(s => s.teacherId && s.teacherId === userId);
        }
        function studentsClass(userId) {
            return classes.find(c => (c.studentIds || []).indexOf(userId) >= 0);
        }
        function linkedParentOf(userId) {
            return parents.find(p => (p.children || []).indexOf(userId) >= 0);
        }
        function childrenOf(userId) {
            return (parents.find(p => p.id === userId) || {}).children || [];
        }

        function setupSearch() {
            const input = document.getElementById('userSearchInput');
            if (!input) return;
            input.addEventListener('input', function() {
                const q = (this.value || '').toLowerCase();
                document.querySelectorAll('#userTableBodyRows tr').forEach(tr => {
                    tr.style.display = tr.textContent.toLowerCase().indexOf(q) >= 0 ? '' : 'none';
                });
            });
        }

        function renderTeachersTable() {
            const tbody = document.getElementById('teachersTableBody');
            if (!tbody) return;
            if (teachers.length === 0) {
                tbody.innerHTML = '<tr><td style="text-align:center;padding:1.5rem;">No teachers found. Add a user with the TEACHER role first.</td></tr>';
                return;
            }
            const rows = users.filter(u => u.role === 'TEACHER');
            let html = rows.map(u => {
                const ts = teachers.filter(t => t.id === u.id);
                const info = ts.length > 0 ? ts[0] : {};
                const classesTaught = (info.classes || []).map(escapeHtml).join(', ') || 'None';
                return '<tr>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-weight:500;">' + escapeHtml(u.name || '') + '</div><div style="font-size:12px;color:#718096;">' + escapeHtml(u.email || '') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-size:14px;color:#43506C;">' + classesTaught + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' + (u.disabled ? '<span class="status-badge badge-red">Disabled</span>' : '<span class="status-badge badge-green">Active</span>') + '</td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' +
                        '<button class="btn btn-outline btn-sm" onclick="openTeacherDetails(' + u.id + ')">View / Manage</button>' +
                    '</td>' +
                '</tr>';
            }).join('');
            tbody.innerHTML = rows.length ? html : '<tr><td style="text-align:center;padding:1.5rem;">No teachers found.</td></tr>';
        }

        function renderStudentsTable() {
            const tbody = document.getElementById('studentsTableBody');
            if (!tbody) return;
            if (students.length === 0) {
                tbody.innerHTML = '<tr><td style="text-align:center;padding:1.5rem;">No students found.</td></tr>';
                return;
            }
            let html = students.map(s => {
                return '<tr>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-weight:500;">' + escapeHtml(s.name || '') + '</div><div style="font-size:12px;color:#718096;">' + escapeHtml(s.email || '') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' + (s.class && s.class !== 'Unassigned' ? escapeHtml(s.class) : '<span style="color:#98A2B3;">Unassigned</span>') + '</td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' + (s.emailVerified ? '<span class="status-badge badge-green">Verified</span>' : '<span class="status-badge badge-amber">Unverified</span>') + '</td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' +
                        '<button class="btn btn-outline btn-sm" onclick="openStudentDetails(' + s.id + ')">View / Manage</button>' +
                    '</td>' +
                '</tr>';
            }).join('');
            tbody.innerHTML = html;
        }

        function renderParentsTable() {
            const tbody = document.getElementById('parentsTableBody');
            if (!tbody) return;
            const rowUsers = users.filter(u => u.role === 'PARENT');
            if (rowUsers.length === 0) {
                tbody.innerHTML = '<tr><td style="text-align:center;padding:1.5rem;">No parents found. Add a user with the PARENT role first.</td></tr>';
                return;
            }
            let html = rowUsers.map(u => {
                const kids = parents.filter(p => p.id === u.id);
                const childNames = (kids.length > 0 && kids[0].children ? kids[0].children : []).map(escapeHtml).join(', ') || 'None linked';
                return '<tr>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-weight:500;">' + escapeHtml(u.name || '') + '</div><div style="font-size:12px;color:#718096;">' + escapeHtml(u.email || '') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-size:14px;color:#43506C;">' + childNames + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' + (u.disabled ? '<span class="status-badge badge-red">Disabled</span>' : '<span class="status-badge badge-green">Active</span>') + '</td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' +
                        '<button class="btn btn-outline btn-sm" onclick="openParentDetails(' + u.id + ')">View / Manage</button>' +
                    '</td>' +
                '</tr>';
            }).join('');
            tbody.innerHTML = html;
        }

        function renderSubjectsTable() {
            const tbody = document.getElementById('subjectsTableBody');
            if (!tbody) return;
            const subs = subjects || [];
            if (subs.length === 0) {
                tbody.innerHTML = '<tr><td style="text-align:center;padding:1.5rem;">No subjects found in the catalog.</td></tr>';
                return;
            }
            let html = subs.map(s => {
                return '<tr>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-weight:500;">' + escapeHtml(s.name || '') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-size:14px;color:#718096;">' + escapeHtml(s.className || (s.classId ? 'Class #' + s.classId : 'General')) + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' +
                        '<button class="btn btn-outline btn-sm" onclick="openSubjectDetails(' + s.id + ')">View / Manage</button>' +
                    '</td>' +
                '</tr>';
            }).join('');
            tbody.innerHTML = html;
        }

        function renderClassesTable() {
            const tbody = document.getElementById('classesTableBody');
            if (!tbody) return;
            if (classes.length === 0) {
                tbody.innerHTML = '<tr><td style="text-align:center;padding:1.5rem;">No classes created yet. Allocate a student below to create one.</td></tr>';
                return;
            }
            let html = '';
            classes.forEach(c => {
                const count = students.filter(s => s.class === c).length;
                html += '<tr>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-weight:500;">' + escapeHtml(c) + '</div><div style="font-size:12px;color:#718096;">' + count + ' students</div></td>' +
                '</tr>';
            });
            tbody.innerHTML = html;
        }

        function renderAnnouncements() {
            const container = document.getElementById('announcementsList');
            if (!container) return;
            if (announcements.length === 0) {
                container.innerHTML = '<div class="empty-state"><p>No announcements broadcast yet.</p></div>';
                return;
            }
            let html = '';
            announcements.slice(0, 20).forEach(a => {
                let audienceLabel = 'Everyone';
                const aud = (a.audience || '').toUpperCase();
                if (aud.indexOf('STUDENT') >= 0) audienceLabel = 'Students';
                else if (aud.indexOf('TEACHER') >= 0) audienceLabel = 'Teachers';
                html += '<div class="announcement-card">' +
                    '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">' +
                        '<h3 style="font-size:15px;font-weight:600;margin:0;">' + escapeHtml(a.title || '') + '</h3>' +
                        '<span class="status-badge badge-blue">' + audienceLabel + '</span>' +
                    '</div>' +
                    '<p style="font-size:14px;color:#43506C;margin:0 0 10px;">' + escapeHtml(a.content || '') + '</p>' +
                    '<div style="font-size:12px;color:#98A2B3;">' + (a.author ? escapeHtml(a.author.name) : 'Admin') + ' &middot; ' + escapeHtml(shortDateTime(a.createdAt)) + '</div>' +
                '</div>';
            });
            container.innerHTML = html;
        }

        // ===== Navigation =====
        window.previousSection = 'overview';
        window.loadSchoolProfile = async function() {
            try {
                const res = await fetch(contextPath + '/admin/api/school');
                const json = await res.json();
                if (!json.success) return;
                document.getElementById('schoolNameInput').value = json.name || '';
                document.getElementById('schoolEmailInput').value = json.email || '';
                document.getElementById('schoolPhoneInput').value = json.phone || '';
                document.getElementById('schoolDomainInput').value = json.domain || '';
                document.getElementById('schoolLogoInput').value = json.logoUrl || '';
                document.getElementById('schoolAddressInput').value = json.address || '';
                document.getElementById('schoolDescriptionInput').value = json.description || '';
                document.getElementById('schoolPlanLabel').textContent = json.plan || '—';
            } catch (err) {
                showToast('Could not load the school profile', 'error');
            }
        };

        window.saveSchoolProfile = async function(event) {
            event.preventDefault();
            const btn = document.getElementById('schoolProfileSaveBtn');
            btn.disabled = true;
            const formData = new URLSearchParams();
            formData.set('action', 'update');
            formData.set('name', document.getElementById('schoolNameInput').value);
            formData.set('email', document.getElementById('schoolEmailInput').value);
            formData.set('phone', document.getElementById('schoolPhoneInput').value);
            formData.set('domain', document.getElementById('schoolDomainInput').value);
            formData.set('logoUrl', document.getElementById('schoolLogoInput').value);
            formData.set('address', document.getElementById('schoolAddressInput').value);
            formData.set('description', document.getElementById('schoolDescriptionInput').value);
            try {
                const res = await fetch(contextPath + '/admin/api/school', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message || 'School profile updated', 'success');
                    if (json.name) {
                        document.querySelectorAll('.current-school-name, .logo-text').forEach(el => { el.textContent = json.name; });
                    }
                } else {
                    showToast(json.message || 'Could not save the school profile', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            } finally {
                btn.disabled = false;
            }
        };

        window.switchSection = function(sectionId) {
            if (sectionId !== 'user-details') {
                window.previousSection = sectionId;
            }
            document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
            document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

            const target = document.getElementById('section-' + sectionId);
            if (!target) return;
            target.classList.add('active');

            document.querySelectorAll('.nav-item').forEach(link => {
                const href = link.getAttribute('href') || '';
                const linkSection = matchSectionParam(href);
                const isActive = linkSection === sectionId || (sectionId === 'overview' && linkSection === null);
                if (isActive) link.classList.add('active');
            });

            const url = new URL(window.location);
            url.searchParams.delete('success');
            if (sectionId === 'overview') url.searchParams.delete('section');
            else url.searchParams.set('section', sectionId);
            window.history.replaceState({}, '', url);
        };

        function matchSectionParam(href) {
            if (!href) return null;
            const q = href.indexOf('?');
            if (q < 0) return null;
            const params = new URLSearchParams(href.slice(q + 1));
            return params.get('section');
        }

        function setupNavigation() {
            const urlParams = new URLSearchParams(window.location.search);
            window.switchSection(urlParams.get('section') || 'overview');
            window.loadSchoolProfile();
        }

        function checkQueryMessages() {
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('success') === 'allocated') showToast('Student allocated to class successfully.', 'success');
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

        window.openAddUserModal = function() { openModal('addUserModal'); };
        window.closeAddUserModal = function() { closeModal('addUserModal'); };

        window.openAllocateStudentModal = function() {
            populateAllocateSelects();
            openModal('allocateModal');
        };
        window.closeAllocateStudentModal = function() { closeModal('allocateModal'); };

        function populateAllocateSelects() {
            const studentSel = document.getElementById('allocateStudentId');
            if (studentSel) {
                let opts = students.map(s => {
                    return '<option value="' + s.id + '">' + escapeHtml(s.name) + ' (' + escapeHtml(s.class !== 'Unassigned' ? s.class : 'Unassigned') + ')</option>';
                }).join('');
                if (!opts) opts = '<option value="" disabled>No students available</option>';
                studentSel.innerHTML = opts;
            }
            const teacherSel = document.getElementById('allocateTeacherId');
            if (teacherSel) {
                let opts = teachers.map(t => {
                    return '<option value="' + t.id + '">' + escapeHtml(t.name) + ' (' + escapeHtml(t.subjects || 'No classes') + ')</option>';
                }).join('');
                if (!opts) opts = '<option value="" disabled>No teachers available</option>';
                teacherSel.innerHTML = opts;
            }
        }

        window.openEditUserModal = function(id, name, email, role, phone) {
            document.getElementById('editUserId').value = id;
            document.getElementById('editUserName').value = name;
            document.getElementById('editUserEmail').value = email;
            document.getElementById('editUserRole').value = role;
            document.getElementById('editUserPhone').value = phone;
            openModal('editUserModal');
        };
        window.closeEditUserModal = function() { closeModal('editUserModal'); };

        window.openAnnouncementModal = function() { openModal('announcementModal'); };
        window.closeAnnouncementModal = function() { closeModal('announcementModal'); };

        window.openAddSubjectModal = function() { openModal('addSubjectModal'); };
        window.closeAddSubjectModal = function() { closeModal('addSubjectModal'); };

        window.openAddClassModal = function() { openModal('addClassModal'); };
        window.closeAddClassModal = function() { closeModal('addClassModal'); };

        // ===== API Actions =====
        window.submitAddSubject = async function(e) {
            e.preventDefault();
            const formData = new URLSearchParams(new FormData(e.target));
            formData.set('action', 'create');
            try {
                const res = await fetch(contextPath + '/api/admin/subjects', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message, 'success');
                    closeAddSubjectModal();
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    showToast(json.message || 'Failed to create subject', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        window.submitAddClass = async function(e) {
            e.preventDefault();
            const formData = new URLSearchParams(new FormData(e.target));
            formData.set('action', 'create');
            try {
                const res = await fetch(contextPath + '/api/admin/classes', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message, 'success');
                    closeAddClassModal();
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    showToast(json.message || 'Failed to create class', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        window.submitAddUser = async function(e) {
            e.preventDefault();
            const formData = new URLSearchParams(new FormData(e.target));
            try {
                const res = await fetch(contextPath + '/api/admin/addUser', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message, 'success');
                    closeAddUserModal();
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    showToast(json.message || 'Failed to add user', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        window.submitEditUser = async function(e) {
            e.preventDefault();
            const formData = new URLSearchParams(new FormData(e.target));
            try {
                const res = await fetch(contextPath + '/api/admin/editUser', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message, 'success');
                    closeEditUserModal();
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    showToast(json.message || 'Failed to edit user', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        window.submitAnnouncement = async function(e) {
            e.preventDefault();
            const formData = new URLSearchParams(new FormData(e.target));
            try {
                const res = await fetch(contextPath + '/api/admin/createAnnouncement', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message, 'success');
                    closeAnnouncementModal();
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    showToast(json.message || 'Failed to create announcement', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        // ===== Utilities =====
        function shortDate(d) {
            if (!d) return '';
            const parsed = parseDate(d);
            if (!parsed || isNaN(parsed.getTime())) return '';
            return parsed.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ', ' + parsed.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
        }
        function shortDateTime(d) {
            return shortDate(d);
        }
        function parseDate(d) {
            if (!d) return null;
            if (d instanceof Date) return d;
            if (Array.isArray(d)) return new Date(d[0], (d[1] || 1) - 1, d[2] || 1, d[3] || 0, d[4] || 0);
            const str = String(d);
            if (/^\d{4}-\d{2}-\d{2}T/.test(str)) return new Date(str);
            if (/^\d{4}-\d{2}-\d{2}$/.test(str)) return new Date(str + 'T00:00:00');
            return new Date(d);
        }
        function escapeHtml(s) { return s ? String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;') : ''; }
        function escapeJs(s) { return s ? String(s).replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"') : ''; }
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

    <!-- Dashboard Section -->
    <section class="content-section active" id="section-overview">
        <div class="section-header">
            <h2 class="section-title" style="font-size:1.5rem;">Admin Overview</h2>
        </div>

        <div class="stats-grid" style="display:grid;grid-template-columns:repeat(auto-fit, minmax(220px, 1fr));gap:1.25rem;margin-bottom:2rem;">
            <div class="stat-card" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.25rem;display:flex;align-items:center;gap:1rem;">
                <div class="stat-icon" style="width:48px;height:48px;border-radius:12px;background:rgba(59,130,246,0.1);color:#3b82f6;display:flex;align-items:center;justify-content:center;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                </div>
                <div class="stat-info">
                    <span class="stat-value" id="statStudents" style="display:block;font-size:1.5rem;font-weight:700;line-height:1;">0</span>
                    <span class="stat-label" style="display:block;font-size:0.875rem;color:var(--text-muted);margin-top:0.25rem;">Total Students</span>
                </div>
            </div>

            <div class="stat-card" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.25rem;display:flex;align-items:center;gap:1rem;">
                <div class="stat-icon" style="width:48px;height:48px;border-radius:12px;background:rgba(16,185,129,0.1);color:#10b981;display:flex;align-items:center;justify-content:center;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
                </div>
                <div class="stat-info">
                    <span class="stat-value" id="statTeachers" style="display:block;font-size:1.5rem;font-weight:700;line-height:1;">0</span>
                    <span class="stat-label" style="display:block;font-size:0.875rem;color:var(--text-muted);margin-top:0.25rem;">Total Teachers</span>
                </div>
            </div>

            <div class="stat-card" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.25rem;display:flex;align-items:center;gap:1rem;">
                <div class="stat-icon" style="width:48px;height:48px;border-radius:12px;background:rgba(139,92,246,0.1);color:#8b5cf6;display:flex;align-items:center;justify-content:center;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
                </div>
                <div class="stat-info">
                    <span class="stat-value" id="statClasses" style="display:block;font-size:1.5rem;font-weight:700;line-height:1;">0</span>
                    <span class="stat-label" style="display:block;font-size:0.875rem;color:var(--text-muted);margin-top:0.25rem;">Classes</span>
                </div>
            </div>

            <div class="stat-card" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.25rem;display:flex;align-items:center;gap:1rem;">
                <div class="stat-icon" style="width:48px;height:48px;border-radius:12px;background:rgba(245,158,11,0.1);color:#f59e0b;display:flex;align-items:center;justify-content:center;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                </div>
                <div class="stat-info">
                    <span class="stat-value" id="statAssignments" style="display:block;font-size:1.5rem;font-weight:700;line-height:1;">0</span>
                    <span class="stat-label" style="display:block;font-size:0.875rem;color:var(--text-muted);margin-top:0.25rem;">Assignments</span>
                </div>
            </div>

            <div class="stat-card" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.25rem;display:flex;align-items:center;gap:1rem;">
                <div class="stat-icon" style="width:48px;height:48px;border-radius:12px;background:rgba(99,102,241,0.1);color:#6366f1;display:flex;align-items:center;justify-content:center;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                </div>
                <div class="stat-info">
                    <span class="stat-value" id="statSubjects" style="display:block;font-size:1.5rem;font-weight:700;line-height:1;">0</span>
                    <span class="stat-label" style="display:block;font-size:0.875rem;color:var(--text-muted);margin-top:0.25rem;">Subjects</span>
                </div>
            </div>
        </div>

        <div style="display:grid;grid-template-columns:2fr 1fr;gap:1.5rem;">
            <div class="section-block" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.5rem;">
                <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:1rem;">
                    <h2 class="section-title" style="margin:0;font-size:1.125rem;">System Activity Log</h2>
                    <a href="#" onclick="switchSection('users'); return false;" style="color:#4379EE;text-decoration:none;font-size:0.875rem;font-weight:500;">View Users &rarr;</a>
                </div>
                <div id="activityFeed"></div>
            </div>
            <div class="section-block" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.5rem;height:fit-content;">
                <h2 class="section-title" style="margin-bottom:1rem;font-size:1.125rem;">Quick Actions</h2>
                <div style="display:flex;flex-direction:column;gap:0.75rem;">
                    <button class="btn btn-primary" style="width:100%;justify-content:flex-start;padding:0.75rem 1rem;" onclick="switchSection('users')">
                        <span style="margin-right:0.5rem;">👥</span> Manage Users
                    </button>
                    <button class="btn btn-outline" style="width:100%;justify-content:flex-start;padding:0.75rem 1rem;" onclick="switchSection('classes')">
                        <span style="margin-right:0.5rem;">📚</span> Manage Classes
                    </button>
                    <button class="btn btn-outline" style="width:100%;justify-content:flex-start;padding:0.75rem 1rem;" onclick="switchSection('announcements')">
                        <span style="margin-right:0.5rem;">📣</span> Broadcast Announcement
                    </button>
                </div>
            </div>
        </div>
    </section>

    <!-- Users Section -->
    <section class="content-section" id="section-users">
        <div class="section-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;margin:0;">User Management</h2>
            <button class="btn btn-primary" onclick="openAddUserModal()">➕ Add User</button>
        </div>

        <div class="section-block" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;overflow:hidden;">
            <div style="padding:1rem 1.5rem;border-bottom:1px solid var(--border-color);display:flex;justify-content:space-between;align-items:center;background:var(--bg-hover);">
                <h3 style="font-size:1rem;font-weight:600;margin:0;">All System Users</h3>
                <input type="text" id="userSearchInput" class="form-control" placeholder="Search users..." style="max-width: 250px;height:36px;font-size:0.875rem;">
            </div>
            <div style="overflow-x:auto;">
                <table class="data-table">
                    <thead>
                        <tr style="border-bottom:1px solid var(--border-color);">
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Name</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Email</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Status</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Role</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Actions</th>
                        </tr>
                    </thead>
                    <tbody id="userTableBodyRows">
                    </tbody>
                </table>
            </div>
        </div>
    </section>

    <!-- Classes Section -->
    <section class="content-section" id="section-classes">
        <div class="section-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;margin:0;">Classes &amp; Subjects</h2>
            <div style="display:flex;gap:0.75rem;">
                <button class="btn btn-outline" onclick="openAddClassModal()">Add Class</button>
                <button class="btn btn-primary" onclick="openAllocateStudentModal()">🎓 Allocate Student</button>
            </div>
        </div>
        <div class="section-block" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;overflow:hidden;">
            <div style="padding:1rem 1.5rem;border-bottom:1px solid var(--border-color);display:flex;justify-content:space-between;align-items:center;background:var(--bg-hover);">
                <h3 style="font-size:1rem;font-weight:600;margin:0;">Active Classes</h3>
            </div>
            <div style="overflow-x:auto;">
                <table class="data-table">
                    <thead>
                        <tr style="border-bottom:1px solid var(--border-color);">
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Class Name</th>
                        </tr>
                    </thead>
                    <tbody id="classesTableBody">
                    </tbody>
                </table>
            </div>
        </div>
    </section>

    <!-- Allocate Student Modal -->
    <div id="allocateModal" class="modal-overlay" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2>Allocate Student to Class</h2>
                <button type="button" class="modal-close" onclick="closeAllocateStudentModal()">&times;</button>
            </div>
              <form action="${pageContext.request.contextPath}/admin/allocate" method="POST">
                  <input type="hidden" name="csrfToken" value="${csrfToken}">
                  <input type="hidden" name="action" value="allocate">
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Student</label>
                    <select name="studentId" id="allocateStudentId" class="form-control" required></select>
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Teacher</label>
                    <select name="teacherId" id="allocateTeacherId" class="form-control" required></select>
                </div>
                <div class="form-group" style="margin-bottom:1.5rem;">
                    <label class="form-label">Class Name</label>
                    <input type="text" name="className" class="form-control" placeholder="e.g. Mathematics 101" required>
                </div>
                <div style="display:flex;justify-content:flex-end;gap:1rem;">
                    <button type="button" class="btn btn-outline" onclick="closeAllocateStudentModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Allocate</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Announcements Section -->
    <section class="content-section" id="section-announcements">
        <div class="section-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;margin:0;">Announcements</h2>
            <button class="btn btn-primary" onclick="openAnnouncementModal()">➕ New Announcement</button>
        </div>
        <div id="announcementsList"></div>
    </section>

    <!-- Teachers Section -->
    <section class="content-section" id="section-teachers">
        <div class="section-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;margin:0;">Teachers</h2>
            <button class="btn btn-primary" onclick="openAddUserModal()">➕ Add Teacher</button>
        </div>
        <div style="background:var(--bg-card);border:1px solid #E2E4EB;border-radius:10px;overflow:hidden;">
            <table style="width:100%;border-collapse:collapse;">
                <thead>
                    <tr style="text-align:left;background:#F7F8FA;">
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Name</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Classes</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Status</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Actions</th>
                    </tr>
                </thead>
                <tbody id="teachersTableBody"></tbody>
            </table>
        </div>
    </section>

    <!-- Students Section -->
    <section class="content-section" id="section-students">
        <div class="section-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;margin:0;">Students</h2>
            <button class="btn btn-primary" onclick="openAddUserModal()">➕ Add Student</button>
        </div>
        <div style="background:var(--bg-card);border:1px solid #E2E4EB;border-radius:10px;overflow:hidden;">
            <table style="width:100%;border-collapse:collapse;">
                <thead>
                    <tr style="text-align:left;background:#F7F8FA;">
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Name</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Class</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Verified</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Actions</th>
                    </tr>
                </thead>
                <tbody id="studentsTableBody"></tbody>
            </table>
        </div>
    </section>

    <!-- Parents Section -->
    <section class="content-section" id="section-parents">
        <div class="section-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;margin:0;">Parents</h2>
            <button class="btn btn-primary" onclick="openAddUserModal()">➕ Add Parent</button>
        </div>
        <div style="background:var(--bg-card);border:1px solid #E2E4EB;border-radius:10px;overflow:hidden;">
            <table style="width:100%;border-collapse:collapse;">
                <thead>
                    <tr style="text-align:left;background:#F7F8FA;">
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Parent</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Linked Students</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Status</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Actions</th>
                    </tr>
                </thead>
                <tbody id="parentsTableBody"></tbody>
            </table>
        </div>
    </section>

    <!-- Subjects Section -->
    <section class="content-section" id="section-subjects">
        <div class="section-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;margin:0;">Subjects</h2>
            <button class="btn btn-primary" onclick="openAddSubjectModal()">➕ Add Subject</button>
        </div>
        <div style="background:var(--bg-card);border:1px solid #E2E4EB;border-radius:10px;overflow:hidden;">
            <table style="width:100%;border-collapse:collapse;">
                <thead>
                    <tr style="text-align:left;background:#F7F8FA;">
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Subject</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Class</th>
                        <th style="padding:0.75rem 1.5rem;font-size:12px;text-transform:uppercase;letter-spacing:0.05em;color:#718096;">Actions</th>
                    </tr>
                </thead>
                <tbody id="subjectsTableBody"></tbody>
            </table>
        </div>
    </section>

    <!-- School Profile Section -->
    <section class="content-section" id="section-school">
        <div class="section-header" style="margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;margin:0;">School Profile</h2>
            <p style="font-size:0.875rem;color:var(--text-muted);margin:0.25rem 0 0;">This is how your school appears to everyone signed in to it.</p>
        </div>
        <div class="section-block" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:2rem;max-width:760px;">
            <form id="schoolProfileForm" onsubmit="saveSchoolProfile(event); return false;">
                <div style="display:flex;flex-direction:column;gap:1.25rem;">
                    <div>
                        <label class="form-label" for="schoolNameInput">School Name</label>
                        <input type="text" id="schoolNameInput" class="form-control" required maxlength="150">
                    </div>
                    <div style="display:grid;grid-template-columns:1fr 1fr;gap:1.25rem;">
                        <div>
                            <label class="form-label" for="schoolEmailInput">Contact Email</label>
                            <input type="email" id="schoolEmailInput" class="form-control" placeholder="office@school.rw">
                        </div>
                        <div>
                            <label class="form-label" for="schoolPhoneInput">Contact Phone</label>
                            <input type="text" id="schoolPhoneInput" class="form-control" placeholder="+250 ...">
                        </div>
                    </div>
                    <div>
                        <label class="form-label" for="schoolDomainInput">Domain</label>
                        <input type="text" id="schoolDomainInput" class="form-control" placeholder="school.rw">
                    </div>
                    <div>
                        <label class="form-label" for="schoolLogoInput">Logo URL</label>
                        <input type="url" id="schoolLogoInput" class="form-control" placeholder="https://...">
                    </div>
                    <div>
                        <label class="form-label" for="schoolAddressInput">Address</label>
                        <textarea id="schoolAddressInput" class="form-control" rows="2"></textarea>
                    </div>
                    <div>
                        <label class="form-label" for="schoolDescriptionInput">Description</label>
                        <textarea id="schoolDescriptionInput" class="form-control" rows="3" placeholder="A short description of the school"></textarea>
                    </div>
                    <div style="display:flex;align-items:center;justify-content:space-between;gap:1rem;padding-top:0.5rem;border-top:1px solid var(--border-color);">
                        <div style="font-size:0.8rem;color:var(--text-muted);">
                            Plan <strong id="schoolPlanLabel">—</strong> and school URL are managed by the platform administrator.
                        </div>
                        <button type="submit" class="btn btn-primary" id="schoolProfileSaveBtn">Save Profile</button>
                    </div>
                </div>
            </form>
        </div>
    </section>

    <!-- User Details Section -->
    <section class="content-section" id="section-user-details">
        <div class="section-header" style="display:flex;align-items:center;gap:1rem;margin-bottom:2rem;">
            <button class="btn btn-outline" onclick="closeUserDetails()" style="padding:0.5rem;display:flex;align-items:center;justify-content:center;border-radius:8px;">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            </button>
            <div>
                <h2 id="userDetailsHeader" class="section-title" style="font-size:1.5rem;margin:0;">Record Details</h2>
                <div style="font-size:0.875rem;color:var(--text-muted);margin-top:0.25rem;">View and manage this record</div>
            </div>
        </div>
        <div class="section-block" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:2rem;">
            <div id="userDetailsBody"></div>
        </div>
    </section>

    <!-- Add User Modal -->
    <div id="addUserModal" class="modal-overlay" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2>Add New User</h2>
                <button type="button" class="modal-close" onclick="closeAddUserModal()">&times;</button>
            </div>
            <form onsubmit="submitAddUser(event)">
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Full Name</label>
                    <input type="text" name="name" class="form-control" required>
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Email</label>
                    <input type="email" name="email" class="form-control" required>
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Phone</label>
                    <input type="text" name="phone" class="form-control">
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Password</label>
                    <input type="password" name="password" class="form-control" required minlength="6">
                </div>
                <div class="form-group" style="margin-bottom:1.5rem;">
                    <label class="form-label">Role</label>
                    <select name="role" class="form-control" required>
                        <option value="STUDENT">Student</option>
                        <option value="TEACHER">Teacher</option>
                        <option value="PARENT">Parent</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                </div>
                <div style="display:flex;justify-content:flex-end;gap:1rem;">
                    <button type="button" class="btn btn-outline" onclick="closeAddUserModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Add User</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Edit User Modal -->
    <div id="editUserModal" class="modal-overlay" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2>Edit User</h2>
                <button type="button" class="modal-close" onclick="closeEditUserModal()">&times;</button>
            </div>
            <form onsubmit="submitEditUser(event)">
                <input type="hidden" name="userId" id="editUserId">
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Full Name</label>
                    <input type="text" name="name" id="editUserName" class="form-control" required>
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Email</label>
                    <input type="email" name="email" id="editUserEmail" class="form-control" required>
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Phone</label>
                    <input type="text" name="phone" id="editUserPhone" class="form-control">
                </div>
                <div class="form-group" style="margin-bottom:1.5rem;">
                    <label class="form-label">Role</label>
                    <select name="role" id="editUserRole" class="form-control" required>
                        <option value="STUDENT">Student</option>
                        <option value="TEACHER">Teacher</option>
                        <option value="PARENT">Parent</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                </div>
                <div style="display:flex;justify-content:flex-end;gap:1rem;">
                    <button type="button" class="btn btn-outline" onclick="closeEditUserModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Save Changes</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Announcement Modal -->
    <div id="announcementModal" class="modal-overlay" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2>Broadcast Announcement</h2>
                <button type="button" class="modal-close" onclick="closeAnnouncementModal()">&times;</button>
            </div>
            <form onsubmit="submitAnnouncement(event)">
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Title</label>
                    <input type="text" name="title" class="form-control" required>
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Content</label>
                    <textarea name="content" class="form-control" rows="4" required></textarea>
                </div>
                <div class="form-group" style="margin-bottom:1.5rem;">
                    <label class="form-label">Audience</label>
                    <select name="audience" class="form-control" required>
                        <option value="ALL">Everyone</option>
                        <option value="STUDENTS">Students Only</option>
                        <option value="TEACHERS">Teachers Only</option>
                    </select>
                </div>
                <div style="display:flex;justify-content:flex-end;gap:1rem;">
                    <button type="button" class="btn btn-outline" onclick="closeAnnouncementModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Broadcast</button>
                </div>
            </form>
        </div>
    </div>

    <div id="addSubjectModal" class="modal-overlay" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2>Add Subject</h2>
                <button type="button" class="modal-close" onclick="closeAddSubjectModal()">&times;</button>
            </div>
            <form onsubmit="submitAddSubject(event)">
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Subject Code</label>
                    <input type="text" name="code" class="form-control" required maxlength="20" placeholder="e.g. MATH101">
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Subject Name</label>
                    <input type="text" name="name" class="form-control" required placeholder="e.g. Mathematics">
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Department</label>
                    <input type="text" name="department" class="form-control" placeholder="e.g. Science">
                </div>
                <div class="form-group" style="margin-bottom:1.5rem;">
                    <label class="form-label">Credits</label>
                    <input type="number" name="credits" class="form-control" min="0" max="60" step="1" placeholder="e.g. 3">
                </div>
                <div style="display:flex;justify-content:flex-end;gap:1rem;">
                    <button type="button" class="btn btn-outline" onclick="closeAddSubjectModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Create Subject</button>
                </div>
            </form>
        </div>
    </div>

    <div id="addClassModal" class="modal-overlay" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2>Add Class</h2>
                <button type="button" class="modal-close" onclick="closeAddClassModal()">&times;</button>
            </div>
            <form onsubmit="submitAddClass(event)">
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Class Name</label>
                    <input type="text" name="name" class="form-control" required maxlength="100" placeholder="e.g. Senior 1A">
                </div>
                <div class="form-group" style="margin-bottom:1.5rem;">
                    <label class="form-label">Description</label>
                    <textarea name="description" class="form-control" rows="3" placeholder="Optional"></textarea>
                </div>
                <div style="display:flex;justify-content:flex-end;gap:1rem;">
                    <button type="button" class="btn btn-outline" onclick="closeAddClassModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Create Class</button>
                </div>
            </form>
        </div>
    </div>

    </jsp:body>
</t:layout>