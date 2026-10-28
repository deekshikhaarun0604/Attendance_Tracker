package com.smartattend.config;

import com.smartattend.model.Faculty;
import com.smartattend.model.Role;
import com.smartattend.model.Subject;
import com.smartattend.model.User;
import com.smartattend.repository.FacultyRepository;
import com.smartattend.repository.SubjectRepository;
import com.smartattend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           SubjectRepository subjectRepository,
                           FacultyRepository facultyRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initDefaultSubjects();
        initDefaultFaculty();
        initDefaultUsers();
    }

    private void initDefaultSubjects() {
        if (subjectRepository.count() == 0) {
            logger.info("Initializing default college curriculum subjects...");
            List<Subject> subjects = List.of(
                    new Subject("CS301", "Data Structures & Algorithms", "Computer Science", 5, 3),
                    new Subject("CS302", "Operating Systems", "Computer Science", 5, 3),
                    new Subject("IT201", "Database Management Systems", "Information Technology", 3, 2),
                    new Subject("EC401", "Digital Signal Processing", "Electronics", 7, 4)
            );
            subjectRepository.saveAll(subjects);
            logger.info("Default subjects created.");
        }
    }

    private void initDefaultFaculty() {
        if (facultyRepository.count() == 0) {
            Faculty faculty = new Faculty("EMP-2024-001", "Dr. Sarah Jenkins", "Computer Science", "faculty@college.edu", "9876543200");
            faculty.setAssignedClasses(List.of("CSE-3-A", "CSE-3-B"));
            facultyRepository.save(faculty);
        }
    }

    private void initDefaultUsers() {
        // Admin account
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                    "admin",
                    passwordEncoder.encode("Admin@123"),
                    Role.ROLE_ADMIN,
                    "College Administrator",
                    "admin@college.edu"
            );
            admin.setCreatedAt(Instant.now());
            admin.setUpdatedAt(Instant.now());
            userRepository.save(admin);
            logger.info("Default Admin account created: username 'admin', password 'Admin@123'");
        }

        // Faculty account
        if (!userRepository.existsByUsername("faculty")) {
            Faculty faculty = facultyRepository.findByEmployeeId("EMP-2024-001").orElse(null);
            User facultyUser = new User(
                    "faculty",
                    passwordEncoder.encode("Faculty@123"),
                    Role.ROLE_FACULTY,
                    "Dr. Sarah Jenkins",
                    "faculty@college.edu"
            );
            if (faculty != null) {
                facultyUser.setLinkedFacultyId(faculty.getId());
            }
            facultyUser.setCreatedAt(Instant.now());
            facultyUser.setUpdatedAt(Instant.now());
            userRepository.save(facultyUser);
            logger.info("Default Faculty account created: username 'faculty', password 'Faculty@123'");
        }

        // Student account
        if (!userRepository.existsByUsername("student")) {
            User studentUser = new User(
                    "student",
                    passwordEncoder.encode("Student@123"),
                    Role.ROLE_STUDENT,
                    "College Student",
                    "student@college.edu"
            );
            studentUser.setCreatedAt(Instant.now());
            studentUser.setUpdatedAt(Instant.now());
            userRepository.save(studentUser);
            logger.info("Default Student account created: username 'student', password 'Student@123'");
        }
    }
}
