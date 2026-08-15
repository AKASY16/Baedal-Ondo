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

- [ ] **점수 분포 시뮬레이션** — 조사만, 가중치 변경 금지
  근거: `BASE 50` + time(`-12`~`+14`) + day(`-6`~`+6`, 공휴일 `+8`) + weather(`0`~`+20`) + air(`0`~`+8`) + interaction(`0`~`+10`)
  → 이론 최소 **32**, 최대 **110**(100으로 clamp). 게스트는 DayWeight가 0이라 하한 **38**
  결과: `ScoreStatusLevel.CLOSED`(0~19) **도달 불가능**, 상단은 정보 손실
  재료: `time-weight-local.csv`(27,775행), `day-weight-local.csv`, 업종 9종 × 시간대 6구간

- [ ] **분포 결과에 따른 재보정**
  `BASE_SCORE` / 컴포넌트 범위 / status threshold 중 필요한 부분만
  ⚠️ 위 항목과 절대 합치지 말 것. 분포를 보기 전에 가중치를 바꾸면 "데이터 기반"이라는 근거가 무너짐

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
  무한 증가합니다. 사전 적재 스케줄러를 도입하면 증가 속도가 빨라지므로 보존 기간 정리를 함께 넣어야 합니다.
