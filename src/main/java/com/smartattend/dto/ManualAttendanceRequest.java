package com.smartattend.dto;

import jakarta.validation.constraints.NotBlank;

public class ManualAttendanceRequest {

    @NotBlank(message = "Attendance session ID is required")
    private String attendanceSessionId;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Attendance status is required (PRESENT or ABSENT)")
    private String attendanceStatus;

    @NotBlank(message = "Mandatory correction reason is required")
    private String reason;

    public ManualAttendanceRequest() {
    }

    public String getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public void setAttendanceSessionId(String attendanceSessionId) {
        this.attendanceSessionId = attendanceSessionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
