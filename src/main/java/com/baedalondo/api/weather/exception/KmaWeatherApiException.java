package com.baedalondo.api.weather.exception;

public class KmaWeatherApiException extends RuntimeException {

    public KmaWeatherApiException(String message) {
        super(message);
    }

    public KmaWeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}