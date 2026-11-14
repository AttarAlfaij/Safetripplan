package com.safetrip.repository;

import com.safetrip.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByGithubId(String githubId);
    Optional<Trip> findByIdAndGithubId(Long id, String githubId);
}