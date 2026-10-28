package com.ikaansh.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikaansh.taskmanager.domain.AppUser;
import com.ikaansh.taskmanager.domain.Employee;
import com.ikaansh.taskmanager.domain.Role;
import com.ikaansh.taskmanager.dto.LoginRequest;
import com.ikaansh.taskmanager.dto.RegisterRequest;
import com.ikaansh.taskmanager.repository.AppUserRepository;
import com.ikaansh.taskmanager.repository.EmployeeRepository;
import com.ikaansh.taskmanager.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final JwtService jwtService = new JwtService("01234567890123456789012345678901", 3600000);

    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, employeeRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerPersistsEmployeeAccountAndReturnsToken() {
        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = authService.register(new RegisterRequest("Asha Rao", "asha@example.com",
            "Engineering", "SecurePass123!"));

        assertEquals("asha@example.com", jwtService.extractEmail(response.accessToken()));
        assertEquals("Bearer", response.tokenType());
        assertEquals(Role.EMPLOYEE, response.role());
        verify(userRepository).save(any(AppUser.class));
        verify(employeeRepository).save(any(Employee.class));
        verify(passwordEncoder).encode("SecurePass123!");
    }

    @Test
    void loginIssuesTokenForPersistedMatchingCredentials() {
        AppUser user = new AppUser("asha@example.com", "bcrypt-hash", Role.EMPLOYEE);
        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass123!", "bcrypt-hash")).thenReturn(true);
        var response = authService.login(new LoginRequest("asha@example.com", "SecurePass123!"));

        assertEquals("asha@example.com", jwtService.extractEmail(response.accessToken()));
        assertEquals(Role.EMPLOYEE, response.role());
        verify(passwordEncoder).matches("SecurePass123!", "bcrypt-hash");
    }
}