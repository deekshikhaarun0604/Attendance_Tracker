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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SmartAttendanceApplicationTests {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRecordRepository recordRepository;

    @Autowired
    private SubjectRepository subjectRepository;

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
}
