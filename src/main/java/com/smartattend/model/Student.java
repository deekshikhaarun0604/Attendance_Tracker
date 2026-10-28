package com.smartattend.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "students")
public class Student {

    @Id
    private String id;

    private String studentName;

    @Indexed(unique = true)
    private String registerNumber;

    @Indexed(unique = true)
    private String studentId;

    private String department;

    private String academicYear;

    private Integer yearOfStudy;

    private String section;

    @Indexed(unique = true)
    private String email;

    private String phone;

    @Indexed(unique = true, sparse = true)
    private String barcodeValue;

    private boolean barcodeRegistered = false;

    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    private String profilePhotoUrl;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Student() {
    }

    public Student(String studentName, String registerNumber, String studentId, String department,
                   String academicYear, Integer yearOfStudy, String section, String email, String phone) {
        this.studentName = studentName;
        this.registerNumber = registerNumber;
        this.studentId = studentId;
        this.department = department;
        this.academicYear = academicYear;
        this.yearOfStudy = yearOfStudy;
        this.section = section;
        this.email = email;
        this.phone = phone;
        this.status = "ACTIVE";
        this.barcodeRegistered = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBarcodeValue() {
        return barcodeValue;
    }

    public void setBarcodeValue(String barcodeValue) {
        this.barcodeValue = barcodeValue;
    }

    public boolean isBarcodeRegistered() {
        return barcodeRegistered;
    }

    public void setBarcodeRegistered(boolean barcodeRegistered) {
        this.barcodeRegistered = barcodeRegistered;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
