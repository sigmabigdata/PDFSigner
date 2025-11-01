package com.example.model;

import java.util.List;

public class ProtocolData {
    private final String docNumber;
    private final String docDate;
    private final String verificationDate;
    private final String employeeName;
    private final String docType;
    private final List<String> signers;

    public ProtocolData(String docType, String docNumber, String docDate,
                        String verificationDate, String employeeName, List<String> signers) {
        this.docType = docType;
        this.docNumber = docNumber;
        this.docDate = docDate;
        this.verificationDate = verificationDate;
        this.employeeName = employeeName;
        this.signers = signers;
    }

    // Геттеры и сеттеры
    public String getDocNumber() { return docNumber; }
    public String getDocDate() { return docDate; }
    public String getVerificationDate() { return verificationDate; }
    public String getEmployeeName() { return employeeName; }
    public String getDocType() { return docType; }
    public List<String> getSigners() { return signers; }
}