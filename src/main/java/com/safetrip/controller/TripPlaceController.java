package com.safetrip.controller;

import com.safetrip.model.TripPlace;
import com.safetrip.model.User;
import com.safetrip.service.TripService;
import com.safetrip.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/my-places")
public class TripPlaceController {

    private final TripService tripService;
    private final UserService userService;

    public TripPlaceController(TripService tripService, UserService userService) {
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

    // ✅ Add place — saved under the authenticated user's ID
    @PostMapping
    public TripPlace savePlace(@RequestBody TripPlace place) {
        User user = getRequiredUser();
        return tripService.savePlace(place, user);
    }

    // ✅ Get all places — only the current user's own data
    @GetMapping
    public List<TripPlace> getAllPlaces() {
        User user = getRequiredUser();
        return tripService.getPlacesForUser(user);
    }

    // ✅ Delete place — only if it belongs to the current user
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlace(@PathVariable Long id) {
        User user = getRequiredUser();
        boolean deleted = tripService.deletePlaceForUser(id, user);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ✅ Delete ALL places for the current user (Remove All button)
    @DeleteMapping
    public ResponseEntity<?> deleteAllPlaces() {
        User user = getRequiredUser();
        tripService.deleteAllPlacesForUser(user);
        return ResponseEntity.noContent().build();
    }
}
