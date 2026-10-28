package com.smartattend.repository;

import com.smartattend.model.AttendanceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends MongoRepository<AttendanceRecord, String> {
    Optional<AttendanceRecord> findByAttendanceSessionIdAndStudentId(String attendanceSessionId, String studentId);
    boolean existsByAttendanceSessionIdAndStudentId(String attendanceSessionId, String studentId);

    List<AttendanceRecord> findByAttendanceSessionId(String attendanceSessionId);
    List<AttendanceRecord> findByStudentIdOrderByRecordedAtDesc(String studentId);
    List<AttendanceRecord> findBySubjectIdAndStudentId(String subjectId, String studentId);
    List<AttendanceRecord> findByAttendanceSessionIdAndAttendanceStatus(String attendanceSessionId, String attendanceStatus);

    long countByAttendanceSessionId(String attendanceSessionId);
    long countByAttendanceSessionIdAndAttendanceStatus(String attendanceSessionId, String attendanceStatus);

    long countByStudentId(String studentId);
    long countByStudentIdAndAttendanceStatus(String studentId, String attendanceStatus);

    long countByRecordedAtBetween(Instant start, Instant end);
    List<AttendanceRecord> findByRecordedAtBetween(Instant start, Instant end);

    List<AttendanceRecord> findAllByOrderByRecordedAtDesc();
}
