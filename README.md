# 배달온도 BeadalOndo

배달온도는 매장 위치, 현재 날씨, 미세먼지, 시간대, 요일/공휴일 정보를 조합해 현재 배달 수요 가능성을 0-100점으로 보여주는 Spring Boot 기반 MVP 서비스입니다.

현재 버전은 서울 지역 소규모 매장 운영자가 "지금 배달 수요가 높아질 가능성이 있는지"를 빠르게 판단할 수 있도록 가중치 기반 점수와 대시보드 화면을 제공합니다.

## 주요 기능

- 매장 등록
  - 도로명주소 검색 팝업 연동
  - 주소 기반 기상청 격자 좌표 `nx`, `ny` 계산
  - Store 정보를 H2 DB에 저장

- 대시보드
  - 현재 배달온도 점수 표시
  - 점수 상태와 운영 메시지 표시
  - 시간대, 요일/공휴일, 현재 날씨, 미세먼지 영향 요인 표시
  - 각 요인이 최종 점수에 기여한 방향을 화살표로 표시
  - 로그인 사용자의 매장 목록을 드롭다운으로 표시
  - Store ID 선택 드롭다운으로 특정 매장 대시보드 확인
  - 등록 매장이 없는 경우 게스트 지역 기반 대시보드 표시
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

- 인증/게스트 모드
  - Spring Security 기반 로그인/로그아웃
  - 로그인 사용자 소유 Store만 조회
  - 비로그인 사용자를 위한 게스트 모드 제공

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
├── auth          # 로그인 화면, 현재 사용자 조회, UserDetailsService
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
| `/` | 인증 상태에 따라 `/dashboard/main` 진입 |
| `/dashboard/main` | 로그인 사용자 기준 대시보드 메인 화면 |
| `/dashboard/main/{storeId}` | 선택한 Store ID를 세션에 저장한 뒤 `/dashboard/main`으로 리다이렉트 |
| `/guest` | 게스트 모드 진입 |
| `/dashboard/guest` | 게스트 지역 기반 대시보드 화면 |
| `/store/register` | 매장 등록 화면 |
| `/api/stores` | 매장 등록 API |
| `/api/guest-regions` | 게스트 지역 등록 API |
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

최종 배달온도 점수는 평균 수요를 의미하는 50점을 기준으로 두고, 시간대/요일/날씨/대기질/상호작용 요인을 가중치 기반으로 반영한 뒤 0-100 범위로 제한합니다.

```text
score = 50
      + 시간대 기여도
      + 요일/공휴일 기여도
      + 현재 날씨 기여도
      + 대기질 기여도
      + 상호작용 보너스
```

현재 적용 중인 기여도 범위:

| 요인 | 반영 방식 | 범위 |
| --- | --- | --- |
| 시간대 | 점심/저녁/야식/비활성 시간대 구간형 점수 | -18 ~ +24 |
| 요일/공휴일 | 금요일, 주말, 공휴일 보정 | 0 ~ +8 |
| 현재 날씨 | 강수량, 강수형태, 기온, 풍속 원점수를 정규화 | 0 ~ +20 |
| 대기질 | PM10, PM2.5, O3 원점수를 정규화 | 0 ~ +8 |
| 상호작용 | 피크 시간+주말/금요일, 비+피크 시간 등 조합 보너스 | 0 ~ +10 |

날씨와 대기질은 기존 계산기에서 만든 원점수를 그대로 사용하되, 최종 점수에서는 영향 범위를 제한해 특정 요인이 과도하게 점수를 끌어올리지 않도록 합니다.
또한 시간대 의미가 최종 점수를 압도당하지 않도록 시간대별 상한을 적용합니다.

| 시간대 | 최종 점수 상한 |
| --- | --- |
| 배달앱 비활성 시간대 | 39 |
| 낮은 수요 시간대 | 59 |
| 보통 수요 시간대 | 79 |
| 높은/피크 수요 시간대 | 100 |

상호작용 보너스는 최종 점수에는 반영하지만, 사용자 화면에는 별도 세부 항목으로 노출하지 않습니다. 화면에서는 시간대, 요일/공휴일, 날씨, 대기질이 점수에 영향을 준 방향만 간단히 표시합니다.

점수 구간:

| 점수 | 상태 |
| --- | --- |
| 0-19 | 매우 낮음 |
| 20-39 | 낮음 |
| 40-59 | 보통 |
| 60-79 | 높음 |
| 80-100 | 매우 높음 |

외부 API 실패는 전체 대시보드를 중단시키지 않고 해당 요소만 제외하는 방식으로 처리합니다.

## 현재 개발 상태

구현 완료:

- Store 등록 및 DB 저장
- Store ID 기반 대시보드 확인
- 로그인 사용자 기반 Store 조회
- 대시보드 내 Store 선택 드롭다운
- Spring Security 기반 로그인/로그아웃
- 게스트 모드 대시보드
- 기상청 현재 날씨 API 연동
- 날씨 DB 캐시/재사용
- AirKorea 미세먼지 API 연동
- 미세먼지 DB 저장/재사용
- 공휴일 API 연동
- 가중치 기반 배달온도 점수 계산
- 점수 영향 방향 표시
- 배달온도 계산기 테스트
- 대시보드 UI 및 디버그 토글

정리 필요:

- 코드 주석의 오래된 TODO 정리
- 개발용 페이지 및 H2 콘솔 접근 제한 정리
- 사용하지 않는 빈 LocationController/LocationService 정리
- CurrentWeatherService, KmaTimeCalculator 등 서비스 계층 테스트 보강

현재 제품 방향에서 제외/보류:

- 별도 Store 목록 페이지
  - 현재는 대시보드 드롭다운으로 매장 선택 흐름을 제공한다.
- ScoreHistory 저장 및 과거 유사 상황 비교
  - 현재 제품은 사장님이 빠르게 판단할 수 있는 현재 점수와 핵심 요인 제공에 집중한다.
  - 과거 데이터 상세 제공은 사용자에게 과도한 정보가 될 수 있어 MVP 범위에서 제외한다.
- 정기 수집 스케줄러
  - 현재는 대시보드 요청 시 DB 재사용 후, 없으면 외부 API를 호출해 저장한다.
  - 외부 API 최초 호출 지연이 문제가 될 때 성능 개선 작업으로 검토한다.

## 개발 참고사항

- 현재 DB는 H2 파일 DB를 사용합니다.
- 기본 DB 경로는 `jdbc:h2:~/beadalondo`입니다.
- 앱 실행 중에는 H2 파일 DB가 잠길 수 있으므로 테스트 실행 전 실행 중인 서버나 H2 콘솔 연결을 종료해야 합니다.
- `/dashboard/main/{storeId}`는 URL을 유지하지 않고 선택 Store ID를 세션에 저장한 뒤 `/dashboard/main`으로 리다이렉트합니다.
- `/dashboard/main`은 로그인 사용자의 Store를 조회하는 정식 진입점입니다.
- 등록된 Store가 없는 경우 게스트 지역 기반 대시보드로 대체 표시합니다.
