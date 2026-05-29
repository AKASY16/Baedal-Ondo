package com.beadalondo.api.store.dto;

import com.beadalondo.api.location.dto.JusoAddressRequest;

public class StoreRegisterRequest {

    private String name;
    private String businessType;
    private JusoAddressRequest jusoAddress;

    public String getName() {
        return name;
    }

    public String getBusinessType() {
        return businessType;
    }

    public JusoAddressRequest getJusoAddress() {
        return jusoAddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public void setJusoAddress(JusoAddressRequest jusoAddress) {
        this.jusoAddress = jusoAddress;
    }
}