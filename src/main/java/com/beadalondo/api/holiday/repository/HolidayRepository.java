package com.beadalondo.api.holiday.repository;

import com.beadalondo.api.holiday.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByDate(LocalDate date);

    List<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate);

    void deleteByDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByDate(LocalDate date);
}
