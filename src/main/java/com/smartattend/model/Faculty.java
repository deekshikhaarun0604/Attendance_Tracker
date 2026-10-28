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

    private List<TimetableEntry> timetable = new ArrayList<>();

    public List<TimetableEntry> getTimetable() {
        return timetable;
    }

    public void setTimetable(List<TimetableEntry> timetable) {
        this.timetable = timetable;
    }

    public static class TimetableEntry {
        private String subjectId;
        private String subjectCode;
        private String subjectName;
        private String department;
        private Integer yearOfStudy;
        private String section;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private String room;

        public TimetableEntry() {}

        public TimetableEntry(String subjectId, String subjectCode, String subjectName,
                              String department, Integer yearOfStudy, String section,
                              String dayOfWeek, String startTime, String endTime, String room) {
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.department = department;
            this.yearOfStudy = yearOfStudy;
            this.section = section;
            this.dayOfWeek = dayOfWeek;
            this.startTime = startTime;
            this.endTime = endTime;
            this.room = room;
        }

        public String getSubjectId() { return subjectId; }
        public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public Integer getYearOfStudy() { return yearOfStudy; }
        public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
        public String getSection() { return section; }
        public void setSection(String section) { this.section = section; }
        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getRoom() { return room; }
        public void setRoom(String room) { this.room = room; }
    }
}
