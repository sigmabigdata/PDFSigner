package com.example.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyInfo {
    private String number;
    private String issueDate;
    private String expiryDate;
    private String fullName;

    public ProxyInfo(String number, String issueDate, String expiryDate, String fullName) {
        this.number = number;
        this.issueDate = formatDate(issueDate);
        this.expiryDate = formatDate(expiryDate);
        this.fullName = fullName;
    }

    private String formatDate(String date) {
        try {
            SimpleDateFormat from = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat to = new SimpleDateFormat("dd.MM.yyyy");
            Date d = from.parse(date);
            return to.format(d);
        } catch (Exception e) {
            return date; // если не удалось преобразовать, оставляем как есть
        }
    }

    // Геттеры
    public String getNumber() { return number; }
    public String getIssueDate() { return issueDate; }
    public String getExpiryDate() { return expiryDate; }
    public String getFullName() { return fullName; }
    public boolean isRequired() { return number != null && !number.isEmpty(); }
}