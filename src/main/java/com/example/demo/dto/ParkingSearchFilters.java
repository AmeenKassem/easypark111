package com.example.demo.dto;

public class ParkingSearchFilters {

    private Boolean coveredOnly;
    private Double maxPrice;
    private String location;


    private String date;
    private String startTime;
    private String endTime;

    // --- Getters & Setters ---

    public Boolean getCoveredOnly() { return coveredOnly; }
    public void setCoveredOnly(Boolean coveredOnly) { this.coveredOnly = coveredOnly; }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}