package com.baedalondo.api.auth.service;

import com.baedalondo.api.auth.dto.SignupRequest;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(UserAccountRepository userAccountRepository,
                         PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount signup(SignupRequest request) {
        String loginId = request.getLoginId().trim();

        if (userAccountRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        UserAccount userAccount = new UserAccount(
                loginId,
                passwordEncoder.encode(request.getPassword()),
                DEFAULT_ROLE
        );

        return userAccountRepository.save(userAccount);
    }
}
