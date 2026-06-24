package com.safetrip.service;

import com.safetrip.model.TripPlace;
import com.safetrip.repository.TripPlaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TripService {

    private final TripPlaceRepository repo;

    public TripService(TripPlaceRepository repo) {
        this.repo = repo;
    }

    // Save a place, always tagging it with the owning GitHub user's id.
    public TripPlace savePlace(TripPlace place, String githubId) {
        place.setGithubId(githubId);
        return repo.save(place);
    }

    // Only this user's saved places — data isolation.
    public List<TripPlace> getPlacesForUser(String githubId) {
        return repo.findByGithubId(githubId);
    }

    // Only delete the place if it belongs to this user.
    public boolean deletePlaceForUser(Long id, String githubId) {
        Optional<TripPlace> place = repo.findByIdAndGithubId(id, githubId);
        if (place.isEmpty()) {
            return false;
        }
        repo.deleteById(id);
        return true;
    }
}
