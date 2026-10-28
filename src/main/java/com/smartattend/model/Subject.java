package com.smartattend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "subjects")
public class Subject {

    @Id
    private String id;

    @Indexed(unique = true)
    private String subjectCode;

    private String subjectName;
    private String department;
    private Integer semester;
    private Integer yearOfStudy;

    public Subject() {
    }

    public Subject(String subjectCode, String subjectName, String department, Integer semester, Integer yearOfStudy) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.department = department;
        this.semester = semester;
        this.yearOfStudy = yearOfStudy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }
}
