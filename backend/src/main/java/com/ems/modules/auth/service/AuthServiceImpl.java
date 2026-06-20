package com.ems.modules.auth.service;

import com.ems.modules.audit.annotation.Auditable;
import com.ems.exception.BadRequestException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.modules.auth.dto.*;
import com.ems.modules.auth.model.*;
import com.ems.modules.auth.repository.RefreshTokenRepository;
import com.ems.modules.auth.repository.RoleRepository;
import com.ems.modules.auth.repository.UserRepository;
import com.ems.security.CustomUserDetails;
import com.ems.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ems.common.event.UserRegisteredEvent;
import com.ems.modules.employee.model.Employee;
import com.ems.modules.employee.model.EmployeeStatus;
import com.ems.modules.employee.repository.EmployeeRepository;
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final EmployeeRepository employeeRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenDurationMs;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            ApplicationEventPublisher eventPublisher,
            EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.eventPublisher = eventPublisher;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    @Auditable(action = "USER_REGISTER", entity = "User")
    public JwtResponse register(RegisterRequest request) {
        log.info("Processing registration request for username: {}, email: {}", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        // Map role string to RoleName enum
        RoleName roleName;
        try {
            roleName = RoleName.valueOf("ROLE_" + request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role. Role must be admin, hr, employee, or candidate.");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        // Create new user entity
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(role))
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Successfully registered user: {}", user.getUsername());

        if (roleName != RoleName.ROLE_CANDIDATE) {
            Employee employee = Employee.builder()
                    .user(user)
                    .employeeId(generateUniqueEmployeeId())
                    .firstName(user.getUsername())
                    .lastName("Staff")
                    .jobTitle(roleName == RoleName.ROLE_ADMIN ? "Administrator" : (roleName == RoleName.ROLE_HR ? "HR Specialist" : "Staff Member"))
                    .salary(BigDecimal.ZERO)
                    .hireDate(LocalDate.now())
                    .status(EmployeeStatus.ACTIVE)
                    .build();
            employeeRepository.save(employee);
            log.info("Automatically created employee profile for registered user: {}", user.getUsername());
        }

        // Publish event for registration email
        eventPublisher.publishEvent(new UserRegisteredEvent(user));

        // Generate tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(user);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(List.of(role.getName().name()))
                .build();
    }

    @Override
    @Transactional
    @Auditable(action = "USER_LOGIN", entity = "User")
    public JwtResponse login(LoginRequest request) {
        log.info("Processing login request for user: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        
        // Remove existing refresh token for this user and create a new one (rotation on login)
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();
        RefreshToken refreshToken = createRefreshToken(user);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public JwtResponse refresh(TokenRefreshRequest request) {
        String tokenStr = request.getRefreshToken();
        log.info("Processing refresh token request");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BadRequestException("Refresh token is not in database"));

        // Validate expiration
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("Refresh token was expired. Please sign in again.");
        }

        User user = refreshToken.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        
        // Generate new access token
        String newAccessToken = jwtUtils.generateAccessToken(userDetails);

        // Security best practice: Rotate refresh token upon reuse
        refreshTokenRepository.delete(refreshToken);
        refreshTokenRepository.flush(); // Force delete execution before insert to avoid UNIQUE constraint violation on user_id
        RefreshToken rotatedRefreshToken = createRefreshToken(user);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rotatedRefreshToken.getToken())
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        log.info("Processing password reset for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // Encode and set new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Security best practice: Revoke all active sessions on password change
        refreshTokenRepository.deleteByUser(user);
        log.info("Password reset successful and refresh tokens revoked for user: {}", user.getUsername());
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private String generateUniqueEmployeeId() {
        String employeeId;
        boolean exists = true;
        java.security.SecureRandom random = new java.security.SecureRandom();
        do {
            int code = 1000 + random.nextInt(9000);
            employeeId = "EMP-" + code;
            exists = employeeRepository.existsByEmployeeId(employeeId);
        } while (exists);
        return employeeId;
    }
}
