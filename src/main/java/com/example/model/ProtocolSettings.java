package com.example.model;

public class ProtocolSettings {
    private boolean addBlankPage;

    public ProtocolSettings(boolean addBlankPage) {
        this.addBlankPage = addBlankPage;
    }

    public boolean isAddBlankPage() {
        return addBlankPage;
    }

    public void setAddBlankPage(boolean addBlankPage) {
        this.addBlankPage = addBlankPage;
    }
}