package com.example.verticalparking.models;

import com.google.gson.annotations.SerializedName;

public class ParkResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("slot")
    private int slot;

    @SerializedName("message")
    private String message;

    public String getStatus() { return status; }
    public int getSlot() { return slot; }
    public String getMessage() { return message; }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
