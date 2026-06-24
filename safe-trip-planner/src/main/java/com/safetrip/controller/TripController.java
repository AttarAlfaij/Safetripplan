package com.safetrip.controller;

import com.safetrip.model.Trip;
import com.safetrip.repository.TripRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripRepository tripRepository;

    public TripController(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @PostMapping
    public Trip createTrip(@RequestBody Trip trip) {
        return tripRepository.save(trip);
    }
    @GetMapping
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }
    @GetMapping("/{id}")
    public Trip getTripById(@PathVariable Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));
    }
    @DeleteMapping("/{id}")
    public String deleteTrip(@PathVariable Long id) {
        tripRepository.deleteById(id);
        return "Trip deleted successfully!";
    }
    @PutMapping("/{id}")
    public Trip updateTrip(@PathVariable Long id, @RequestBody Trip updatedTrip) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        trip.setDestination(updatedTrip.getDestination());
        trip.setMood(updatedTrip.getMood());

        return tripRepository.save(trip);
    }

}