# 배달온도 BaedalOndo

배달온도는 매장 위치, 현재 날씨, 미세먼지, 시간대, 요일/공휴일 정보를 조합해 현재 배달 수요 가능성을 0-100점으로 보여주는 Spring Boot 기반 MVP 서비스입니다.

현재 버전은 서울 지역 소규모 매장 운영자가 "지금 배달 수요가 높아질 가능성이 있는지"를 빠르게 판단할 수 있도록 가중치 기반 점수와 대시보드 화면을 제공합니다.

## 주요 기능

- 매장 등록
  - 도로명주소 검색 팝업 연동
  - 주소 기반 기상청 격자 좌표 `nx`, `ny` 계산
  - WGS84 좌표로 서울시 상권을 판별해 `commercialAreaCode` 저장
  - 업종을 `BusinessType` Enum 9종으로 표준화해 저장
  - Store 정보를 MySQL DB에 저장

- 상권 x 업종 요일·시간대 가중치
  - 서울시 상권분석서비스 추정매출(2023-2025) 기반 오프라인 전처리 결과 사용
  - 상권별 값이 있으면 상권별, 없으면 서울 전체 업종 평균으로 fallback
  - 자세한 계산 근거는 `data-processing/README.md` 참고

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
- Spring Security
- MySQL 8
- H2 Database (테스트 전용)
- Gradle
- Lombok
- PROJ4J
- JTS Topology Suite
- JUnit 5
- Mockito
- Python 3 (오프라인 데이터 전처리, 표준 라이브러리만 사용)

## 프로젝트 구조

```text
src/main/java/com/baedalondo/api
├── airquality      # AirKorea API, 미세먼지 기록, 공기질 점수 계산
├── auth            # 로그인 화면, 현재 사용자 조회, UserDetailsService
├── commercialarea  # 서울시 상권 GeoJSON 로딩, 좌표 기반 상권 판별
├── config          # Spring Security, 인터셉터, 비밀번호 인코더 설정
├── dashboard       # 대시보드 화면, DashboardView 조립
├── guest           # 고정 게스트 지역 CSV 로딩 및 조회
├── holiday         # 공휴일 API, 공휴일 DB 저장/조회
├── location        # 주소 좌표 변환, 기상청 격자 변환
├── score           # 최종 점수 조립, DayWeight/TimeWeight 조회
├── store           # 매장 등록, Store 엔티티, BusinessType
├── user            # UserAccount 엔티티, 사용자 조회
└── weather         # 기상청 현재 날씨 API, 날씨 기록, 날씨 점수 계산

data-processing/    # 서울시 추정매출 CSV -> DayWeight/TimeWeight 전처리 (Python)
```

## 실행 방법

### 1. 저장소 이동

```bash
cd backend/baedal-ondo-api
```

### 2. Secret 설정

`src/main/resources/application-secret.yaml` 파일을 생성하고 발급받은 API 키를 설정합니다. 이 파일은 `.gitignore`에 등록되어 있어 저장소에 올라가지 않습니다.

```yaml
kma:
  api:
    auth-key: "기상청_API_KEY"                       # 필수

dataportal:
  api:
    auth-key: "공공데이터포털_API_KEY"                  # 필수
    holiday-auth-key: "공휴일_API_KEY"               # 선택, 미설정 시 auth-key 사용

jusogokr:
  api:
    coordinate-auth-key: "도로명주소_좌표제공_API_KEY"    # 필수
    popup-auth-key: "도로명주소_팝업_API_KEY"           # 선택, 기본값 TESTJUSOGOKR

kasi:
  api:
    startup-refresh-enabled: true                   # 선택, 기본값 true
```

필수 키 3개가 없으면 애플리케이션이 시작되지 않습니다. 선택 항목은 기본값이 있어 생략할 수 있습니다.

`application.yaml`은 `application-secret.yaml`을 optional import 하도록 설정되어 있습니다.

### 3. 데이터베이스 설정

MySQL 8에 스키마와 계정을 생성합니다.

```sql
CREATE DATABASE baedalondo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'baedalondo_app'@'localhost' IDENTIFIED BY '비밀번호';
GRANT ALL PRIVILEGES ON baedalondo.* TO 'baedalondo_app'@'localhost';
FLUSH PRIVILEGES;
```

접속 정보는 환경변수로 주입합니다.

| 환경변수 | 기본값 | 필수 여부 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/baedalondo` | 선택 |
| `DB_USERNAME` | `baedalondo_app` | 선택 |
| `DB_PASSWORD` | 없음 | **필수** |

Windows에서는 사용자 환경변수로 등록합니다. 등록 후 터미널과 IDE를 재시작해야 반영됩니다.

```bash
setx DB_PASSWORD "비밀번호"
```

### 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

Windows 환경에서는 다음 명령을 사용할 수 있습니다.

```bash
./gradlew.bat bootRun
```

### 5. 테스트 실행

```bash
./gradlew test
```

Windows 환경에서는 다음 명령을 사용할 수 있습니다.

```bash
./gradlew.bat test
```

## 주요 URL

| URL | 설명 | 로그인 |
| --- | --- | --- |
| `/` | `/dashboard/main`으로 리다이렉트 | 필요 |
| `/dashboard/main` | 로그인 사용자 기준 대시보드 메인 화면 | 필요 |
| `/dashboard/main/{storeId}` | 선택한 Store ID를 세션에 저장한 뒤 `/dashboard/main`으로 리다이렉트 | 필요 |
| `/store/register` | 매장 등록 화면 | 필요 |
| `/store/{storeId}/edit` | 매장 정보 수정 화면 | 필요 |
| `/api/stores` | 매장 등록 API | 필요 |
| `/api/stores/{storeId}` | 매장 정보 수정 API (PUT) | 필요 |
| `/guest` | 게스트 모드 진입 | 불필요 |
| `/dashboard/guest` | 게스트 지역 기반 대시보드 화면 | 불필요 |
| `/login` | 로그인 화면 | 불필요 |
| `/signup` | 회원가입 화면 | 불필요 |

로그인이 필요한 URL에 비로그인 상태로 접근하면 `/login`으로 리다이렉트됩니다.

## 매장 등록 API

```http
POST /api/stores
Content-Type: application/json
```

요청 예시:

```json
{
  "name": "온도분식",
  "businessType": "BUNSIK",
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

`businessType`은 아래 9개 값만 허용합니다. 그 외 문자열은 요청 단계에서 거부됩니다.

`KOREAN_FOOD` `CHINESE_FOOD` `JAPANESE_FOOD` `WESTERN_FOOD` `CHICKEN` `FAST_FOOD` `BUNSIK` `CAFE_BEVERAGE` `BAKERY`

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
| 시간대 | 상권 x 업종 x 시간대 TimeWeight | -12 ~ +14 |
| 요일 | 상권 x 업종 x 요일 DayWeight (공휴일은 고정 +8) | -6 ~ +8 |
| 현재 날씨 | 강수량, 강수형태, 기온, 풍속 원점수를 정규화 | 0 ~ +20 |
| 대기질 | PM10, PM2.5, O3 원점수를 정규화 | 0 ~ +8 |
| 상호작용 | 피크 시간+강한 요일, 비+피크 시간, 공휴일 조합 보너스 | 0 ~ +10 |

### 시간대 기여도

시간대는 서울시 원본의 6개 시간 구간별 매출건수를 구간 길이로 나눈 뒤,
같은 상권·업종의 24시간 평균 시간당 활동량을 100으로 둔 `TimeIndex`로 정규화합니다.

```text
commercialAreaCode + businessType + 시간대  ->  Local TimeWeight
없으면  businessType + 시간대             ->  City TimeWeight
업종이 없는 게스트                         ->  기존 공통 시간표
```

시간 구간은 `00~06`, `06~11`, `11~14`, `14~17`, `17~21`, `21~24`입니다.
원본이 배달 전용 데이터가 아니므로 정확한 주문량 예측값이 아니라 업종별 시간대 상업활동을
배달 잠재 수요의 보조 지표로 사용합니다.

### 요일 기여도

요일 점수는 요일 이름이 아니라 **그 상권 그 업종의 실제 요일별 수요 패턴**으로 결정합니다. 같은 일요일이라도 오피스 상권 카페는 -6, 주거지 카페는 +6이 될 수 있습니다.

```text
commercialAreaCode + businessType + 요일  ->  Local DayWeight
없으면  businessType + 요일             ->  City DayWeight
그래도 없으면                            ->  0
```

공휴일에는 전처리 데이터가 공휴일 효과를 분리하지 못하므로 DayWeight 대신 고정 +8을 사용합니다.

상호작용 보너스도 요일 이름을 조건으로 쓰지 않습니다. 피크 시간대에 DayWeight가 양수일 때만 `ceil(DayWeight / 2)`를 최대 +3까지 더합니다. 수요가 약한 요일을 상호작용이 뒤집지 않도록 음수에는 적용하지 않으며, 공휴일에도 적용하지 않습니다.

날씨와 대기질은 기존 계산기에서 만든 원점수를 그대로 사용하되, 최종 점수에서는 영향 범위를 제한해 특정 요인이 과도하게 점수를 끌어올리지 않도록 합니다.

시간대 기여도는 `CLOSED -12`, `LOW -6`, `MEDIUM 0`, `HIGH +8`,
`VERY_HIGH +14`를 적용합니다. TimeWeight가 배달 전용 주문량이 아닌 상업활동 보조지표라는
점을 고려해 영향 폭을 제한했으며, 시간대 등급만으로 최종 결과를 강제하지 않도록
시간대별 최종 점수 상한은 적용하지 않습니다.

상호작용 보너스는 최종 점수에는 반영하지만, 사용자 화면에는 별도 세부 항목으로 노출하지 않습니다. 화면에서는 시간대, 요일/공휴일, 날씨, 대기질이 점수에 영향을 준 방향만 간단히 표시합니다.

점수 구간:

| 점수 | 화면에 표시되는 상태 |
| --- | --- |
| 0-19 | 마감 · 매우 낮은 수요 구간 |
| 20-39 | 하 · 수요 둔화 구간 |
| 40-59 | 중 · 평균 수요 구간 |
| 60-79 | 상 · 높은 수요 구간 |
| 80-100 | 상 · 수요 급등 구간 |

외부 API 실패는 전체 대시보드를 중단시키지 않고 해당 요소만 제외하는 방식으로 처리합니다.

## 현재 개발 상태

구현 완료:

- Store 등록 및 DB 저장
- Store ID 기반 대시보드 확인
- 로그인 사용자 기반 Store 조회
- 대시보드 내 Store 선택 드롭다운
- Spring Security 기반 로그인/로그아웃
- 회원가입 및 아이디 중복 검사
- Store 정보 수정
- 서울 25개 자치구청 CSV 기반 게스트 모드 대시보드
- 기상청 현재 날씨 API 연동
- 날씨 DB 캐시/재사용
- AirKorea 미세먼지 API 연동
- 미세먼지 DB 저장/재사용
- 공휴일 API 연동
- 가중치 기반 배달온도 점수 계산
- 점수 영향 방향 표시
- 배달온도 계산기 테스트
- 대시보드 UI 및 디버그 토글
- GeoJSON + JTS 기반 서울시 상권 판별 및 저장
- businessType의 BusinessType Enum 표준화
- 서울시 추정매출 기반 상권 x 업종 x 요일 DayWeight 전처리
- DayWeight 런타임 조회 계층 및 Local -> City -> 0 fallback
- 요일 heuristic을 데이터 기반 DayWeight로 대체
- 서울시 추정매출 기반 상권 x 업종 x 시간대 TimeWeight 전처리
- TimeWeight 런타임 조회 계층 및 Local -> City -> 기존 시간표 fallback
- 업종 공통 시간표를 데이터 기반 TimeWeight로 대체

정리 필요:

- 코드 주석의 오래된 TODO 정리
- 어디에서도 호출하지 않는 DayDemandLevel.getWeight 정리
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

- 현재 DB는 MySQL 8을 사용하며 접속 정보는 환경변수로 주입합니다.
- 스키마는 아직 `ddl-auto: update`로 관리하므로 엔티티 필드의 이름이나 타입을 바꾸면 실제 스키마와 조용히 어긋날 수 있습니다. 구조를 바꾸기 전에 `mysqldump`로 백업하는 것을 권장합니다.
- `@DataJpaTest`는 인메모리 H2로 자동 대체되지만 `@SpringBootTest`는 위 MySQL에 그대로 접속합니다.
- `/dashboard/main/{storeId}`는 URL을 유지하지 않고 선택 Store ID를 세션에 저장한 뒤 `/dashboard/main`으로 리다이렉트합니다.
- `/dashboard/main`은 로그인 사용자의 Store를 조회하는 정식 진입점입니다.
- 게스트 지역은 `src/main/resources/guest-regions.csv`에 고정된 서울 25개 자치구청 정보를 사용하며 DB에 저장하지 않습니다.
- 등록된 Store가 없는 로그인 사용자도 같은 게스트 지역 CSV에서 무작위 지역을 골라 대시보드를 대체 표시합니다.

## 트러블슈팅

### 환경변수 미설정이 비밀번호 오류로 나타나는 경우

**증상**

터미널에서 `./gradlew test`를 실행하면 다음 오류로 컨텍스트 로드가 실패합니다. IDE에서 앱을 실행할 때는 정상 동작합니다.

```text
java.sql.SQLException: Access denied for user 'baedalondo_app'@'localhost' (using password: YES)
```

**원인**

`DB_PASSWORD` 환경변수가 없어서 발생하지만, 오류 메시지는 비밀번호가 틀린 것처럼 보입니다. `application.yaml`의 `password: "${DB_PASSWORD}"`에는 기본값이 없는데, Spring Boot는 `@ConfigurationProperties` 바인딩 과정에서 해결하지 못한 플레이스홀더를 예외로 던지지 않고 문자열 그대로 남깁니다. 그 결과 `${DB_PASSWORD}`라는 문자열이 비밀번호로 전송되어 MySQL이 인증을 거부합니다.

IDE 실행 구성에만 환경변수를 등록한 경우 IDE에서만 성공하고 터미널에서는 실패하므로 원인을 찾기 어렵습니다.

**해결**

시스템 사용자 환경변수로 등록한 뒤 터미널과 IDE를 재시작합니다. IDE 실행 구성에 중복 등록하면 비밀번호 변경 시 한쪽만 고치게 되므로 한 곳에서만 관리합니다.

### 점수 구간 색상이 항상 회색으로 표시되는 경우

**증상**

점수와 상관없이 대시보드 온도 패널이 모두 마감 상태 색상으로 표시됩니다. 테스트는 전부 통과합니다.

**원인**

템플릿이 화면에 표시할 상태 문구를 파싱해 CSS 클래스를 결정하고 있었습니다.

```html
<!-- 수정 전 -->
th:classappend="${#strings.startsWith(dashboard.status, '상') ? ' status-high' : ...}"
```

상태 문구를 `상 · 높은 수요 구간`에서 `높음 · 높은 수요 구간`으로 바꾸자 어떤 조건에도 걸리지 않아 모든 점수가 기본값인 마감 상태로 떨어졌습니다. 표시용 문자열에 로직이 결합되어 있었기 때문에 컴파일 오류도 테스트 실패도 발생하지 않았습니다.

**해결**

표시 문구와 분리된 `ScoreStatusLevel` enum을 두고 구간 경계를 이 enum이 단일 기준으로 관리하도록 했습니다. 템플릿은 문구 대신 `dashboard.statusLevel.cssClass`를 사용합니다. 사용자에게 보여줄 문구는 언제든 바뀔 수 있으므로 화면 로직의 판단 근거로 삼지 않습니다.

### 주소 검색 팝업과 CSRF 설정

`SecurityConfig`의 CSRF 예외 목록에 있는 `/store/register`, `/store/*/edit`는 삭제하면 주소 검색 기능이 동작하지 않습니다.

도로명주소 팝업은 외부 도메인인 `business.juso.go.kr`에서 선택 결과를 애플리케이션 화면으로 POST 전송합니다. 이 요청에는 CSRF 토큰이 포함될 수 없으므로 예외 처리가 필요합니다. 두 경로는 화면을 렌더링하기만 하고 데이터를 변경하지 않습니다.

실제로 데이터를 변경하는 `/api/stores`는 CSRF 보호를 유지하며, 화면에서 `<meta name="_csrf">` 값을 읽어 요청 헤더에 담아 전송합니다.
