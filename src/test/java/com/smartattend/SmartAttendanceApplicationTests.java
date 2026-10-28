package com.smartattend;

import com.smartattend.dto.AttendanceScanRequest;
import com.smartattend.dto.AttendanceScanResult;
import com.smartattend.dto.CreateSessionRequest;
import com.smartattend.dto.StudentRequest;
import com.smartattend.model.AttendanceSession;
import com.smartattend.model.Student;
import com.smartattend.model.Subject;
import com.smartattend.repository.*;
import com.smartattend.service.AttendanceService;
import com.smartattend.service.StudentService;
import com.smartattend.controller.AttendanceController;
import com.smartattend.model.Faculty;
import com.smartattend.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SmartAttendanceApplicationTests {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceController attendanceController;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRecordRepository recordRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        sessionRepository.deleteAll();
        studentRepository.deleteAll();
    }

    @Test
    void testManualStudentRegistrationWithAndWithoutBarcode() {
        // 1. Register student without barcode (Barcode Pending)
        StudentRequest req1 = new StudentRequest();
        req1.setStudentName("Test Student Pending");
        req1.setRegisterNumber("REG-UNIT-001");
        req1.setStudentId("STU-UNIT-001");
        req1.setDepartment("Computer Science");
        req1.setAcademicYear("2024-2028");
        req1.setYearOfStudy(3);
        req1.setSection("A");
        req1.setEmail("pending.student@college.edu");
        req1.setBarcodeValue(null); // No barcode yet

        Student s1 = studentService.addStudent(req1);
        assertNotNull(s1.getId());
        assertFalse(s1.isBarcodeRegistered(), "Student barcode status should be pending (false)");
        assertNull(s1.getBarcodeValue());

        // 2. Register student with ID barcode
        StudentRequest req2 = new StudentRequest();
        req2.setStudentName("Test Student Barcode");
        req2.setRegisterNumber("REG-UNIT-002");
        req2.setStudentId("STU-UNIT-002");
        req2.setDepartment("Computer Science");
        req2.setAcademicYear("2024-2028");
        req2.setYearOfStudy(3);
        req2.setSection("A");
        req2.setEmail("barcode.student@college.edu");
        req2.setBarcodeValue("CARD-BARCODE-999");

        Student s2 = studentService.addStudent(req2);
        assertNotNull(s2.getId());
        assertTrue(s2.isBarcodeRegistered(), "Student barcode should be registered (true)");
        assertEquals("CARD-BARCODE-999", s2.getBarcodeValue());

        // 3. Registering duplicate register number must throw IllegalArgumentException
        StudentRequest reqDupReg = new StudentRequest();
        reqDupReg.setStudentName("Duplicate Reg Test");
        reqDupReg.setRegisterNumber("REG-UNIT-001");
        reqDupReg.setStudentId("STU-UNIT-NEW");
        reqDupReg.setDepartment("Computer Science");
        reqDupReg.setAcademicYear("2024-2028");
        reqDupReg.setYearOfStudy(3);
        reqDupReg.setSection("A");
        reqDupReg.setEmail("dup.reg@college.edu");

        assertThrows(IllegalArgumentException.class, () -> studentService.addStudent(reqDupReg));

        // 4. Registering duplicate barcode must throw IllegalArgumentException
        StudentRequest reqDupBar = new StudentRequest();
        reqDupBar.setStudentName("Duplicate Barcode Test");
        reqDupBar.setRegisterNumber("REG-UNIT-003");
        reqDupBar.setStudentId("STU-UNIT-003");
        reqDupBar.setDepartment("Computer Science");
        reqDupBar.setAcademicYear("2024-2028");
        reqDupBar.setYearOfStudy(3);
        reqDupBar.setSection("A");
        reqDupBar.setEmail("dup.bar@college.edu");
        reqDupBar.setBarcodeValue("CARD-BARCODE-999");

        assertThrows(IllegalArgumentException.class, () -> studentService.addStudent(reqDupBar));
    }

    @Test
    void testRegisterBarcodeForExistingPendingStudent() {
        // Register pending student
        StudentRequest req = new StudentRequest();
        req.setStudentName("Late Card Student");
        req.setRegisterNumber("REG-UNIT-004");
        req.setStudentId("STU-UNIT-004");
        req.setDepartment("Information Technology");
        req.setAcademicYear("2024-2028");
        req.setYearOfStudy(2);
        req.setSection("B");
        req.setEmail("late.card@college.edu");

        Student student = studentService.addStudent(req);
        assertFalse(student.isBarcodeRegistered());

        // Student later brings ID card: scan & register barcode
        Student updated = studentService.registerBarcode(student.getId(), "LATE-CARD-777");
        assertTrue(updated.isBarcodeRegistered());
        assertEquals("LATE-CARD-777", updated.getBarcodeValue());
    }

    @Test
    void testWebcamAttendanceWorkflowAndDuplicatePrevention() {
        // 1. Create registered student with barcode
        StudentRequest req = new StudentRequest();
        req.setStudentName("Attendance Student");
        req.setRegisterNumber("REG-ATT-001");
        req.setStudentId("STU-ATT-001");
        req.setDepartment("Computer Science");
        req.setAcademicYear("2024-2028");
        req.setYearOfStudy(3);
        req.setSection("A");
        req.setEmail("att.student@college.edu");
        req.setBarcodeValue("ATT-BARCODE-12345");

        Student student = studentService.addStudent(req);

        // 2. Ensure subject exists
        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() ->
                subjectRepository.save(new Subject("CS301", "Data Structures", "Computer Science", 5, 3))
        );

        // 3. Create active session
        CreateSessionRequest sessReq = new CreateSessionRequest();
        sessReq.setSubjectId(subject.getId());
        sessReq.setDepartment("Computer Science");
        sessReq.setYearOfStudy(3);
        sessReq.setSection("A");
        sessReq.setSessionDate(LocalDate.now());

        AttendanceSession session = attendanceService.createSession(sessReq, "Dr. Sarah Jenkins");
        assertEquals("OPEN", session.getStatus());

        // 4. Perform valid scan
        AttendanceScanRequest scanReq = new AttendanceScanRequest(session.getId(), "ATT-BARCODE-12345");
        AttendanceScanResult result1 = attendanceService.processScan(scanReq);

        assertEquals(AttendanceScanResult.Outcome.SUCCESS, result1.getOutcome());
        assertEquals(student.getStudentName(), result1.getStudentName());
        assertEquals(1, result1.getTotalPresentInSession());

        // 5. Scan the exact same card in the same session -> Must reject duplicate attendance!
        AttendanceScanResult result2 = attendanceService.processScan(scanReq);
        assertEquals(AttendanceScanResult.Outcome.ATTENDANCE_ALREADY_RECORDED, result2.getOutcome());

        // 6. Scan an unregistered barcode -> Must reject
        AttendanceScanRequest unknownReq = new AttendanceScanRequest(session.getId(), "UNKNOWN-9999");
        AttendanceScanResult resultUnknown = attendanceService.processScan(unknownReq);
        assertEquals(AttendanceScanResult.Outcome.BARCODE_NOT_REGISTERED, resultUnknown.getOutcome());

        // 7. Close session and attempt scan -> Must reject with SESSION_CLOSED
        attendanceService.closeSession(session.getId());
        AttendanceScanResult resultClosed = attendanceService.processScan(scanReq);
        assertEquals(AttendanceScanResult.Outcome.SESSION_CLOSED, resultClosed.getOutcome());
    }

    @Test
    void testStudentAttendancePercentageCalculationAndZeroState() {
        // Create student in Computer Science Year 3 Section A
        StudentRequest req = new StudentRequest();
        req.setStudentName("Calculation Test Student");
        req.setRegisterNumber("REG-CALC-001");
        req.setStudentId("STU-CALC-001");
        req.setDepartment("Computer Science");
        req.setAcademicYear("2024-2028");
        req.setYearOfStudy(3);
        req.setSection("A");
        req.setEmail("calc.student@college.edu");
        req.setBarcodeValue("CALC-BARCODE-001");

        Student student = studentService.addStudent(req);

        // 1. Zero conducted sessions state
        Map<String, Object> zeroSummary = attendanceService.getStudentAttendanceSummary(student.getId());
        assertEquals(false, zeroSummary.get("hasAttendanceData"), "Should indicate no attendance data when 0 conducted sessions");
        assertEquals("No attendance data yet", zeroSummary.get("displayPercentage"));
        assertEquals(0.0, zeroSummary.get("overallPercentage"));
        assertEquals(0L, ((Number) zeroSummary.get("totalClasses")).longValue());

        // 2. Conduct 4 eligible sessions for Computer Science Year 3 Sec A
        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() ->
                subjectRepository.save(new Subject("CS301", "Data Structures", "Computer Science", 5, 3))
        );

        CreateSessionRequest sess1 = new CreateSessionRequest();
        sess1.setSubjectId(subject.getId()); sess1.setDepartment("Computer Science"); sess1.setYearOfStudy(3); sess1.setSection("A");
        AttendanceSession s1 = attendanceService.createSession(sess1, "Dr. Sarah");

        CreateSessionRequest sess2 = new CreateSessionRequest();
        sess2.setSubjectId(subject.getId()); sess2.setDepartment("Computer Science"); sess2.setYearOfStudy(3); sess2.setSection("A");
        AttendanceSession s2 = attendanceService.createSession(sess2, "Dr. Sarah");

        CreateSessionRequest sess3 = new CreateSessionRequest();
        sess3.setSubjectId(subject.getId()); sess3.setDepartment("Computer Science"); sess3.setYearOfStudy(3); sess3.setSection("A");
        AttendanceSession s3 = attendanceService.createSession(sess3, "Dr. Sarah");

        CreateSessionRequest sess4 = new CreateSessionRequest();
        sess4.setSubjectId(subject.getId()); sess4.setDepartment("Computer Science"); sess4.setYearOfStudy(3); sess4.setSection("A");
        AttendanceSession s4 = attendanceService.createSession(sess4, "Dr. Sarah");

        // 3. Mark student present in exactly 3 of the 4 sessions (s1, s2, s3)
        attendanceService.processScan(new AttendanceScanRequest(s1.getId(), "CALC-BARCODE-001"));
        attendanceService.processScan(new AttendanceScanRequest(s2.getId(), "CALC-BARCODE-001"));
        attendanceService.processScan(new AttendanceScanRequest(s3.getId(), "CALC-BARCODE-001"));
        // Absent in s4

        // 4. Verify exactly 3 of 4 = 75.0%
        Map<String, Object> summary = attendanceService.getStudentAttendanceSummary(student.getId());
        assertEquals(true, summary.get("hasAttendanceData"));
        assertEquals(4L, ((Number) summary.get("totalClasses")).longValue());
        assertEquals(3L, ((Number) summary.get("presentClasses")).longValue());
        assertEquals(1L, ((Number) summary.get("absentClasses")).longValue());
        assertEquals(75.0, (Double) summary.get("overallPercentage"), 0.001, "3 of 4 conducted sessions must equal exactly 75.0%");
        assertEquals("75.0%", summary.get("displayPercentage"));
        assertEquals(false, summary.get("isLowAttendance"), "75.0% satisfies threshold, so isLowAttendance must be false");
    }

    @Test
    void testStudentDataIsolation() {
        // Register Student A
        StudentRequest reqA = new StudentRequest();
        reqA.setStudentName("Student Alice");
        reqA.setRegisterNumber("REG-ISO-A");
        reqA.setStudentId("STU-ISO-A");
        reqA.setDepartment("Computer Science");
        reqA.setAcademicYear("2024-2028");
        reqA.setYearOfStudy(3);
        reqA.setSection("A");
        reqA.setEmail("alice.iso@college.edu");
        Student studentA = studentService.addStudent(reqA);

        // Register Student B
        StudentRequest reqB = new StudentRequest();
        reqB.setStudentName("Student Bob");
        reqB.setRegisterNumber("REG-ISO-B");
        reqB.setStudentId("STU-ISO-B");
        reqB.setDepartment("Computer Science");
        reqB.setAcademicYear("2024-2028");
        reqB.setYearOfStudy(3);
        reqB.setSection("A");
        reqB.setEmail("bob.iso@college.edu");
        Student studentB = studentService.addStudent(reqB);

        // Mock UserDetails for Student A
        UserDetailsImpl studentAUser = new UserDetailsImpl(
                "user-a-id", "REG-ISO-A", "pwd", "Student Alice", "alice.iso@college.edu",
                "ROLE_STUDENT", studentA.getId(), null, true,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        // Student A accesses own profile via /student/{id} -> 200 OK
        ResponseEntity<?> respOwn = attendanceController.getStudentAttendance(studentA.getId(), studentAUser);
        assertEquals(HttpStatus.OK, respOwn.getStatusCode());

        // Student A accesses own profile via /student/me -> 200 OK
        ResponseEntity<?> respMe = attendanceController.getMyAttendance(studentAUser);
        assertEquals(HttpStatus.OK, respMe.getStatusCode());

        // Student A attempts to access Student B's profile -> 403 FORBIDDEN
        ResponseEntity<?> respOther = attendanceController.getStudentAttendance(studentB.getId(), studentAUser);
        assertEquals(HttpStatus.FORBIDDEN, respOther.getStatusCode(), "Student A must be forbidden from accessing Student B's attendance");
    }

    @Test
    void testFacultyAssignmentValidation() {
        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() ->
                subjectRepository.save(new Subject("CS301", "Data Structures", "Computer Science", 5, 3))
        );

        // Setup Faculty assigned specifically to CSE-3-A
        Faculty faculty = facultyRepository.findByEmployeeId("EMP-TEST-FAC").orElseGet(() ->
                new Faculty("EMP-TEST-FAC", "Prof. Alan Turing", "Computer Science", "turing@college.edu", "9998887770")
        );
        faculty.setAssignedClasses(List.of("CSE-3-A"));
        faculty = facultyRepository.save(faculty);

        UserDetailsImpl facultyUser = new UserDetailsImpl(
                "fac-user-id", "faculty_turing", "pwd", "Prof. Alan Turing", "turing@college.edu",
                "ROLE_FACULTY", null, faculty.getId(), true,
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY"))
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                facultyUser, null, facultyUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            // 1. Authorized class: Computer Science, Year 3, Section A -> SUCCESS (201 Created)
            CreateSessionRequest reqValid = new CreateSessionRequest();
            reqValid.setSubjectId(subject.getId());
            reqValid.setDepartment("Computer Science");
            reqValid.setYearOfStudy(3);
            reqValid.setSection("A");
            reqValid.setSessionDate(LocalDate.now());

            ResponseEntity<?> respValid = attendanceController.createSession(reqValid, facultyUser);
            assertEquals(HttpStatus.CREATED, respValid.getStatusCode());
            assertTrue(respValid.getBody() instanceof AttendanceSession);
            AttendanceSession session = (AttendanceSession) respValid.getBody();
            assertEquals(faculty.getId(), session.getFacultyId());

            // 2. Unauthorized class: Electronics, Year 4, Section A -> FORBIDDEN (403)
            CreateSessionRequest reqInvalid = new CreateSessionRequest();
            reqInvalid.setSubjectId(subject.getId());
            reqInvalid.setDepartment("Electronics");
            reqInvalid.setYearOfStudy(4);
            reqInvalid.setSection("A");
            reqInvalid.setSessionDate(LocalDate.now());

            ResponseEntity<?> respInvalid = attendanceController.createSession(reqInvalid, facultyUser);
            assertEquals(HttpStatus.FORBIDDEN, respInvalid.getStatusCode(), "Faculty must be rejected when opening session for unassigned class");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
