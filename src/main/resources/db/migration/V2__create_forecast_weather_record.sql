-- 초단기예보 저장 테이블.
-- 한 번의 발표(base_date + base_time)에 대해 예보 시각(forecast_at)별로 여러 행이 쌓인다.
CREATE TABLE `forecast_weather_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `forecast_at` DATETIME(6) NOT NULL,
    `nx` INT NOT NULL,
    `ny` INT NOT NULL,
    -- 유니크 제약에 참여하는 값이다. MySQL은 NULL을 서로 다른 값으로 보므로
    -- NULL을 허용하면 중복 방지가 깨진다. from()도 항상 채운다.
    `base_date` VARCHAR(255) NOT NULL,
    `base_time` VARCHAR(255) NOT NULL,
    `precipitation_type` INT NOT NULL,
    `rainfall` DOUBLE NOT NULL,
    `temperature` DOUBLE NOT NULL,
    `humidity` INT NOT NULL,
    `wind_speed` DOUBLE NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    -- 조회는 nx, ny, base_date, base_time으로 하고 forecast_at으로 정렬한다.
    -- 복합 인덱스는 맨 왼쪽부터 순서대로 사용되므로 조회 조건을 앞에 두고
    -- 정렬 기준을 마지막에 둔다. forecast_at이 앞에 오면 인덱스를 쓰지 못하고
    -- 정렬도 filesort가 된다.
    CONSTRAINT `uk_forecast_weather_record_location_time`
        UNIQUE (`nx`, `ny`, `base_date`, `base_time`, `forecast_at`)
) ENGINE = InnoDB;
