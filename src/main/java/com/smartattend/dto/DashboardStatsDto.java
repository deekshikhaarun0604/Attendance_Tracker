package com.smartattend.dto;

import java.util.List;
import java.util.Map;

public class DashboardStatsDto {
    private long totalStudents;
    private long totalActiveStudents;
    private long totalFaculty;
    private long totalClassesToday;
    private double todayAttendancePercentage;
    private long pendingBarcodeCount;
    private long registeredBarcodeCount;
    private long lowAttendanceCount;
    private long activeAnomaliesCount;

    private List<Map<String, Object>> lowAttendanceStudents;
    private List<Map<String, Object>> recentAttendanceActivity;
    private Map<String, Long> departmentStudentCount;
    private Map<String, Double> departmentAttendancePercentage;

    public DashboardStatsDto() {
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalActiveStudents() {
        return totalActiveStudents;
    }

    public void setTotalActiveStudents(long totalActiveStudents) {
        this.totalActiveStudents = totalActiveStudents;
    }

    public long getTotalFaculty() {
        return totalFaculty;
    }

    public void setTotalFaculty(long totalFaculty) {
        this.totalFaculty = totalFaculty;
    }

    public long getTotalClassesToday() {
        return totalClassesToday;
    }

    public void setTotalClassesToday(long totalClassesToday) {
        this.totalClassesToday = totalClassesToday;
    }

    public double getTodayAttendancePercentage() {
        return todayAttendancePercentage;
    }

    public void setTodayAttendancePercentage(double todayAttendancePercentage) {
        this.todayAttendancePercentage = todayAttendancePercentage;
    }

    public long getPendingBarcodeCount() {
        return pendingBarcodeCount;
    }

    public void setPendingBarcodeCount(long pendingBarcodeCount) {
        this.pendingBarcodeCount = pendingBarcodeCount;
    }

    public long getRegisteredBarcodeCount() {
        return registeredBarcodeCount;
    }

    public void setRegisteredBarcodeCount(long registeredBarcodeCount) {
        this.registeredBarcodeCount = registeredBarcodeCount;
    }

    public long getLowAttendanceCount() {
        return lowAttendanceCount;
    }

    public void setLowAttendanceCount(long lowAttendanceCount) {
        this.lowAttendanceCount = lowAttendanceCount;
    }

    public long getActiveAnomaliesCount() {
        return activeAnomaliesCount;
    }

    public void setActiveAnomaliesCount(long activeAnomaliesCount) {
        this.activeAnomaliesCount = activeAnomaliesCount;
    }

    public List<Map<String, Object>> getLowAttendanceStudents() {
        return lowAttendanceStudents;
    }

    public void setLowAttendanceStudents(List<Map<String, Object>> lowAttendanceStudents) {
        this.lowAttendanceStudents = lowAttendanceStudents;
    }

    public List<Map<String, Object>> getRecentAttendanceActivity() {
        return recentAttendanceActivity;
    }

    public void setRecentAttendanceActivity(List<Map<String, Object>> recentAttendanceActivity) {
        this.recentAttendanceActivity = recentAttendanceActivity;
    }

    public Map<String, Long> getDepartmentStudentCount() {
        return departmentStudentCount;
    }

    public void setDepartmentStudentCount(Map<String, Long> departmentStudentCount) {
        this.departmentStudentCount = departmentStudentCount;
    }

    public Map<String, Double> getDepartmentAttendancePercentage() {
        return departmentAttendancePercentage;
    }

    public void setDepartmentAttendancePercentage(Map<String, Double> departmentAttendancePercentage) {
        this.departmentAttendancePercentage = departmentAttendancePercentage;
    }
}
