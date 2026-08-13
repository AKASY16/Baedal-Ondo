package com.baedalondo.api.auth.service;

import com.baedalondo.api.user.domain.AccountStatus;
import com.baedalondo.api.user.domain.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String loginId;
    private final String password;
    private final String role;
    private final AccountStatus accountStatus;

    public CustomUserDetails(UserAccount userAccount) {
        this.userId = userAccount.getId();
        this.loginId = userAccount.getLoginId();
        this.password = userAccount.getPassword();
        this.role = userAccount.getRole();
        this.accountStatus = userAccount.getAccountStatus();
    }

    public Long getUserId() {
        return userId;
    }

    public String getLoginId() {
        return loginId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus != AccountStatus.WITHDRAWN;
    }
}
