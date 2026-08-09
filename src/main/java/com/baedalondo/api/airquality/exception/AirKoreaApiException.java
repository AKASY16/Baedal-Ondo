package com.baedalondo.api.airquality.exception;

public class AirKoreaApiException extends RuntimeException {
    public AirKoreaApiException(String message) {
        super(message);
    }

    public AirKoreaApiException(String messagem, Throwable cause) {
        super(messagem, cause);
    }
}
