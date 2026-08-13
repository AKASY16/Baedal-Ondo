package com.baedalondo.api.guest.domain;

/**
 * 게스트 대시보드에서 사용하는 고정 지역 정보.
 * 운영 DB 엔티티가 아니라 classpath CSV에서 읽는 불변 데이터다.
 */
public class GuestRegion {

    private final Long id;
    private final String displayName;
    private final String address;
    private final String roadAddress;
    private final String jibunAddress;
    private final String addressDetail;
    private final String postalCode;
    private final String sidoName;
    private final String sigunguName;
    private final String dongName;
    private final String addressRegionCode;
    private final String roadNameCode;
    private final String buildingManagementNumber;
    private final String roadName;
    private final String undergroundYn;
    private final String buildingMainNumber;
    private final String buildingSubNumber;
    private final Integer nx;
    private final Integer ny;

    public GuestRegion(Long id,
                       String displayName,
                       String address,
                       String roadAddress,
                       String jibunAddress,
                       String addressDetail,
                       String postalCode,
                       String sidoName,
                       String sigunguName,
                       String dongName,
                       String addressRegionCode,
                       String roadNameCode,
                       String buildingManagementNumber,
                       String roadName,
                       String undergroundYn,
                       String buildingMainNumber,
                       String buildingSubNumber,
                       Integer nx,
                       Integer ny) {
        this.id = id;
        this.displayName = displayName;
        this.address = address;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.addressDetail = addressDetail;
        this.postalCode = postalCode;
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.dongName = dongName;
        this.addressRegionCode = addressRegionCode;
        this.roadNameCode = roadNameCode;
        this.buildingManagementNumber = buildingManagementNumber;
        this.roadName = roadName;
        this.undergroundYn = undergroundYn;
        this.buildingMainNumber = buildingMainNumber;
        this.buildingSubNumber = buildingSubNumber;
        this.nx = nx;
        this.ny = ny;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
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
}
