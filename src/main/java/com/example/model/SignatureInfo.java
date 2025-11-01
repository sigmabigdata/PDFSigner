package com.example.model;

import java.util.ArrayList;
import java.util.List;

public class SignatureInfo {
    public List<String> bankSignerInfos = new ArrayList<>();
    public List<String> rightSignerInfos = new ArrayList<>();
    public List<String> additionalSignerInfos = new ArrayList<>();

    public boolean isEmpty() {
        return bankSignerInfos.isEmpty() && rightSignerInfos.isEmpty() && additionalSignerInfos.isEmpty();
    }
}
