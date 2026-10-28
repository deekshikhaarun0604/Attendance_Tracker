package com.smartattend.dto;

import java.time.Instant;

public class AttendanceScanResult {

    public enum Outcome {
        SUCCESS,
        STUDENT_NOT_REGISTERED,
        BARCODE_NOT_REGISTERED,
        STUDENT_INACTIVE,
        ATTENDANCE_ALREADY_RECORDED,
        INVALID_CLASS_OR_SESSION,
        SESSION_CLOSED,
        CAMERA_PERMISSION_DENIED,
        BARCODE_COULD_NOT_BE_READ,
        SERVER_OR_DATABASE_ERROR
    }

    private Outcome outcome;
    private String message;
    private String studentId;
    private String studentName;
    private String registerNumber;
    private String collegeStudentId;
    private String department;
    private Integer yearOfStudy;
    private String section;
    private Instant recordedAt;
    private long totalPresentInSession;

    public AttendanceScanResult() {
    }

    public static AttendanceScanResult success(String message, String studentId, String studentName,
                                              String registerNumber, String collegeStudentId,
                                              String department, Integer yearOfStudy, String section,
                                              Instant recordedAt, long totalPresent) {
        AttendanceScanResult r = new AttendanceScanResult();
        r.setOutcome(Outcome.SUCCESS);
        r.setMessage(message);
        r.setStudentId(studentId);
        r.setStudentName(studentName);
        r.setRegisterNumber(registerNumber);
        r.setCollegeStudentId(collegeStudentId);
        r.setDepartment(department);
        r.setYearOfStudy(yearOfStudy);
        r.setSection(section);
        r.setRecordedAt(recordedAt);
        r.setTotalPresentInSession(totalPresent);
        return r;
    }

    public static AttendanceScanResult failure(Outcome outcome, String message) {
        AttendanceScanResult r = new AttendanceScanResult();
        r.setOutcome(outcome);
        r.setMessage(message);
        return r;
    }

    public static AttendanceScanResult alreadyRecorded(String studentName, String registerNumber, Instant recordedAt, long totalPresent) {
        AttendanceScanResult r = new AttendanceScanResult();
        r.setOutcome(Outcome.ATTENDANCE_ALREADY_RECORDED);
        r.setMessage("Attendance already recorded for " + studentName + " (" + registerNumber + ")");
        r.setStudentName(studentName);
        r.setRegisterNumber(registerNumber);
        r.setRecordedAt(recordedAt);
        r.setTotalPresentInSession(totalPresent);
        return r;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getCollegeStudentId() {
        return collegeStudentId;
    }

    public void setCollegeStudentId(String collegeStudentId) {
        this.collegeStudentId = collegeStudentId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public long getTotalPresentInSession() {
        return totalPresentInSession;
    }

    public void setTotalPresentInSession(long totalPresentInSession) {
        this.totalPresentInSession = totalPresentInSession;
    }
}
