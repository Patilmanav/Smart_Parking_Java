package com.example.smart_parking.Model;

public class ParkingSlot {
    private String slotId;
    private int slotNumber;
    private String status;
    private String bookedBy;
    private int bookingDuration;
    private boolean isOccupied;
    private String currentBookingId;
    private String vehicleType; // For special slots (e.g., handicapped, electric vehicle)

    public ParkingSlot() {
        // Required empty constructor for Firestore
    }

    public ParkingSlot(int slotNumber, String vehicleType) {
        this.slotNumber = slotNumber;
        this.vehicleType = vehicleType;
        this.status = "Available";
        this.bookedBy = null;
        this.bookingDuration = 0;
        this.isOccupied = false;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookedBy() {
        return bookedBy;
    }

    public void setBookedBy(String bookedBy) {
        this.bookedBy = bookedBy;
    }

    public int getBookingDuration() {
        return bookingDuration;
    }

    public void setBookingDuration(int bookingDuration) {
        this.bookingDuration = bookingDuration;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }

    public String getCurrentBookingId() {
        return currentBookingId;
    }

    public void setCurrentBookingId(String currentBookingId) {
        this.currentBookingId = currentBookingId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
} 