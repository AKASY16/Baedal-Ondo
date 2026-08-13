package com.baedalondo.api.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_agreement",
        indexes = @Index(
                name = "idx_user_agreement_user_type",
                columnList = "user_account_id, agreement_type"
        )
)
public class UserAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    private UserAccount userAccount;

    // 약관, 개인정보 안내, 연령 확인, 광고성 이메일 중 기록 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_type", nullable = false, length = 50)
    private AgreementType agreementType;

    // 사용자가 확인하거나 동의한 문서의 버전
    @Column(nullable = false, length = 30)
    private String documentVersion;

    // 확인 또는 동의 의사를 표시한 시각
    @Column(nullable = false)
    private LocalDateTime agreedAt;

    // 선택 동의를 철회한 시각. 철회하지 않았다면 null
    private LocalDateTime withdrawnAt;

    protected UserAgreement() {
    }

    private UserAgreement(UserAccount userAccount,
                          AgreementType agreementType,
                          String documentVersion,
                          LocalDateTime agreedAt) {
        this.userAccount = userAccount;
        this.agreementType = agreementType;
        this.documentVersion = documentVersion;
        this.agreedAt = agreedAt;
    }

    public static UserAgreement record(UserAccount userAccount,
                                       AgreementType agreementType,
                                       String documentVersion,
                                       LocalDateTime agreedAt) {
        return new UserAgreement(userAccount, agreementType, documentVersion, agreedAt);
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public AgreementType getAgreementType() {
        return agreementType;
    }

    public String getDocumentVersion() {
        return documentVersion;
    }

    public LocalDateTime getAgreedAt() {
        return agreedAt;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public boolean isActive() {
        return withdrawnAt == null;
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }
}
