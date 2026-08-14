-- 기존 current_air_quality_record에서 더 이상 사용하지 않는 O3 데이터를 제거한다.
-- 애플리케이션을 중지하고 DB를 백업한 뒤 한 번만 실행한다.
ALTER TABLE current_air_quality_record
    DROP COLUMN o3_value,
    DROP COLUMN o3_grade;
