package com.baedalondo.api.user.repository;

import com.baedalondo.api.user.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmailIgnoreCase(String email);
}
