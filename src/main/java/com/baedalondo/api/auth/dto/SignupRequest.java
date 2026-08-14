package com.baedalondo.api.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank(message = "아이디를 입력해 주세요.")
    @Size(min = 4, max = 30, message = "아이디는 4~30자로 입력해 주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "아이디에는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.")
    private String loginId;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "이메일 형식을 확인해 주세요.")
    @Size(max = 254, message = "이메일은 254자 이내로 입력해 주세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 72, message = "비밀번호는 8~72자로 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호를 한 번 더 입력해 주세요.")
    private String passwordConfirm;

    @AssertTrue(message = "이용약관에 동의해 주세요.")
    private boolean termsAccepted;

    @AssertTrue(message = "개인정보 수집·이용에 동의해 주세요.")
    private boolean privacyNoticeAcknowledged;

    @AssertTrue(message = "만 14세 이상임을 확인해 주세요.")
    private boolean ageConfirmed;

    private boolean marketingEmailAgreed;

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
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

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }

    public boolean isPrivacyNoticeAcknowledged() {
        return privacyNoticeAcknowledged;
    }

    public void setPrivacyNoticeAcknowledged(boolean privacyNoticeAcknowledged) {
        this.privacyNoticeAcknowledged = privacyNoticeAcknowledged;
    }

    public boolean isAgeConfirmed() {
        return ageConfirmed;
    }

    public void setAgeConfirmed(boolean ageConfirmed) {
        this.ageConfirmed = ageConfirmed;
    }

    public boolean isMarketingEmailAgreed() {
        return marketingEmailAgreed;
    }

    public void setMarketingEmailAgreed(boolean marketingEmailAgreed) {
        this.marketingEmailAgreed = marketingEmailAgreed;
    }
}
