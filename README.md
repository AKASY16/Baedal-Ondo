# 배달온도 BeadalOndo

배달온도는 매장 위치, 현재 날씨, 미세먼지, 시간대, 요일/공휴일 정보를 조합해 현재 배달 수요 가능성을 0-100점으로 보여주는 Spring Boot 기반 MVP 서비스입니다.

현재 버전은 서울 지역 소규모 매장 운영자가 "지금 배달 수요가 높아질 가능성이 있는지"를 빠르게 판단할 수 있도록 대시보드 화면을 제공합니다.

## 주요 기능

- 매장 등록
  - 도로명주소 검색 팝업 연동
  - 주소 기반 기상청 격자 좌표 `nx`, `ny` 계산
  - Store 정보를 H2 DB에 저장

- 대시보드
  - 현재 배달온도 점수 표시
  - 점수 상태와 운영 메시지 표시
  - 시간대, 요일/공휴일, 현재 날씨, 미세먼지 영향 요인 표시
  - Store ID 선택 드롭다운으로 특정 매장 대시보드 확인
  - 디버그 정보 토글 표시

- 현재 날씨 연동
  - 기상청 초단기실황 API 호출
  - `PTY`, `RN1`, `T1H`, `REH`, `WSD` 파싱
  - `nx`, `ny`, `baseDate`, `baseTime` 기준 DB 재사용
  - API 실패 시 날씨 보정 점수 제외

- 미세먼지 연동
  - AirKorea 시도별 실시간 측정정보 API 호출
  - PM10, PM2.5, O3 기반 공기질 보정 점수 계산
  - 측정소/측정시각 기준 DB 저장 및 재사용
  - API 실패 시 공기질 보정 점수 제외

- 공휴일 처리
  - 공공데이터포털 특일정보 API 기반 공휴일 여부 조회
  - 서버 시작 시 현재 연도 공휴일 갱신
  - 조회 날짜가 DB에 없으면 해당 월 공휴일을 API로 재수집

## 기술 스택

- Java 17
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Data JPA
- Thymeleaf
- H2 Database
- Gradle
- Lombok
- PROJ4J
- JUnit 5
- Mockito

## 프로젝트 구조

```text
src/main/java/com/beadalondo/api
├── airquality    # AirKorea API, 미세먼지 기록, 공기질 점수 계산
├── auth          # 로그인 화면 컨트롤러
├── dashboard     # 대시보드 화면, DashboardView 조립
├── holiday       # 공휴일 API, 공휴일 DB 저장/조회
├── location      # 주소 좌표 변환, 기상청 격자 변환
├── score         # 최종 배달온도 점수 조립, 시간/요일 계산기
├── store         # 매장 등록, Store 엔티티, StoreRepository
└── weather       # 기상청 현재 날씨 API, 날씨 기록, 날씨 점수 계산
```

## 실행 방법

### 1. 저장소 이동

```bash
cd backend/beadal-ondo-api
```

### 2. Secret 설정

`src/main/resources/application-secret.yaml` 파일을 생성하고 API 키를 설정합니다.

```yaml
kma:
  api:
    auth-key: "기상청_API_KEY"

dataportal:
  api:
    auth-key: "공공데이터포털_API_KEY"
    holiday-auth-key: "공휴일_API_KEY"

jusogokr:
  api:
    popup-auth-key: "도로명주소_팝업_API_KEY"

kasi:
  api:
    startup-refresh-enabled: true
```

`application.yaml`은 `application-secret.yaml`을 optional import 하도록 설정되어 있습니다.

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

Windows 환경에서는 다음 명령을 사용할 수 있습니다.

```bash
./gradlew.bat bootRun
```

### 4. 테스트 실행

```bash
./gradlew test
```

Windows 환경에서는 다음 명령을 사용할 수 있습니다.

```bash
./gradlew.bat test
```

## 주요 URL

| URL | 설명 |
| --- | --- |
| `/` | `/dashboard/main`으로 이동 |
| `/dashboard/main` | 대시보드 메인 화면 |
| `/dashboard/main/{storeId}` | 선택한 Store ID를 세션에 저장한 뒤 `/dashboard/main`으로 리다이렉트 |
| `/store/register` | 매장 등록 화면 |
| `/api/stores` | 매장 등록 API |
| `/login` | 로그인 화면 |
| `/h2-console` | H2 콘솔 |

## 매장 등록 API

```http
POST /api/stores
Content-Type: application/json
```

요청 예시:

```json
{
  "name": "온도분식",
  "businessType": "분식",
  "jusoAddress": {
    "roadFullAddr": "서울특별시 송파구 ...",
    "roadAddrPart1": "서울특별시 송파구 ...",
    "roadAddrPart2": "",
    "addrDetail": "101호",
    "jibunAddr": "서울특별시 송파구 ...",
    "zipNo": "00000",
    "siNm": "서울특별시",
    "sggNm": "송파구",
    "emdNm": "잠실동",
    "admCd": "1171010100",
    "rnMgtSn": "117103123001",
    "bdMgtSn": "1171010100100000000000001",
    "rn": "올림픽로",
    "udrtYn": "0",
    "buldMnnm": "300",
    "buldSlno": "0"
  }
}
```

응답:

```json
1
```

## 점수 계산 개요

최종 배달온도 점수는 기본 점수에 다음 보정 요소를 더한 뒤 0-100 범위로 제한합니다.

- 시간대 보정
- 요일/공휴일 보정
- 현재 날씨 보정
- 미세먼지 보정

외부 API 실패는 전체 대시보드를 중단시키지 않고 해당 요소만 제외하는 방식으로 처리합니다.

## 현재 개발 상태

구현 완료:

- Store 등록 및 DB 저장
- Store ID 기반 대시보드 확인
- 기상청 현재 날씨 API 연동
- 날씨 DB 캐시/재사용
- AirKorea 미세먼지 API 연동
- 미세먼지 DB 저장/재사용
- 공휴일 API 연동
- 배달온도 계산기 테스트
- 대시보드 UI 및 디버그 토글

추후 작업:

- 로그인 사용자 기반 Store 조회
- Store 목록 화면 정리
- 대시보드 운영 UI 정리
- ScoreHistory 저장
- 과거 유사 상황 비교
- 정기 수집 스케줄러
- README와 API 문서 지속 보강

## 개발 참고사항

- 현재 DB는 H2 파일 DB를 사용합니다.
- 기본 DB 경로는 `jdbc:h2:~/beadalondo`입니다.
- 앱 실행 중에는 H2 파일 DB가 잠길 수 있으므로 테스트 실행 전 실행 중인 서버나 H2 콘솔 연결을 종료해야 합니다.
- `/dashboard/main/{storeId}`는 URL을 유지하지 않고 선택 Store ID를 세션에 저장한 뒤 `/dashboard/main`으로 리다이렉트합니다.
- 로그인 기능이 구현되면 `/dashboard/main`은 로그인 사용자의 Store를 조회하는 정식 진입점이 됩니다.

