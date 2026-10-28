package com.smartattend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartattend.dto.ChatMessageResponse;
import com.smartattend.model.AttendanceRecord;
import com.smartattend.model.AttendanceSession;
import com.smartattend.model.Student;
import com.smartattend.repository.*;
import com.smartattend.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class GeminiChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiChatbotService.class);

    @Value("${smartattend.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${smartattend.gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String geminiApiUrl;

    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceAnomalyRepository anomalyRepository;
    private final AttendanceService attendanceService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiChatbotService(StudentRepository studentRepository,
                                AttendanceRecordRepository recordRepository,
                                AttendanceSessionRepository sessionRepository,
                                AttendanceAnomalyRepository anomalyRepository,
                                AttendanceService attendanceService) {
        this.studentRepository = studentRepository;
        this.recordRepository = recordRepository;
        this.sessionRepository = sessionRepository;
        this.anomalyRepository = anomalyRepository;
        this.attendanceService = attendanceService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public ChatMessageResponse processUserMessage(String userMessage, UserDetailsImpl user) {
        if (!StringUtils.hasText(userMessage)) {
            return new ChatMessageResponse("Please provide a question or instruction.", "SMARTATTEND_ENGINE");
        }

        // 1. Gather strictly role-authorized real MongoDB data
        String role = user.getRole();
        String context = buildRoleAuthorizedContext(user);

        // 2. Try Gemini API if key is present
        if (StringUtils.hasText(geminiApiKey) && !geminiApiKey.equalsIgnoreCase("your_gemini_api_key_here")) {
            try {
                String aiResponse = callGeminiApi(userMessage, role, context, user.getFullName());
                if (StringUtils.hasText(aiResponse)) {
                    return new ChatMessageResponse(aiResponse, "GEMINI_AI");
                }
            } catch (Exception e) {
                logger.warn("Gemini API call failed or unavailable: {}. Falling back to SmartAttend Engine.", e.getMessage());
            }
        }

        // 3. Fallback to intelligent rule-based knowledge engine using the same real MongoDB data
        String fallbackResponse = generateIntelligentFallback(userMessage, user);
        return new ChatMessageResponse(fallbackResponse, "SMARTATTEND_ENGINE");
    }

    private String buildRoleAuthorizedContext(UserDetailsImpl user) {
        String role = user.getRole();
        StringBuilder sb = new StringBuilder();

        if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
            long totalStudents = studentRepository.count();
            long activeStudents = studentRepository.countByStatus("ACTIVE");
            long pendingBarcodes = studentRepository.countByBarcodeRegistered(false);
            long registeredBarcodes = studentRepository.countByBarcodeRegistered(true);
            long activeAnomalies = anomalyRepository.countByResolvedFalse();
            long todaySessions = sessionRepository.findBySessionDate(LocalDate.now()).size();

            sb.append("COLLEGE STATS FOR ADMIN:\n")
              .append("- Total Registered Students: ").append(totalStudents).append("\n")
              .append("- Active Students: ").append(activeStudents).append("\n")
              .append("- Students Awaiting Barcode Registration: ").append(pendingBarcodes).append("\n")
              .append("- Students with Registered Barcodes: ").append(registeredBarcodes).append("\n")
              .append("- Unresolved Attendance Anomalies/Alerts: ").append(activeAnomalies).append("\n")
              .append("- Total Attendance Sessions Today: ").append(todaySessions).append("\n");

        } else if ("ROLE_FACULTY".equalsIgnoreCase(role)) {
            List<AttendanceSession> sessions = sessionRepository.findByFacultyIdOrderByCreatedAtDesc(user.getId());
            long openSessions = sessions.stream().filter(s -> "OPEN".equalsIgnoreCase(s.getStatus())).count();

            sb.append("FACULTY STATS:\n")
              .append("- Faculty Name: ").append(user.getFullName()).append("\n")
              .append("- Total Sessions Created: ").append(sessions.size()).append("\n")
              .append("- Currently Open Sessions: ").append(openSessions).append("\n");

        } else if ("ROLE_STUDENT".equalsIgnoreCase(role)) {
            String studentId = user.getLinkedStudentId();
            if (studentId != null) {
                try {
                    Map<String, Object> summary = attendanceService.getStudentAttendanceSummary(studentId);
                    Student s = (Student) summary.get("student");
                    sb.append("STUDENT PERSONAL ATTENDANCE:\n")
                      .append("- Student Name: ").append(s.getStudentName()).append("\n")
                      .append("- Register Number: ").append(s.getRegisterNumber()).append("\n")
                      .append("- Department: ").append(s.getDepartment()).append("\n")
                      .append("- Year & Section: Year ").append(s.getYearOfStudy()).append(", Sec ").append(s.getSection()).append("\n")
                      .append("- Overall Attendance: ").append(summary.get("overallPercentage")).append("%\n")
                      .append("- Total Classes: ").append(summary.get("totalClasses")).append("\n")
                      .append("- Present: ").append(summary.get("presentClasses")).append("\n")
                      .append("- Absent: ").append(summary.get("absentClasses")).append("\n")
                      .append("- Classes needed to reach 75%: ").append(summary.get("classesNeededFor75")).append("\n");
                } catch (Exception e) {
                    sb.append("Student profile record not yet linked.\n");
                }
            } else {
                sb.append("No student profile linked to this user account.\n");
            }
        }

        return sb.toString();
    }

    private String callGeminiApi(String userMessage, String role, String context, String userName) throws Exception {
        String url = geminiApiUrl + "?key=" + geminiApiKey.trim();

        String systemPrompt = "You are SmartAttend AI Assistant, a college attendance management AI. "
                + "User: " + userName + " (Role: " + role + "). "
                + "Answer the user's question accurately using ONLY the real system facts below. "
                + "Be concise, professional, friendly, and helpful. Do not disclose private records of other users. "
                + "Context:\n" + context;

        Map<String, Object> part = Map.of("text", systemPrompt + "\n\nUser Question: " + userMessage);
        Map<String, Object> content = Map.of("role", "user", "parts", List.of(part));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!textNode.isMissingNode()) {
                return textNode.asText().trim();
            }
        }

        return null;
    }

    private String generateIntelligentFallback(String query, UserDetailsImpl user) {
        String q = query.toLowerCase();
        String role = user.getRole();

        if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
            if (q.contains("registered") || q.contains("how many students") || q.contains("total student")) {
                long total = studentRepository.count();
                long active = studentRepository.countByStatus("ACTIVE");
                return "There are currently **" + total + " registered students** in the database (" + active + " active).";
            }
            if (q.contains("pending") || q.contains("barcode")) {
                long pending = studentRepository.countByBarcodeRegistered(false);
                long registered = studentRepository.countByBarcodeRegistered(true);
                return "There are **" + pending + " students** with pending barcode registration. **" + registered + " students** already have active barcodes registered.";
            }
            if (q.contains("low") || q.contains("shortage")) {
                long total = studentRepository.count();
                return "You have low attendance tracking enabled in the Admin Dashboard. Students falling below 75% are highlighted with automatic warning badges.";
            }
            if (q.contains("anomal") || q.contains("alert") || q.contains("fraud") || q.contains("suspicious")) {
                long anomalies = anomalyRepository.countByResolvedFalse();
                return "There are **" + anomalies + " active attendance anomalies** currently requiring administrative review in the Alerts section.";
            }
            return "As College Administrator, you have full control over registered students (" + studentRepository.count() + " total), barcode assignments, attendance records, and anomaly tracking. How can I assist you further?";
        }

        if ("ROLE_FACULTY".equalsIgnoreCase(role)) {
            if (q.contains("present") || q.contains("today") || q.contains("attendance")) {
                List<AttendanceSession> sessions = sessionRepository.findByFacultyIdOrderByCreatedAtDesc(user.getId());
                return "You have created **" + sessions.size() + " attendance sessions**. Use the Webcam Scanner tab to scan physical ID cards in real time!";
            }
            if (q.contains("absent") || q.contains("list")) {
                return "To view the absent student list for an active session, open the session card on your Faculty Dashboard and view the real-time session summary.";
            }
            return "Hello Professor " + user.getFullName() + "! You can start an attendance session, scan student ID barcodes with your webcam, and review present/absent counts in real time.";
        }

        if ("ROLE_STUDENT".equalsIgnoreCase(role)) {
            String studentId = user.getLinkedStudentId();
            if (studentId != null) {
                try {
                    Map<String, Object> summary = attendanceService.getStudentAttendanceSummary(studentId);
                    double pct = (double) summary.get("overallPercentage");
                    long needed = (long) summary.get("classesNeededFor75");
                    long total = (long) summary.get("totalClasses");
                    long present = (long) summary.get("presentClasses");

                    if (q.contains("percentage") || q.contains("my attendance") || q.contains("percent")) {
                        return "Your current overall attendance is **" + pct + "%** (" + present + " attended out of " + total + " total classes).";
                    }
                    if (q.contains("how many") || q.contains("more classes") || q.contains("reach") || q.contains("75")) {
                        if (pct >= 75.0) {
                            return "Great job! Your attendance is **" + pct + "%**, which meets or exceeds the 75% college requirement.";
                        } else {
                            return "Your attendance is currently **" + pct + "%**. You need to attend the next **" + needed + " consecutive classes** to reach the 75% threshold.";
                        }
                    }
                    if (q.contains("subject") || q.contains("improve")) {
                        return "Your overall attendance is **" + pct + "%**. Check your Subject Breakdown table on your dashboard to see which specific subjects need attention.";
                    }
                    return "Hi " + user.getFullName() + "! Your overall attendance is **" + pct + "%** (" + present + "/" + total + " classes). You can ask me 'What is my attendance percentage?' or 'How many more classes do I need to attend?'.";
                } catch (Exception e) {
                    return "Your student profile is currently being loaded. Please check back in a moment.";
                }
            }
            return "Welcome! Your student account is active. Log into the Student Portal to track your attendance and progress.";
        }

        return "SmartAttend Assistant is ready to help! You can ask questions about student registration, webcam scanning, and attendance reports.";
    }
}
