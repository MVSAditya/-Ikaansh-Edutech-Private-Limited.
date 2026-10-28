package com.ikaansh.taskmanager.dto;

import com.ikaansh.taskmanager.domain.Employee;
import java.time.Instant;

public record EmployeeResponse(Long id, String name, String email, String department,
                               Instant createdAt, Instant updatedAt) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getName(), employee.getEmail(),
                employee.getDepartment(), employee.getCreatedAt(), employee.getUpdatedAt());
    }
}