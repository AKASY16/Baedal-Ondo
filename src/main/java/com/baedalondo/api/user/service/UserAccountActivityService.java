package com.baedalondo.api.user.service;

import com.baedalondo.api.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserAccountActivityService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountActivityService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public void recordSuccessfulLogin(Long userId) {
        userAccountRepository.findById(userId)
                .ifPresent(userAccount -> userAccount.recordLogin(LocalDateTime.now()));
    }
}
