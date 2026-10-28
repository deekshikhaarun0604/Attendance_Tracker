package com.smartattend.repository;

import com.smartattend.model.AttendanceSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends MongoRepository<AttendanceSession, String> {
    Optional<AttendanceSession> findBySessionCode(String sessionCode);
    List<AttendanceSession> findByFacultyIdOrderByCreatedAtDesc(String facultyId);
    List<AttendanceSession> findByDepartmentAndYearOfStudyAndSection(String department, Integer yearOfStudy, String section);
    List<AttendanceSession> findByStatus(String status);
    List<AttendanceSession> findBySessionDate(LocalDate sessionDate);
    List<AttendanceSession> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
