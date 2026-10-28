package com.smartattend.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "attendance_anomalies")
public class AttendanceAnomaly {

    @Id
    private String id;

    private String attendanceSessionId;
    private String studentId;
    private String registerNumber;
    private String studentName;
    private String scannedBarcodeValue;

    private String anomalyType; // DUPLICATE_ATTEMPT, UNREGISTERED_BARCODE, STUDENT_INACTIVE, NOT_ENROLLED_CLASS, SESSION_CLOSED, TIME_MISMATCH, LOW_ATTENDANCE
    private String severity; // SUSPICIOUS, VIOLATION
    private String description;
    private String resolutionNotes;
    private boolean resolved = false;

    @CreatedDate
    private Instant timestamp;

    public AttendanceAnomaly() {
        this.timestamp = Instant.now();
    }

    public AttendanceAnomaly(String attendanceSessionId, String studentId, String registerNumber,
                             String studentName, String scannedBarcodeValue, String anomalyType,
                             String severity, String description) {
        this.attendanceSessionId = attendanceSessionId;
        this.studentId = studentId;
        this.registerNumber = registerNumber;
        this.studentName = studentName;
        this.scannedBarcodeValue = scannedBarcodeValue;
        this.anomalyType = anomalyType;
        this.severity = severity;
        this.description = description;
        this.resolved = false;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getScannedBarcodeValue() {
        return scannedBarcodeValue;
    }

    public void setScannedBarcodeValue(String scannedBarcodeValue) {
        this.scannedBarcodeValue = scannedBarcodeValue;
    }

    public String getAnomalyType() {
        return anomalyType;
    }

    public void setAnomalyType(String anomalyType) {
        this.anomalyType = anomalyType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
