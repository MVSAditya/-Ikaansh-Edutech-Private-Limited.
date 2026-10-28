package com.ikaansh.taskmanager.dto;

import com.ikaansh.taskmanager.domain.Task;
import com.ikaansh.taskmanager.domain.TaskPriority;
import com.ikaansh.taskmanager.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(Long id, String title, String description, TaskStatus status,
                           TaskPriority priority, LocalDate dueDate, Long assignedEmployeeId,
                           String assignedEmployeeName, Instant createdAt, Instant updatedAt) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getPriority(), task.getDueDate(), task.getAssignedEmployee().getId(),
                task.getAssignedEmployee().getName(), task.getCreatedAt(), task.getUpdatedAt());
    }
}