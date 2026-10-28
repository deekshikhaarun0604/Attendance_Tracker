package com.smartattend.service;

import com.smartattend.dto.DashboardStatsDto;
import com.smartattend.model.AttendanceRecord;
import com.smartattend.model.AttendanceSession;
import com.smartattend.model.Student;
import com.smartattend.repository.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReportService {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceAnomalyRepository anomalyRepository;

    public ReportService(StudentRepository studentRepository,
                         FacultyRepository facultyRepository,
                         AttendanceSessionRepository sessionRepository,
                         AttendanceRecordRepository recordRepository,
                         AttendanceAnomalyRepository anomalyRepository) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.anomalyRepository = anomalyRepository;
    }

    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        long totalStudents = studentRepository.count();
        long activeStudents = studentRepository.countByStatus("ACTIVE");
        long totalFaculty = facultyRepository.count();
        long pendingBarcodes = studentRepository.countByBarcodeRegistered(false);
        long registeredBarcodes = studentRepository.countByBarcodeRegistered(true);
        long activeAnomalies = anomalyRepository.countByResolvedFalse();

        // Today's classes
        LocalDate today = LocalDate.now();
        List<AttendanceSession> todaySessions = sessionRepository.findBySessionDate(today);
        long totalClassesToday = todaySessions.size();

        // Today's attendance percentage
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);
        List<AttendanceRecord> todayRecords = recordRepository.findByRecordedAtBetween(startOfDay, endOfDay);

        long todayPresent = todayRecords.stream().filter(r -> "PRESENT".equalsIgnoreCase(r.getAttendanceStatus())).count();
        double todayPercentage = todayRecords.isEmpty() ? 0.0 : Math.round((todayPresent * 1000.0) / todayRecords.size()) / 10.0;

        stats.setTotalStudents(totalStudents);
        stats.setTotalActiveStudents(activeStudents);
        stats.setTotalFaculty(totalFaculty);
        stats.setTotalClassesToday(totalClassesToday);
        stats.setTodayAttendancePercentage(todayPercentage);
        stats.setPendingBarcodeCount(pendingBarcodes);
        stats.setRegisteredBarcodeCount(registeredBarcodes);
        stats.setActiveAnomaliesCount(activeAnomalies);

        // Low attendance students (< 75%)
        List<Student> allStudents = studentRepository.findAll();
        List<Map<String, Object>> lowAttendanceList = new ArrayList<>();
        Map<String, Long> deptStudentCounts = new HashMap<>();
        Map<String, long[]> deptAttendanceTotals = new HashMap<>(); // dept -> [present, total]

        for (Student s : allStudents) {
            String dept = s.getDepartment() != null ? s.getDepartment() : "General";
            deptStudentCounts.put(dept, deptStudentCounts.getOrDefault(dept, 0L) + 1);

            List<AttendanceRecord> sRecords = recordRepository.findByStudentIdOrderByRecordedAtDesc(s.getId());
            if (!sRecords.isEmpty()) {
                long pres = sRecords.stream().filter(r -> "PRESENT".equalsIgnoreCase(r.getAttendanceStatus())).count();
                double pct = (pres * 100.0) / sRecords.size();

                long[] deptTotals = deptAttendanceTotals.computeIfAbsent(dept, k -> new long[2]);
                deptTotals[0] += pres;
                deptTotals[1] += sRecords.size();

                if (pct < 75.0) {
                    Map<String, Object> lowItem = new HashMap<>();
                    lowItem.put("studentId", s.getId());
                    lowItem.put("studentName", s.getStudentName());
                    lowItem.put("registerNumber", s.getRegisterNumber());
                    lowItem.put("department", s.getDepartment());
                    lowItem.put("yearOfStudy", s.getYearOfStudy());
                    lowItem.put("section", s.getSection());
                    lowItem.put("totalClasses", sRecords.size());
                    lowItem.put("presentClasses", pres);
                    lowItem.put("percentage", Math.round(pct * 10.0) / 10.0);
                    lowAttendanceList.add(lowItem);
                }
            }
        }

        stats.setLowAttendanceCount(lowAttendanceList.size());
        stats.setLowAttendanceStudents(lowAttendanceList.size() > 10 ? lowAttendanceList.subList(0, 10) : lowAttendanceList);
        stats.setDepartmentStudentCount(deptStudentCounts);

        Map<String, Double> deptPercentages = new HashMap<>();
        for (Map.Entry<String, long[]> entry : deptAttendanceTotals.entrySet()) {
            long p = entry.getValue()[0];
            long t = entry.getValue()[1];
            deptPercentages.put(entry.getKey(), t > 0 ? Math.round((p * 1000.0) / t) / 10.0 : 0.0);
        }
        stats.setDepartmentAttendancePercentage(deptPercentages);

        // Recent attendance activity
        List<AttendanceRecord> recentRecords = recordRepository.findAllByOrderByRecordedAtDesc();
        List<Map<String, Object>> recentList = new ArrayList<>();
        int limit = Math.min(recentRecords.size(), 8);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

        for (int i = 0; i < limit; i++) {
            AttendanceRecord r = recentRecords.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("studentName", r.getStudentName());
            map.put("registerNumber", r.getRegisterNumber());
            map.put("status", r.getAttendanceStatus());
            map.put("method", r.getAttendanceMethod());
            map.put("time", timeFmt.format(r.getRecordedAt()));
            map.put("recordedAt", r.getRecordedAt());
            recentList.add(map);
        }
        stats.setRecentAttendanceActivity(recentList);

        return stats;
    }

    public ByteArrayInputStream generateAttendanceCsv() {
        List<AttendanceRecord> records = recordRepository.findAllByOrderByRecordedAtDesc();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            // CSV Header
            writer.println("Record ID,Student Name,Register Number,Subject ID,Attendance Status,Method,Recorded At,Verification Notes");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

            for (AttendanceRecord r : records) {
                String recordedAtStr = r.getRecordedAt() != null ? dtf.format(r.getRecordedAt()) : "";
                String notes = r.getVerificationNotes() != null ? r.getVerificationNotes().replace("\"", "\"\"") : "";

                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        r.getId(),
                        r.getStudentName() != null ? r.getStudentName() : "",
                        r.getRegisterNumber() != null ? r.getRegisterNumber() : "",
                        r.getSubjectId() != null ? r.getSubjectId() : "",
                        r.getAttendanceStatus(),
                        r.getAttendanceMethod(),
                        recordedAtStr,
                        notes
                );
            }
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
