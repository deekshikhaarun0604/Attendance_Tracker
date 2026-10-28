package com.smartattend.dto;

import jakarta.validation.constraints.NotBlank;

public class BarcodeRegisterRequest {

    @NotBlank(message = "Barcode value is required")
    private String barcodeValue;

    public BarcodeRegisterRequest() {
    }

    public BarcodeRegisterRequest(String barcodeValue) {
        this.barcodeValue = barcodeValue;
    }

    public String getBarcodeValue() {
        return barcodeValue;
    }

    public void setBarcodeValue(String barcodeValue) {
        this.barcodeValue = barcodeValue;
    }
}
