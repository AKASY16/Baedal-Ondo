package com.baedalondo.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(min = 4, max = 30, message = "아이디는 4~30자로 입력해 주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "아이디에는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8~72자로 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호를 한 번 더 입력해 주세요.")
    private String passwordConfirm;

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
