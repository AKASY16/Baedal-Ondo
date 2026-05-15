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

    private String address; // 주소
    private Double latitude; // 위도
    private Double longitude; // 경도

    private String district; // 자치구
    private String dongCode; // 행정동 코드

    private LocalDateTime createdAt; //생성 시간

    protected Store() {
    }

    public Store(String name, String businessType, String address,
                 Double latitude, Double longitude,
                 String district, String dongCode) {
        this.name = name;
        this.businessType = businessType;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.district = district;
        this.dongCode = dongCode;
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

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getDistrict() {
        return district;
    }

    public String getDongCode() {
        return dongCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}