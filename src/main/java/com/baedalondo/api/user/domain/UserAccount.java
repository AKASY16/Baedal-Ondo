package com.baedalondo.api.user.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    // 계정 복구와 이메일 인증에 사용하는 주소
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    // 이메일 주소의 소유 확인을 마쳤는지 여부
    @Column(nullable = false)
    private boolean emailVerified;

    // 이메일 인증을 완료한 시각
    private LocalDateTime emailVerifiedAt;

    // 정상, 정지, 탈퇴 중 현재 계정 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus accountStatus;

    // 회원가입으로 계정이 생성된 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 계정 정보가 마지막으로 변경된 시각
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 마지막 로그인 성공 시각
    private LocalDateTime lastLoginAt;

    // 비밀번호를 마지막으로 설정하거나 변경한 시각
    @Column(nullable = false)
    private LocalDateTime passwordChangedAt;

    // 회원 탈퇴가 처리된 시각
    private LocalDateTime withdrawnAt;

    protected UserAccount() {
    }

    public UserAccount(String loginId, String email, String password, String role) {
        LocalDateTime now = LocalDateTime.now();
        this.loginId = loginId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.emailVerified = false;
        this.accountStatus = AccountStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
        this.passwordChangedAt = now;
    }

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (passwordChangedAt == null) {
            passwordChangedAt = now;
        }
        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public LocalDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus == null ? AccountStatus.ACTIVE : accountStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void verifyEmail(LocalDateTime verifiedAt) {
        this.emailVerified = true;
        this.emailVerifiedAt = verifiedAt;
    }

    public void recordLogin(LocalDateTime loggedInAt) {
        this.lastLoginAt = loggedInAt;
    }

    public void changePassword(String encodedPassword, LocalDateTime changedAt) {
        this.password = encodedPassword;
        this.passwordChangedAt = changedAt;
    }

    public void suspend() {
        this.accountStatus = AccountStatus.SUSPENDED;
    }

    public void activate() {
        this.accountStatus = AccountStatus.ACTIVE;
        this.withdrawnAt = null;
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        this.accountStatus = AccountStatus.WITHDRAWN;
        this.withdrawnAt = withdrawnAt;
    }
}
