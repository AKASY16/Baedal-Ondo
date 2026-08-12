package com.baedalondo.api;

import com.baedalondo.api.auth.dto.SignupRequest;
import com.baedalondo.api.auth.service.SignupService;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SignupService signupService;

    @Test
    void signupEncodesPasswordAndSavesUserAccount() {
        SignupRequest request = signupRequest("owner01", "password123");
        when(userAccountRepository.existsByLoginId("owner01")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        signupService.signup(request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertEquals("owner01", captor.getValue().getLoginId());
        assertEquals("encoded-password", captor.getValue().getPassword());
        assertEquals("USER", captor.getValue().getRole());
    }

    @Test
    void signupRejectsDuplicateLoginId() {
        SignupRequest request = signupRequest("owner01", "password123");
        when(userAccountRepository.existsByLoginId("owner01")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> signupService.signup(request)
        );

        assertEquals("이미 사용 중인 아이디입니다.", exception.getMessage());
        verify(userAccountRepository, never()).save(any());
    }

    private SignupRequest signupRequest(String loginId, String password) {
        SignupRequest request = new SignupRequest();
        request.setLoginId(loginId);
        request.setPassword(password);
        request.setPasswordConfirm(password);
        return request;
    }
}
