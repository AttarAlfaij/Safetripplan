package com.safetrip.controller;

import com.safetrip.dto.PlanTripRequest;
import com.safetrip.dto.PlanTripResponse;
import com.safetrip.model.User;
import com.safetrip.service.TripPlanningService;
import com.safetrip.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PlanTripController {

    private final TripPlanningService tripPlanningService;
    private final UserService userService;

    public PlanTripController(TripPlanningService tripPlanningService, UserService userService) {
        this.tripPlanningService = tripPlanningService;
        this.userService = userService;
    }

    @PostMapping(path = "/api/plan-trip", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PlanTripResponse planTrip(@RequestBody PlanTripRequest request) {
        User user = userService.getAuthenticatedUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return tripPlanningService.planTrip(request, user);
    }
}
