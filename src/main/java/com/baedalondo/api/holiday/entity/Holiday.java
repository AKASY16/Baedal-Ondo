package com.baedalondo.api.holiday.entity;

import com.baedalondo.api.common.ServiceTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "holiday",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_holiday_date",
                        columnNames = "holiday_date"
                )
        }
)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String name;

    private String dateKind;

    @Column(nullable = false)
    private Boolean holiday;

    private Integer seq;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Holiday() {
    }

    public Holiday(LocalDate date,
                   String name,
                   String dateKind,
                   Boolean holiday,
                   Integer seq) {
        this.date = date;
        this.name = name;
        this.dateKind = dateKind;
        this.holiday = holiday;
        this.seq = seq;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = ServiceTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getDateKind() {
        return dateKind;
    }

    public Boolean getHoliday() {
        return holiday;
    }

    public Integer getSeq() {
        return seq;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
