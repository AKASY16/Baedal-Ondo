package com.baedalondo.api.store.domain;

import com.baedalondo.api.commercialarea.dto.CommercialAreaMatch;
import com.baedalondo.api.user.domain.UserAccount;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 가게명

    @Enumerated(EnumType.STRING)
    private BusinessType businessType; // 업종

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

    private String roadName; // 도로명

    private String undergroundYn; // 지하여부, udrtYn
    private String buildingMainNumber; // 건물본번, buldMnnm
    private String buildingSubNumber; // 건물부번, buldSlno

    private Integer nx; // 기상청 API 격자 X좌표
    private Integer ny; // 기상청 API 격자 Y좌표

    // 서울시 상권 정보, 상권 밖 매장이 있을 수 있으므로 nullable 이다.
    private String commercialAreaCode; // 서울시 상권코드, TRDAR_CD
    private String commercialAreaName; // 상권명, TRDAR_CD_NM
    private String commercialAreaTypeCode; // 상권 구분 코드, TRDAR_SE_CD
    private String commercialAreaTypeName; // 상권 구분명, TRDAR_SE_CD_NM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    private LocalDateTime createdAt; // 생성 시간

    protected Store() {
    }

    public Store(String name,
                 BusinessType businessType,
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

    public BusinessType getBusinessType() {
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

    public String getCommercialAreaCode() {
        return commercialAreaCode;
    }

    public String getCommercialAreaName() {
        return commercialAreaName;
    }

    public String getCommercialAreaTypeCode() {
        return commercialAreaTypeCode;
    }

    public String getCommercialAreaTypeName() {
        return commercialAreaTypeName;
    }

    public UserAccount getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setUser(UserAccount user) {
        this.user = user;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setRoadAddress(String roadAddress) {
        this.roadAddress = roadAddress;
    }

    public void setJibunAddress(String jibunAddress) {
        this.jibunAddress = jibunAddress;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setSidoName(String sidoName) {
        this.sidoName = sidoName;
    }

    public void setSigunguName(String sigunguName) {
        this.sigunguName = sigunguName;
    }

    public void setDongName(String dongName) {
        this.dongName = dongName;
    }

    public void setAddressRegionCode(String addressRegionCode) {
        this.addressRegionCode = addressRegionCode;
    }

    public void setRoadNameCode(String roadNameCode) {
        this.roadNameCode = roadNameCode;
    }

    public void setBuildingManagementNumber(String buildingManagementNumber) {
        this.buildingManagementNumber = buildingManagementNumber;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public void setUndergroundYn(String undergroundYn) {
        this.undergroundYn = undergroundYn;
    }

    public void setBuildingMainNumber(String buildingMainNumber) {
        this.buildingMainNumber = buildingMainNumber;
    }

    public void setBuildingSubNumber(String buildingSubNumber) {
        this.buildingSubNumber = buildingSubNumber;
    }

    public void setNx(Integer nx) {
        this.nx = nx;
    }

    public void setNy(Integer ny) {
        this.ny = ny;
    }

    /**
     상권 판별 결과를 반영한다.
     상권을 찾지 못한 경우 match가 null이며, 이때 상권 정보는 null로 남는다.
     상권 판별 실패는 매장 등록 실패가 아니다.
     **/
    public void assignCommercialArea(CommercialAreaMatch match) {
        if (match == null) {
            this.commercialAreaCode = null;
            this.commercialAreaName = null;
            this.commercialAreaTypeCode = null;
            this.commercialAreaTypeName = null;
            return;
        }

        this.commercialAreaCode = match.getCommercialAreaCode();
        this.commercialAreaName = match.getCommercialAreaName();
        this.commercialAreaTypeCode = match.getCommercialAreaTypeCode();
        this.commercialAreaTypeName = match.getCommercialAreaTypeName();
    }
}

