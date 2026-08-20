package com.baedalondo.api.startup;

import com.baedalondo.api.airquality.service.CurrentAirQualityService;
import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.holiday.service.HolidayService;
import com.baedalondo.api.weather.service.ForecastWeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 서버가 뜰 때 외부 데이터를 채우는지, 그리고 그 실패가 기동을 막지 않는지 확인한다.

 사전 적재는 빨라지게 하는 장치지 없으면 안 되는 경로가 아니다.
 여기서 예외가 새어 나가면 외부 API 장애가 곧 서버 기동 실패가 된다.
 **/
@ExtendWith(MockitoExtension.class)
class ExternalDataPreloaderTest {

    @Mock
    private HolidayService holidayService;

    @Mock
    private CurrentAirQualityService currentAirQualityService;

    @Mock
    private ForecastWeatherService forecastWeatherService;

    private ExternalDataPreloader preloader(boolean enabled) {
        return new ExternalDataPreloader(
                holidayService, currentAirQualityService, forecastWeatherService, enabled);
    }

    @Test
    @DisplayName("공휴일, 대기질, 예보를 모두 채운다")
    void preloadsEverySource() {
        preloader(true).preloadOnStartup();

        LocalDate today = ServiceTime.today();

        verify(holidayService).refreshHolidaysForMonthAndNextMonth(today.getYear(), today.getMonthValue());
        verify(currentAirQualityService).preloadStoreSidoNames();
        verify(forecastWeatherService).preloadDashboardGrids();
    }

    @Test
    @DisplayName("하나가 실패해도 나머지는 계속 채운다")
    void keepsGoingWhenOneSourceFails() {
        when(currentAirQualityService.preloadStoreSidoNames())
                .thenThrow(new IllegalStateException("에어코리아 응답 없음"));

        assertDoesNotThrow(() -> preloader(true).preloadOnStartup());

        verify(holidayService).refreshHolidaysForMonthAndNextMonth(anyInt(), anyInt());
        verify(forecastWeatherService).preloadDashboardGrids();
    }

    @Test
    @DisplayName("전부 실패해도 기동을 막지 않는다")
    void neverFailsStartup() {
        doThrow(new IllegalStateException("공휴일 API 실패"))
                .when(holidayService).refreshHolidaysForMonthAndNextMonth(anyInt(), anyInt());
        when(currentAirQualityService.preloadStoreSidoNames())
                .thenThrow(new IllegalStateException("에어코리아 응답 없음"));
        when(forecastWeatherService.preloadDashboardGrids())
                .thenThrow(new IllegalStateException("기상청 응답 없음"));

        assertDoesNotThrow(() -> preloader(true).preloadOnStartup());
    }

    @Test
    @DisplayName("꺼져 있으면 외부 API를 부르지 않는다")
    void skipsEverythingWhenDisabled() {
        // 테스트와 CI는 이 플래그로 외부 호출을 막는다.
        preloader(false).preloadOnStartup();

        verifyNoInteractions(holidayService, currentAirQualityService, forecastWeatherService);
        verify(forecastWeatherService, never()).preloadDashboardGrids();
    }
}
