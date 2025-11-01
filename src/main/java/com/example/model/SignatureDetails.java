package com.example.model;

public class SignatureDetails {
    private String position;
    private String company;
    private String fullName;
    private boolean isLegalEntity;

    // Конструктор
    public SignatureDetails() {
        this.position = "";
        this.company = "";
        this.fullName = "";
        this.isLegalEntity = false;
    }

    // Геттеры и сеттеры
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getCompany() { return company; }
    public void setCompany(String company) {
        this.company = company;
        if (!company.isEmpty()) {
            this.isLegalEntity = true;
        }
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isLegalEntity() { return isLegalEntity; }
    public void setLegalEntity(boolean legalEntity) { isLegalEntity = legalEntity; }
}