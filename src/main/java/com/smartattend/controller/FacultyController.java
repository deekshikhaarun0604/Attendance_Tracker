package com.smartattend.controller;

import com.smartattend.model.Faculty;
import com.smartattend.repository.FacultyRepository;
import com.smartattend.repository.UserRepository;
import com.smartattend.security.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;

    public FacultyController(FacultyRepository facultyRepository, UserRepository userRepository) {
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Faculty>> getAllFaculty() {
        return ResponseEntity.ok(facultyRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFacultyById(@PathVariable String id) {
        return facultyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAssignments(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        Optional<Faculty> opt = facultyRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Faculty not found"));
        }

        Faculty faculty = opt.get();

        if (body.containsKey("assignedClasses")) {
            @SuppressWarnings("unchecked")
            List<String> classes = (List<String>) body.get("assignedClasses");
            faculty.setAssignedClasses(classes != null ? classes : List.of());
        }

        if (body.containsKey("assignedSubjectIds")) {
            @SuppressWarnings("unchecked")
            List<String> subjects = (List<String>) body.get("assignedSubjectIds");
            faculty.setAssignedSubjectIds(subjects != null ? subjects : List.of());
        }

        Faculty saved = facultyRepository.save(faculty);
        return ResponseEntity.ok(Map.of(
                "message", "Assignments updated successfully for " + saved.getFacultyName(),
                "faculty", saved
        ));
    }

    @GetMapping("/me/assignments")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<?> getMyAssignments(@AuthenticationPrincipal UserDetailsImpl user) {
        String facultyId = user.getLinkedFacultyId();
        Faculty faculty = null;

        if (facultyId != null) {
            faculty = facultyRepository.findById(facultyId).orElse(null);
        }
        if (faculty == null) {
            faculty = facultyRepository.findByEmail(user.getEmail()).orElse(null);
        }
        if (faculty == null) {
            faculty = facultyRepository.findAll().stream().findFirst().orElse(null);
        }

        if (faculty == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("hasAssignments", false);
            resp.put("message", "No faculty profile found for your account.");
            resp.put("assignedClasses", List.of());
            resp.put("assignedSubjectIds", List.of());
            return ResponseEntity.ok(resp);
        }

        boolean hasClasses = faculty.getAssignedClasses() != null && !faculty.getAssignedClasses().isEmpty();
        boolean hasSubjects = faculty.getAssignedSubjectIds() != null && !faculty.getAssignedSubjectIds().isEmpty();
        boolean hasAssignments = hasClasses || hasSubjects;

        Map<String, Object> resp = new HashMap<>();
        resp.put("facultyId", faculty.getId());
        resp.put("facultyName", faculty.getFacultyName());
        resp.put("department", faculty.getDepartment());
        resp.put("assignedClasses", faculty.getAssignedClasses() != null ? faculty.getAssignedClasses() : List.of());
        resp.put("assignedSubjectIds", faculty.getAssignedSubjectIds() != null ? faculty.getAssignedSubjectIds() : List.of());
        resp.put("timetable", faculty.getTimetable() != null ? faculty.getTimetable() : List.of());
        resp.put("hasAssignments", hasAssignments);
        if (!hasAssignments) {
            resp.put("message", "No classes or timetable assigned yet. Please contact the college administrator.");
        }

        return ResponseEntity.ok(resp);
    }
}
