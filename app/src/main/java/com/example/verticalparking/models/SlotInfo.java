package com.example.verticalparking.models;

import com.google.gson.annotations.SerializedName;

public class SlotInfo {
    @SerializedName("slot")
    private int slot;

    @SerializedName("occupied")
    private boolean occupied;

    public int getSlot() { return slot; }
    public boolean isOccupied() { return occupied; }
}
