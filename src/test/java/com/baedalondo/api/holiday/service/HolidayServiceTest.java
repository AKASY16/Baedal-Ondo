package com.baedalondo.api.holiday.service;

import com.baedalondo.api.holiday.client.HolidayClient;
import com.baedalondo.api.holiday.entity.Holiday;
import com.baedalondo.api.holiday.repository.HolidayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayClient holidayClient;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Captor
    private ArgumentCaptor<Iterable<Holiday>> savedCaptor;

    private HolidayService holidayService() {
        return new HolidayService(holidayClient, holidayRepository, transactionTemplate, false);
    }

    @Test
    @DisplayName("두 달 범위의 모든 날짜를 채운다")
    void fillsEveryDateInRange() {
        // 공휴일만 저장하면 비공휴일 날짜는 항상 조회 miss가 되어
        // 대시보드 요청마다 외부 API를 다시 부른다.
        when(holidayClient.fetchHolidays(2026, 8)).thenReturn(List.of());
        when(holidayClient.fetchHolidays(2026, 9)).thenReturn(List.of());

        holidayService().refreshHolidaysForMonthAndNextMonth(2026, 8);

        Map<LocalDate, Holiday> saved = captureSaved();

        assertEquals(61, saved.size());
        assertNotNull(saved.get(LocalDate.of(2026, 8, 1)));
        assertNotNull(saved.get(LocalDate.of(2026, 8, 31)));
        assertNotNull(saved.get(LocalDate.of(2026, 9, 1)));
        assertNotNull(saved.get(LocalDate.of(2026, 9, 30)));
    }

    @Test
    @DisplayName("실제 공휴일은 비공휴일 자리채움에 덮이지 않는다")
    void keepsRealHolidayOverPlaceholder() {
        when(holidayClient.fetchHolidays(2026, 8))
                .thenReturn(List.of(new Holiday(LocalDate.of(2026, 8, 15), "광복절", "01", true, 1)));
        when(holidayClient.fetchHolidays(2026, 9)).thenReturn(List.of());

        holidayService().refreshHolidaysForMonthAndNextMonth(2026, 8);

        Map<LocalDate, Holiday> saved = captureSaved();
        Holiday liberationDay = saved.get(LocalDate.of(2026, 8, 15));

        assertEquals("광복절", liberationDay.getName());
        assertTrue(liberationDay.getHoliday());
        assertFalse(saved.get(LocalDate.of(2026, 8, 14)).getHoliday());
    }

    @Test
    @DisplayName("12월이면 다음 해 1월까지 채운다")
    void rollsOverToNextYear() {
        when(holidayClient.fetchHolidays(2026, 12)).thenReturn(List.of());
        when(holidayClient.fetchHolidays(2027, 1)).thenReturn(List.of());

        holidayService().refreshHolidaysForMonthAndNextMonth(2026, 12);

        Map<LocalDate, Holiday> saved = captureSaved();

        assertEquals(62, saved.size());
        assertNotNull(saved.get(LocalDate.of(2027, 1, 31)));
    }

    @Test
    @DisplayName("외부 API가 실패하면 기존 데이터를 지우지 않는다")
    void keepsExistingDataWhenClientFails() {
        // 지우고 나서 받아오면 API 장애 때 공휴일 정보가 통째로 사라진다.
        when(holidayClient.fetchHolidays(2026, 8))
                .thenThrow(new RuntimeException("공휴일 API 실패"));

        HolidayService holidayService = holidayService();

        assertThrows(RuntimeException.class,
                () -> holidayService.refreshHolidaysForMonthAndNextMonth(2026, 8));

        verify(holidayRepository, never()).deleteByDateBetween(any(), any());
        verify(holidayRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("저장된 날짜를 조회하면 외부 API를 부르지 않는다")
    void doesNotCallClientWhenDateIsAlreadyStored() {
        LocalDate date = LocalDate.of(2026, 8, 19);
        when(holidayRepository.findByDate(date))
                .thenReturn(Optional.of(new Holiday(date, "비공휴일", null, false, null)));

        assertFalse(holidayService().isHoliday(date));

        verify(holidayClient, never()).fetchHolidays(anyInt(), anyInt());
    }

    @Test
    @DisplayName("공휴일로 저장된 날짜는 true를 돌려준다")
    void returnsTrueForStoredHoliday() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        when(holidayRepository.findByDate(date))
                .thenReturn(Optional.of(new Holiday(date, "광복절", "01", true, 1)));

        assertTrue(holidayService().isHoliday(date));

        verify(holidayClient, never()).fetchHolidays(anyInt(), anyInt());
    }

    private Map<LocalDate, Holiday> captureSaved() {
        verify(holidayRepository).saveAll(savedCaptor.capture());

        return java.util.stream.StreamSupport
                .stream(savedCaptor.getValue().spliterator(), false)
                .collect(Collectors.toMap(Holiday::getDate, Function.identity()));
    }
}
