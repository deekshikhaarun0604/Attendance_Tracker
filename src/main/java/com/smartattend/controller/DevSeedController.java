package com.smartattend.controller;

import com.smartattend.model.AttendanceRecord;
import com.smartattend.model.AttendanceSession;
import com.smartattend.model.Student;
import com.smartattend.repository.AttendanceRecordRepository;
import com.smartattend.repository.AttendanceSessionRepository;
import com.smartattend.repository.StudentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class DevSeedController {

    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;

    public DevSeedController(StudentRepository studentRepository,
                             AttendanceRecordRepository recordRepository,
                             AttendanceSessionRepository sessionRepository) {
        this.studentRepository = studentRepository;
        this.recordRepository = recordRepository;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping("/seed")
    public ResponseEntity<?> seedTestData() {
        // Clear any existing test data first
        clearTestDataInternal();

        List<Student> testStudents = new ArrayList<>();

        // Student 1: Computer Science Year 3 Sec A (With Barcode)
        Student s1 = new Student("Alice Johnson (Demo)", "REG-TEST-001", "STU-TEST-001",
                "Computer Science", "2023-2027", 3, "A", "alice.test@college.edu", "9876543210");
        s1.setBarcodeValue("123456789012");
        s1.setBarcodeRegistered(true);
        testStudents.add(s1);

        // Student 2: Computer Science Year 3 Sec A (With Barcode)
        Student s2 = new Student("Bob Smith (Demo)", "REG-TEST-002", "STU-TEST-002",
                "Computer Science", "2023-2027", 3, "A", "bob.test@college.edu", "9876543211");
        s2.setBarcodeValue("987654321098");
        s2.setBarcodeRegistered(true);
        testStudents.add(s2);

        // Student 3: Computer Science Year 3 Sec A (Barcode Pending)
        Student s3 = new Student("Charlie Brown (Demo)", "REG-TEST-003", "STU-TEST-003",
                "Computer Science", "2023-2027", 3, "A", "charlie.test@college.edu", "9876543212");
        s3.setBarcodeValue(null);
        s3.setBarcodeRegistered(false); // Barcode Pending
        testStudents.add(s3);

        // Student 4: Information Technology Year 2 Sec B (With Barcode)
        Student s4 = new Student("Diana Prince (Demo)", "REG-TEST-004", "STU-TEST-004",
                "Information Technology", "2024-2028", 2, "B", "diana.test@college.edu", "9876543213");
        s4.setBarcodeValue("456789012345");
        s4.setBarcodeRegistered(true);
        testStudents.add(s4);

        // Student 5: Electronics Year 4 Sec A (Barcode Pending)
        Student s5 = new Student("Evan Wright (Demo)", "REG-TEST-005", "STU-TEST-005",
                "Electronics", "2022-2026", 4, "A", "evan.test@college.edu", "9876543214");
        s5.setBarcodeValue(null);
        s5.setBarcodeRegistered(false); // Barcode Pending
        testStudents.add(s5);

        for (Student s : testStudents) {
            s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
        }

        List<Student> saved = studentRepository.saveAll(testStudents);

        return ResponseEntity.ok(Map.of(
                "message", "Fictional development test students seeded successfully.",
                "count", saved.size(),
                "students", saved
        ));
    }

    @PostMapping("/clear-test-data")
    public ResponseEntity<?> clearTestData() {
        int deleted = clearTestDataInternal();
        return ResponseEntity.ok(Map.of(
                "message", "Fictional test data cleared successfully.",
                "deletedCount", deleted
        ));
    }

    private int clearTestDataInternal() {
        List<Student> all = studentRepository.findAll();
        int count = 0;
        for (Student s : all) {
            if (s.getRegisterNumber() != null && s.getRegisterNumber().startsWith("REG-TEST-")) {
                studentRepository.delete(s);
                count++;
            }
        }
        return count;
    }
}
