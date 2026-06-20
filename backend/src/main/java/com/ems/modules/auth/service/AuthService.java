package com.ems.modules.auth.service;

import com.ems.modules.auth.dto.JwtResponse;
import com.ems.modules.auth.dto.LoginRequest;
import com.ems.modules.auth.dto.PasswordResetRequest;
import com.ems.modules.auth.dto.RegisterRequest;
import com.ems.modules.auth.dto.TokenRefreshRequest;

public interface AuthService {
    JwtResponse register(RegisterRequest request);
    JwtResponse login(LoginRequest request);
    JwtResponse refresh(TokenRefreshRequest request);
    void resetPassword(PasswordResetRequest request);
}
