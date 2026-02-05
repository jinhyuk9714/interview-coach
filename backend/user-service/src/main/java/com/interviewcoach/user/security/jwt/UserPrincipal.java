package com.interviewcoach.user.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class UserPrincipal implements Principal {

    private Long userId;
    private String email;

    @Override
    public String getName() {
        return email;
    }
}
