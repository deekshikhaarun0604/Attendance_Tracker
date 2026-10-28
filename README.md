# Smart Attendance Management System with Webcam Barcode Scanning and AI Chatbot

A complete, functional, software-only **Smart Attendance Management System** for colleges and universities.

Built with **Spring Boot 3 (Java 21)**, **Spring Data MongoDB**, **Spring Security (RBAC)**, **ZXing Browser** for real-time webcam barcode scanning, **Chart.js** for interactive analytics, and an integrated **Google Gemini AI Chatbot** with an intelligent database-backed fallback engine.

Featuring a modern **Cream & Blue** college theme (`#FAF7F2` warm cream and `#1A365D` navy/royal blue).

---

## Key Features

1. **Zero Hardware Dependencies**: Runs 100% in software using your laptop or desktop webcam. No external barcode guns, RFID scanners, Arduino, or ESP32 required.
2. **Manual Student Registration Without Pre-existing Files**: The administrator can register students manually through the web UI without needing an existing Excel, CSV, or preloaded database.
3. **"Barcode Pending" Lifecycle**: Students can be registered immediately even if their physical college ID card is not yet issued or available.
4. **Webcam ID Card Barcode Registration**: Built-in webcam scanner decodes physical ID cards and automatically associates the barcode with new or existing student records.
5. **Live Webcam Attendance Scanner**: Faculty can start an attendance session and scan students' ID cards in real time with audio chime feedback, visual targeting reticle, and instant verification cards.
6. **Robust Anti-Duplicate & Anti-Fraud Engine**:
   - Rejects duplicate scans of the same card in the same session.
   - Enforces unique register numbers, college student IDs, and unique non-empty barcodes in MongoDB.
   - Flags scans of inactive students, unassigned barcodes, and students not enrolled in the active class session.
7. **Role-Based Access Control (RBAC)**:
   - **ADMIN**: Student registration, barcode assignment, student database management, fraud alerts, timetable, curriculum, reports, AI assistant.
   - **FACULTY**: Attendance session creation, live webcam scanner, manual attendance corrections with mandatory audit reasons, present/absent summaries.
   - **STUDENT**: Personal profile, overall attendance %, target threshold calculator (classes needed for 75%), subject breakdown.
8. **SmartAttend AI Assistant (Google Gemini)**:
   - Queries real database data via secure backend services.
   - Enforces strict server-side authorization (students can never access other students' records).
   - Keeps `GEMINI_API_KEY` private on the backend.
   - Includes an intelligent rule-based knowledge engine that responds to attendance questions with real statistics even if the Gemini API key is not configured!
9. **Visual Analytics & CSV Export**: Chart.js charts for department attendance percentages and barcode registration ratios, plus one-click CSV report export.

---

## Technology Stack

- **Frontend**: HTML5, CSS3, JavaScript (ES6+), Bootstrap 5.3, Bootstrap Icons, ZXing Browser (`@zxing/library`), Chart.js
- **Backend**: Java 21, Spring Boot 3.2.x, Spring Data MongoDB, Spring Security, JJWT 0.12.x, Jakarta Bean Validation
- **Database**: MongoDB (Persisting students, users, attendance records, sessions, anomalies, and subjects)
- **AI Integration**: Google Gemini 1.5 Flash (`https://generativelanguage.googleapis.com`) with role-scoped retrieval

---

## Safe Development Accounts

The application automatically initializes safe default testing accounts upon startup:

| Role | Username | Password | Access / Purpose |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `Admin@123` | Full administrative control, student registration, barcode mapping, reports, fraud alerts |
| **Faculty** | `faculty` | `Faculty@123` | Dr. Sarah Jenkins (Computer Science), create sessions, live webcam scanning, class rosters |
| **Student** | `student` | `Student@123` | Student Portal, personal attendance %, classes required calculator |

> [!TIP]
> Use the **"Quick Login"** buttons on the login page to immediately fill credentials with a single click.

---

## Prerequisites & Installation

### 1. Java 21
Verify Java 21 is installed:
```powershell
java -version
```
If using Eclipse Adoptium / Temurin JDK 21 on Windows:
```powershell
$env:JAVA_HOME = "C:\Users\Indian\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
```

### 2. MongoDB
MongoDB must be running on `localhost:27017`.
Check the Windows service:
```powershell
Get-Service -Name MongoDB
```
Or start MongoDB manually if not running as a service:
```powershell
mongod --dbpath="C:\data\db"
```

### 3. Google Gemini API Key (Optional)
To enable generative natural language conversational capabilities in the SmartAttend AI chatbot:
```powershell
$env:GEMINI_API_KEY = "your_actual_gemini_api_key_here"
```
*(If omitted, the chatbot automatically uses the built-in intelligent data engine to answer attendance and student statistics from MongoDB without errors).*

---

## Running the Application

In the project root directory (`d:\projects\attendance tracker`), execute:

```powershell
# Set network preference for Maven
$env:MAVEN_OPTS = "-Djava.net.preferIPv4Stack=true"
$env:JAVA_HOME = "C:\Users\Indian\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.101-hotspot"

# Launch Spring Boot
.\mvnw.cmd spring-boot:run
```

Once started, open your web browser and navigate to:
👉 **`http://localhost:8080`**

---

## Step-by-Step Complete Workflow Walkthrough

### Workflow 1: Admin Manually Registers Students (Method A & B)
1. Navigate to `http://localhost:8080` and log in as `admin` (`Admin@123`).
2. Go to **"Add Student"** in the sidebar.
3. **Method A (Manual without card)**:
   - Fill in Student Name: `Alex Rivera`
   - Register No: `710023104050`
   - Student ID: `CS2024-050`
   - Department: `Computer Science`, Year: `3`, Section: `A`
   - Email: `alex.rivera@college.edu`
   - Leave Barcode Value **empty**.
   - Click **"Save Student Record"**. The student is created with status **"Barcode Pending"**.
4. **Method B (Scan Card directly)**:
   - Switch to **"METHOD B: Registration Using ID Card Webcam Scan"**.
   - Position a physical ID card or barcode in front of the laptop camera.
   - The scanner decodes the barcode, plays a chime, and autofills the Barcode input field.
   - Enter remaining details and save.

### Workflow 2: Register ID Barcode for Pending Students
1. Navigate to **"Register ID Barcode"** in the sidebar.
2. Find `Alex Rivera` (marked as *Barcode Pending*).
3. Click **"Scan ID Card"**.
4. In the camera modal, hold up Alex's new physical ID card.
5. The scanner automatically populates the barcode value.
6. Click **"Save Barcode to Student"**.
7. The student record is updated persistently in MongoDB.
8. *(Anti-Duplicate Test)*: If you attempt to assign this same barcode to another student, the backend rejects it and identifies the existing student.

### Workflow 3: Faculty Starts Attendance Session & Scans ID Cards
1. Log in as `faculty` (`Faculty@123`) or switch to **"Scan Attendance"**.
2. Select or create an attendance session (e.g. `Computer Science Year 3 Section A`).
3. Click **"Start Scanner"** and grant camera permissions when prompted.
4. Scan Alex Rivera's ID card:
   - Green banner appears: **"Attendance Marked Successfully"**.
   - Pleasant high-frequency chime sounds.
   - Live Session Present count increments to `1`.
5. Scan the **same card again**:
   - Yellow warning banner appears: **"Attendance Already Recorded"**.
   - Does not duplicate attendance; logs suspicious repeat scan in anomaly audit trail.
6. Scan an **unknown card**:
   - Red alert appears: **"Barcode Not Registered"**.
   - Logs anomaly for admin review.

### Workflow 4: Student Attendance Portal
1. Log in as `student` (`Student@123`).
2. View your personal attendance dashboard:
   - Overall percentage gauge.
   - Subject breakdown table.
   - **Target Threshold Calculator**: Shows how many consecutive classes you must attend to achieve 75% attendance.

### Workflow 5: SmartAttend AI Assistant
1. Click the floating robot badge in the bottom-right corner.
2. Ask questions such as:
   - *Admin*: "How many students are registered?" or "How many have pending barcodes?"
   - *Faculty*: "How many students are present today?"
   - *Student*: "What is my attendance percentage?" or "How many classes do I need to attend to reach 75%?"
3. Receive instant real-time data grounded directly in MongoDB!

---

## Webcam Troubleshooting

- **Localhost Requirement**: Web browsers (Chrome, Edge, Firefox) permit camera access (`getUserMedia`) on `localhost` or via `HTTPS`. If accessing across a local network IP, HTTPS is required by browser security policies.
- **Permission Denied**: Click the camera icon in your browser's address bar and select "Always allow `localhost:8080` to access your camera".
- **Multiple Cameras**: Use the camera device dropdown on the scanner screen to switch between built-in webcam and external USB webcams.
- **Lighting & Focus**: Hold the barcode 10–20 cm (4–8 inches) in front of the lens. Ensure even lighting and avoid direct glare on glossy plastic ID cards.
- **Manual Fallback**: If lighting is poor or the card is damaged, the manual barcode input box is always available beneath the camera viewport.

---

## REST API Reference

| Method | Endpoint | Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Public | Authenticates user credentials and returns JWT token |
| `GET` | `/api/auth/me` | Authenticated | Retrieves current authenticated user profile |
| `POST` | `/api/students` | Admin | Registers a student manually or after barcode scan |
| `GET` | `/api/students` | Authenticated | Search & filter registered students by dept/year/section/barcode |
| `GET` | `/api/students/{id}` | Authenticated | Retrieves single student details |
| `PUT` | `/api/students/{id}` | Admin | Updates student details |
| `DELETE` | `/api/students/{id}` | Admin | Deletes student record |
| `POST` | `/api/students/{id}/register-barcode` | Admin | Associates barcode with an existing pending student |
| `PATCH` | `/api/students/{id}/toggle-status` | Admin | Toggles student account ACTIVE / INACTIVE |
| `POST` | `/api/attendance/sessions` | Faculty/Admin | Creates and opens a new class attendance session |
| `GET` | `/api/attendance/sessions` | Faculty/Admin | Lists attendance sessions |
| `GET` | `/api/attendance/sessions/{id}` | Faculty/Admin | Retrieves session summary with present/absent student roster |
| `POST` | `/api/attendance/sessions/{id}/close` | Faculty/Admin | Closes an active attendance session |
| `POST` | `/api/attendance/scan` | Faculty/Admin | Processes webcam barcode scan with real-time verification |
| `POST` | `/api/attendance/manual` | Faculty/Admin | Manual attendance override with mandatory reason |
| `GET` | `/api/attendance/student/{id}` | Student/Faculty/Admin | Retrieves student personal attendance breakdown |
| `GET` | `/api/attendance/anomalies` | Admin | Lists detected attendance fraud and anomaly alerts |
| `POST` | `/api/attendance/anomalies/{id}/resolve`| Admin | Resolves an anomaly alert with audit notes |
| `GET` | `/api/reports/dashboard` | Authenticated | Computes live dashboard metrics from MongoDB |
| `GET` | `/api/reports/export-csv` | Authenticated | Downloads complete attendance records as CSV |
| `POST` | `/api/chatbot/message` | Authenticated | SmartAttend AI Assistant conversational endpoint |
| `POST` | `/api/dev/seed` | Public | Development helper: seeds fictional demo students |
| `POST` | `/api/dev/clear-test-data` | Public | Development helper: clears fictional demo students |

---

## Project Structure

```
attendance-tracker/
├── .mvn/wrapper/                  # Maven wrapper binaries
├── pom.xml                        # Maven dependencies & build definition
├── README.md                      # Complete setup and user guide
└── src/
    ├── main/
    │   ├── java/com/smartattend/
    │   │   ├── SmartAttendanceApplication.java   # Spring Boot entry point
    │   │   ├── config/
    │   │   │   └── DataInitializer.java          # Safe dev accounts & subjects setup
    │   │   ├── controller/
    │   │   │   ├── AttendanceController.java     # Sessions & live scan endpoints
    │   │   │   ├── AuthController.java           # JWT login & auth endpoints
    │   │   │   ├── ChatbotController.java        # Gemini AI chatbot endpoint
    │   │   │   ├── DevSeedController.java        # Test seed & clear endpoints
    │   │   │   ├── ReportController.java         # KPIs & CSV export endpoints
    │   │   │   └── StudentController.java        # Manual registration & CRUD
    │   │   ├── dto/                              # Request & response transfer objects
    │   │   ├── model/
    │   │   │   ├── AttendanceAnomaly.java        # Fraud alerts document model
    │   │   │   ├── AttendanceRecord.java         # Unique session-student attendance model
    │   │   │   ├── AttendanceSession.java        # Class attendance session model
    │   │   │   ├── Faculty.java                  # Faculty profile model
    │   │   │   ├── Role.java                     # RBAC enum
    │   │   │   ├── Student.java                  # Student document model
    │   │   │   ├── Subject.java                  # College curriculum model
    │   │   │   └── User.java                     # Login credentials document model
    │   │   ├── repository/                       # Spring Data Mongo repositories
    │   │   ├── security/                         # Spring Security, JWT & RBAC filters
    │   │   └── service/                          # Business logic & validation services
    │   └── resources/
    │       ├── application.properties            # MongoDB, JWT, and Gemini configuration
    │       └── static/                           # Modern Cream & Blue Single-Page App
    │           ├── css/style.css                 # Custom Cream & Blue college styling
    │           ├── js/app.js                     # SPA routing, API client & UI controller
    │           ├── js/scanner.js                 # ZXing webcam barcode scanner & audio chime
    │           ├── js/chatbot.js                 # SmartAttend AI chat widget
    │           └── index.html                    # Responsive application interface
    └── test/
        └── java/com/smartattend/
            └── SmartAttendanceApplicationTests.java # Automated integration & unit tests
```
