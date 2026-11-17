package com.safetrip.repository;

import com.safetrip.model.TripPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripPlaceRepository extends JpaRepository<TripPlace, Long> {

    /** Fetch all places belonging to a specific trip. */
    List<TripPlace> findByTripId(Long tripId);

    /** Fetch all places belonging to a specific user (strict data isolation). */
    List<TripPlace> findByUserId(Long userId);

    /** Fetch a single place only if it belongs to the given user. */
    Optional<TripPlace> findByIdAndUserId(Long id, Long userId);

    /** Delete every place belonging to a specific user (used by Remove All). */
    void deleteByUserId(Long userId);

    // Legacy fallback methods for backwards compatibility
    List<TripPlace> findByGithubId(String githubId);
    Optional<TripPlace> findByIdAndGithubId(Long id, String githubId);
    void deleteByGithubId(String githubId);
}
