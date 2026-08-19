# 배달온도 개선 작업 목록

코드 검증을 거쳐 정리한 목록입니다. 각 항목의 `근거`는 실제 코드에서 확인한 사실입니다.

방향: **기능을 더 넣기보다, 지금 있는 데이터와 점수가 정확히 무엇을 뜻하는지 보장하는 것**을 우선합니다.

---

## 완료

- [x] **외부 API 타임아웃** — `RestClientConfig`에서 connect 2초 / read 4초를 4개 클라이언트에 공통 적용
- [x] **오류 페이지** — `error/404.html`, `error/500.html`, `error.html` + `SecurityConfig`에 `/error` 허용
- [x] **헬스체크** — `/actuator/health`만 노출, `show-details: never`
- [x] **계측 로그 제거** — `logTiming` 22곳 삭제
- [x] **DB 백업 스크립트** — `scripts/backup-db.sh`
- [x] **테스트 재현성** — `src/test/resources/application.yaml`. 인메모리 H2 + 더미 키로 개인 설정 없이 `./gradlew test` 통과
- [x] **GitHub Actions CI** — push·PR마다 테스트 실행
- [x] **API 키 환경변수화** — `application-secret.yaml`이 jar에 포장되어 이미지까지 따라가던 문제.
  키를 전부 환경변수로 옮기고 리소스에서 파일을 제거
- [x] **시간대 일원화** — `common/ServiceTime` 신설. 시간대 미지정 `.now()` 12곳(main 9 + test 3)을 제거하고,
  각자 정의하던 `Asia/Seoul` 5곳을 한 곳으로 통합. 테스트와 운영 코드가 같은 기준 시각을 쓰므로
  JVM 기본 시간대와 무관하게 동일한 결과가 나온다

---

## 1단계 · 명백한 동작 결함

- [ ] **공휴일 조회 구조 수정** ⚠️ 최우선
  근거: `HolidayService.isHoliday()`가 `findByDate().orElseGet(refreshMonthAndCheck)` 구조.
  공휴일만 저장되므로 **비공휴일에는 항상 miss** → 대시보드 요청마다
  `DELETE 월 전체 → 외부 API 호출 → INSERT`가 실행됨. 읽기 요청이 쓰기 트랜잭션과 외부 I/O를 유발
  방향: "공휴일 아님"과 "아직 동기화 안 됨"을 구분(`holiday_sync` 등).
  request path에서 외부 API 제거, fetch와 DB 트랜잭션 분리
  같이: `refreshHolidaysForYear`가 트랜잭션 안에서 API를 12회 호출하는 것도 분리

- [ ] **`StoreFactory.editStore`의 sidoName 정규화** + editStore 테스트
  근거: `StoreFactory:47`은 `extractSidoName(...)`, `StoreFactory:82`는 `newAddress.getSiNm()` 원본 그대로.
  같은 필드에 `서울`과 `서울특별시`가 공존 가능
  성격: 즉시 장애는 아님(`CurrentAirQualityService`가 사용 시 다시 정규화). Store 불변조건 불일치 문제

---

## 2단계 · 점수 척도 (제품 핵심)

- [x] **점수 분포 시뮬레이션** — 조사만, 가중치 변경 없음
  도구: `data-processing/simulate_score_distribution.py` (자바 계산 로직 이식, 표준 라이브러리만)
  산출물: `output/score-distribution-report.txt`, `output/score-distribution.csv`(194,418행)
  표본: 상권×업종×요일×시간대 격자를 시간 길이로 가중 → 777,672 매장·시간

  확인된 것
  - 이론 최소 **32**, 최대 **110**(100으로 clamp). 게스트 하한 **38**
  - `CLOSED`(0~19) 도달 불가능 — 모든 날씨×대기질×공휴일 조합에서 0건
  - **맑은 날 상한 73** → `VERY_HIGH`(80~)도 악천후에만 나옴. 상·하위 두 구간이 모두 죽어 있었음
  - 실제 수요 배수(audit `time_index`/`day_index`): 시간대 **6~14배**, 요일 **1.75배**,
    날씨 **1.2~1.35배**(문헌값). 점수 폭은 시간 26 / 요일 12 / 대기질 8 / 날씨 4(흔한 범위)
    → 평상시 배분은 실제 영향 순서와 일치. **날씨 과대는 태풍·폭설 꼬리 구간에만** 발생

- [x] **분포 결과에 따른 재보정** — status threshold만 조정
  `ScoreStatusLevel` 경계 `80/60/40/20` → **`64/56/42/37`**
  기준: 맑은 날 분포. 악천후는 수요를 실제로 올리므로 위 구간으로 밀어올리는 게 맞고 기준선은 평범한 날이어야 함
  맑은 날 점유율 `0 / 22.4 / 57.0 / 20.6 / 0` → **`12.6 / 18.7 / 41.0 / 19.4 / 8.4`**
  `ScoreMessageFactory.createMessage()`가 경계를 따로 들고 있어 `ScoreStatusLevel`에 위임하도록 정리
  `BASE_SCORE`·컴포넌트 범위는 건드리지 않음

- [ ] **게스트 하한 38 처리 결정**
  경계가 37이라 게스트는 `CLOSED`를 사실상 못 봄. 게스트 화면에 이 구간이 필요한지 판단 필요

- [ ] **상단 clamp 정보 손실**
  공휴일 최대가 clamp 전 110. 도달하려면 태풍급이 필요해 실질 영향은 작으나 남아 있음

---

## 3단계 · 데이터 정직성

- [ ] **"주문" 표현 수정**
  근거: 전처리 문서는 `추정매출`·`매출건수`라고 정확히 기술. 화면은 `ScoreMessageFactory`에서
  "점심 주문 흐름이 특히 강한 시간대"처럼 **주문**으로 표현. 매출건수 ≠ 배달 주문건수
  방향: 원본 데이터는 "상업활동 패턴" / "매출활동 패턴"으로.
  최종 지표명 "배달온도", "배달 수요 가능성"은 유지해도 됨

- [ ] **공휴일 설명 문구 분리**
  근거: 공휴일이면 `day="공휴일"`, `appliedDayScore=8`(양수) →
  "이 지역의 치킨 업종은 공휴일 흐름이 비교적 활발한 편". 그러나 `+8`은 `HOLIDAY_SCORE` 상수이지
  해당 상권·업종 통계가 아님
  방향: 공휴일에는 별도 문구, 비공휴일에만 Local 데이터 설명

---

## 4단계 · 데이터 무결성

- [ ] **주소 payload 서버 검증**
  근거: 표시 주소(`roadFullAddr`, `siNm`, `sggNm`)는 클라이언트 값을 그대로 저장하고,
  좌표는 `admCd`/`rnMgtSn`/건물번호로 계산. API를 직접 호출하면 둘이 다른 지역을 가리킬 수 있음
  → 화면 주소는 강남, 기상청 좌표·상권은 종로가 되는 상태가 가능
  방향: ① 좌표 API 결과와 클라이언트 `siNm`/`sggNm` 일치 검증(경량) → ② 전체 canonicalization
  ⚠️ 이 목록에서 비용이 가장 큼. 등록·수정 흐름과 프런트 계약까지 바뀜

---

## 5단계 · API 계약·견고성

- [ ] **`JusoCoordinateClient` 응답 검증** (비용 작음, 앞당겨도 좋음)
  근거: `entX`/`entY`를 `asDouble()`로 읽어 필드 누락 시 `0.0`이 정상 좌표처럼 전달됨.
  errorCode/resultCode 검증도 주소 검색 클라이언트보다 얕음

- [ ] **Bean Validation 도입**
  근거: `validateRegisterRequest`/`validateEditRequest`가 거의 같은 검증을 중복. Signup은 이미 잘 사용 중
  구분: DTO shape → Bean Validation / 업무 규칙·소유권 → Service

- [ ] **`@RestControllerAdvice` 에러 계약**
  근거: 프런트가 `response.text()`를 사용자에게 그대로 노출
  예: `400 INVALID_STORE_REQUEST`, `404 STORE_NOT_FOUND`, `502 ADDRESS_API_FAILED`

- [ ] **외부 I/O와 DB 트랜잭션 경계 분리**
  근거: `StoreService`가 클래스 레벨 `@Transactional`. 특히 `editStore`는 Store 조회 후
  트랜잭션을 연 채로 외부 주소 API를 호출

- [ ] **Weather 캐시 최초 요청 race**
  근거: SELECT miss → API → INSERT 구조에 `uk_current_weather_record_location_time` 유니크 제약.
  동시 최초 요청에서 제약 위반 가능
  방향: 충돌 시 재조회해 기존 레코드 사용

- [ ] **AirQuality 캐시 단위 재검토**
  방향: "자치구의 fallback 결과"가 아니라 "시도의 해당 기준시간 원본 응답"을 캐시
  (시도 평균 결과를 자치구 캐시로 90분 저장하면 측정소 정상화 후에도 평균이 실제 값을 가림)
  추가: 재사용 조건 `measuredAt + 90분`과 `getSafeAirQualityBaseTime()`이 정확히 일치하지 않음.
  의도된 여유면 문서화, 아니면 baseTime 기준으로 통일

---

## 배포 후 1순위 · 외부 데이터 사전 적재 스케줄러

서버가 24시간 켜져 있는 환경이 전제라 배포 이후에 의미가 생긴다.
`@EnableScheduling`도 아직 없으므로 활성화부터 필요하다.

- [ ] **날씨·대기질 사전 적재 스케줄러**
  현재: 캐시가 비면 **사용자 요청이 외부 API 왕복을 그대로 기다린다**.
  `CurrentWeatherService`에 개선 방향이 주석으로 남아 있다
  방향: 기준 시각이 바뀐 뒤 스케줄러가 미리 채워 두고, 사용자 요청은 캐시만 읽는다
  - 대기질: **시도 1회 호출로 서울 25개 자치구 전부** 채워진다. 이득이 가장 크다
  - 날씨: 격자 단위 호출. 게스트 지역 25개 자치구가 **격자로는 16개**이고
    여기에 등록 매장 격자가 더해진다
  - 실행 시각은 기존 로직 재사용: `KmaTimeCalculator.getSafeBaseDateTime()`,
    `AirQualityCalculator.getSafeAirQualityBaseTime()`
  ⚠️ **기존 lazy 경로는 남긴다.** 스케줄러 직후 등록된 매장, 스케줄러 실행 실패,
  서울 외 지역을 위한 fallback이 필요하다
  ⚠️ 단일 인스턴스 전제. 인스턴스를 2대로 늘리면 중복 실행된다

- [ ] **캐시 테이블 보존 기간 정리** (위 항목과 세트)
  근거: `current_weather_record`, `current_air_quality_record`에 삭제 로직이 전혀 없다.
  대기질은 **한 번 호출에 25행**이 쌓인다
  스케줄러를 넣으면 아무도 안 봐도 매시간 쌓이므로 증가 속도가 붙는다
  - 대기질 시간당 25행 → 연 약 219,000행
  - 날씨 시간당 약 20행 → 연 약 175,000행
  방향: 보존 기간(예: 7일)을 넘긴 행을 같은 스케줄러에서 정리

이 작업이 완결되면 **문제 인지 → 계측 → 원인 분석 → 구조적 해결 → 개선 수치**라는
서사가 완성된다. 계측 로그로 지연을 확인한 이력이 이미 있으므로 앞부분은 확보돼 있다.

---

## 6단계 · 스키마·테스트

- [ ] **DB NOT NULL 정합** (Flyway V2)
  근거: 앱에서는 사실상 필수인 `name`, `business_type`, `user_id`, `nx`, `ny`, `created_at`이 V1에서 nullable
  순서: 기존 데이터 검사 → backfill/삭제 → V2 마이그레이션 → 엔티티 nullable 일치 확인

- [ ] **Store edit 테스트 보강**
  ① name만 변경 → 주소 API 미호출 ② businessType만 변경 → 미호출
  ③ addrDetail만 변경 → 좌표·상권 재계산 없음 ④ 주소 변경 → 재계산
  ⑤ 상권 밖으로 이동 → 기존 상권 정보 null 정리 ⑥ sidoName 정규화 유지
  ⑦ 타인 Store 수정 거부 ⑧ 변화 없음 → 외부 API·save 불필요

---

## 7단계 · 마감

- [ ] **시각 고정 가능한 구조 (선택)**
  시간대 문제는 `ServiceTime`으로 해소됨. 남은 것은 **테스트에서 시각을 고정하지 못한다**는 점 하나다.
  `ServiceTime`이 정적 메서드라 `Clock.fixed(...)`로 대체할 수 없어서 이런 검증이 불가능하다.
  - 자정 경계 동작 (23:59 → 00:00)
  - 특정 요일·공휴일을 가정한 점수 계산
  - 기상청 기준시각 전환 경계
  방향: `Clock` 빈 주입 또는 `ServiceTime`을 인터페이스로 분리.
  단 엔티티(`@PrePersist`)는 빈을 주입받을 수 없어 정적 경로가 일부 남는다.
  ⚠️ 2단계 점수 시뮬레이션에서 특정 시각을 가정해야 한다면 그때 필요해진다. 그 전까지는 선택 사항

- [ ] **SQL 로깅 프로필 분리**
  근거: `show-sql: true`, `org.hibernate.SQL: debug`가 운영에도 그대로 적용됨
  ⚠️ 배포 전 필수

- [ ] **REH(습도) 정책 결정**
  근거: `KmaCurrentWeatherClient`가 `hasReh`를 필수로 요구하나 점수 계산에는 미사용.
  저장 자체는 향후 확장을 위해 정당하나, 누락 시 날씨 전체를 실패 처리할 필요가 있는지 결정

- [ ] **죽은 코드 정리**
  - `AddressCoordinateResolver.addressCoordinateResolver()` — **호출부 0건**
  - `CurrentAirQualityRecord` — 주석 처리된 줄 17개
  - Lombok — 실사용이 `CustomUserDetailsService` 1개 파일뿐. 유지 여부 결정
  - 미사용 import 정리
  - ⚠️ **`CurrentWeatherWeightCalculator`는 호출부가 0건이지만 지우지 않는다.**
    현재 점수가 예보 기준으로 바뀌면서 빠졌다. v2에서 예보 오차를 점수 단위로 재려면
    같은 관측에 실황·예보 두 계산기를 돌려 비교해야 하므로 그때 필요하다.
    실황 수집(`ScoreService.collectCurrentWeather`)도 같은 이유로 유지한다

---

## 별도 트랙 · 배포

위 목록과 병행합니다. AWS 계정이 필요한 시점부터 갈라집니다.

- [x] **`Dockerfile`** — 단일 스테이지. 멀티스테이지는 쓰지 않는다.
  CI가 Gradle 캐시로 빌드하는 편이 빠르고, `Dockerfile`은 결과물만 담는다
- [x] **`.dockerignore`** — 모두 제외 후 실행 jar만 포함. 빌드 컨텍스트 112MB → 73MB
- [x] **`docker-compose.yml`** (앱 + MySQL) — 볼륨, 헬스체크, `service_healthy` 대기, `.env` 주입
- [ ] **EC2 + Elastic IP + swap**
- [ ] **Nginx 리버스 프록시**
- [ ] **HTTPS** (Let's Encrypt) — A 레코드 전파 확인 후 발급. 실패에도 rate limit 있음
- [ ] **GitHub Actions 배포** (이미지 빌드 → GHCR → EC2 pull)

EC2 단계에서 처리할 것
- **compose의 `build: .` 를 `image:` 로 바꿔야 한다.** 서버에는 소스와 jar를 두지 않는다.
  로컬은 빌드, 서버는 pull 하도록 override 파일로 나누는 방법을 검토
- **서버 `.env`는 새로 만든다.** API 키는 동일하지만 `DB_PASSWORD`와 `DB_ROOT_PASSWORD`는
  운영용으로 따로 정한다. 만든 뒤 `chmod 600`
- ⚠️ **MySQL 계정은 볼륨이 빈 최초 실행에만 생성된다.** 이후 `.env`의 비밀번호를 바꿔도
  이미 만들어진 계정에는 반영되지 않아 `Access denied`가 난다. 첫 `up` 전에 값을 확정할 것

메모
- 인스턴스는 2GB 예정. JVM 기본 힙이 512MB가 되어 GeoJSON·CSV 로딩에 여유가 생김
- **EC2에서 Gradle 빌드는 하지 않음.** fat jar 빌드는 2GB에서도 빠듯하므로 Actions에서 빌드
- 이미지는 약 600MB(JRE 베이스 + jar)지만 베이스 레이어는 한 번만 받고 이후엔 jar 레이어만 갱신됨
- Flyway는 새 스키마로 시작하면 그대로 통과. 데이터가 있는 스키마에 처음 적용하면 실패함

---

## 별도 트랙 · 배포 검증

배포 트랙 완료 후. 세 단계로 나눈다.

① 배포 스모크 — 외부에서 1회, 기능 확인
② 내부 부하 — EC2 → EC2 사설 IP, 용량 측정
③ 외부 E2E — PC → 인터넷 → HTTPS, 체감 지연

②를 ③보다 먼저 하는 이유: 외부부터 재면 느릴 때 앱 탓인지 경로 탓인지 나눌 수 없다.

### ① 배포 스모크

- [ ] **외부 API 키의 IP 제한 확인** ⚠️ 배포 전
  로컬에서 신청한 키는 출발지가 집 IP다. EC2로 올리면 바뀌어서 서버에서만 실패한다.
  기상청 실황·예보, 에어코리아, 주소, 공휴일 전부 해당

- [ ] **스택 확인** — HTTPS 인증서와 중간 체인 / Nginx 프록시 경로 / DB 연결 /
  Flyway V1~V3 적용 / EC2에서 외부 API 도달 / `/health` /
  `/dashboard/guest`가 점수와 `status-*` 클래스까지 렌더되는지

### ② 내부 부하

EC2 2대. 대상은 운영 인스턴스(2GB, app + MySQL), 생성기는 t3.micro에 k6.

같은 AZ에 두고 사설 IP로 호출한다. 같은 AZ + 사설 IP만 양방향 무료이고
다른 AZ는 $0.01/GB 양방향, 도메인 경유도 과금된다.
로컬 PC에서 안 돌리는 건 비용보다 다른 프로세스의 간섭을 빼기 위함이다.

- [ ] **외부 API 일일 한도 보호** ⚠️
  캐시 미스를 대량 유발하면 기상청·에어코리아 한도를 소진한다. 운영 키가 막히면 그대로 장애다.
  캐시를 채운 상태로 돌리거나, 외부 클라이언트를 스텁으로 바꾸는 테스트 프로필을 만든다.
  스텁을 만들지는 미결. 안 만들면 매번 캐시 상태를 신경 써야 한다

- [ ] **t3 CPU 크레딧 잔량을 테스트 전후로 기록**
  버스터블이라 baseline 초과가 지속되면 크레딧이 고갈되고 성능이 급락한다.
  앱 문제로 오인하기 쉽다. unlimited 모드면 초과분이 과금된다

- [ ] **k6 인스턴스는 테스트할 때만 기동**
  퍼블릭 IPv4가 시간당 $0.005. 프리티어 750시간은 한 개 상시 기준이라 두 번째가 잠식한다.
  SSM Session Manager를 쓰면 퍼블릭 IP 없이 접속할 수 있다

대상은 `/dashboard/guest`. 날씨·대기질·점수·렌더를 모두 거치는 가장 무거운 경로다.
인증 경로는 k6에서 세션을 다뤄야 해서 후순위.

`req/s = VU / (응답시간 + think time)`. VU만 적으면 의미가 없으니 think time을 같이 남긴다.

- [ ] **Smoke** — 1 VU / 1분
- [ ] **Baseline** — 10 VU / 5분, think 3~10초 (약 1.5 req/s)
  1,000 매장이 저녁에 몰려도 0.2 req/s 수준이라 예상 부하의 몇 배다.
  p95 300ms 미만(캐시 히트), 에러 0
- [ ] **Peak** — 50 VU / 10분, think 3~10초 (약 7 req/s). p95 1s 미만, 에러 1% 미만
- [ ] **Stress** — 0 → 200 VU 계단식, think 없음
  통과 개념이 없다. 200까지 멀쩡하면 더 올린다.
  커넥션 풀 / JVM 힙 / CPU 크레딧 / MySQL 중 무엇이 먼저 터지는지 기록
- [ ] **Soak** — 10 VU / 30~60분. 순간값이 아니라 추세를 본다.
  JVM 힙, 커넥션 반환, 캐시 테이블 행 증가 속도.
  "캐시 테이블 보존 기간 정리"의 필요성을 여기서 수치로 확인할 수 있다
- [ ] **Cache stampede** — 캐시를 비우고 50 VU 동시 진입
  5단계 "Weather 캐시 최초 요청 race"가 실재하는지 보는 테스트다.
  외부 API 호출 횟수를 앱 로그에서 센다. 1회면 정상, 50회면 race.
  한도를 쓰므로 1~2회만

기록할 것
- k6 — p50/p95/p99, 에러율, req/s, VU, think time
- 앱 — 외부 API 호출 횟수, DB 커넥션 풀, JVM 힙, GC 빈도
- 대상 EC2 — CPU 크레딧, 메모리, 디스크 I/O
- k6 EC2 — CPU. 여기가 100%면 서버가 아니라 생성기가 병목이다

평균은 보지 않는다. 평균 200ms에 p99 5초가 흔하다.
워밍업 구간은 측정에서 뺀다. JIT, 커넥션 풀 초기화, 캐시 콜드가 겹친다.

### ③ 외부 E2E

내 PC에서 k6로 public endpoint를 친다. 소량만.
용량 측정이 아니라 Nginx·HTTPS·인터넷 구간까지 포함한 체감 지연 확인이 목적이다.

- [ ] **②와 같은 시나리오로 1~5 VU / 2~3분**
  파라미터를 맞춰야 차이를 뽑을 수 있다. 내부 p95와의 차이가 RTT + TLS + Nginx다.
  이게 나와야 Nginx 설정이 비용을 더하는 건지 그냥 물리적 거리인지 구분된다

- [ ] **`noConnectionReuse: true`로 한 번 더** — 콜드 핸드셰이크 측정
  기본 설정은 VU 안에서 커넥션을 재사용해 TLS 비용이 첫 요청에만 잡히고 묻힌다.
  두 모드를 비교하면 첫 방문과 재방문 차이가 나온다

- [ ] **외부에서만 보이는 것** — gzip이 실제로 걸리는지 / HTTP/2 여부 / 인증서 체인 /
  리다이렉트 체인(하나당 RTT 하나씩) / Nginx 타임아웃·버퍼가 앱 응답보다 짧으면 502

- [ ] **Nginx rate limit이 본인 테스트를 막는다** ⚠️
  `limit_req`나 fail2ban을 걸어둔 상태로 k6를 돌리면 429가 쏟아진다. 느린 걸로 오독하기 쉽다.
  테스트 중에는 본인 IP를 빼거나 완화한다. rate limit이 의도대로 도는지도 따로 확인

- [ ] **3회 이상 돌려 중앙값을 쓴다**
  가정용 회선은 업로드가 비대칭이고 ISP 상태가 변한다. 외부 측정은 재현성이 낮다.
  한 번 재고 결론 내리지 않는다. 편차가 크면 편차 자체를 기록

### 브라우저 E2E

전면 도입은 하지 않는다. 서버 렌더링이라 SPA만큼 얻는 게 없고 유지비가 붙는다.
다만 템플릿과 컨트롤러 사이 배선은 지금 아무도 검증하지 않는다.
점수 색깔 회귀가 정확히 그 구간이었다. 단위 테스트는 전부 통과했는데 화면만 회색이었다.

- [ ] **핵심 경로 3~5개만**
  게스트 대시보드에 점수와 `status-*`가 렌더되는지 /
  회원가입 → 로그인 → 매장 등록 → 대시보드 / 매장 수정 / 에러 페이지

### 결과

- [ ] **README에 부하 테스트 결과 추가**
  시나리오, 결과, 병목, 조치를 표로. 내부와 외부의 차이도 같이 적는다.
  stampede에서 race를 수치로 잡으면 5단계 항목의 근거가 된다

---

## 별도 트랙 · v2 개인화 수요예측

설계 확정, 착수 전. 상세는 **[docs/v2-design.md](docs/v2-design.md)**

점주가 업로드한 쿠팡이츠 주문 CSV로 매장별 시간당 주문량을 예측하고,
그 예측값을 OOS 예측분포 percentile로 0~100 배달온도로 변환한다.
v1은 폐기하지 않고 **CSV 업로드 이전 구간의 cold-start 전용**으로 남는다.

```
CSV 없음  → v1
CSV 소량  → CREDIBILITY_BASELINE   (상권 prior + Gamma-Poisson shrinkage)
3개월+    → Time Model 후보
12개월+   → Weather Model 후보
```

**착수 전 반드시 확인** (둘 중 하나라도 실패하면 설계가 바뀐다)
- [ ] 쿠팡이츠 CSV `time(1)` 컬럼이 **주문 시각**인지 — 고유값 개수로 판별
- [ ] 기상청 API허브 초단기예보 **과거 자료 보관 기간** — `tmfc`에 12·9·6·3개월 전을
  지정해 응답이 오는지 integration test. API·파라미터 존재는 확인됨, 보관 기간이 관건

**선행 작업**
- [ ] 전처리에 **city 레벨 index 출력 추가** — 현재 index는 `*-audit.csv`의 local에만 있음.
  rate floor의 City fallback 전제
- [ ] **prior cutoff snapshot 생성** — 백테스트 fold보다 미래의 상권 데이터가
  prior에 섞이면 안 됨. `preprocess_*.py`의 연도 범위를 제한해 재실행
- [ ] `Store`에 요일별 영업시간 필드 — 노출시간 `E` 계산의 전제
- [ ] 예보 아카이빙 — 보관 기간이 12개월에 미달하는 만큼만 필요.
  ⚠️ 위 "배포 후 1순위"의 캐시 보존 기간 정리와 충돌하지 않도록 할 것

**설계에서 특히 놓치기 쉬운 것**
- **`asOf` / `forecastAt` / `leadHour`** — 학습을 (예측시점, 대상시점) 쌍으로 구성해야 한다.
  "주문 시각 당시 예보"만 쓰면 nowcast를 학습하고 +6시간 예보로 서빙하게 됨
- **`ScoreStatusLevel` 임계값 `64/56/42/37`은 v2에서 폐기.** percentile 공간에서는
  20점 균등 구간이 옳다. 두 엔진 공존 기간에는 모델 종류로 분기 필요

**연결점**
- `output/*-audit.csv`의 `time_index` / `day_index`가 그대로 `MarketDemandPrior` 재료가 된다.
  v1 전처리 때 검증용으로 남긴 중간 산출물이 v2 prior의 원천이 됨
- `simulate_score_distribution.py`를 백테스트 하네스로 전환하면 v1 병렬 비교가 저렴해진다

---

## 지금은 하지 않을 것

Redis · Kafka · MSA · Kubernetes · Repository 추상화 · Service마다 Interface+Impl ·
모든 DTO를 record로 변환 · CommercialArea STRtree 최적화 · 복잡한 이벤트 아키텍처 ·
근거 없는 ML 도입

현재 병목은 기술 스택 부족이 아니라 **정확성 · 캐시 의미론 · 데이터 정직성 · 실패 처리 · 점수 보정**입니다.

---

## 알려진 검증 공백

- **Flyway 마이그레이션의 자동 검증이 없습니다.** 테스트는 H2에서 엔티티 기준 `create-drop`을 쓰고,
  V1은 `ENUM`/`ENGINE=InnoDB`/`BIT(1)` 등 MySQL 전용 문법이라 H2에서 실행할 수 없습니다.
  compose로 빈 스키마에 V1이 적용되는 것은 **수동으로 확인했지만**, 매번 자동으로 확인되지는 않습니다.
  Testcontainers 기반 통합 테스트로 메워야 합니다.
- **캐시 테이블에 정리 로직이 없습니다.** `current_weather_record`, `current_air_quality_record`가
  무한 증가합니다. 작업 항목은 "배포 후 1순위" 절에 있습니다.
- **외부 API 호출이 사용자 요청 경로에 그대로 남아 있습니다.** 캐시가 비면 사용자가 왕복을 기다립니다.
  타임아웃으로 최악의 지연은 막았지만 근본 해결은 사전 적재입니다. 같은 절 참고.
