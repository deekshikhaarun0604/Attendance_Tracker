package com.smartattend.service;

import com.smartattend.dto.StudentRequest;
import com.smartattend.model.Role;
import com.smartattend.model.Student;
import com.smartattend.model.User;
import com.smartattend.repository.StudentRepository;
import com.smartattend.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          MongoTemplate mongoTemplate) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    public Student addStudent(StudentRequest request) {
        // 1. Validate Register Number Uniqueness
        if (studentRepository.existsByRegisterNumber(request.getRegisterNumber())) {
            throw new IllegalArgumentException("Register Number '" + request.getRegisterNumber() + "' is already registered.");
        }

        // 2. Validate College Student ID Uniqueness
        if (studentRepository.existsByStudentId(request.getStudentId())) {
            throw new IllegalArgumentException("Student ID '" + request.getStudentId() + "' is already registered.");
        }

        // 3. Validate Email Uniqueness
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already associated with another student.");
        }

        Student student = new Student();
        student.setStudentName(request.getStudentName().trim());
        student.setRegisterNumber(request.getRegisterNumber().trim());
        student.setStudentId(request.getStudentId().trim());
        student.setDepartment(request.getDepartment().trim());
        student.setAcademicYear(request.getAcademicYear().trim());
        student.setYearOfStudy(request.getYearOfStudy());
        student.setSection(request.getSection().trim().toUpperCase());
        student.setEmail(request.getEmail().trim().toLowerCase());
        student.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        student.setProfilePhotoUrl(request.getProfilePhotoUrl());
        student.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        // 4. Validate and set Barcode
        String rawBarcode = request.getBarcodeValue();
        if (StringUtils.hasText(rawBarcode)) {
            String trimmedBarcode = rawBarcode.trim();
            Optional<Student> existingWithBarcode = studentRepository.findByBarcodeValue(trimmedBarcode);
            if (existingWithBarcode.isPresent()) {
                Student s = existingWithBarcode.get();
                throw new IllegalArgumentException("Barcode '" + trimmedBarcode + "' is already assigned to student " 
                        + s.getStudentName() + " (" + s.getRegisterNumber() + ").");
            }
            student.setBarcodeValue(trimmedBarcode);
            student.setBarcodeRegistered(true);
        } else {
            // Null so sparse unique index ignores it
            student.setBarcodeValue(null);
            student.setBarcodeRegistered(false);
        }

        student.setCreatedAt(Instant.now());
        student.setUpdatedAt(Instant.now());

        Student saved = studentRepository.save(student);

        // Auto-provision student user login account with their registerNumber
        if (!userRepository.existsByUsername(saved.getRegisterNumber())) {
            User studentUser = new User(
                    saved.getRegisterNumber(),
                    passwordEncoder.encode("Student@123"),
                    Role.ROLE_STUDENT,
                    saved.getStudentName(),
                    saved.getEmail()
            );
            studentUser.setLinkedStudentId(saved.getId());
            studentUser.setCreatedAt(Instant.now());
            studentUser.setUpdatedAt(Instant.now());
            userRepository.save(studentUser);
        }

        // Link default 'student' account if not yet linked
        userRepository.findByUsername("student").ifPresent(defaultUser -> {
            if (defaultUser.getLinkedStudentId() == null) {
                defaultUser.setLinkedStudentId(saved.getId());
                userRepository.save(defaultUser);
            }
        });

        return saved;
    }

    public Student registerBarcode(String studentId, String barcodeValue) {
        if (!StringUtils.hasText(barcodeValue)) {
            throw new IllegalArgumentException("Barcode value cannot be empty.");
        }

        String trimmedBarcode = barcodeValue.trim();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student with ID '" + studentId + "' not found."));

        // Check if another student has this barcode
        Optional<Student> existing = studentRepository.findByBarcodeValue(trimmedBarcode);
        if (existing.isPresent() && !existing.get().getId().equals(student.getId())) {
            Student other = existing.get();
            throw new IllegalArgumentException("Barcode '" + trimmedBarcode + "' is already registered to " 
                    + other.getStudentName() + " (" + other.getRegisterNumber() + ").");
        }

        student.setBarcodeValue(trimmedBarcode);
        student.setBarcodeRegistered(true);
        student.setUpdatedAt(Instant.now());

        return studentRepository.save(student);
    }

    public Student updateStudent(String id, StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student with ID '" + id + "' not found."));

        // Check register number uniqueness
        studentRepository.findByRegisterNumber(request.getRegisterNumber().trim())
                .filter(s -> !s.getId().equals(id))
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Register Number '" + request.getRegisterNumber() + "' is already in use.");
                });

        // Check studentId uniqueness
        studentRepository.findByStudentId(request.getStudentId().trim())
                .filter(s -> !s.getId().equals(id))
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Student ID '" + request.getStudentId() + "' is already in use.");
                });

        // Check email uniqueness
        studentRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .filter(s -> !s.getId().equals(id))
                .ifPresent(s -> {
                    throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already in use.");
                });

        // Check barcode uniqueness if provided
        if (StringUtils.hasText(request.getBarcodeValue())) {
            String trimmedBarcode = request.getBarcodeValue().trim();
            studentRepository.findByBarcodeValue(trimmedBarcode)
                    .filter(s -> !s.getId().equals(id))
                    .ifPresent(s -> {
                        throw new IllegalArgumentException("Barcode '" + trimmedBarcode + "' is already registered to "
                                + s.getStudentName() + " (" + s.getRegisterNumber() + ").");
                    });
            student.setBarcodeValue(trimmedBarcode);
            student.setBarcodeRegistered(true);
        } else if (request.getBarcodeValue() != null && request.getBarcodeValue().isEmpty()) {
            student.setBarcodeValue(null);
            student.setBarcodeRegistered(false);
        }

        student.setStudentName(request.getStudentName().trim());
        student.setRegisterNumber(request.getRegisterNumber().trim());
        student.setStudentId(request.getStudentId().trim());
        student.setDepartment(request.getDepartment().trim());
        student.setAcademicYear(request.getAcademicYear().trim());
        student.setYearOfStudy(request.getYearOfStudy());
        student.setSection(request.getSection().trim().toUpperCase());
        student.setEmail(request.getEmail().trim().toLowerCase());
        student.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        if (request.getStatus() != null) {
            student.setStatus(request.getStatus());
        }
        student.setUpdatedAt(Instant.now());

        Student updated = studentRepository.save(student);
        userRepository.findByLinkedStudentId(id).ifPresent(u -> {
            u.setFullName(updated.getStudentName());
            u.setEmail(updated.getEmail());
            u.setUsername(updated.getRegisterNumber());
            userRepository.save(u);
        });

        return updated;
    }

    public Student toggleStatus(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        student.setStatus("ACTIVE".equalsIgnoreCase(student.getStatus()) ? "INACTIVE" : "ACTIVE");
        student.setUpdatedAt(Instant.now());
        return studentRepository.save(student);
    }

    public void deleteStudent(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        userRepository.findByLinkedStudentId(id).ifPresent(userRepository::delete);
        studentRepository.delete(student);
    }

    public Student getStudentById(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student with ID '" + id + "' not found."));
    }

    public List<Student> searchAndFilter(String search, String department, Integer yearOfStudy,
                                         String section, String status, Boolean barcodeRegistered) {
        Query query = new Query();

        if (StringUtils.hasText(search)) {
            String regex = ".*" + search.trim() + ".*";
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where("studentName").regex(regex, "i"),
                    Criteria.where("registerNumber").regex(regex, "i"),
                    Criteria.where("studentId").regex(regex, "i"),
                    Criteria.where("barcodeValue").regex(regex, "i")
            );
            query.addCriteria(searchCriteria);
        }

        if (StringUtils.hasText(department)) {
            query.addCriteria(Criteria.where("department").is(department.trim()));
        }

        if (yearOfStudy != null && yearOfStudy > 0) {
            query.addCriteria(Criteria.where("yearOfStudy").is(yearOfStudy));
        }

        if (StringUtils.hasText(section)) {
            query.addCriteria(Criteria.where("section").is(section.trim().toUpperCase()));
        }

        if (StringUtils.hasText(status)) {
            query.addCriteria(Criteria.where("status").is(status.trim().toUpperCase()));
        }

        if (barcodeRegistered != null) {
            query.addCriteria(Criteria.where("barcodeRegistered").is(barcodeRegistered));
        }

        return mongoTemplate.find(query, Student.class);
    }

    public Map<String, Object> getRegistrationStats() {
        long total = studentRepository.count();
        long registeredBarcodes = studentRepository.countByBarcodeRegistered(true);
        long pendingBarcodes = studentRepository.countByBarcodeRegistered(false);
        long activeStudents = studentRepository.countByStatus("ACTIVE");
        long inactiveStudents = studentRepository.countByStatus("INACTIVE");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", total);
        stats.put("registeredBarcodes", registeredBarcodes);
        stats.put("pendingBarcodes", pendingBarcodes);
        stats.put("activeStudents", activeStudents);
        stats.put("inactiveStudents", inactiveStudents);
        stats.put("barcodePercentage", total > 0 ? Math.round((registeredBarcodes * 100.0) / total) : 0);
        return stats;
    }
}
