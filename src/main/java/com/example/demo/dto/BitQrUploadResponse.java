package com.example.demo.dto;

public class BitQrUploadResponse {
    private String bitQrImageUrl;
    private String bitPaymentUrl;

    public BitQrUploadResponse() {
    }

    public BitQrUploadResponse(String bitQrImageUrl, String bitPaymentUrl) {
        this.bitQrImageUrl = bitQrImageUrl;
        this.bitPaymentUrl = bitPaymentUrl;
    }

    public String getBitQrImageUrl() { return bitQrImageUrl; }
    public void setBitQrImageUrl(String bitQrImageUrl) { this.bitQrImageUrl = bitQrImageUrl; }

    public String getBitPaymentUrl() { return bitPaymentUrl; }
    public void setBitPaymentUrl(String bitPaymentUrl) { this.bitPaymentUrl = bitPaymentUrl; }
}
