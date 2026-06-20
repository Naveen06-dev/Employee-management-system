package com.ems.modules.auth.service;

import com.ems.exception.DuplicateResourceException;
import com.ems.modules.auth.dto.RegisterRequest;
import com.ems.modules.auth.model.Role;
import com.ems.modules.auth.model.RoleName;
import com.ems.modules.auth.model.User;
import com.ems.modules.auth.repository.RefreshTokenRepository;
import com.ems.modules.auth.repository.RoleRepository;
import com.ems.modules.auth.repository.UserRepository;
import com.ems.security.JwtUtils;
import com.ems.common.event.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private com.ems.modules.employee.repository.EmployeeRepository employeeRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtils,
                eventPublisher,
                employeeRepository
        );
    }

    @Test
    void register_ShouldThrowException_WhenUsernameExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existingUser");
        request.setEmail("test@email.com");
        request.setPassword("password123");
        request.setRole("candidate");

        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setEmail("existing@email.com");
        request.setPassword("password123");
        request.setRole("candidate");

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldSaveUser_WhenRequestIsValid() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setEmail("new@email.com");
        request.setPassword("password123");
        request.setRole("candidate");

        Role role = Role.builder().id(1L).name(RoleName.ROLE_CANDIDATE).build();

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CANDIDATE)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        
        // Mock save
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock token generation
        when(jwtUtils.generateAccessToken(any())).thenReturn("mockedAccessToken");
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authService.register(request);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
        verify(eventPublisher, times(1)).publishEvent(any(UserRegisteredEvent.class));
    }
}
