package com.example.verticalparking.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a user's vehicle parking session.
 * PIN is always stored as a SHA-256 hash — never plain text.
 */
@Entity(tableName = "user_vehicle")
public class UserVehicle {

    @PrimaryKey
    @NonNull
    private String userId;

    private String vehicleName;
    private String licensePlate;
    private boolean isParked;
    private int slotNumber;
    private String pinHash;
    private long parkedAt;

    public UserVehicle() {
        this.userId = "";
    }

    // Getters
    @NonNull
    public String getUserId() { return userId; }
    public String getVehicleName() { return vehicleName; }
    public String getLicensePlate() { return licensePlate; }
    public boolean isParked() { return isParked; }
    public int getSlotNumber() { return slotNumber; }
    public String getPinHash() { return pinHash; }
    public long getParkedAt() { return parkedAt; }

    // Setters
    public void setUserId(@NonNull String userId) { this.userId = userId; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public void setParked(boolean parked) { isParked = parked; }
    public void setSlotNumber(int slotNumber) { this.slotNumber = slotNumber; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public void setParkedAt(long parkedAt) { this.parkedAt = parkedAt; }
}
