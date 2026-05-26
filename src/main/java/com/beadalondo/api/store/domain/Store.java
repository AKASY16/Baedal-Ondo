package com.beadalondo.api.store.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 가게명
    private String businessType; // 업종

    private String address; // 대표 표시 주소
    private String roadAddress; // 도로명주소
    private String jibunAddress; // 지번주소
    private String addressDetail; // 상세주소
    private String postalCode; // 우편번호

    private String sidoName; // 시도명, 예: 서울
    private String sigunguName; // 시군구명, 예: 송파구
    private String dongName; // 읍면동명, 예: 잠실동

    private String addressRegionCode; // 행안부 행정구역코드, admCd
    private String roadNameCode; // 행안부 도로명코드, rnMgtSn
    private String buildingManagementNumber; // 행안부 건물관리번호, bdMgtSn

    private String roadName; //

    private String undergroundYn; // 지하여부, udrtYn
    private String buildingMainNumber; // 건물본번, buldMnnm
    private String buildingSubNumber; // 건물부번, buldSlno

    private Integer nx; // 기상청 API 격자 X좌표
    private Integer ny; // 기상청 API 격자 Y좌표

    private LocalDateTime createdAt; // 생성 시간

    protected Store() {
    }

    public Store(String name,
                 String businessType,
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
        this.name = name;
        this.businessType = businessType;
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
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBusinessType() {
        return businessType;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}