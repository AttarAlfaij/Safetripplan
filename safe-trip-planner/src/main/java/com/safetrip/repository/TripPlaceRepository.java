package com.safetrip.repository;

import com.safetrip.model.TripPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripPlaceRepository extends JpaRepository<TripPlace, Long> {

    /** Fetch all places belonging to a specific trip. */
    List<TripPlace> findByTripId(Long tripId);

    /** Fetch all places belonging to a specific GitHub user (data isolation). */
    List<TripPlace> findByGithubId(String githubId);

    /** Fetch a single place only if it belongs to the given GitHub user. */
    java.util.Optional<TripPlace> findByIdAndGithubId(Long id, String githubId);
}