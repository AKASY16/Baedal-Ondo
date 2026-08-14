-- Initial schema generated from the current JPA entities.
CREATE TABLE `user_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `login_id` VARCHAR(255) NOT NULL,
    `email` VARCHAR(254) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(255) NOT NULL,
    `email_verified` BIT(1) NOT NULL,
    `email_verified_at` DATETIME(6) NULL,
    `account_status` ENUM('ACTIVE', 'SUSPENDED', 'WITHDRAWN') NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `last_login_at` DATETIME(6) NULL,
    `password_changed_at` DATETIME(6) NOT NULL,
    `withdrawn_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_user_account_login_id` UNIQUE (`login_id`),
    CONSTRAINT `uk_user_account_email` UNIQUE (`email`)
) ENGINE = InnoDB;

CREATE TABLE `user_agreement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_account_id` BIGINT NOT NULL,
    `agreement_type` ENUM(
        'AGE_OVER_14_CONFIRMATION',
        'MARKETING_EMAIL',
        'PRIVACY_NOTICE_ACKNOWLEDGEMENT',
        'TERMS_OF_SERVICE'
    ) NOT NULL,
    `document_version` VARCHAR(30) NOT NULL,
    `agreed_at` DATETIME(6) NOT NULL,
    `withdrawn_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_user_agreement_user_type` (`user_account_id`, `agreement_type`),
    CONSTRAINT `fk_user_agreement_user_account`
        FOREIGN KEY (`user_account_id`) REFERENCES `user_account` (`id`)
) ENGINE = InnoDB;

CREATE TABLE `store` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NULL,
    `business_type` ENUM(
        'BAKERY',
        'BUNSIK',
        'CAFE_BEVERAGE',
        'CHICKEN',
        'CHINESE_FOOD',
        'FAST_FOOD',
        'JAPANESE_FOOD',
        'KOREAN_FOOD',
        'WESTERN_FOOD'
    ) NULL,
    `address` VARCHAR(255) NULL,
    `road_address` VARCHAR(255) NULL,
    `jibun_address` VARCHAR(255) NULL,
    `address_detail` VARCHAR(255) NULL,
    `postal_code` VARCHAR(255) NULL,
    `sido_name` VARCHAR(255) NULL,
    `sigungu_name` VARCHAR(255) NULL,
    `dong_name` VARCHAR(255) NULL,
    `address_region_code` VARCHAR(255) NULL,
    `road_name_code` VARCHAR(255) NULL,
    `building_management_number` VARCHAR(255) NULL,
    `road_name` VARCHAR(255) NULL,
    `underground_yn` VARCHAR(255) NULL,
    `building_main_number` VARCHAR(255) NULL,
    `building_sub_number` VARCHAR(255) NULL,
    `nx` INT NULL,
    `ny` INT NULL,
    `commercial_area_code` VARCHAR(255) NULL,
    `commercial_area_name` VARCHAR(255) NULL,
    `commercial_area_type_code` VARCHAR(255) NULL,
    `commercial_area_type_name` VARCHAR(255) NULL,
    `user_id` BIGINT NULL,
    `created_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_store_user_id` (`user_id`),
    CONSTRAINT `fk_store_user_account`
        FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE = InnoDB;

CREATE TABLE `holiday` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `holiday_date` DATE NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `date_kind` VARCHAR(255) NULL,
    `holiday` BIT(1) NOT NULL,
    `seq` INT NULL,
    `created_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_holiday_date` UNIQUE (`holiday_date`)
) ENGINE = InnoDB;

CREATE TABLE `current_weather_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
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
    CONSTRAINT `uk_current_weather_record_location_time`
        UNIQUE (`nx`, `ny`, `base_date`, `base_time`)
) ENGINE = InnoDB;

CREATE TABLE `current_air_quality_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `sido_name` VARCHAR(255) NULL,
    `district_name` VARCHAR(255) NULL,
    `station_name` VARCHAR(255) NULL,
    `station_code` VARCHAR(255) NULL,
    `mang_name` VARCHAR(255) NULL,
    `measured_at` DATETIME(6) NULL,
    `pm10_value` INT NULL,
    `pm25_value` INT NULL,
    `khai_value` INT NULL,
    `khai_grade` INT NULL,
    `pm10_grade` INT NULL,
    `pm25_grade` INT NULL,
    `pm10_grade_1h` INT NULL,
    `pm25_grade_1h` INT NULL,
    `created_at` DATETIME(6) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_air_quality_record_location_station_time`
        UNIQUE (`sido_name`, `district_name`, `station_name`, `measured_at`)
) ENGINE = InnoDB;
