package com.beadalondo.api.auth.service;

import com.beadalondo.api.user.domain.UserAccount;
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

    public CustomUserDetails(UserAccount userAccount) {
        this.userId = userAccount.getId();
        this.loginId = userAccount.getLoginId();
        this.password = userAccount.getPassword();
        this.role = userAccount.getRole();
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
}