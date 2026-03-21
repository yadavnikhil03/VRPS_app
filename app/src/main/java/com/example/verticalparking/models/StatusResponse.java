package com.example.verticalparking.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StatusResponse {
    @SerializedName("totalSlots")
    private int totalSlots;

    @SerializedName("occupied")
    private int occupied;

    @SerializedName("slots")
    private List<SlotInfo> slots;

    public int getTotalSlots() { return totalSlots; }
    public int getOccupied() { return occupied; }
    public List<SlotInfo> getSlots() { return slots; }

    public int getAvailable() {
        return Math.max(0, totalSlots - occupied);
    }
}
