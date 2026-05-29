package com.beadalondo.api.location.dto;

public class JusoAddressRequest {

    private String roadFullAddr; // 전체 도로명
    private String roadAddrPart1; // 도로명주소(참고항목 제외)
    private String roadAddrPart2; // 도로명주소 참고 항목
    private String addrDetail; // 고객 입력 상세 주소
    private String jibunAddr; // 지번주소
    private String zipNo; // 우편번호

    private String siNm; // 시도명
    private String sggNm; // 시군구명
    private String emdNm; // 읍면동명

    private String admCd; // 행정구역코드
    private String rnMgtSn; // 도로명코드
    private String bdMgtSn; // 건물관리번호

    private String rn; // 도로명
    private String udrtYn; // 지하여부 (0:지상,1:지하)
    private String buldMnnm; // 건물 본번
    private String buldSlno; // 건물 부번

    public String getRoadFullAddr() {
        return roadFullAddr;
    }

    public void setRoadFullAddr(String roadFullAddr) {
        this.roadFullAddr = roadFullAddr;
    }

    public String getRoadAddrPart1() {
        return roadAddrPart1;
    }

    public void setRoadAddrPart1(String roadAddrPart1) {
        this.roadAddrPart1 = roadAddrPart1;
    }

    public String getRoadAddrPart2() {
        return roadAddrPart2;
    }

    public void setRoadAddrPart2(String roadAddrPart2) {
        this.roadAddrPart2 = roadAddrPart2;
    }

    public String getAddrDetail() {
        return addrDetail;
    }

    public void setAddrDetail(String addrDetail) {
        this.addrDetail = addrDetail;
    }

    public String getJibunAddr() {
        return jibunAddr;
    }

    public void setJibunAddr(String jibunAddr) {
        this.jibunAddr = jibunAddr;
    }

    public String getZipNo() {
        return zipNo;
    }

    public void setZipNo(String zipNo) {
        this.zipNo = zipNo;
    }

    public String getSiNm() {
        return siNm;
    }

    public void setSiNm(String siNm) {
        this.siNm = siNm;
    }

    public String getSggNm() {
        return sggNm;
    }

    public void setSggNm(String sggNm) {
        this.sggNm = sggNm;
    }

    public String getEmdNm() {
        return emdNm;
    }

    public void setEmdNm(String emdNm) {
        this.emdNm = emdNm;
    }

    public String getAdmCd() {
        return admCd;
    }

    public void setAdmCd(String admCd) {
        this.admCd = admCd;
    }

    public String getRnMgtSn() {
        return rnMgtSn;
    }

    public void setRnMgtSn(String rnMgtSn) {
        this.rnMgtSn = rnMgtSn;
    }

    public String getBdMgtSn() {
        return bdMgtSn;
    }

    public void setBdMgtSn(String bdMgtSn) {
        this.bdMgtSn = bdMgtSn;
    }

    public String getRn() {
        return rn;
    }

    public void setRn(String rn) {
        this.rn = rn;
    }

    public String getUdrtYn() {
        return udrtYn;
    }

    public void setUdrtYn(String udrtYn) {
        this.udrtYn = udrtYn;
    }

    public String getBuldMnnm() {
        return buldMnnm;
    }

    public void setBuldMnnm(String buldMnnm) {
        this.buldMnnm = buldMnnm;
    }

    public String getBuldSlno() {
        return buldSlno;
    }

    public void setBuldSlno(String buldSlno) {
        this.buldSlno = buldSlno;
    }
}
