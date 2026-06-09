package com.beadalondo.api.guest.dto;

import com.beadalondo.api.location.dto.JusoAddressRequest;

public class GuestRegionRegisterRequest {

    private JusoAddressRequest jusoAddress;

    public JusoAddressRequest getJusoAddress() {
        return jusoAddress;
    }

    public void setJusoAddress(JusoAddressRequest jusoAddress) {
        this.jusoAddress = jusoAddress;
    }
}
