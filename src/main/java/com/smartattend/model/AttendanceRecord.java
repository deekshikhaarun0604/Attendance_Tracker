package com.smartattend.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "attendance_records")
@CompoundIndexes({
    @CompoundIndex(name = "session_student_unique_idx", def = "{'attendanceSessionId': 1, 'studentId': 1}", unique = true)
})
public class AttendanceRecord {

    @Id
    private String id;

    @Indexed
    private String attendanceSessionId;

    @Indexed
    private String studentId;

    private String registerNumber;

    private String studentName;

    private String subjectId;

    private String facultyId;

    private String attendanceStatus; // PRESENT, ABSENT

    private String scannedBarcodeValue;

    @CreatedDate
    private Instant recordedAt;

    private String attendanceMethod; // WEBCAM_BARCODE, MANUAL_OVERRIDE

    private String verificationNotes;

    private String correctedBy;

    private String correctionReason;

    public AttendanceRecord() {
    }

    public AttendanceRecord(String attendanceSessionId, String studentId, String registerNumber,
                            String studentName, String subjectId, String facultyId,
                            String attendanceStatus, String scannedBarcodeValue,
                            String attendanceMethod, String verificationNotes) {
        this.attendanceSessionId = attendanceSessionId;
        this.studentId = studentId;
        this.registerNumber = registerNumber;
        this.studentName = studentName;
        this.subjectId = subjectId;
        this.facultyId = facultyId;
        this.attendanceStatus = attendanceStatus;
        this.scannedBarcodeValue = scannedBarcodeValue;
        this.attendanceMethod = attendanceMethod;
        this.verificationNotes = verificationNotes;
        this.recordedAt = Instant.now();
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

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public String getScannedBarcodeValue() {
        return scannedBarcodeValue;
    }

    public void setScannedBarcodeValue(String scannedBarcodeValue) {
        this.scannedBarcodeValue = scannedBarcodeValue;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getAttendanceMethod() {
        return attendanceMethod;
    }

    public void setAttendanceMethod(String attendanceMethod) {
        this.attendanceMethod = attendanceMethod;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }

    public String getCorrectedBy() {
        return correctedBy;
    }

    public void setCorrectedBy(String correctedBy) {
        this.correctedBy = correctedBy;
    }

    public String getCorrectionReason() {
        return correctionReason;
    }

    public void setCorrectionReason(String correctionReason) {
        this.correctionReason = correctionReason;
    }
}
