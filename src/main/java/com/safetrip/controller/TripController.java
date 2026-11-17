package com.safetrip.controller;

import com.safetrip.model.Trip;
import com.safetrip.model.User;
import com.safetrip.service.TripService;
import com.safetrip.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final UserService userService;

    public TripController(TripService tripService, UserService userService) {
        this.tripService = tripService;
        this.userService = userService;
    }

    private User getRequiredUser() {
        User user = userService.getAuthenticatedUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return user;
    }

    @PostMapping
    public Trip createTrip(@RequestBody Trip trip) {
        User user = getRequiredUser();
        return tripService.saveTrip(trip, user);
    }

    @GetMapping
    public List<Trip> getAllTrips() {
        User user = getRequiredUser();
        return tripService.getTripsForUser(user);
    }

    @GetMapping("/{id}")
    public Trip getTripById(@PathVariable Long id) {
        User user = getRequiredUser();
        return tripService.getTripByIdForUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String deleteTrip(@PathVariable Long id) {
        User user = getRequiredUser();
        tripService.deleteTripForUser(id, user);
        return "Trip deleted successfully!";
    }

    @PutMapping("/{id}")
    public Trip updateTrip(@PathVariable Long id, @RequestBody Trip updatedTrip) {
        User user = getRequiredUser();
        return tripService.updateTripForUser(id, updatedTrip, user);
    }
}
