package com.example.model;

import java.util.List;

public class StampPosition {
    private final SignatureInfo remaining;

    public StampPosition(List<String> remainingLeft, List<String> remainingRight, List<String> remainingAdditional) {
        this.remaining = new SignatureInfo();
        this.remaining.bankSignerInfos = remainingLeft;
        this.remaining.rightSignerInfos = remainingRight;
        this.remaining.additionalSignerInfos = remainingAdditional;
    }

    public boolean hasRemainingStamps() {
        return !remaining.isEmpty();
    }

    public SignatureInfo getRemainingSignatureInfo() {
        return remaining;
    }
}