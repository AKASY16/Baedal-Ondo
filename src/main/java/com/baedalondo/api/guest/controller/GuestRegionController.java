package com.baedalondo.api.guest.controller;

import com.baedalondo.api.guest.domain.GuestRegion;
import com.baedalondo.api.guest.dto.GuestRegionRegisterRequest;
import com.baedalondo.api.guest.dto.GuestRegionResponse;
import com.baedalondo.api.guest.service.GuestRegionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest-regions")
public class GuestRegionController {

    private final GuestRegionService guestRegionService;

    public GuestRegionController(GuestRegionService guestRegionService) {
        this.guestRegionService = guestRegionService;
    }

    @PostMapping
    public GuestRegionResponse registerGuestRegion(@RequestBody GuestRegionRegisterRequest request) {
        GuestRegion savedGuestRegion = guestRegionService.registerGuestRegion(request);
        return GuestRegionResponse.from(savedGuestRegion);
    }
}
