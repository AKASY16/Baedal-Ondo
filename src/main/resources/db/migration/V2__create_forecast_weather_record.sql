-- 초단기예보 저장 테이블.
-- 한 번의 발표(base_date + base_time)에 대해 예보 시각(forecast_at)별로 여러 행이 쌓인다.
CREATE TABLE `forecast_weather_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `forecast_at` DATETIME(6) NOT NULL,
    `nx` INT NOT NULL,
    `ny` INT NOT NULL,
    `base_date` VARCHAR(255) NULL,
    `base_time` VARCHAR(255) NULL,
    `precipitation_type` INT NOT NULL,
    `rainfall` DOUBLE NOT NULL,
    `temperature` DOUBLE NOT NULL,
    `humidity` INT NOT NULL,
    `wind_speed` DOUBLE NOT NULL,
    `created_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_forecast_weather_record_location_time`
        UNIQUE (`forecast_at`, `nx`, `ny`, `base_date`, `base_time`)
) ENGINE = InnoDB;
