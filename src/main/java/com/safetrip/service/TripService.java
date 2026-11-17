package com.safetrip.service;

import com.safetrip.model.Trip;
import com.safetrip.model.TripPlace;
import com.safetrip.model.User;
import com.safetrip.repository.TripPlaceRepository;
import com.safetrip.repository.TripRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    private final TripPlaceRepository tripPlaceRepo;
    private final TripRepository tripRepo;

    public TripService(TripPlaceRepository tripPlaceRepo, TripRepository tripRepo) {
        this.tripPlaceRepo = tripPlaceRepo;
        this.tripRepo = tripRepo;
    }

    // ==========================================
    // PLACES (Data Isolation by authenticated User)
    // ==========================================

    @Transactional
    public TripPlace savePlace(TripPlace place, User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated.");
        }
        place.setUser(user);
        place.setGithubId(user.getId().toString());

        // If this place is linked to a Trip, verify the trip belongs to this user
        if (place.getTrip() != null && place.getTrip().getId() != null) {
            Trip trip = tripRepo.findById(place.getTrip().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Associated trip not found."));
            if (trip.getUser() != null && !trip.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot add places to a trip owned by another user.");
            }
            place.setTrip(trip);
        }

        return tripPlaceRepo.save(place);
    }

    @Transactional
    public List<TripPlace> getPlacesForUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated.");
        }

        List<TripPlace> places = tripPlaceRepo.findByUserId(user.getId());

        // Backward compatibility migration for legacy rows
        if (places.isEmpty()) {
            List<TripPlace> legacyPlaces = tripPlaceRepo.findByGithubId(user.getId().toString());
            if (!legacyPlaces.isEmpty()) {
                for (TripPlace p : legacyPlaces) {
                    p.setUser(user);
                }
                tripPlaceRepo.saveAll(legacyPlaces);
                places = legacyPlaces;
            }
        }

        return places;
    }

    @Transactional
    public boolean deletePlaceForUser(Long id, User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated.");
        }

        Optional<TripPlace> placeOpt = tripPlaceRepo.findById(id);
        if (placeOpt.isEmpty()) {
            return false;
        }

        TripPlace place = placeOpt.get();

        // Enforce strict ownership check
        boolean isOwner = (place.getUser() != null && place.getUser().getId().equals(user.getId()))
                || (place.getUser() == null && user.getId().toString().equals(place.getGithubId()));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this place.");
        }

        tripPlaceRepo.delete(place);
        return true;
    }

    @Transactional
    public void deleteAllPlacesForUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated.");
        }
        tripPlaceRepo.deleteByUserId(user.getId());
        tripPlaceRepo.deleteByGithubId(user.getId().toString());
    }

    // ==========================================
    // TRIPS (Data Isolation by authenticated User)
    // ==========================================

    @Transactional
    public Trip saveTrip(Trip trip, User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated.");
        }
        trip.setUser(user);
        trip.setGithubId(user.getId().toString());
        return tripRepo.save(trip);
    }

    @Transactional
    public List<Trip> getTripsForUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated.");
        }

        List<Trip> trips = tripRepo.findByUserId(user.getId());

        // Backward compatibility migration for legacy rows
        if (trips.isEmpty()) {
            List<Trip> legacyTrips = tripRepo.findByGithubId(user.getId().toString());
            if (!legacyTrips.isEmpty()) {
                for (Trip t : legacyTrips) {
                    t.setUser(user);
                }
                tripRepo.saveAll(legacyTrips);
                trips = legacyTrips;
            }
        }

        return trips;
    }

    @Transactional(readOnly = true)
    public Trip getTripByIdForUser(Long id, User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated.");
        }

        Trip trip = tripRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        boolean isOwner = (trip.getUser() != null && trip.getUser().getId().equals(user.getId()))
                || (trip.getUser() == null && user.getId().toString().equals(trip.getGithubId()));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not own this trip.");
        }

        return trip;
    }

    @Transactional
    public void deleteTripForUser(Long id, User user) {
        Trip trip = getTripByIdForUser(id, user);

        // Delete associated places first to maintain referential integrity
        List<TripPlace> places = tripPlaceRepo.findByTripId(trip.getId());
        if (!places.isEmpty()) {
            tripPlaceRepo.deleteAll(places);
        }

        tripRepo.delete(trip);
    }

    @Transactional
    public Trip updateTripForUser(Long id, Trip updatedTrip, User user) {
        Trip trip = getTripByIdForUser(id, user);

        trip.setDestination(updatedTrip.getDestination());
        trip.setMood(updatedTrip.getMood());

        return tripRepo.save(trip);
    }

    // ==========================================
    // Backward compatibility methods
    // ==========================================

    @Transactional
    public TripPlace savePlace(TripPlace place, String githubId) {
        place.setGithubId(githubId);
        return tripPlaceRepo.save(place);
    }

    @Transactional(readOnly = true)
    public List<TripPlace> getPlacesForUser(String githubId) {
        return tripPlaceRepo.findByGithubId(githubId);
    }

    @Transactional
    public boolean deletePlaceForUser(Long id, String githubId) {
        Optional<TripPlace> place = tripPlaceRepo.findByIdAndGithubId(id, githubId);
        if (place.isEmpty()) {
            return false;
        }
        tripPlaceRepo.deleteById(id);
        return true;
    }

    @Transactional
    public void deleteAllPlacesForUser(String githubId) {
        tripPlaceRepo.deleteByGithubId(githubId);
    }
}
