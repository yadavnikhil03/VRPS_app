package com.example.verticalparking.models;

import com.google.gson.annotations.SerializedName;

public class RetrieveRequest {
    @SerializedName("userId")
    private String userId;

    @SerializedName("pin")
    private String pin;

    public RetrieveRequest(String userId, String pin) {
        this.userId = userId;
        this.pin = pin;
    }

    public String getUserId() { return userId; }
    public String getPin() { return pin; }
}
