package com.safetrip.dto;

public class PlannedPlace {

    private String name;
    private String category;
    private double lat;
    private double lon;
    private String osmLink;

    // New field for Google search link
    private String googleLink;

    // Scoring fields for sorting
    private boolean hasWikipedia;
    private boolean hasTourism;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getOsmLink() {
        return osmLink;
    }

    public void setOsmLink(String osmLink) {
        this.osmLink = osmLink;
    }

    public String getGoogleLink() {
        return googleLink;
    }

    public void setGoogleLink(String googleLink) {
        this.googleLink = googleLink;
    }

    public boolean isHasWikipedia() {
        return hasWikipedia;
    }

    public void setHasWikipedia(boolean hasWikipedia) {
        this.hasWikipedia = hasWikipedia;
    }

    public boolean isHasTourism() {
        return hasTourism;
    }

    public void setHasTourism(boolean hasTourism) {
        this.hasTourism = hasTourism;
    }
}