package com.safetrip.controller;

import com.safetrip.model.Trip;
import com.safetrip.repository.TripRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripRepository tripRepository;

    public TripController(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    private String githubId(OAuth2User oAuth2User) {
        Object id = oAuth2User.getAttribute("id");
        return id != null ? id.toString() : oAuth2User.getAttribute("login");
    }

    @PostMapping
    public Trip createTrip(@RequestBody Trip trip, @AuthenticationPrincipal OAuth2User oAuth2User) {
        trip.setGithubId(githubId(oAuth2User));
        return tripRepository.save(trip);
    }
    @GetMapping
    public List<Trip> getAllTrips(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return tripRepository.findByGithubId(githubId(oAuth2User));
    }
    @GetMapping("/{id}")
    public Trip getTripById(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oAuth2User) {
        return tripRepository.findByIdAndGithubId(id, githubId(oAuth2User))
                .orElseThrow(() -> new RuntimeException("Trip not found"));
    }
    @DeleteMapping("/{id}")
    public String deleteTrip(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oAuth2User) {
        Trip trip = tripRepository.findByIdAndGithubId(id, githubId(oAuth2User))
                .orElseThrow(() -> new RuntimeException("Trip not found"));
        tripRepository.delete(trip);
        return "Trip deleted successfully!";
    }
    @PutMapping("/{id}")
    public Trip updateTrip(@PathVariable Long id, @RequestBody Trip updatedTrip, @AuthenticationPrincipal OAuth2User oAuth2User) {
        Trip trip = tripRepository.findByIdAndGithubId(id, githubId(oAuth2User))
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        trip.setDestination(updatedTrip.getDestination());
        trip.setMood(updatedTrip.getMood());

        return tripRepository.save(trip);
    }

}