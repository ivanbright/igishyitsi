<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>

<t:layout role="SUPER_ADMIN" pageTitle="Platform Administration" activeSection="overview">
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

        const stats = serverData.stats || {};
        const schools = serverData.schools || [];
        const contextPath = '${pageContext.request.contextPath}';
        const metaCsrf = document.querySelector('meta[name="csrf-token"]');
        const csrfToken = (metaCsrf && metaCsrf.content) || serverData.csrfToken || '';

        function escapeHtml(s) { return s ? String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;') : ''; }
        function escapeJs(s) { return s ? String(s).replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"') : ''; }

        function init() {
            renderStats();
            renderSchools();
            const msg = new URLSearchParams(window.location.search).get('message');
            if (msg === 'platform_scope') {
                showToast('A school console is run by that school\'s own administrator.', 'info');
            }
        }

        function renderStats() {
            const el = function(id) { return document.getElementById(id); };
            if (el('statSchools')) el('statSchools').textContent = stats.schools || 0;
            if (el('statMembers')) el('statMembers').textContent = stats.totalMembers || 0;
        }

        function planBadge(plan) {
            const cls = plan === 'PREMIUM' ? 'badge-amber' : (plan === 'PLUS' ? 'badge-blue' : 'badge-green');
            return '<span class="status-badge ' + cls + '">' + escapeHtml(plan || 'FREE') + '</span>';
        }

        function renderSchools() {
            const tbody = document.getElementById('schoolTableBody');
            if (!tbody) return;
            if (schools.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:1.5rem;">No schools yet. Create your first school to get going.</td></tr>';
                return;
            }
            let html = '';
            schools.forEach(s => {
                const statusBadge = s.active ? '<span class="status-badge badge-green">Active</span>' : '<span class="status-badge badge-red">Suspended</span>';
                html += '<tr>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="font-weight:500;">' + escapeHtml(s.name || '') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="color:#718096;font-size:14px;">' + escapeHtml(s.slug || '') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="color:#718096;font-size:14px;">' + escapeHtml(s.domain || '—') + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' + planBadge(s.plan) + '</td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;">' + statusBadge + '</td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;"><div style="color:#718096;">' + (s.memberCount || 0) + '</div></td>' +
                    '<td style="padding:0.75rem 1.5rem;border-bottom:1px solid #E2E4EB;white-space:nowrap;">' +
                        '<button class="btn btn-outline btn-sm" onclick="openAddAdminModal(' + s.id + ')">Add Admin</button> ' +
                        '<button class="btn btn-outline btn-sm" onclick="toggleSchool(' + s.id + ')">' + (s.active ? 'Suspend' : 'Activate') + '</button> ' +
                        '<button class="btn btn-outline btn-sm" onclick="openEditSchoolModal(' + s.id + ')">Edit</button> ' +
                        '<button class="btn btn-outline btn-sm" onclick="deleteSchool(' + s.id + ')">Delete</button>' +
                    '</td>' +
                '</tr>';
            });
            tbody.innerHTML = html;
        }

        // ===== Modal helpers =====
        window.syncSlug = function() {
            const nameEl = document.getElementById('name');
            const slugEl = document.getElementById('slug');
            if (!nameEl || !slugEl) return;
            if (slugEl.dataset.touched === '1') return;
            slugEl.value = nameEl.value.toLowerCase()
                .replace(/[^a-z0-9]+/g, '-')
                .replace(/^-+|-+$/g, '');
        };
        window.openCreateSchoolModal = function() {
            document.getElementById('schoolModalTitle').textContent = 'Create School';
            document.getElementById('schoolForm').reset();
            document.getElementById('schoolId').value = '';
            document.getElementById('slug').dataset.touched = '';
            document.getElementById('slugField').style.display = '';
            document.getElementById('adminFields').style.display = '';
            openModal('schoolModal');
        };
        window.openEditSchoolModal = function(schoolId) {
            const s = schools.find(x => x.id === schoolId);
            if (!s) return;
            document.getElementById('schoolModalTitle').textContent = 'Edit School';
            document.getElementById('schoolId').value = s.id;
            document.getElementById('name').value = s.name || '';
            document.getElementById('domain').value = s.domain || '';
            document.getElementById('plan').value = s.plan || 'FREE';
            document.getElementById('active').checked = !!s.active;
            document.getElementById('slugField').style.display = 'none';
            document.getElementById('adminFields').style.display = 'none';
            openModal('schoolModal');
        };
        window.closeSchoolModal = function() { closeModal('schoolModal'); };

        function openModal(id) { document.getElementById(id).style.display = 'flex'; }
        function closeModal(id) { document.getElementById(id).style.display = 'none'; }

        // ===== API actions =====
        window.submitSchoolForm = async function(e) {
            e.preventDefault();
            const schoolId = document.getElementById('schoolId').value;
            const action = schoolId ? 'update' : 'create';
            const formData = new URLSearchParams(new FormData(e.target));
            if (schoolId) formData.set('id', schoolId);
            try {
                const res = await fetch(contextPath + '/api/super/schools?action=' + action, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message, 'success');
                    closeSchoolModal();
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    showToast(json.message || 'Request failed', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        window.openAddAdminModal = function(schoolId) {
            const school = (serverData.schools || []).find(s => s.id === schoolId);
            document.getElementById('addAdminSchool').value = schoolId;
            document.getElementById('addAdminSchoolName').textContent = school ? school.name : '';
            document.getElementById('addAdminName').value = '';
            document.getElementById('addAdminEmail').value = '';
            document.getElementById('addAdminPassword').value = '';
            openModal('addAdminModal');
        };

        window.submitAddAdmin = async function() {
            const id = document.getElementById('addAdminSchool').value;
            const formData = new URLSearchParams();
            formData.set('id', id);
            formData.set('adminName', document.getElementById('addAdminName').value);
            formData.set('adminEmail', document.getElementById('addAdminEmail').value);
            formData.set('adminPassword', document.getElementById('addAdminPassword').value);
            try {
                const res = await fetch(contextPath + '/api/super/schools?action=addAdmin', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    closeModal('addAdminModal');
                    showToast(json.message, 'success');
                    await loadSchools();
                } else {
                    showToast(json.message || 'Request failed', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        window.toggleSchool = async function(schoolId) {
            const formData = new URLSearchParams();
            formData.set('id', schoolId);
            try {
                const res = await fetch(contextPath + '/api/super/schools?action=toggle', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message, 'success');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    showToast(json.message || 'Request failed', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        window.deleteSchool = async function(schoolId) {
            if (!confirm('Delete this school? This cannot be undone and only works while the school has no members.')) return;
            const formData = new URLSearchParams();
            formData.set('id', schoolId);
            try {
                const res = await fetch(contextPath + '/api/super/schools?action=delete', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'X-CSRF-Token': csrfToken },
                    body: formData.toString()
                });
                const json = await res.json();
                if (json.success) {
                    showToast(json.message, 'success');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    showToast(json.message || 'Request failed', 'error');
                }
            } catch (err) {
                showToast('Network error', 'error');
            }
        };

        document.addEventListener('DOMContentLoaded', init);
    })();
    </script>
    </jsp:attribute>

    <jsp:attribute name="topBarActions">
        <button class="btn btn-primary" style="height:38px;font-size:0.85rem;" onclick="openCreateSchoolModal()">＋ New School</button>
    </jsp:attribute>

    <jsp:body>
    <script id="server-data" type="application/json">${realData != null && realData != '' ? realData : "{}"}</script>
    <script>
        try { window.serverData = JSON.parse(document.getElementById('server-data').textContent); }
        catch (e) { window.serverData = {}; console.error('Failed to parse serverData', e); }
    </script>
    <div style="padding:1.5rem;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem;">
            <h2 class="section-title" style="font-size:1.5rem;">Platform Administration</h2>
        </div>

        <div class="stats-grid" style="display:grid;grid-template-columns:repeat(auto-fit, minmax(220px, 1fr));gap:1.25rem;margin-bottom:2rem;">
            <div class="stat-card" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.25rem;display:flex;align-items:center;gap:1rem;">
                <div class="stat-icon" style="width:48px;height:48px;border-radius:12px;background:rgba(67,121,238,0.1);color:#4379EE;display:flex;align-items:center;justify-content:center;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 21h18M5 21V7l7-4 7 4v14"/><path d="M9 21v-4h6v4"/></svg>
                </div>
                <div class="stat-info">
                    <span class="stat-value" id="statSchools" style="display:block;font-size:1.5rem;font-weight:700;line-height:1;">0</span>
                    <span class="stat-label" style="display:block;font-size:0.875rem;color:var(--text-muted);margin-top:0.25rem;">Schools</span>
                </div>
            </div>
            <div class="stat-card" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;padding:1.25rem;display:flex;align-items:center;gap:1rem;">
                <div class="stat-icon" style="width:48px;height:48px;border-radius:12px;background:rgba(16,185,129,0.1);color:#10b981;display:flex;align-items:center;justify-content:center;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                </div>
                <div class="stat-info">
                    <span class="stat-value" id="statMembers" style="display:block;font-size:1.5rem;font-weight:700;line-height:1;">0</span>
                    <span class="stat-label" style="display:block;font-size:0.875rem;color:var(--text-muted);margin-top:0.25rem;">Accounts</span>
                </div>
            </div>
        </div>

        <div class="section-block" style="background:var(--bg-card);border:1px solid var(--border-color);border-radius:12px;overflow:hidden;">
            <div style="padding:1rem 1.5rem;border-bottom:1px solid var(--border-color);display:flex;justify-content:space-between;align-items:center;background:var(--bg-hover);">
                <h3 style="font-size:1rem;font-weight:600;margin:0;">All Schools</h3>
            </div>
            <div style="overflow-x:auto;">
                <table class="data-table">
                    <thead>
                        <tr style="border-bottom:1px solid var(--border-color);">
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Name</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Slug</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Domain</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Plan</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Status</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Members</th>
                            <th style="padding:1rem 1.5rem;font-weight:600;color:var(--text-muted);font-size:0.75rem;text-transform:uppercase;letter-spacing:0.05em;">Actions</th>
                        </tr>
                    </thead>
                    <tbody id="schoolTableBody"></tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- Create / Edit School Modal -->
    <div id="schoolModal" class="modal-overlay" style="display:none;">
        <div class="modal">
            <div class="modal-header">
                <h2 id="schoolModalTitle">Create School</h2>
                <button type="button" class="modal-close" onclick="closeSchoolModal()">&times;</button>
            </div>
            <form id="schoolForm" onsubmit="submitSchoolForm(event)">
                <input type="hidden" id="schoolId" name="id">
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">School Name</label>
                      <input type="text" id="name" name="name" class="form-control" placeholder="e.g. Green Hills Academy" required oninput="syncSlug()">
                  </div>
                  <div class="form-group" style="margin-bottom:1rem;" id="slugField">
                      <label class="form-label">Slug (url identifier)</label>
                      <input type="text" id="slug" name="slug" class="form-control" placeholder="auto-generated from the name" oninput="this.dataset.touched='1'">
                </div>
                <div class="form-group" style="margin-bottom:1rem;">
                    <label class="form-label">Domain (optional)</label>
                    <input type="text" id="domain" name="domain" class="form-control" placeholder="e.g. greenhills.ac.rw">
                </div>
                  <div class="form-group" style="margin-bottom:1rem;">
                      <label class="form-label">Plan</label>
                      <select id="plan" name="plan" class="form-control">
                          <option value="FREE">FREE</option>
                          <option value="PLUS">PLUS</option>
                          <option value="PREMIUM">PREMIUM</option>
                      </select>
                  </div>
                  <div id="adminFields">
                      <h3 style="font-size:0.9rem;font-weight:600;margin:1.5rem 0 0.25rem;">School Administrator</h3>
                      <p style="font-size:0.8rem;color:#718096;margin:0 0 1rem;">This account runs the school. The platform administrator does not have access to it.</p>
                      <div class="form-group" style="margin-bottom:1rem;">
                          <label class="form-label">Administrator Name</label>
                          <input type="text" id="adminName" name="adminName" class="form-control" placeholder="e.g. School Principal">
                      </div>
                      <div class="form-group" style="margin-bottom:1rem;">
                          <label class="form-label">Administrator Email</label>
                          <input type="email" id="adminEmail" name="adminEmail" class="form-control" placeholder="principal@school.rw">
                      </div>
                      <div class="form-group" style="margin-bottom:1rem;">
                          <label class="form-label">Temporary Password</label>
                          <input type="text" id="adminPassword" name="adminPassword" class="form-control" minlength="8" placeholder="at least 8 characters">
                      </div>
                  </div>
                <div class="form-group" style="margin-bottom:1.5rem;display:flex;align-items:center;gap:0.5rem;">
                    <input type="checkbox" id="active" name="active" value="true" style="width:auto;">
                    <label class="form-label" for="active" style="margin-bottom:0;">Active</label>
                </div>
                <div style="display:flex;justify-content:flex-end;gap:1rem;">
                    <button type="button" class="btn btn-outline" onclick="closeSchoolModal()">Cancel</button>
                    <button type="submit" class="btn btn-primary">Save School</button>
                </div>
            </form>
        </div>
    </div>

    <div id="addAdminModal" class="modal-overlay" style="display:none;">
        <div class="modal-content">
            <form onsubmit="submitAddAdmin(); return false;">
                <div class="modal-header">
                    <h2>Add Administrator</h2>
                    <button type="button" class="modal-close" onclick="closeModal('addAdminModal')">&times;</button>
                </div>
                <div class="modal-body">
                    <p style="font-size:0.85rem;color:#718096;margin:0 0 1rem;">Adds an administrator to <strong id="addAdminSchoolName"></strong>. The school and its data are not changed.</p>
                    <input type="hidden" id="addAdminSchool" value="">
                    <div class="form-group" style="margin-bottom:1rem;">
                        <label class="form-label">Administrator Name</label>
                        <input type="text" id="addAdminName" class="form-control" placeholder="e.g. School Principal">
                    </div>
                    <div class="form-group" style="margin-bottom:1rem;">
                        <label class="form-label">Administrator Email</label>
                        <input type="email" id="addAdminEmail" class="form-control" placeholder="principal@school.rw">
                    </div>
                    <div class="form-group" style="margin-bottom:1.5rem;">
                        <label class="form-label">Temporary Password</label>
                        <input type="text" id="addAdminPassword" class="form-control" minlength="8" placeholder="at least 8 characters">
                    </div>
                    <div style="display:flex;justify-content:flex-end;gap:1rem;">
                        <button type="button" class="btn btn-outline" onclick="closeModal('addAdminModal')">Cancel</button>
                        <button type="submit" class="btn btn-primary">Add Administrator</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
    </jsp:body>
</t:layout>