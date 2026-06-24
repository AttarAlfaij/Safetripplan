package com.safetrip.controller;

import com.safetrip.model.TripPlace;
import com.safetrip.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my-places")
public class TripPlaceController {

    private final TripService tripService;

    public TripPlaceController(TripService tripService) {
        this.tripService = tripService;
    }

    // ✅ Add place — always saved under the logged-in GitHub user's id
    @PostMapping
    public TripPlace savePlace(@RequestBody TripPlace place, @AuthenticationPrincipal OAuth2User oAuth2User) {
        return tripService.savePlace(place, githubId(oAuth2User));
    }

    // ✅ Get all places — only the current user's own data
    @GetMapping
    public List<TripPlace> getAllPlaces(@AuthenticationPrincipal OAuth2User oAuth2User) {
        return tripService.getPlacesForUser(githubId(oAuth2User));
    }

    // ✅ Delete place — only if it belongs to the current user
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlace(@PathVariable Long id, @AuthenticationPrincipal OAuth2User oAuth2User) {
        boolean deleted = tripService.deletePlaceForUser(id, githubId(oAuth2User));
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Stable GitHub user identifier used for data isolation.
     * This endpoint is already gated by Spring Security (/api/** requires
     * authentication), so oAuth2User is never null in practice here.
     */
    private String githubId(OAuth2User oAuth2User) {
        Object id = oAuth2User.getAttribute("id");
        return id != null ? id.toString() : oAuth2User.getAttribute("login");
    }
}
