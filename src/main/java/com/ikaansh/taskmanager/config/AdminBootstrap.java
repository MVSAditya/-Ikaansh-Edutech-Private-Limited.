package com.ikaansh.taskmanager.config;

import com.ikaansh.taskmanager.domain.AppUser;
import com.ikaansh.taskmanager.domain.Role;
import com.ikaansh.taskmanager.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {

    @Bean
    ApplicationRunner createBootstrapAdmin(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        return args -> {
            if (email.isBlank()) {
                return;
            }
            if (password.isBlank()) {
                throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD is required when an admin email is configured.");
            }
            if (!userRepository.existsByEmail(email)) {
                userRepository.save(new AppUser(email, passwordEncoder.encode(password), Role.ADMIN));
            }
        };
    }
}