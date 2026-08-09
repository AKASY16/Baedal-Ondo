package com.baedalondo.api.guest.dto;

import com.baedalondo.api.location.dto.JusoAddressRequest;

public class GuestRegionRegisterRequest {

    private JusoAddressRequest jusoAddress;

    public JusoAddressRequest getJusoAddress() {
        return jusoAddress;
    }

    public void setJusoAddress(JusoAddressRequest jusoAddress) {
        this.jusoAddress = jusoAddress;
    }
}
