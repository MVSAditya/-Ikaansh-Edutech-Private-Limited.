package com.ikaansh.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikaansh.taskmanager.domain.Employee;
import com.ikaansh.taskmanager.domain.Project;
import com.ikaansh.taskmanager.domain.ProjectStatus;
import com.ikaansh.taskmanager.domain.Task;
import com.ikaansh.taskmanager.domain.TaskPriority;
import com.ikaansh.taskmanager.domain.TaskStatus;
import com.ikaansh.taskmanager.dto.TaskRequest;
import com.ikaansh.taskmanager.exception.ResourceNotFoundException;
import com.ikaansh.taskmanager.repository.EmployeeRepository;
import com.ikaansh.taskmanager.repository.ProjectRepository;
import com.ikaansh.taskmanager.repository.TaskRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createResolvesAssignedEmployeeAndPersistsTask() {
        Employee employee = new Employee("Asha Rao", "asha@example.com", "Engineering");
        Task task = new Task("Ship API", "Implement endpoints", TaskStatus.TODO, TaskPriority.HIGH,
                LocalDate.of(2026, 11, 1), employee);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        var result = taskService.create(new TaskRequest("Ship API", "Implement endpoints", TaskStatus.TODO,
            TaskPriority.HIGH, LocalDate.of(2026, 11, 1), null, 7L));

        assertEquals("Ship API", result.title());
        assertEquals("Asha Rao", result.assignedEmployeeName());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createRejectsUnknownAssignee() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.create(new TaskRequest("Ship API", null,
            TaskStatus.TODO, TaskPriority.MEDIUM, null, null, 99L)));
    }

    @Test
    void updateChangesTaskAndAssignee() {
        Employee originalEmployee = new Employee("Asha Rao", "asha@example.com", "Engineering");
        Employee newEmployee = new Employee("Dev Shah", "dev@example.com", "Platform");
        Task task = new Task("Ship API", "Implement endpoints", TaskStatus.TODO, TaskPriority.MEDIUM,
                null, originalEmployee);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(employeeRepository.findById(8L)).thenReturn(Optional.of(newEmployee));
        when(taskRepository.save(task)).thenReturn(task);

        var result = taskService.update(5L, new TaskRequest("Ship v2", "Release endpoints", TaskStatus.DONE,
            TaskPriority.HIGH, LocalDate.of(2026, 11, 5), null, 8L));

        assertEquals("Ship v2", result.title());
        assertEquals(TaskStatus.DONE, result.status());
        assertEquals("Dev Shah", result.assignedEmployeeName());
    }

    @Test
    void createAssignsTaskToProject() {
        Employee employee = new Employee("Asha Rao", "asha@example.com", "Engineering");
        Project project = new Project("Website refresh", "Public site", ProjectStatus.ACTIVE);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(3L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = taskService.create(new TaskRequest("Ship API", null, TaskStatus.TODO,
                TaskPriority.MEDIUM, null, 3L, 7L));

        assertEquals("Website refresh", result.projectName());
        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void deleteRemovesExistingTask() {
        Employee employee = new Employee("Asha Rao", "asha@example.com", "Engineering");
        Task task = new Task("Ship API", null, TaskStatus.TODO, TaskPriority.LOW, null, employee);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        taskService.delete(5L);

        verify(taskRepository).delete(task);
    }
}