package com.ikaansh.taskmanager.service;

import com.ikaansh.taskmanager.domain.AppUser;
import com.ikaansh.taskmanager.domain.Employee;
import com.ikaansh.taskmanager.domain.Role;
import com.ikaansh.taskmanager.dto.AuthResponse;
import com.ikaansh.taskmanager.dto.LoginRequest;
import com.ikaansh.taskmanager.dto.RegisterRequest;
import com.ikaansh.taskmanager.exception.InvalidCredentialsException;
import com.ikaansh.taskmanager.repository.AppUserRepository;
import com.ikaansh.taskmanager.repository.EmployeeRepository;
import com.ikaansh.taskmanager.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final AppUserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository, EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("An account with this email already exists.");
        }
        AppUser user = userRepository.save(new AppUser(request.email(),
                passwordEncoder.encode(request.password()), Role.EMPLOYEE));
        employeeRepository.save(new Employee(request.name(), request.email(), request.department()));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return tokenFor(user);
    }

    private AuthResponse tokenFor(AppUser user) {
        return new AuthResponse(jwtService.generateToken(user.getEmail()), "Bearer", user.getRole());
    }
}