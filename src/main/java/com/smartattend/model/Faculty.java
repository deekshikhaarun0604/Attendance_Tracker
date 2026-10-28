package com.smartattend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "faculties")
public class Faculty {

    @Id
    private String id;

    @Indexed(unique = true)
    private String employeeId;

    private String facultyName;
    private String department;

    @Indexed(unique = true)
    private String email;

    private String phone;
    private List<String> assignedSubjectIds = new ArrayList<>();
    private List<String> assignedClasses = new ArrayList<>(); // e.g. "CSE-3-A", "IT-2-B"

    public Faculty() {
    }

    public Faculty(String employeeId, String facultyName, String department, String email, String phone) {
        this.employeeId = employeeId;
        this.facultyName = facultyName;
        this.department = department;
        this.email = email;
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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

    public List<String> getAssignedSubjectIds() {
        return assignedSubjectIds;
    }

    public void setAssignedSubjectIds(List<String> assignedSubjectIds) {
        this.assignedSubjectIds = assignedSubjectIds;
    }

    public List<String> getAssignedClasses() {
        return assignedClasses;
    }

    public void setAssignedClasses(List<String> assignedClasses) {
        this.assignedClasses = assignedClasses;
    }
}
