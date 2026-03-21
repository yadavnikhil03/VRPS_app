package com.example.verticalparking.models;

import com.google.gson.annotations.SerializedName;

public class ParkRequest {
    @SerializedName("userId")
    private String userId;

    @SerializedName("pin")
    private String pin;

    @SerializedName("vehicleName")
    private String vehicleName;

    public ParkRequest(String userId, String pin, String vehicleName) {
        this.userId = userId;
        this.pin = pin;
        this.vehicleName = vehicleName;
    }

    public String getUserId() { return userId; }
    public String getPin() { return pin; }
    public String getVehicleName() { return vehicleName; }
}
