package com.baedalondo.api.auth.service;

import com.baedalondo.api.common.ServiceTime;
import com.baedalondo.api.auth.dto.SignupRequest;
import com.baedalondo.api.legal.LegalDocumentVersions;
import com.baedalondo.api.user.domain.AgreementType;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.domain.UserAgreement;
import com.baedalondo.api.user.repository.UserAccountRepository;
import com.baedalondo.api.user.repository.UserAgreementRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SignupService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_user_account_email";
    private static final String AGE_CONFIRMATION_VERSION = "1.0";
    private static final String MARKETING_EMAIL_VERSION = "1.0";

    private final UserAccountRepository userAccountRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(UserAccountRepository userAccountRepository,
                         UserAgreementRepository userAgreementRepository,
                         PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.userAgreementRepository = userAgreementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount signup(SignupRequest request) {
        String loginId = request.getLoginId().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userAccountRepository.existsByLoginId(loginId)) {
            throw new SignupConflictException("loginId", "이미 사용 중인 아이디입니다.");
        }

        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new SignupConflictException("email", "이미 사용 중인 이메일입니다.");
        }

        UserAccount userAccount = new UserAccount(
                loginId,
                email,
                passwordEncoder.encode(request.getPassword()),
                DEFAULT_ROLE
        );

        // 위 중복 검사와 저장 사이에 같은 값으로 다른 요청이 끼어들 수 있다.
        // 유니크 제약 위반을 그대로 두면 500이 되므로 같은 필드 오류로 변환한다.
        // 커밋 시점이 아니라 이 자리에서 제약을 확인하기 위해 saveAndFlush를 쓴다.
        UserAccount savedUserAccount;
        try {
            savedUserAccount = userAccountRepository.saveAndFlush(userAccount);
        } catch (DataIntegrityViolationException exception) {
            throw toConflictException(exception);
        }

        LocalDateTime agreedAt = ServiceTime.now();

        List<UserAgreement> agreements = new ArrayList<>();
        agreements.add(UserAgreement.record(
                savedUserAccount,
                AgreementType.TERMS_OF_SERVICE,
                LegalDocumentVersions.TERMS,
                agreedAt
        ));
        agreements.add(UserAgreement.record(
                savedUserAccount,
                AgreementType.PRIVACY_NOTICE_ACKNOWLEDGEMENT,
                LegalDocumentVersions.PRIVACY,
                agreedAt
        ));
        agreements.add(UserAgreement.record(
                savedUserAccount,
                AgreementType.AGE_OVER_14_CONFIRMATION,
                AGE_CONFIRMATION_VERSION,
                agreedAt
        ));

        if (request.isMarketingEmailAgreed()) {
            agreements.add(UserAgreement.record(
                    savedUserAccount,
                    AgreementType.MARKETING_EMAIL,
                    MARKETING_EMAIL_VERSION,
                    agreedAt
            ));
        }

        userAgreementRepository.saveAll(agreements);
        return savedUserAccount;
    }

    /**
     * 어떤 유니크 제약이 깨졌는지 제약 이름으로 구분한다.
     * 이 시점의 트랜잭션은 롤백 대상이라 다시 조회해서 확인할 수 없다.
     */
    private SignupConflictException toConflictException(DataIntegrityViolationException exception) {
        String cause = exception.getMostSpecificCause().getMessage();

        if (cause != null && cause.contains(EMAIL_UNIQUE_CONSTRAINT)) {
            return new SignupConflictException("email", "이미 사용 중인 이메일입니다.");
        }

        return new SignupConflictException("loginId", "이미 사용 중인 아이디입니다.");
    }
}
