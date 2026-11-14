package com.safetrip.dto;

import java.util.List;
import java.util.Map;

public class PlanTripResponse {

    private String city;
    private int days;
    private Map<String, List<PlannedPlace>> itinerary;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public Map<String, List<PlannedPlace>> getItinerary() {
        return itinerary;
    }

    public void setItinerary(Map<String, List<PlannedPlace>> itinerary) {
        this.itinerary = itinerary;
    }
}