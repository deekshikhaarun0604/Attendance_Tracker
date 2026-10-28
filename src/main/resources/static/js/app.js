/**
 * SmartAttend - Main Application Script
 */

class SmartAttendApp {
    constructor() {
        this.token = localStorage.getItem('smartattend_token') || null;
        this.currentUser = JSON.parse(localStorage.getItem('smartattend_user') || 'null');
        this.currentView = 'dashboard';
        this.activeSessions = [];
        this.selectedSessionId = null;
        this.activeModalStudent = null;
        this.deptChart = null;
        this.barcodeChart = null;
    }

    async init() {
        if (this.token && this.currentUser) {
            try {
                // Verify token validity
                const res = await this.apiGet('/api/auth/me');
                this.currentUser = res;
                localStorage.setItem('smartattend_user', JSON.stringify(res));
                this.showAuthenticatedApp();
            } catch (e) {
                this.logout();
            }
        } else {
            this.showLoginView();
        }
    }

    // ==========================================================================
    // API Helper with JWT Authorization
    // ==========================================================================
    async apiCall(endpoint, method = 'GET', body = null) {
        const headers = { 'Content-Type': 'application/json' };
        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        const options = { method, headers };
        if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
            options.body = JSON.stringify(body);
        }

        const res = await fetch(endpoint, options);
        if (res.status === 401) {
            this.logout();
            throw new Error('Session expired. Please log in again.');
        }

        const contentType = res.headers.get('content-type');
        let data = null;
        if (contentType && contentType.includes('application/json')) {
            data = await res.json();
        } else {
            data = await res.text();
        }

        if (!res.ok) {
            const errorMsg = (data && data.error) ? data.error : (data && data.message) ? data.message : res.statusText;
            throw new Error(errorMsg);
        }

        return data;
    }

    apiGet(endpoint) { return this.apiCall(endpoint, 'GET'); }
    apiPost(endpoint, body) { return this.apiCall(endpoint, 'POST', body); }
    apiPut(endpoint, body) { return this.apiCall(endpoint, 'PUT', body); }
    apiPatch(endpoint, body) { return this.apiCall(endpoint, 'PATCH', body); }
    apiDelete(endpoint) { return this.apiCall(endpoint, 'DELETE'); }

    // ==========================================================================
    // Authentication & Role Management
    // ==========================================================================
    showLoginView() {
        document.getElementById('loginView').classList.remove('d-none');
        document.getElementById('appContainer').classList.add('d-none');
        document.getElementById('navUserMenu').classList.add('d-none');
        document.getElementById('navRoleBadge').innerText = 'Portal';
    }

    showAuthenticatedApp() {
        document.getElementById('loginView').classList.add('d-none');
        document.getElementById('appContainer').classList.remove('d-none');
        document.getElementById('navUserMenu').classList.remove('d-none');

        document.getElementById('navUserName').innerText = this.currentUser.fullName || this.currentUser.username;
        document.getElementById('navUserEmail').innerText = this.currentUser.email || '';
        
        let roleName = 'Student';
        if (this.currentUser.role === 'ROLE_ADMIN') roleName = 'Administrator';
        else if (this.currentUser.role === 'ROLE_FACULTY') roleName = 'Faculty';
        document.getElementById('navRoleBadge').innerText = roleName;

        this.renderSidebarMenu();

        // Initialize Chatbot with token & role
        if (window.smartChatbot) {
            window.smartChatbot.init(this.token, this.currentUser.role);
        }

        // Navigate based on role
        if (this.currentUser.role === 'ROLE_STUDENT') {
            this.navigate('student-portal');
        } else {
            this.navigate('dashboard');
        }
    }

    fillLogin(username, password) {
        document.getElementById('loginUsername').value = username;
        document.getElementById('loginPassword').value = password;
    }

    togglePasswordVisibility(fieldId) {
        const input = document.getElementById(fieldId);
        if (input.type === 'password') {
            input.type = 'text';
        } else {
            input.type = 'password';
        }
    }

    async handleLogin(event) {
        event.preventDefault();
        const username = document.getElementById('loginUsername').value.trim();
        const password = document.getElementById('loginPassword').value;
        const errAlert = document.getElementById('loginErrorAlert');
        errAlert.classList.add('d-none');

        const submitBtn = document.getElementById('loginSubmitBtn');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span> Authenticating...';

        try {
            const data = await this.apiPost('/api/auth/login', { username, password });
            this.token = data.token;
            this.currentUser = data;
            localStorage.setItem('smartattend_token', data.token);
            localStorage.setItem('smartattend_user', JSON.stringify(data));
            this.showAuthenticatedApp();
        } catch (e) {
            errAlert.innerText = e.message || 'Login failed. Please verify credentials.';
            errAlert.classList.remove('d-none');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = '<i class="bi bi-box-arrow-in-right me-2"></i> Secure Sign In';
        }
    }

    logout() {
        this.token = null;
        this.currentUser = null;
        localStorage.removeItem('smartattend_token');
        localStorage.removeItem('smartattend_user');
        if (window.smartScanner) window.smartScanner.stopScan();
        this.showLoginView();
    }

    // ==========================================================================
    // Navigation & Sidebar
    // ==========================================================================
    renderSidebarMenu() {
        const menu = document.getElementById('sidebarMenu');
        const role = this.currentUser.role;
        let html = '';

        if (role === 'ROLE_ADMIN') {
            html = `
                <div class="sidebar-heading">Admin Control</div>
                <button class="nav-item-btn" id="nav-dashboard" onclick="app.navigate('dashboard')">
                    <i class="bi bi-speedometer2"></i> Dashboard
                </button>
                <button class="nav-item-btn" id="nav-add-student" onclick="app.navigate('add-student')">
                    <i class="bi bi-person-plus"></i> Add Student
                </button>
                <button class="nav-item-btn" id="nav-manage-students" onclick="app.navigate('manage-students')">
                    <i class="bi bi-people"></i> Manage Students
                </button>
                <button class="nav-item-btn" id="nav-register-barcode" onclick="app.navigate('register-barcode')">
                    <i class="bi bi-upc-scan text-warning"></i> Register ID Barcode
                </button>
                <button class="nav-item-btn" id="nav-scan-attendance" onclick="app.navigate('scan-attendance')">
                    <i class="bi bi-camera"></i> Scan Attendance
                </button>
                <button class="nav-item-btn" id="nav-sessions" onclick="app.navigate('sessions')">
                    <i class="bi bi-calendar-event"></i> Attendance Sessions
                </button>
                <button class="nav-item-btn" id="nav-faculty" onclick="app.navigate('faculty')">
                    <i class="bi bi-person-workspace"></i> Faculty & Timetable
                </button>
                <button class="nav-item-btn" id="nav-anomalies" onclick="app.navigate('anomalies')">
                    <i class="bi bi-shield-exclamation text-danger"></i> Fraud & Anomaly Alerts
                </button>
                <button class="nav-item-btn" id="nav-reports" onclick="app.navigate('reports')">
                    <i class="bi bi-graph-up-arrow"></i> Reports & Analytics
                </button>
            `;
        } else if (role === 'ROLE_FACULTY') {
            html = `
                <div class="sidebar-heading">Faculty Attendance</div>
                <button class="nav-item-btn" id="nav-dashboard" onclick="app.navigate('dashboard')">
                    <i class="bi bi-speedometer2"></i> Dashboard
                </button>
                <button class="nav-item-btn" id="nav-scan-attendance" onclick="app.navigate('scan-attendance')">
                    <i class="bi bi-camera"></i> Live Webcam Scanner
                </button>
                <button class="nav-item-btn" id="nav-sessions" onclick="app.navigate('sessions')">
                    <i class="bi bi-calendar-check"></i> My Class Sessions
                </button>
            `;
        } else if (role === 'ROLE_STUDENT') {
            html = `
                <div class="sidebar-heading">Student Portal</div>
                <button class="nav-item-btn" id="nav-student-portal" onclick="app.navigate('student-portal')">
                    <i class="bi bi-person-badge"></i> My Attendance
                </button>
            `;
        }

        menu.innerHTML = html;
    }

    navigate(viewName) {
        // Stop any active camera streams when leaving scanner views
        if (this.currentView === 'scan-attendance' || this.currentView === 'add-student') {
            if (window.smartScanner) window.smartScanner.stopScan();
            this.stopRegCamera();
        }

        this.currentView = viewName;

        // Update active class on sidebar buttons
        document.querySelectorAll('.nav-item-btn').forEach(btn => btn.classList.remove('active'));
        const activeNavBtn = document.getElementById(`nav-${viewName}`);
        if (activeNavBtn) activeNavBtn.classList.add('active');

        // Hide all views and show target view
        document.querySelectorAll('.app-view').forEach(view => view.classList.add('d-none'));
        const targetView = document.getElementById(`view-${viewName}`);
        if (targetView) targetView.classList.remove('d-none');

        // Trigger view-specific data loading
        if (viewName === 'dashboard') this.loadDashboardStats();
        else if (viewName === 'manage-students') this.loadStudentsTable();
        else if (viewName === 'register-barcode') this.loadPendingBarcodeTable();
        else if (viewName === 'scan-attendance') this.prepareAttendanceScannerView();
        else if (viewName === 'sessions') this.loadSessionsTable();
        else if (viewName === 'faculty') this.loadFacultyTable();
        else if (viewName === 'anomalies') this.loadAnomaliesTable();
        else if (viewName === 'reports') this.loadReportsView();
        else if (viewName === 'student-portal') this.loadStudentPortal();
    }

    notify(message, type = 'success') {
        const banner = document.getElementById('globalNotification');
        const text = document.getElementById('globalNotificationText');
        banner.className = `alert alert-${type} alert-dismissible fade show`;
        text.innerHTML = message;
        banner.classList.remove('d-none');
        setTimeout(() => this.hideNotification(), 6000);
    }

    hideNotification() {
        const banner = document.getElementById('globalNotification');
        if (banner) banner.classList.add('d-none');
    }

    // ==========================================================================
    // VIEW 1: DASHBOARD
    // ==========================================================================
    async loadDashboardStats() {
        try {
            const stats = await this.apiGet('/api/reports/dashboard');
            document.getElementById('kpiTotalStudents').innerText = stats.totalStudents;
            document.getElementById('kpiPendingBarcodes').innerText = stats.pendingBarcodeCount;
            document.getElementById('kpiTodayPercentage').innerText = stats.todayAttendancePercentage + '%';
            document.getElementById('kpiActiveAnomalies').innerText = stats.activeAnomaliesCount;

            // Low attendance watchlist
            const lowBody = document.getElementById('dashLowAttendanceBody');
            if (stats.lowAttendanceStudents && stats.lowAttendanceStudents.length > 0) {
                lowBody.innerHTML = stats.lowAttendanceStudents.map(s => `
                    <tr>
                        <td class="fw-semibold">${s.studentName}</td>
                        <td><code>${s.registerNumber}</code></td>
                        <td>${s.department}</td>
                        <td><span class="badge bg-danger">${s.percentage}%</span></td>
                    </tr>
                `).join('');
            } else {
                lowBody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">No low attendance students detected!</td></tr>';
            }

            // Recent activity
            const actBody = document.getElementById('dashRecentActivityBody');
            if (stats.recentAttendanceActivity && stats.recentAttendanceActivity.length > 0) {
                actBody.innerHTML = stats.recentAttendanceActivity.map(a => `
                    <tr>
                        <td class="fw-semibold">${a.studentName}</td>
                        <td><code>${a.registerNumber}</code></td>
                        <td><span class="badge bg-success">${a.status}</span></td>
                        <td><small class="text-muted">${a.method}</small></td>
                        <td>${a.time}</td>
                    </tr>
                `).join('');
            } else {
                actBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3">No recent attendance recorded yet today.</td></tr>';
            }

            // Faculty Assignments & Timetable Card
            const facCard = document.getElementById('facultyAssignmentsCard');
            const facContainer = document.getElementById('facultyTimetableContainer');
            if (this.currentUser && this.currentUser.role === 'ROLE_FACULTY') {
                if (facCard) facCard.classList.remove('d-none');
                try {
                    const myAssign = await this.apiGet('/api/faculty/me/assignments');
                    if (myAssign.hasAssignments) {
                        const classesBadges = (myAssign.assignedClasses || []).map(c => 
                            `<span class="badge bg-primary me-2 mb-1 fs-6 p-2"><i class="bi bi-mortarboard me-1"></i>${c}</span>`
                        ).join('');

                        let timetableHtml = '';
                        if (myAssign.timetable && myAssign.timetable.length > 0) {
                            timetableHtml = `
                                <div class="table-responsive mt-3">
                                    <table class="table table-sm table-bordered align-middle">
                                        <thead class="table-light">
                                            <tr>
                                                <th>Day</th><th>Time</th><th>Subject</th><th>Class</th><th>Room</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${myAssign.timetable.map(t => `
                                                <tr>
                                                    <td><strong>${t.dayOfWeek}</strong></td>
                                                    <td>${t.startTime} - ${t.endTime}</td>
                                                    <td>${t.subjectCode} (${t.subjectName})</td>
                                                    <td>${t.department} Yr ${t.yearOfStudy} (${t.section})</td>
                                                    <td>${t.room || 'Room 101'}</td>
                                                </tr>
                                            `).join('')}
                                        </tbody>
                                    </table>
                                </div>
                            `;
                        }

                        facContainer.innerHTML = `
                            <div class="mb-2"><strong>Assigned Authorized Classes:</strong></div>
                            <div class="d-flex flex-wrap mb-2">${classesBadges || '<span class="text-muted">None</span>'}</div>
                            ${timetableHtml}
                        `;
                    } else {
                        facContainer.innerHTML = `
                            <div class="alert alert-info py-2 mb-0">
                                <i class="bi bi-info-circle me-1"></i> ${myAssign.message || 'No classes or timetable assigned yet. Please contact the college administrator.'}
                            </div>
                        `;
                    }
                } catch (facErr) {
                    if (facContainer) facContainer.innerHTML = '<div class="text-muted small">Could not load timetable assignments.</div>';
                }
            } else if (facCard) {
                facCard.classList.add('d-none');
            }
        } catch (e) {
            console.error('Failed to load dashboard statistics:', e);
        }
    }

    // ==========================================================================
    // VIEW 2: MANUAL STUDENT REGISTRATION (METHOD A & METHOD B)
    // ==========================================================================
    switchRegMode(mode) {
        const pills = document.querySelectorAll('#regModePills .nav-link');
        pills.forEach(p => p.classList.remove('active'));

        if (mode === 'webcam') {
            pills[1].classList.add('active');
            this.launchRegCamera();
        } else {
            pills[0].classList.add('active');
            this.stopRegCamera();
        }
    }

    async launchRegCamera() {
        const sec = document.getElementById('webcamRegSection');
        sec.classList.remove('d-none');

        if (window.smartScanner) {
            window.smartScanner.startScan('registration', 'regVideo', (barcode) => {
                window.smartScanner.playBeep(true);
                document.getElementById('regBarcodeValue').value = barcode;
                this.updateRegPreview();
                this.notify(`✅ ID Barcode scanned: <strong>${barcode}</strong>`, 'success');
            });
        }
    }

    stopRegCamera() {
        const sec = document.getElementById('webcamRegSection');
        if (sec) sec.classList.add('d-none');
        if (window.smartScanner) window.smartScanner.stopScan();
    }

    updateRegPreview() {
        const name = document.getElementById('regStudentName').value;
        const reg = document.getElementById('regRegisterNumber').value;
        const id = document.getElementById('regStudentId').value;
        const dept = document.getElementById('regDepartment').value;
        const year = document.getElementById('regYearOfStudy').value;
        const sec = document.getElementById('regSection').value;
        const barcode = document.getElementById('regBarcodeValue').value.trim();

        const barcodeStatusHtml = barcode 
            ? `<span class="badge-barcode-registered"><i class="bi bi-upc me-1"></i>${barcode}</span>`
            : `<span class="badge-barcode-pending"><i class="bi bi-hourglass-split me-1"></i>Barcode Pending</span>`;

        document.getElementById('regPreviewDetails').innerHTML = `
            <strong>${name || 'Student Name'}</strong> | Register No: <code>${reg || '--'}</code> | Student ID: <code>${id || '--'}</code> | Class: <strong>${dept} Year ${year} (${sec})</strong> | Barcode: ${barcodeStatusHtml}
        `;
    }

    resetAddStudentForm() {
        document.getElementById('addStudentForm').reset();
        this.stopRegCamera();
        this.updateRegPreview();
    }

    async handleSaveStudent(event) {
        event.preventDefault();

        const payload = {
            studentName: document.getElementById('regStudentName').value.trim(),
            registerNumber: document.getElementById('regRegisterNumber').value.trim(),
            studentId: document.getElementById('regStudentId').value.trim(),
            department: document.getElementById('regDepartment').value,
            academicYear: document.getElementById('regAcademicYear').value.trim(),
            yearOfStudy: parseInt(document.getElementById('regYearOfStudy').value),
            section: document.getElementById('regSection').value.trim().toUpperCase(),
            email: document.getElementById('regEmail').value.trim(),
            phone: document.getElementById('regPhone').value.trim() || null,
            barcodeValue: document.getElementById('regBarcodeValue').value.trim() || null,
            status: document.getElementById('regStatus').value
        };

        try {
            const saved = await this.apiPost('/api/students', payload);
            this.notify(`🎉 Student <strong>${saved.studentName}</strong> registered successfully! (Barcode: ${saved.barcodeRegistered ? saved.barcodeValue : 'Pending'})`, 'success');
            this.resetAddStudentForm();
            this.navigate('manage-students');
        } catch (e) {
            alert('Registration Error: ' + e.message);
        }
    }

    // ==========================================================================
    // VIEW 3: MANAGE STUDENTS
    // ==========================================================================
    async loadStudentsTable() {
        const search = document.getElementById('filterSearch').value;
        const dept = document.getElementById('filterDept').value;
        const year = document.getElementById('filterYear').value;
        const sec = document.getElementById('filterSection').value;
        const barcodeReg = document.getElementById('filterBarcode').value;

        let query = `/api/students?search=${encodeURIComponent(search)}`;
        if (dept) query += `&department=${encodeURIComponent(dept)}`;
        if (year) query += `&yearOfStudy=${year}`;
        if (sec) query += `&section=${encodeURIComponent(sec)}`;
        if (barcodeReg) query += `&barcodeRegistered=${barcodeReg}`;

        try {
            const students = await this.apiGet(query);
            const body = document.getElementById('studentsTableBody');

            if (students.length === 0) {
                body.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No students found matching current filters.</td></tr>';
                return;
            }

            body.innerHTML = students.map(s => {
                const barcodeBadge = s.barcodeRegistered 
                    ? `<span class="badge-barcode-registered" title="Barcode Value: ${s.barcodeValue}"><i class="bi bi-upc me-1"></i>${s.barcodeValue}</span>`
                    : `<span class="badge-barcode-pending"><i class="bi bi-hourglass-split me-1"></i>Barcode Pending</span>`;
                
                const statusBadge = s.status === 'ACTIVE' 
                    ? `<span class="badge-active">ACTIVE</span>` 
                    : `<span class="badge-inactive">INACTIVE</span>`;

                return `
                    <tr>
                        <td class="fw-semibold">${s.studentName}</td>
                        <td><code>${s.registerNumber}</code></td>
                        <td>${s.department}</td>
                        <td>Yr ${s.yearOfStudy} (${s.section})</td>
                        <td>${barcodeBadge}</td>
                        <td>${statusBadge}</td>
                        <td class="text-end">
                            ${!s.barcodeRegistered ? `
                                <button class="btn btn-sm btn-outline-warning me-1" onclick="app.openBarcodeModal('${s.id}', '${s.studentName}', '${s.registerNumber}')" title="Register Barcode">
                                    <i class="bi bi-upc-scan"></i>
                                </button>
                            ` : ''}
                            <button class="btn btn-sm btn-outline-secondary me-1" onclick="app.toggleStudentStatus('${s.id}')" title="Toggle Active/Inactive">
                                <i class="bi bi-arrow-repeat"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-danger" onclick="app.deleteStudent('${s.id}', '${s.studentName}')" title="Delete Student">
                                <i class="bi bi-trash3"></i>
                            </button>
                        </td>
                    </tr>
                `;
            }).join('');
        } catch (e) {
            console.error('Error fetching students:', e);
        }
    }

    async toggleStudentStatus(id) {
        try {
            await this.apiPatch(`/api/students/${id}/toggle-status`);
            this.loadStudentsTable();
        } catch (e) {
            alert('Error toggling status: ' + e.message);
        }
    }

    async deleteStudent(id, name) {
        if (!confirm(`Are you sure you want to delete student "${name}"? This action cannot be undone.`)) {
            return;
        }
        try {
            await this.apiDelete(`/api/students/${id}`);
            this.notify(`Student "${name}" deleted successfully.`, 'info');
            this.loadStudentsTable();
        } catch (e) {
            alert('Error deleting student: ' + e.message);
        }
    }

    // ==========================================================================
    // VIEW 4: REGISTER ID BARCODE FOR PENDING STUDENTS
    // ==========================================================================
    async loadPendingBarcodeTable() {
        try {
            const students = await this.apiGet('/api/students?barcodeRegistered=false');
            const body = document.getElementById('pendingBarcodeTableBody');

            if (students.length === 0) {
                body.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-success fw-semibold"><i class="bi bi-check-circle me-1"></i>All registered students have active barcodes assigned!</td></tr>';
                return;
            }

            body.innerHTML = students.map(s => `
                <tr>
                    <td class="fw-semibold">${s.studentName}</td>
                    <td><code>${s.registerNumber}</code></td>
                    <td><code>${s.studentId}</code></td>
                    <td>${s.department}</td>
                    <td>Year ${s.yearOfStudy} (${s.section})</td>
                    <td><span class="badge-barcode-pending"><i class="bi bi-hourglass-split me-1"></i>Barcode Pending</span></td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-primary-blue" onclick="app.openBarcodeModal('${s.id}', '${s.studentName}', '${s.registerNumber}')">
                            <i class="bi bi-camera me-1"></i> Scan ID Card
                        </button>
                    </td>
                </tr>
            `).join('');
        } catch (e) {
            console.error('Error loading pending barcodes:', e);
        }
    }

    openBarcodeModal(studentId, studentName, registerNumber) {
        this.activeModalStudent = { id: studentId, name: studentName, reg: registerNumber };
        document.getElementById('modalPendingStudentName').innerText = studentName;
        document.getElementById('modalPendingStudentReg').innerText = registerNumber;
        document.getElementById('modalBarcodeValueInput').value = '';
        document.getElementById('modalBarcodeAlert').classList.add('d-none');

        const modal = new bootstrap.Modal(document.getElementById('registerBarcodeModal'));
        modal.show();

        // Start modal scanner
        if (window.smartScanner) {
            window.smartScanner.startScan('registration', 'modalScanVideo', (barcode) => {
                window.smartScanner.playBeep(true);
                document.getElementById('modalBarcodeValueInput').value = barcode;
            });
        }
    }

    stopModalCamera() {
        if (window.smartScanner) window.smartScanner.stopScan();
    }

    async submitPendingBarcode() {
        if (!this.activeModalStudent) return;
        const barcode = document.getElementById('modalBarcodeValueInput').value.trim();
        const alertBox = document.getElementById('modalBarcodeAlert');
        alertBox.classList.add('d-none');

        if (!barcode) {
            alertBox.innerText = 'Please scan or type a valid barcode value.';
            alertBox.classList.remove('d-none');
            return;
        }

        try {
            await this.apiPost(`/api/students/${this.activeModalStudent.id}/register-barcode`, { barcodeValue: barcode });
            this.stopModalCamera();
            bootstrap.Modal.getInstance(document.getElementById('registerBarcodeModal')).hide();
            this.notify(`🎉 Barcode <strong>${barcode}</strong> linked to student <strong>${this.activeModalStudent.name}</strong> successfully!`, 'success');
            this.loadPendingBarcodeTable();
            this.loadStudentsTable();
        } catch (e) {
            alertBox.innerText = e.message || 'Failed to register barcode.';
            alertBox.classList.remove('d-none');
        }
    }

    // ==========================================================================
    // VIEW 5: WEBCAM ATTENDANCE SCANNER
    // ==========================================================================
    async prepareAttendanceScannerView() {
        // 1. Populate camera devices
        if (window.smartScanner) {
            await window.smartScanner.populateCameraDevices('scanCameraSelect');
        }

        // 2. Load open sessions
        try {
            const sessions = await this.apiGet('/api/attendance/sessions?status=OPEN');
            this.activeSessions = sessions;
            const select = document.getElementById('scanSessionSelect');
            select.innerHTML = '<option value="">-- Select an Open Attendance Session --</option>';

            sessions.forEach(s => {
                const opt = document.createElement('option');
                opt.value = s.id;
                opt.text = `${s.sessionCode} - ${s.subjectName || s.subjectCode} (${s.department} Yr ${s.yearOfStudy} Sec ${s.section})`;
                select.appendChild(opt);
            });

            if (sessions.length > 0) {
                select.value = sessions[0].id;
                this.onSessionSelectChanged();
            } else {
                document.getElementById('scanSessionDetails').innerHTML = `
                    <span class="text-danger fw-semibold">No open sessions found.</span> 
                    <a href="#" onclick="app.openCreateSessionModal()">Click here to start an attendance session</a>.
                `;
            }
        } catch (e) {
            console.error('Failed to load active sessions:', e);
        }
    }

    onSessionSelectChanged() {
        const select = document.getElementById('scanSessionSelect');
        this.selectedSessionId = select.value;
        const session = this.activeSessions.find(s => s.id === this.selectedSessionId);

        if (session) {
            document.getElementById('scanSessionDetails').innerHTML = `
                Active Class: <strong>${session.department} Year ${session.yearOfStudy} (${session.section})</strong> | Subject: <strong>${session.subjectName || session.subjectCode}</strong> | Faculty: <strong>${session.facultyName}</strong>
            `;
            document.getElementById('scanSessionStatusBadge').innerText = session.status;
            this.updateSessionPresentCount();
        }
    }

    async updateSessionPresentCount() {
        if (!this.selectedSessionId) return;
        try {
            const summary = await this.apiGet(`/api/attendance/sessions/${this.selectedSessionId}`);
            document.getElementById('scanSessionPresentCount').innerText = summary.presentCount || 0;
        } catch (e) {
            // Ignore if session not yet ready
        }
    }

    startAttendanceScanner() {
        if (!this.selectedSessionId) {
            alert('Please select an active attendance session first.');
            return;
        }

        document.getElementById('startScannerBtn').classList.add('d-none');
        document.getElementById('stopScannerBtn').classList.remove('d-none');
        document.getElementById('scannerLiveBadge').style.display = 'inline-block';

        if (window.smartScanner) {
            window.smartScanner.startScan('attendance', 'attendanceVideo', (barcode) => {
                this.handleScannedBarcode(barcode);
            });
        }
    }

    stopAttendanceScanner() {
        document.getElementById('startScannerBtn').classList.remove('d-none');
        document.getElementById('stopScannerBtn').classList.add('d-none');
        document.getElementById('scannerLiveBadge').style.display = 'none';

        if (window.smartScanner) {
            window.smartScanner.stopScan();
        }
    }

    async handleScannedBarcode(barcode) {
        if (!this.selectedSessionId) return;

        try {
            const result = await this.apiPost('/api/attendance/scan', {
                attendanceSessionId: this.selectedSessionId,
                barcodeValue: barcode
            });

            this.renderScanResult(result);

            if (result.outcome === 'SUCCESS') {
                if (window.smartScanner) window.smartScanner.playBeep(true);
                document.getElementById('scanSessionPresentCount').innerText = result.totalPresentInSession;
            } else {
                if (window.smartScanner) window.smartScanner.playBeep(false);
            }
        } catch (e) {
            this.renderScanResult({
                outcome: 'SERVER_OR_DATABASE_ERROR',
                message: e.message || 'Error communicating with attendance server.'
            });
            if (window.smartScanner) window.smartScanner.playBeep(false);
        }
    }

    submitManualBarcodeScan() {
        const input = document.getElementById('manualBarcodeScanInput');
        const code = input.value.trim();
        if (!code) {
            alert('Please enter a barcode number.');
            return;
        }
        input.value = '';
        this.handleScannedBarcode(code);
    }

    renderScanResult(result) {
        const container = document.getElementById('scanResultContainer');
        let cardClass = 'danger';
        let icon = 'bi-x-circle-fill';
        let title = 'Scan Error';

        if (result.outcome === 'SUCCESS') {
            cardClass = 'success';
            icon = 'bi-check-circle-fill';
            title = 'Attendance Marked Successfully';
        } else if (result.outcome === 'ATTENDANCE_ALREADY_RECORDED') {
            cardClass = 'warning';
            icon = 'bi-exclamation-circle-fill';
            title = 'Attendance Already Recorded';
        } else if (result.outcome === 'BARCODE_NOT_REGISTERED') {
            cardClass = 'danger';
            icon = 'bi-question-circle-fill';
            title = 'Barcode Not Registered';
        } else if (result.outcome === 'INVALID_CLASS_OR_SESSION') {
            cardClass = 'danger';
            icon = 'bi-slash-circle-fill';
            title = 'Class Mismatch / Not Enrolled';
        } else if (result.outcome === 'STUDENT_INACTIVE') {
            cardClass = 'danger';
            icon = 'bi-person-x-fill';
            title = 'Student Record Inactive';
        } else if (result.outcome === 'SESSION_CLOSED') {
            cardClass = 'danger';
            icon = 'bi-lock-fill';
            title = 'Session Closed';
        }

        const timeStr = result.recordedAt ? new Date(result.recordedAt).toLocaleTimeString() : new Date().toLocaleTimeString();

        container.innerHTML = `
            <div class="scan-result-card ${cardClass}">
                <div class="d-flex align-items-center gap-2 mb-2">
                    <i class="bi ${icon} fs-4"></i>
                    <h6 class="fw-bold mb-0">${title}</h6>
                </div>
                <div class="small mb-2">${result.message}</div>
                ${result.studentName ? `
                    <div class="p-2 rounded bg-white small border">
                        <div><strong>Student:</strong> ${result.studentName}</div>
                        <div><strong>Register No:</strong> <code>${result.registerNumber}</code></div>
                        ${result.department ? `<div><strong>Class:</strong> ${result.department} Yr ${result.yearOfStudy} (${result.section})</div>` : ''}
                        <div><strong>Time:</strong> ${timeStr}</div>
                    </div>
                ` : ''}
            </div>
        `;
    }

    // ==========================================================================
    // VIEW 6: ATTENDANCE SESSIONS & FACULTY
    // ==========================================================================
    async loadSessionsTable() {
        try {
            const sessions = await this.apiGet('/api/attendance/sessions');
            const body = document.getElementById('sessionsTableBody');

            if (sessions.length === 0) {
                body.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No attendance sessions created yet.</td></tr>';
                return;
            }

            body.innerHTML = sessions.map(s => `
                <tr>
                    <td><code>${s.sessionCode}</code></td>
                    <td class="fw-semibold">${s.subjectName || s.subjectCode || '--'}</td>
                    <td>${s.department} Yr ${s.yearOfStudy} (${s.section})</td>
                    <td>${s.sessionDate}</td>
                    <td>${s.facultyName}</td>
                    <td>
                        <span class="badge ${s.status === 'OPEN' ? 'bg-success' : 'bg-secondary'}">
                            ${s.status}
                        </span>
                    </td>
                    <td class="text-end">
                        ${s.status === 'OPEN' ? `
                            <button class="btn btn-sm btn-outline-danger me-1" onclick="app.closeSession('${s.id}')">
                                Close Session
                            </button>
                        ` : ''}
                        <button class="btn btn-sm btn-outline-blue" onclick="app.openSessionSummary('${s.id}')">
                            View Summary
                        </button>
                    </td>
                </tr>
            `).join('');
        } catch (e) {
            console.error('Error fetching sessions:', e);
        }
    }

    async openCreateSessionModal() {
        try {
            // Check faculty assignment restrictions
            if (this.currentUser && this.currentUser.role === 'ROLE_FACULTY') {
                const myAssign = await this.apiGet('/api/faculty/me/assignments');
                if (!myAssign.hasAssignments || !myAssign.assignedClasses || myAssign.assignedClasses.length === 0) {
                    alert('You have no classes assigned yet. Please contact the administrator before creating an attendance session.');
                    return;
                }
                // Pre-fill department, year, section from first assigned class if available
                const firstClass = myAssign.assignedClasses[0]; // e.g. "CSE-3-A"
                if (firstClass) {
                    const parts = firstClass.split('-');
                    if (parts.length >= 3) {
                        const deptMap = { 'CSE': 'Computer Science', 'IT': 'Information Technology', 'ECE': 'Electronics', 'EC': 'Electronics' };
                        const targetDept = deptMap[parts[0].toUpperCase()] || parts[0];
                        const deptSelect = document.getElementById('newSessionDept');
                        if (deptSelect) {
                            for (let opt of deptSelect.options) {
                                if (opt.value.toLowerCase().includes(targetDept.toLowerCase()) || targetDept.toLowerCase().includes(opt.value.toLowerCase())) {
                                    deptSelect.value = opt.value;
                                    break;
                                }
                            }
                        }
                        const yearSelect = document.getElementById('newSessionYear');
                        if (yearSelect) yearSelect.value = parts[1];
                        const secSelect = document.getElementById('newSessionSection');
                        if (secSelect) secSelect.value = parts[2].toUpperCase();
                    }
                }
            }

            // Load subjects dropdown
            const subjects = await this.apiGet('/api/attendance/subjects');
            const select = document.getElementById('newSessionSubject');
            select.innerHTML = subjects.map(sub => 
                `<option value="${sub.id}">${sub.subjectCode} - ${sub.subjectName} (${sub.department})</option>`
            ).join('');
            new bootstrap.Modal(document.getElementById('createSessionModal')).show();
        } catch (e) {
            alert('Failed to load session modal: ' + e.message);
        }
    }

    async handleCreateSession(event) {
        event.preventDefault();
        const payload = {
            subjectId: document.getElementById('newSessionSubject').value,
            department: document.getElementById('newSessionDept').value,
            yearOfStudy: parseInt(document.getElementById('newSessionYear').value),
            section: document.getElementById('newSessionSection').value
        };

        try {
            const session = await this.apiPost('/api/attendance/sessions', payload);
            bootstrap.Modal.getInstance(document.getElementById('createSessionModal')).hide();
            this.notify(`🎉 Attendance session <strong>${session.sessionCode}</strong> created!`, 'success');
            this.loadSessionsTable();
            this.prepareAttendanceScannerView();
        } catch (e) {
            alert('Error creating session: ' + e.message);
        }
    }

    async closeSession(sessionId) {
        if (!confirm('Are you sure you want to close this attendance session? No further barcode scans will be accepted.')) {
            return;
        }
        try {
            await this.apiPost(`/api/attendance/sessions/${sessionId}/close`, {});
            this.notify('Attendance session closed successfully.', 'info');
            this.loadSessionsTable();
        } catch (e) {
            alert('Error closing session: ' + e.message);
        }
    }

    async openSessionSummary(sessionId) {
        try {
            const summary = await this.apiGet(`/api/attendance/sessions/${sessionId}`);
            const session = summary.session;
            const students = summary.students || [];

            let listHtml = students.map(s => `
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${s.studentName}</strong> <code>${s.registerNumber}</code>
                    </div>
                    <span class="badge ${s.status === 'PRESENT' ? 'bg-success' : 'bg-danger'}">${s.status}</span>
                </li>
            `).join('');

            alert(`Session: ${session.sessionCode}\nClass: ${session.department} Year ${session.yearOfStudy} (${session.section})\nPresent: ${summary.presentCount} / ${summary.enrolledCount} (${summary.attendancePercentage}%)\nAbsent: ${summary.absentCount}`);
        } catch (e) {
            alert('Error loading session summary: ' + e.message);
        }
    }

    // ==========================================================================
    // VIEW: FACULTY & TIMETABLE ASSIGNMENTS
    // ==========================================================================
    async loadFacultyTable() {
        try {
            const facultyList = await this.apiGet('/api/faculty');
            const tbody = document.getElementById('facultyTableBody');
            if (!facultyList || facultyList.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">No faculty members found.</td></tr>';
                return;
            }

            tbody.innerHTML = facultyList.map(f => {
                const classesBadges = (f.assignedClasses && f.assignedClasses.length > 0)
                    ? f.assignedClasses.map(c => `<span class="badge bg-primary me-1">${c}</span>`).join('')
                    : '<span class="badge bg-warning text-dark">No Classes Assigned</span>';

                return `
                    <tr>
                        <td class="fw-semibold">${f.facultyName}</td>
                        <td><code>${f.employeeId}</code></td>
                        <td>${f.department}</td>
                        <td>${f.email}</td>
                        <td>${classesBadges}</td>
                        <td class="text-end">
                            <button class="btn btn-sm btn-outline-primary" onclick="app.openAssignFacultyModal('${f.id}')">
                                <i class="bi bi-pencil-square me-1"></i> Edit Assignments
                            </button>
                        </td>
                    </tr>
                `;
            }).join('');
        } catch (e) {
            console.error('Error loading faculty list:', e);
            document.getElementById('facultyTableBody').innerHTML = `<tr><td colspan="6" class="text-danger text-center">Failed to load faculty: ${e.message}</td></tr>`;
        }
    }

    async openAssignFacultyModal(facultyId) {
        try {
            const faculty = await this.apiGet(`/api/faculty/${facultyId}`);
            const subjects = await this.apiGet('/api/attendance/subjects');

            document.getElementById('assignFacultyId').value = faculty.id;
            document.getElementById('assignFacultyName').value = faculty.facultyName;
            document.getElementById('assignFacultyDept').value = faculty.department;
            document.getElementById('assignFacultyClasses').value = (faculty.assignedClasses || []).join(', ');

            const container = document.getElementById('assignFacultySubjectsContainer');
            const currentSubIds = new Set(faculty.assignedSubjectIds || []);

            container.innerHTML = subjects.map(s => `
                <div class="form-check">
                    <input class="form-check-input assign-subj-checkbox" type="checkbox" value="${s.id}" id="sub_${s.id}" ${currentSubIds.has(s.id) ? 'checked' : ''}>
                    <label class="form-check-label" for="sub_${s.id}">
                        <strong>${s.subjectCode}</strong> - ${s.subjectName} (${s.department})
                    </label>
                </div>
            `).join('');

            new bootstrap.Modal(document.getElementById('assignFacultyModal')).show();
        } catch (e) {
            alert('Failed to load faculty details: ' + e.message);
        }
    }

    async handleSaveFacultyAssignments(event) {
        event.preventDefault();
        const facultyId = document.getElementById('assignFacultyId').value;
        const classesRaw = document.getElementById('assignFacultyClasses').value;
        const classes = classesRaw.split(',').map(s => s.trim()).filter(s => s.length > 0);

        const checkedSubjects = Array.from(document.querySelectorAll('.assign-subj-checkbox:checked')).map(cb => cb.value);

        try {
            await this.apiPut(`/api/faculty/${facultyId}/assignments`, {
                assignedClasses: classes,
                assignedSubjectIds: checkedSubjects
            });

            bootstrap.Modal.getInstance(document.getElementById('assignFacultyModal')).hide();
            this.notify('Faculty assignments updated successfully!', 'success');
            this.loadFacultyTable();
        } catch (e) {
            alert('Error updating assignments: ' + e.message);
        }
    }

    // ==========================================================================
    // VIEW 7: FRAUD & ANOMALY ALERTS
    // ==========================================================================
    async loadAnomaliesTable() {
        try {
            const anomalies = await this.apiGet('/api/attendance/anomalies');
            const body = document.getElementById('anomaliesTableBody');

            if (anomalies.length === 0) {
                body.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-success"><i class="bi bi-shield-check me-1"></i>No attendance fraud or suspicious anomalies detected.</td></tr>';
                return;
            }

            body.innerHTML = anomalies.map(a => {
                const sevBadge = a.severity === 'VIOLATION' 
                    ? '<span class="badge bg-danger">VIOLATION</span>' 
                    : '<span class="badge bg-warning text-dark">SUSPICIOUS</span>';

                const timeStr = a.timestamp ? new Date(a.timestamp).toLocaleString() : '--';

                return `
                    <tr>
                        <td><small>${timeStr}</small></td>
                        <td><code>${a.anomalyType}</code></td>
                        <td>${sevBadge}</td>
                        <td>${a.studentName ? `${a.studentName} (<code>${a.registerNumber}</code>)` : '<span class="text-muted">Unregistered</span>'}</td>
                        <td><code>${a.scannedBarcodeValue || '--'}</code></td>
                        <td>${a.description}</td>
                        <td class="text-end">
                            ${a.resolved ? '<span class="badge bg-success">Resolved</span>' : `
                                <button class="btn btn-sm btn-outline-secondary" onclick="app.resolveAnomalyPrompt('${a.id}')">
                                    Resolve
                                </button>
                            `}
                        </td>
                    </tr>
                `;
            }).join('');
        } catch (e) {
            console.error('Error loading anomalies:', e);
        }
    }

    async resolveAnomalyPrompt(anomalyId) {
        const notes = prompt('Enter resolution notes for this anomaly:');
        if (!notes) return;

        try {
            await this.apiPost(`/api/attendance/anomalies/${anomalyId}/resolve`, { resolutionNotes: notes });
            this.notify('Anomaly marked as resolved.', 'info');
            this.loadAnomaliesTable();
        } catch (e) {
            alert('Error resolving anomaly: ' + e.message);
        }
    }

    // ==========================================================================
    // VIEW 8: REPORTS & ANALYTICS
    // ==========================================================================
    async loadReportsView() {
        try {
            const stats = await this.apiGet('/api/reports/dashboard');

            // 1. Department Attendance Bar Chart
            const deptCtx = document.getElementById('deptAttendanceChart');
            if (deptCtx && typeof Chart !== 'undefined') {
                if (this.deptChart) this.deptChart.destroy();

                const labels = Object.keys(stats.departmentAttendancePercentage || {});
                const data = Object.values(stats.departmentAttendancePercentage || {});

                this.deptChart = new Chart(deptCtx, {
                    type: 'bar',
                    data: {
                        labels: labels.length ? labels : ['Computer Science', 'Information Tech', 'Electronics'],
                        datasets: [{
                            label: 'Attendance %',
                            data: data.length ? data : [85.5, 78.0, 92.0],
                            backgroundColor: '#2B6CB0',
                            borderRadius: 6
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: { y: { min: 0, max: 100 } }
                    }
                });
            }

            // 2. Barcode Ratio Doughnut Chart
            const barcodeCtx = document.getElementById('barcodeRatioChart');
            if (barcodeCtx && typeof Chart !== 'undefined') {
                if (this.barcodeChart) this.barcodeChart.destroy();

                this.barcodeChart = new Chart(barcodeCtx, {
                    type: 'doughnut',
                    data: {
                        labels: ['Barcodes Registered', 'Barcode Pending'],
                        datasets: [{
                            data: [stats.registeredBarcodeCount || 0, stats.pendingBarcodeCount || 0],
                            backgroundColor: ['#2E7D32', '#D97706']
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false
                    }
                });
            }
        } catch (e) {
            console.error('Error loading reports:', e);
        }
    }

    downloadAttendanceCsv() {
        window.open('/api/reports/export-csv', '_blank');
    }

    // ==========================================================================
    // VIEW 9: STUDENT PERSONAL PORTAL
    // ==========================================================================
    async loadStudentPortal() {
        try {
            const summary = await this.apiGet('/api/attendance/student/me');
            this.renderStudentPortalSummary(summary);
        } catch (e) {
            console.error('Failed to load my attendance via /api/attendance/student/me:', e);
            if (this.currentUser && this.currentUser.linkedStudentId) {
                try {
                    const summary = await this.apiGet(`/api/attendance/student/${this.currentUser.linkedStudentId}`);
                    this.renderStudentPortalSummary(summary);
                } catch (fallbackErr) {
                    this.notify('Could not load student profile: ' + fallbackErr.message, 'danger');
                }
            } else {
                this.notify('No student record is linked to this account.', 'warning');
            }
        }
    }

    renderStudentPortalSummary(summary) {
        if (!summary || !summary.student) return;
        const student = summary.student;

        document.getElementById('studentPortalName').innerText = student.studentName;
        document.getElementById('studentPortalRegNo').innerText = `${student.registerNumber} (${student.department} Yr ${student.yearOfStudy})`;

        const percentageEl = document.getElementById('studentPortalPercentage');
        const statusBadge = document.getElementById('studentPortalStatusBadge');
        const warnBox = document.getElementById('studentPortalThresholdWarning');

        if (!summary.hasAttendanceData || summary.totalClasses === 0) {
            percentageEl.innerText = 'No attendance data yet';
            percentageEl.style.fontSize = '1.75rem';
            statusBadge.className = 'small fw-bold text-muted';
            statusBadge.innerText = 'No Attendance Sessions Conducted Yet';
            warnBox.classList.add('d-none');
        } else {
            percentageEl.innerText = summary.overallPercentage + '%';
            percentageEl.style.fontSize = '';
            if (summary.isLowAttendance) {
                statusBadge.className = 'small fw-bold text-danger';
                statusBadge.innerText = 'Attendance Shortage (< 75%)';
                document.getElementById('studentPortalClassesNeeded').innerText = summary.classesNeededFor75;
                warnBox.classList.remove('d-none');
            } else {
                statusBadge.className = 'small fw-bold text-success';
                statusBadge.innerText = 'Good Attendance Standing';
                warnBox.classList.add('d-none');
            }
        }

        // Subject breakdown
        const body = document.getElementById('studentPortalSubjectsBody');
        if (summary.subjectBreakdown && summary.subjectBreakdown.length > 0) {
            body.innerHTML = summary.subjectBreakdown.map(sub => `
                <tr>
                    <td><code>${sub.subjectCode}</code></td>
                    <td class="fw-semibold">${sub.subjectName}</td>
                    <td>${sub.present} / ${sub.total}</td>
                    <td>
                        <div class="d-flex align-items-center gap-2">
                            <div class="progress flex-grow-1" style="height: 8px;">
                                <div class="progress-bar ${sub.percentage >= 75 ? 'bg-success' : 'bg-danger'}" style="width: ${sub.percentage}%"></div>
                            </div>
                            <span class="small fw-bold">${sub.percentage}%</span>
                        </div>
                    </td>
                    <td>
                        <span class="badge ${sub.percentage >= 75 ? 'bg-success' : 'bg-danger'}">
                            ${sub.percentage >= 75 ? 'ELIGIBLE' : 'SHORTAGE'}
                        </span>
                    </td>
                </tr>
            `).join('');
        } else {
            body.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">No subject attendance recorded yet.</td></tr>';
        }
    }

    async renderStudentPortalData(studentId) {
        try {
            const summary = await this.apiGet(`/api/attendance/student/${studentId}`);
            this.renderStudentPortalSummary(summary);
        } catch (e) {
            console.error('Error loading student portal data:', e);
        }
    }

    // ==========================================================================
    // DEV SEED & TEST DATA HELPERS
    // ==========================================================================
    async seedDemoData() {
        try {
            const res = await this.apiPost('/api/dev/seed', {});
            this.notify(`🎉 ${res.message} (${res.count} students added)`, 'success');
            this.loadDashboardStats();
            if (this.currentView === 'manage-students') this.loadStudentsTable();
            if (this.currentView === 'register-barcode') this.loadPendingBarcodeTable();
        } catch (e) {
            alert('Failed to seed test data: ' + e.message);
        }
    }

    async clearDemoData() {
        if (!confirm('Are you sure you want to clear fictional test students? Real students will be preserved.')) return;
        try {
            const res = await this.apiPost('/api/dev/clear-test-data', {});
            this.notify(`🗑️ ${res.message} (${res.deletedCount} records removed)`, 'info');
            this.loadDashboardStats();
            if (this.currentView === 'manage-students') this.loadStudentsTable();
            if (this.currentView === 'register-barcode') this.loadPendingBarcodeTable();
        } catch (e) {
            alert('Failed to clear test data: ' + e.message);
        }
    }
}

// Instantiate and initialize on DOM load
window.app = new SmartAttendApp();
document.addEventListener('DOMContentLoaded', () => {
    window.app.init();
});
