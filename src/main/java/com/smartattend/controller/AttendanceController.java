package com.smartattend.controller;

import com.smartattend.dto.AttendanceScanRequest;
import com.smartattend.dto.AttendanceScanResult;
import com.smartattend.dto.CreateSessionRequest;
import com.smartattend.dto.ManualAttendanceRequest;
import com.smartattend.model.AttendanceAnomaly;
import com.smartattend.model.AttendanceRecord;
import com.smartattend.model.AttendanceSession;
import com.smartattend.model.Faculty;
import com.smartattend.model.Subject;
import com.smartattend.repository.AttendanceSessionRepository;
import com.smartattend.repository.SubjectRepository;
import com.smartattend.security.UserDetailsImpl;
import com.smartattend.service.AnomalyService;
import com.smartattend.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceSessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final AnomalyService anomalyService;
    private final com.smartattend.repository.StudentRepository studentRepository;
    private final com.smartattend.repository.FacultyRepository facultyRepository;

    public AttendanceController(AttendanceService attendanceService,
                                AttendanceSessionRepository sessionRepository,
                                SubjectRepository subjectRepository,
                                AnomalyService anomalyService,
                                com.smartattend.repository.StudentRepository studentRepository,
                                com.smartattend.repository.FacultyRepository facultyRepository) {
        this.attendanceService = attendanceService;
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.anomalyService = anomalyService;
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
    }

    @GetMapping("/student/me")
    public ResponseEntity<?> getMyAttendance(@AuthenticationPrincipal UserDetailsImpl user) {
        String studentId = user.getLinkedStudentId();
        if (studentId == null && user.getUsername() != null) {
            studentRepository.findByRegisterNumber(user.getUsername()).ifPresent(s -> {
                // found by register number
            });
            var sOpt = studentRepository.findByRegisterNumber(user.getUsername());
            if (sOpt.isPresent()) {
                studentId = sOpt.get().getId();
            }
        }
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No student profile is linked to this account."));
        }
        try {
            Map<String, Object> summary = attendanceService.getStudentAttendanceSummary(studentId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        try {
            if ("ROLE_FACULTY".equalsIgnoreCase(user.getRole())) {
                Faculty faculty = null;
                if (user.getLinkedFacultyId() != null) {
                    faculty = facultyRepository.findById(user.getLinkedFacultyId()).orElse(null);
                }
                if (faculty == null && user.getEmail() != null) {
                    faculty = facultyRepository.findByEmail(user.getEmail()).orElse(null);
                }
                if (faculty == null) {
                    faculty = facultyRepository.findAll().stream().findFirst().orElse(null);
                }

                if (faculty == null || faculty.getAssignedClasses() == null || faculty.getAssignedClasses().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error", "No classes are assigned to your faculty profile. Please contact the administrator."
                    ));
                }

                if (!isClassAssigned(faculty, request.getDepartment(), request.getYearOfStudy(), request.getSection())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                            "error", "You are not assigned to conduct attendance for " + request.getDepartment() + " Year " + request.getYearOfStudy() + " (" + request.getSection() + ")."
                    ));
                }

                request.setFacultyId(faculty.getId());
            }

            AttendanceSession session = attendanceService.createSession(request, user.getFullName());
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isClassAssigned(Faculty faculty, String dept, int year, String sec) {
        if (faculty == null || faculty.getAssignedClasses() == null || faculty.getAssignedClasses().isEmpty()) {
            return false;
        }
        String dNorm = dept.toLowerCase().replaceAll("[^a-z0-9]", "");
        String sNorm = sec.toLowerCase().trim();
        String yStr = String.valueOf(year);

        for (String ac : faculty.getAssignedClasses()) {
            if (ac == null || ac.isBlank()) continue;
            String norm = ac.toLowerCase().replaceAll("[^a-z0-9]", "");
            
            boolean yearMatches = norm.contains(yStr);
            boolean secMatches = norm.endsWith(sNorm) || norm.contains("-" + sNorm) || norm.contains(sNorm);
            
            boolean deptMatches = false;
            if (dNorm.contains("computer") || dNorm.contains("cse")) {
                deptMatches = norm.contains("cse") || norm.contains("computer");
            } else if (dNorm.contains("information") || dNorm.contains("it")) {
                deptMatches = norm.contains("it") || norm.contains("information");
            } else if (dNorm.contains("electronic") || dNorm.contains("ece") || dNorm.contains("ec")) {
                deptMatches = norm.contains("ec") || norm.contains("electronic");
            } else {
                deptMatches = norm.contains(dNorm) || dNorm.contains(norm);
            }

            if (deptMatches && yearMatches && secMatches) {
                return true;
            }
        }
        return false;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AttendanceSession>> getSessions(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        if ("ROLE_FACULTY".equalsIgnoreCase(user.getRole())) {
            String facId = user.getLinkedFacultyId();
            if (facId == null && user.getEmail() != null) {
                facId = facultyRepository.findByEmail(user.getEmail()).map(Faculty::getId).orElse(null);
            }
            if (facId != null) {
                List<AttendanceSession> sessions = sessionRepository.findByFacultyIdOrderByCreatedAtDesc(facId);
                return ResponseEntity.ok(sessions);
            }
            return ResponseEntity.ok(sessionRepository.findByFacultyIdOrderByCreatedAtDesc(user.getId()));
        }
        if (status != null && !status.isEmpty()) {
            return ResponseEntity.ok(sessionRepository.findByStatus(status.toUpperCase()));
        }
        return ResponseEntity.ok(sessionRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<?> getSessionSummary(@PathVariable String id) {
        try {
            Map<String, Object> summary = attendanceService.getSessionSummary(id);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sessions/{id}/close")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> closeSession(@PathVariable String id) {
        try {
            AttendanceSession session = attendanceService.closeSession(id);
            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<AttendanceScanResult> processScan(
            @Valid @RequestBody AttendanceScanRequest request
    ) {
        AttendanceScanResult result = attendanceService.processScan(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> manualOverride(
            @Valid @RequestBody ManualAttendanceRequest request,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        try {
            AttendanceRecord record = attendanceService.manualOverride(request, user.getFullName());
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentAttendance(
            @PathVariable String studentId,
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        // Enforce role-based security: students can only access their own records
        if ("ROLE_STUDENT".equalsIgnoreCase(user.getRole())) {
            boolean isOwnRecord = (user.getLinkedStudentId() != null && user.getLinkedStudentId().equals(studentId));
            if (!isOwnRecord && user.getUsername() != null) {
                isOwnRecord = studentRepository.findById(studentId)
                        .filter(s -> s.getRegisterNumber().equalsIgnoreCase(user.getUsername()))
                        .isPresent();
            }
            if (!isOwnRecord) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You can only view your own attendance records."));
            }
        }

        try {
            Map<String, Object> summary = attendanceService.getStudentAttendanceSummary(studentId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/anomalies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AttendanceAnomaly>> getAnomalies() {
        return ResponseEntity.ok(anomalyService.getAllAnomalies());
    }

    @PostMapping("/anomalies/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolveAnomaly(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        try {
            String notes = body.getOrDefault("resolutionNotes", "Resolved by Admin");
            AttendanceAnomaly anomaly = anomalyService.resolveAnomaly(id, notes);
            return ResponseEntity.ok(anomaly);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> getSubjects() {
        return ResponseEntity.ok(subjectRepository.findAll());
    }
}
