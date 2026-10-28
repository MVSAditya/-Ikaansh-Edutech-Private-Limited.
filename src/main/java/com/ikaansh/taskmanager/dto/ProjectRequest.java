package com.ikaansh.taskmanager.dto;

import com.ikaansh.taskmanager.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 5000) String description,
        @NotNull ProjectStatus status) {
}