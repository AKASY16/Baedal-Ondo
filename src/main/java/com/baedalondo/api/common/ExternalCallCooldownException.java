package com.baedalondo.api.common;

/**
 쿨다운 중인 대상을 다시 호출하려 했을 때 던진다.

 호출부는 쿨다운을 미리 확인하고 폴백으로 빠지는 것이 정상 경로다.
 이 예외는 그 확인이 빠진 경로를 잡아내기 위한 안전망이다.
 */
public class ExternalCallCooldownException extends RuntimeException {

    public ExternalCallCooldownException(String key) {
        super("외부 호출이 연속 실패해 잠시 호출을 멈춘 대상입니다. key=" + key);
    }
}
