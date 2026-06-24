package com.safetrip.controller;

import com.safetrip.dto.PlanTripRequest;
import com.safetrip.dto.PlanTripResponse;
import com.safetrip.service.TripPlanningService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlanTripController {

    private final TripPlanningService tripPlanningService;

    public PlanTripController(TripPlanningService tripPlanningService) {
        this.tripPlanningService = tripPlanningService;
    }

    @PostMapping(path = "/api/plan-trip", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PlanTripResponse planTrip(@RequestBody PlanTripRequest request,
                                      @AuthenticationPrincipal OAuth2User oAuth2User) {
        return tripPlanningService.planTrip(request, githubId(oAuth2User));
    }

    // This endpoint is under /api/** which Spring Security already requires
    // authentication for, so oAuth2User is never null here in practice.
    private String githubId(OAuth2User oAuth2User) {
        Object id = oAuth2User.getAttribute("id");
        return id != null ? id.toString() : oAuth2User.getAttribute("login");
    }
}
