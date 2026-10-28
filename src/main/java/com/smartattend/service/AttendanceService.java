package com.smartattend.service;

import com.smartattend.dto.AttendanceScanRequest;
import com.smartattend.dto.AttendanceScanResult;
import com.smartattend.dto.CreateSessionRequest;
import com.smartattend.dto.ManualAttendanceRequest;
import com.smartattend.model.*;
import com.smartattend.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final FacultyRepository facultyRepository;
    private final AnomalyService anomalyService;

    public AttendanceService(AttendanceSessionRepository sessionRepository,
                             AttendanceRecordRepository recordRepository,
                             StudentRepository studentRepository,
                             SubjectRepository subjectRepository,
                             FacultyRepository facultyRepository,
                             AnomalyService anomalyService) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.facultyRepository = facultyRepository;
        this.anomalyService = anomalyService;
    }

    public AttendanceSession createSession(CreateSessionRequest request, String facultyUsername) {
        AttendanceSession session = new AttendanceSession();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        session.setSessionCode("SES-" + dateStr + "-" + randomSuffix);

        // Populate Subject Info
        subjectRepository.findById(request.getSubjectId()).ifPresent(sub -> {
            session.setSubjectId(sub.getId());
            session.setSubjectCode(sub.getSubjectCode());
            session.setSubjectName(sub.getSubjectName());
        });

        // Populate Faculty Info
        if (request.getFacultyId() != null) {
            session.setFacultyId(request.getFacultyId());
            facultyRepository.findById(request.getFacultyId()).ifPresent(fac -> {
                session.setFacultyName(fac.getFacultyName());
            });
        }
        if (session.getFacultyName() == null) {
            session.setFacultyName(facultyUsername);
        }

        session.setDepartment(request.getDepartment());
        session.setYearOfStudy(request.getYearOfStudy());
        session.setSection(request.getSection().toUpperCase());
        session.setSessionDate(request.getSessionDate() != null ? request.getSessionDate() : LocalDate.now());
        session.setStartTime(request.getStartTime() != null ? request.getStartTime() : LocalTime.now());
        session.setEndTime(request.getEndTime() != null ? request.getEndTime() : LocalTime.now().plusHours(1));
        session.setStatus("OPEN");
        session.setCreatedAt(Instant.now());

        return sessionRepository.save(session);
    }

    public AttendanceSession closeSession(String sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
        session.setStatus("CLOSED");
        return sessionRepository.save(session);
    }

    public synchronized AttendanceScanResult processScan(AttendanceScanRequest request) {
        String sessionId = request.getAttendanceSessionId();
        String barcode = request.getBarcodeValue() != null ? request.getBarcodeValue().trim() : "";

        if (barcode.isEmpty()) {
            return AttendanceScanResult.failure(
                    AttendanceScanResult.Outcome.BARCODE_COULD_NOT_BE_READ,
                    "Scanned barcode value is blank or unreadable."
            );
        }

        // 1. Verify session exists
        Optional<AttendanceSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return AttendanceScanResult.failure(
                    AttendanceScanResult.Outcome.INVALID_CLASS_OR_SESSION,
                    "Attendance session does not exist."
            );
        }

        AttendanceSession session = sessionOpt.get();

        // 2. Verify session is OPEN
        if (!"OPEN".equalsIgnoreCase(session.getStatus())) {
            anomalyService.logAnomaly(
                    sessionId, null, null, null, barcode,
                    "SESSION_CLOSED", "VIOLATION",
                    "Scan attempted on closed session: " + session.getSessionCode()
            );
            return AttendanceScanResult.failure(
                    AttendanceScanResult.Outcome.SESSION_CLOSED,
                    "This attendance session is closed. No new attendance can be recorded."
            );
        }

        // 3. Find registered student by barcode
        Optional<Student> studentOpt = studentRepository.findByBarcodeValue(barcode);
        if (studentOpt.isEmpty()) {
            anomalyService.logAnomaly(
                    sessionId, null, null, null, barcode,
                    "UNREGISTERED_BARCODE", "SUSPICIOUS",
                    "Barcode '" + barcode + "' is not registered to any student in the database."
            );
            return AttendanceScanResult.failure(
                    AttendanceScanResult.Outcome.BARCODE_NOT_REGISTERED,
                    "Barcode '" + barcode + "' is not registered to any student."
            );
        }

        Student student = studentOpt.get();

        // 4. Verify student is ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) {
            anomalyService.logAnomaly(
                    sessionId, student.getId(), student.getRegisterNumber(), student.getStudentName(), barcode,
                    "STUDENT_INACTIVE", "SUSPICIOUS",
                    "Scan attempted for inactive student account: " + student.getRegisterNumber()
            );
            return AttendanceScanResult.failure(
                    AttendanceScanResult.Outcome.STUDENT_INACTIVE,
                    "Student " + student.getStudentName() + " (" + student.getRegisterNumber() + ") is marked INACTIVE."
            );
        }

        // 5. Verify class eligibility (department, year, section)
        boolean deptMatch = session.getDepartment() == null || session.getDepartment().equalsIgnoreCase(student.getDepartment());
        boolean yearMatch = session.getYearOfStudy() == null || session.getYearOfStudy().equals(student.getYearOfStudy());
        boolean secMatch = session.getSection() == null || session.getSection().equalsIgnoreCase(student.getSection());

        if (!deptMatch || !yearMatch || !secMatch) {
            String expected = session.getDepartment() + " Year " + session.getYearOfStudy() + " (" + session.getSection() + ")";
            String actual = student.getDepartment() + " Year " + student.getYearOfStudy() + " (" + student.getSection() + ")";
            anomalyService.logAnomaly(
                    sessionId, student.getId(), student.getRegisterNumber(), student.getStudentName(), barcode,
                    "NOT_ENROLLED_CLASS", "SUSPICIOUS",
                    "Student " + student.getRegisterNumber() + " belongs to " + actual + ", but session is for " + expected
            );
            return AttendanceScanResult.failure(
                    AttendanceScanResult.Outcome.INVALID_CLASS_OR_SESSION,
                    "Student " + student.getStudentName() + " is enrolled in " + actual + ", not in this session (" + expected + ")."
            );
        }

        // 6. Check if attendance already recorded for this session
        Optional<AttendanceRecord> existingRecord = recordRepository.findByAttendanceSessionIdAndStudentId(sessionId, student.getId());
        long currentPresent = recordRepository.countByAttendanceSessionIdAndAttendanceStatus(sessionId, "PRESENT");

        if (existingRecord.isPresent()) {
            anomalyService.logAnomaly(
                    sessionId, student.getId(), student.getRegisterNumber(), student.getStudentName(), barcode,
                    "DUPLICATE_ATTEMPT", "SUSPICIOUS",
                    "Duplicate scan attempt for " + student.getStudentName() + " (" + student.getRegisterNumber() + ")"
            );
            return AttendanceScanResult.alreadyRecorded(
                    student.getStudentName(),
                    student.getRegisterNumber(),
                    existingRecord.get().getRecordedAt(),
                    currentPresent
            );
        }

        // 7. Save valid AttendanceRecord
        AttendanceRecord record = new AttendanceRecord(
                sessionId,
                student.getId(),
                student.getRegisterNumber(),
                student.getStudentName(),
                session.getSubjectId(),
                session.getFacultyId(),
                "PRESENT",
                barcode,
                "WEBCAM_BARCODE",
                "Webcam barcode verified on " + Instant.now()
        );

        recordRepository.save(record);
        long newPresentCount = currentPresent + 1;

        return AttendanceScanResult.success(
                "Attendance Marked Successfully",
                student.getId(),
                student.getStudentName(),
                student.getRegisterNumber(),
                student.getStudentId(),
                student.getDepartment(),
                student.getYearOfStudy(),
                student.getSection(),
                record.getRecordedAt(),
                newPresentCount
        );
    }

    public AttendanceRecord manualOverride(ManualAttendanceRequest request, String facultyUsername) {
        AttendanceSession session = sessionRepository.findById(request.getAttendanceSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found."));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        Optional<AttendanceRecord> existingOpt = recordRepository.findByAttendanceSessionIdAndStudentId(
                session.getId(), student.getId()
        );

        AttendanceRecord record;
        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            record.setAttendanceStatus(request.getAttendanceStatus().toUpperCase());
            record.setCorrectedBy(facultyUsername);
            record.setCorrectionReason(request.getReason());
            record.setVerificationNotes("Manually updated by " + facultyUsername + " on " + Instant.now() + ". Reason: " + request.getReason());
        } else {
            record = new AttendanceRecord();
            record.setAttendanceSessionId(session.getId());
            record.setStudentId(student.getId());
            record.setRegisterNumber(student.getRegisterNumber());
            record.setStudentName(student.getStudentName());
            record.setSubjectId(session.getSubjectId());
            record.setFacultyId(session.getFacultyId());
            record.setAttendanceStatus(request.getAttendanceStatus().toUpperCase());
            record.setAttendanceMethod("MANUAL_OVERRIDE");
            record.setCorrectedBy(facultyUsername);
            record.setCorrectionReason(request.getReason());
            record.setVerificationNotes("Manually created by " + facultyUsername + ". Reason: " + request.getReason());
            record.setRecordedAt(Instant.now());
        }

        return recordRepository.save(record);
    }

    public Map<String, Object> getSessionSummary(String sessionId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        // All enrolled students in this class
        List<Student> enrolled = studentRepository.findByDepartmentAndYearOfStudyAndSection(
                session.getDepartment(), session.getYearOfStudy(), session.getSection()
        );

        List<AttendanceRecord> records = recordRepository.findByAttendanceSessionId(sessionId);
        Map<String, AttendanceRecord> recordByStudentId = new HashMap<>();
        for (AttendanceRecord r : records) {
            recordByStudentId.put(r.getStudentId(), r);
        }

        List<Map<String, Object>> studentList = new ArrayList<>();
        long presentCount = 0;
        long absentCount = 0;

        for (Student s : enrolled) {
            Map<String, Object> item = new HashMap<>();
            item.put("studentId", s.getId());
            item.put("studentName", s.getStudentName());
            item.put("registerNumber", s.getRegisterNumber());
            item.put("collegeStudentId", s.getStudentId());
            item.put("barcodeRegistered", s.isBarcodeRegistered());

            if (recordByStudentId.containsKey(s.getId())) {
                AttendanceRecord r = recordByStudentId.get(s.getId());
                item.put("status", r.getAttendanceStatus());
                item.put("recordedAt", r.getRecordedAt());
                item.put("method", r.getAttendanceMethod());
                if ("PRESENT".equalsIgnoreCase(r.getAttendanceStatus())) {
                    presentCount++;
                } else {
                    absentCount++;
                }
            } else {
                item.put("status", "ABSENT");
                item.put("recordedAt", null);
                item.put("method", "NONE");
                absentCount++;
            }
            studentList.add(item);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("session", session);
        summary.put("enrolledCount", enrolled.size());
        summary.put("presentCount", presentCount);
        summary.put("absentCount", absentCount);
        summary.put("attendancePercentage", enrolled.isEmpty() ? 0 : Math.round((presentCount * 100.0) / enrolled.size()));
        summary.put("students", studentList);

        return summary;
    }

    public Map<String, Object> getStudentAttendanceSummary(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        // 1. Find all eligible attendance sessions conducted for this student's class (department, year, section)
        List<AttendanceSession> eligibleSessions = sessionRepository.findByDepartmentAndYearOfStudyAndSection(
                student.getDepartment(), student.getYearOfStudy(), student.getSection()
        );
        long totalEligibleConducted = eligibleSessions.size();

        // 2. Find student's attendance records
        List<AttendanceRecord> records = recordRepository.findByStudentIdOrderByRecordedAtDesc(studentId);
        long presentCount = records.stream()
                .filter(r -> "PRESENT".equalsIgnoreCase(r.getAttendanceStatus()))
                .count();
        long absentCount = Math.max(0, totalEligibleConducted - presentCount);

        boolean hasAttendanceData = (totalEligibleConducted > 0);
        Double overallPercentage = null;
        if (hasAttendanceData) {
            overallPercentage = Math.round((presentCount * 1000.0) / totalEligibleConducted) / 10.0;
        }

        // 3. Subject-wise breakdown based on conducted eligible sessions for each subject
        Map<String, long[]> subjectStats = new HashMap<>(); // subjectId -> [present, eligibleConducted]

        for (AttendanceSession sess : eligibleSessions) {
            String subId = sess.getSubjectId() != null ? sess.getSubjectId() : "GEN";
            long[] stats = subjectStats.computeIfAbsent(subId, k -> new long[2]);
            stats[1]++; // increment conducted count for subject
        }

        for (AttendanceRecord r : records) {
            if ("PRESENT".equalsIgnoreCase(r.getAttendanceStatus())) {
                String subId = r.getSubjectId() != null ? r.getSubjectId() : "GEN";
                long[] stats = subjectStats.computeIfAbsent(subId, k -> new long[2]);
                stats[0]++; // increment attended count for subject
            }
        }

        List<Map<String, Object>> subjectBreakdown = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : subjectStats.entrySet()) {
            Map<String, Object> subMap = new HashMap<>();
            String subId = entry.getKey();
            String subName = "Subject";
            String subCode = subId;
            Optional<Subject> subOpt = subjectRepository.findById(subId);
            if (subOpt.isPresent()) {
                subName = subOpt.get().getSubjectName();
                subCode = subOpt.get().getSubjectCode();
            }
            long p = entry.getValue()[0];
            long t = entry.getValue()[1];
            double pct = t > 0 ? Math.round((p * 1000.0) / t) / 10.0 : 0.0;
            subMap.put("subjectId", subId);
            subMap.put("subjectCode", subCode);
            subMap.put("subjectName", subName);
            subMap.put("present", p);
            subMap.put("total", t);
            subMap.put("percentage", pct);
            subjectBreakdown.add(subMap);
        }

        // 4. Classes needed to reach 75% target threshold
        long classesNeeded = 0;
        if (overallPercentage != null && overallPercentage < 75.0 && totalEligibleConducted > 0) {
            classesNeeded = Math.max(1, (long) Math.ceil((0.75 * totalEligibleConducted - presentCount) / 0.25));
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("student", student);
        summary.put("totalClasses", totalEligibleConducted);
        summary.put("presentClasses", presentCount);
        summary.put("absentClasses", absentCount);
        summary.put("hasAttendanceData", hasAttendanceData);
        summary.put("overallPercentage", overallPercentage != null ? overallPercentage : 0.0);
        summary.put("displayPercentage", hasAttendanceData ? (overallPercentage + "%") : "No attendance data yet");
        summary.put("isLowAttendance", overallPercentage != null && overallPercentage < 75.0);
        summary.put("classesNeededFor75", classesNeeded);
        summary.put("subjectBreakdown", subjectBreakdown);
        summary.put("recentRecords", records.size() > 10 ? records.subList(0, 10) : records);

        return summary;
    }
}
