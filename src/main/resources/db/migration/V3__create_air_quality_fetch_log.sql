-- 어떤 시도의 어떤 기준시각 데이터를 이미 조회했는지 기록한다.
--
-- 측정값의 measured_at은 측정소가 실제 측정한 시각이고,
-- 여기의 base_time은 AirQualityCalculator가 계산한 우리 서버의 기준시각이다.
-- 둘을 구분해야 "아직 안 받아왔다"와 "받아왔지만 그 자치구 측정소가 응답에 없었다"를
-- 구별할 수 있다.
CREATE TABLE `air_quality_fetch_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sido_name` VARCHAR(255) NOT NULL,
    `base_time` DATETIME(6) NOT NULL,
    `fetched_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    -- 조회 조건과 동일한 순서로 둔다.
    CONSTRAINT `uk_air_quality_fetch_log_sido_base_time`
        UNIQUE (`sido_name`, `base_time`)
) ENGINE = InnoDB;
