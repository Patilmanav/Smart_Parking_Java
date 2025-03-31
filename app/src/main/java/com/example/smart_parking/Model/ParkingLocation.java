package com.example.smart_parking.Model;

import java.util.List;

public class ParkingLocation {
    private String locationId;
    private String name;
    private String address;
    private int totalSlots;
    private List<ParkingSlot> slots;
    private double hourlyRate;
    private String mapsLink; // Google Maps link
    private double distance; // Distance from user's current location
    private double latitude;
    private double longitude;

    public ParkingLocation() {
        // Required empty constructor for Firestore
    }

    public ParkingLocation(String name, String address, int totalSlots, double hourlyRate) {
        this.name = name;
        this.address = address;
        this.totalSlots = totalSlots;
        this.hourlyRate = hourlyRate;
        this.distance = 0.0;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public List<ParkingSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<ParkingSlot> slots) {
        this.slots = slots;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getMapsLink() {
        return mapsLink;
    }

    public void setMapsLink(String mapsLink) {
        this.mapsLink = mapsLink;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // Calculate distance between two points using Haversine formula
    public double calculateDistance(double userLat, double userLng) {
        double R = 6371; // Earth's radius in kilometers
        double dLat = Math.toRadians(latitude - userLat);
        double dLng = Math.toRadians(longitude - userLng);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(latitude)) *
                   Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
} 