package com.beadalondo.api.guest.dto;

import com.beadalondo.api.guest.domain.GuestRegion;

import java.time.LocalDateTime;

public class GuestRegionResponse {

    private Long id;
    private String address;
    private String roadAddress;
    private String jibunAddress;
    private String addressDetail;
    private String postalCode;
    private String sidoName;
    private String sigunguName;
    private String dongName;
    private String addressRegionCode;
    private String roadNameCode;
    private String buildingManagementNumber;
    private String roadName;
    private String undergroundYn;
    private String buildingMainNumber;
    private String buildingSubNumber;
    private Integer nx;
    private Integer ny;
    private Boolean active;
    private LocalDateTime createdAt;

    public static GuestRegionResponse from(GuestRegion guestRegion) {
        GuestRegionResponse response = new GuestRegionResponse();
        response.id = guestRegion.getId();
        response.address = guestRegion.getAddress();
        response.roadAddress = guestRegion.getRoadAddress();
        response.jibunAddress = guestRegion.getJibunAddress();
        response.addressDetail = guestRegion.getAddressDetail();
        response.postalCode = guestRegion.getPostalCode();
        response.sidoName = guestRegion.getSidoName();
        response.sigunguName = guestRegion.getSigunguName();
        response.dongName = guestRegion.getDongName();
        response.addressRegionCode = guestRegion.getAddressRegionCode();
        response.roadNameCode = guestRegion.getRoadNameCode();
        response.buildingManagementNumber = guestRegion.getBuildingManagementNumber();
        response.roadName = guestRegion.getRoadName();
        response.undergroundYn = guestRegion.getUndergroundYn();
        response.buildingMainNumber = guestRegion.getBuildingMainNumber();
        response.buildingSubNumber = guestRegion.getBuildingSubNumber();
        response.nx = guestRegion.getNx();
        response.ny = guestRegion.getNy();
        response.active = guestRegion.getActive();
        response.createdAt = guestRegion.getCreatedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public String getJibunAddress() {
        return jibunAddress;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getSidoName() {
        return sidoName;
    }

    public String getSigunguName() {
        return sigunguName;
    }

    public String getDongName() {
        return dongName;
    }

    public String getAddressRegionCode() {
        return addressRegionCode;
    }

    public String getRoadNameCode() {
        return roadNameCode;
    }

    public String getBuildingManagementNumber() {
        return buildingManagementNumber;
    }

    public String getRoadName() {
        return roadName;
    }

    public String getUndergroundYn() {
        return undergroundYn;
    }

    public String getBuildingMainNumber() {
        return buildingMainNumber;
    }

    public String getBuildingSubNumber() {
        return buildingSubNumber;
    }

    public Integer getNx() {
        return nx;
    }

    public Integer getNy() {
        return ny;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
