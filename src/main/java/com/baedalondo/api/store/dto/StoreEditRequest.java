package com.baedalondo.api.store.dto;

import com.baedalondo.api.location.dto.JusoAddressRequest;
import com.baedalondo.api.store.domain.BusinessType;

public class StoreEditRequest {

    private String name;
    private BusinessType businessType;
    private JusoAddressRequest jusoAddress;

    public String getName() {
        return name;
    }

    public BusinessType getBusinessType() {
        return businessType;
    }

    public JusoAddressRequest getJusoAddress() {
        return jusoAddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }

    public void setJusoAddress(JusoAddressRequest jusoAddress) {
        this.jusoAddress = jusoAddress;
    }


}
