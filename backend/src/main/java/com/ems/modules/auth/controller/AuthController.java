package com.ems.modules.auth.controller;

import com.ems.common.dto.ApiResponse;
import com.ems.modules.auth.dto.*;
import com.ems.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("API request to register user: {}", registerRequest.getUsername());
        JwtResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("API request to authenticate user: {}", loginRequest.getUsername());
        JwtResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody TokenRefreshRequest refreshRequest) {
        log.info("API request to refresh authentication token");
        JwtResponse response = authService.refresh(refreshRequest);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest) {
        log.info("API request to reset password for email: {}", resetRequest.getEmail());
        authService.resetPassword(resetRequest);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully. All active sessions have been logged out."));
    }
}
