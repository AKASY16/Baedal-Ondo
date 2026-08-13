package com.baedalondo.api;

import com.baedalondo.api.auth.dto.SignupRequest;
import com.baedalondo.api.auth.service.SignupConflictException;
import com.baedalondo.api.auth.service.SignupService;
import com.baedalondo.api.user.domain.AgreementType;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.domain.UserAgreement;
import com.baedalondo.api.user.repository.UserAccountRepository;
import com.baedalondo.api.user.repository.UserAgreementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserAgreementRepository userAgreementRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SignupService signupService;

    @Test
    void signupEncodesPasswordAndSavesUserAccount() {
        SignupRequest request = signupRequest("owner01", "owner@example.com", "password123");
        when(userAccountRepository.existsByLoginId("owner01")).thenReturn(false);
        when(userAccountRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userAccountRepository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        signupService.signup(request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals("owner01", captor.getValue().getLoginId());
        assertEquals("owner@example.com", captor.getValue().getEmail());
        assertEquals("encoded-password", captor.getValue().getPassword());
        assertEquals("USER", captor.getValue().getRole());
        assertEquals("ACTIVE", captor.getValue().getAccountStatus().name());
        assertFalse(captor.getValue().isEmailVerified());
        assertNotNull(captor.getValue().getCreatedAt());
        assertNotNull(captor.getValue().getUpdatedAt());
        assertNotNull(captor.getValue().getPasswordChangedAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserAgreement>> agreementCaptor = ArgumentCaptor.forClass(List.class);
        verify(userAgreementRepository).saveAll(agreementCaptor.capture());
        assertEquals(3, agreementCaptor.getValue().size());
        assertEquals(
                List.of(
                        AgreementType.TERMS_OF_SERVICE,
                        AgreementType.PRIVACY_NOTICE_ACKNOWLEDGEMENT,
                        AgreementType.AGE_OVER_14_CONFIRMATION
                ),
                agreementCaptor.getValue().stream()
                        .map(UserAgreement::getAgreementType)
                        .toList()
        );
    }

    @Test
    void signupRejectsDuplicateLoginId() {
        SignupRequest request = signupRequest("owner01", "owner@example.com", "password123");
        when(userAccountRepository.existsByLoginId("owner01")).thenReturn(true);

        SignupConflictException exception = assertThrows(
                SignupConflictException.class,
                () -> signupService.signup(request)
        );

        assertEquals("loginId", exception.getField());
        assertEquals("이미 사용 중인 아이디입니다.", exception.getMessage());
        verify(userAccountRepository, never()).save(any());
        verify(userAgreementRepository, never()).saveAll(any());
    }

    @Test
    void signupRejectsDuplicateEmail() {
        SignupRequest request = signupRequest("owner01", "OWNER@example.com", "password123");
        when(userAccountRepository.existsByLoginId("owner01")).thenReturn(false);
        when(userAccountRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(true);

        SignupConflictException exception = assertThrows(
                SignupConflictException.class,
                () -> signupService.signup(request)
        );

        assertEquals("email", exception.getField());
        assertEquals("이미 사용 중인 이메일입니다.", exception.getMessage());
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void signupRecordsOptionalMarketingAgreementWhenSelected() {
        SignupRequest request = signupRequest("owner01", "owner@example.com", "password123");
        request.setMarketingEmailAgreed(true);
        when(userAccountRepository.existsByLoginId("owner01")).thenReturn(false);
        when(userAccountRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userAccountRepository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        signupService.signup(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserAgreement>> agreementCaptor = ArgumentCaptor.forClass(List.class);
        verify(userAgreementRepository).saveAll(agreementCaptor.capture());
        assertEquals(4, agreementCaptor.getValue().size());
        assertEquals(
                AgreementType.MARKETING_EMAIL,
                agreementCaptor.getValue().get(3).getAgreementType()
        );
    }

    private SignupRequest signupRequest(String loginId, String email, String password) {
        SignupRequest request = new SignupRequest();
        request.setLoginId(loginId);
        request.setEmail(email);
        request.setPassword(password);
        request.setPasswordConfirm(password);
        request.setTermsAccepted(true);
        request.setPrivacyNoticeAcknowledged(true);
        request.setAgeConfirmed(true);
        return request;
    }
}
