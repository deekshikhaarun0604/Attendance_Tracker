package com.smartattend.dto;

import jakarta.validation.constraints.NotBlank;

public class AttendanceScanRequest {

    @NotBlank(message = "Attendance session ID is required")
    private String attendanceSessionId;

    @NotBlank(message = "Scanned barcode value is required")
    private String barcodeValue;

    public AttendanceScanRequest() {
    }

    public AttendanceScanRequest(String attendanceSessionId, String barcodeValue) {
        this.attendanceSessionId = attendanceSessionId;
        this.barcodeValue = barcodeValue;
    }

    public String getAttendanceSessionId() {
        return attendanceSessionId;
    }

    public void setAttendanceSessionId(String attendanceSessionId) {
        this.attendanceSessionId = attendanceSessionId;
    }

    public String getBarcodeValue() {
        return barcodeValue;
    }

    public void setBarcodeValue(String barcodeValue) {
        this.barcodeValue = barcodeValue;
    }
}
