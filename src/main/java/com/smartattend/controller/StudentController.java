package com.smartattend.controller;

import com.smartattend.dto.BarcodeRegisterRequest;
import com.smartattend.dto.StudentRequest;
import com.smartattend.model.Student;
import com.smartattend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addStudent(@Valid @RequestBody StudentRequest request) {
        try {
            Student student = studentService.addStudent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(student);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Student>> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer yearOfStudy,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean barcodeRegistered
    ) {
        List<Student> students = studentService.searchAndFilter(search, department, yearOfStudy, section, status, barcodeRegistered);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStudentById(@PathVariable String id) {
        try {
            Student student = studentService.getStudentById(id);
            return ResponseEntity.ok(student);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStudent(@PathVariable String id, @Valid @RequestBody StudentRequest request) {
        try {
            Student updated = studentService.updateStudent(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStudent(@PathVariable String id) {
        try {
            studentService.deleteStudent(id);
            return ResponseEntity.ok(Map.of("message", "Student deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/register-barcode")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerBarcode(
            @PathVariable String id,
            @Valid @RequestBody BarcodeRegisterRequest request
    ) {
        try {
            Student updated = studentService.registerBarcode(id, request.getBarcodeValue());
            return ResponseEntity.ok(Map.of(
                    "message", "Barcode registered successfully for " + updated.getStudentName(),
                    "student", updated
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleStatus(@PathVariable String id) {
        try {
            Student updated = studentService.toggleStatus(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRegistrationStats() {
        return ResponseEntity.ok(studentService.getRegistrationStats());
    }
}
