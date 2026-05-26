package com.beadalondo.api.location.controller;

import com.beadalondo.api.location.dto.JusoAddressRequest;
import com.beadalondo.api.location.service.LocationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/grid-preview")
    public WeatherGridPreviewResponse previewGrid(@RequestBody JusoAddressRequest request) {
        return locationService.previewGrid(request);
    }
}