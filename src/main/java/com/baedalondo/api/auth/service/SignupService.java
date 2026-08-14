package com.baedalondo.api.auth.service;

import com.baedalondo.api.auth.dto.SignupRequest;
import com.baedalondo.api.legal.LegalDocumentVersions;
import com.baedalondo.api.user.domain.AgreementType;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.domain.UserAgreement;
import com.baedalondo.api.user.repository.UserAccountRepository;
import com.baedalondo.api.user.repository.UserAgreementRepository;
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

        UserAccount savedUserAccount = userAccountRepository.save(userAccount);
        LocalDateTime agreedAt = LocalDateTime.now();

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
}
