package com.safetrip.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "trip_places")
public class TripPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;
    private Double lat;
    private Double lon;
    private String osmLink;

    /**
     * GitHub user id (OAuth2User attribute "id") that owns this place.
     * Always set server-side from the authenticated session — never trusted
     * from client input — so users can only ever see/modify their own data.
     */
    @Column(name = "github_id")
    private String githubId;

    /**
     * Many TripPlaces belong to one Trip.
     * This creates a "trip_id" foreign key column in trip_places table.
     * 
     * @JsonIgnore prevents infinite recursion during JSON serialization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = true)
    @JsonIgnore
    private Trip trip;

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
    }

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

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getOsmLink() {
        return osmLink;
    }

    public void setOsmLink(String osmLink) {
        this.osmLink = osmLink;
    }

    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }
}