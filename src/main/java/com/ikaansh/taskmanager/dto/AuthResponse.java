package com.ikaansh.taskmanager.dto;

import com.ikaansh.taskmanager.domain.Role;

public record AuthResponse(String accessToken, String tokenType, Role role) {
}