package com.ikaansh.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.ikaansh.taskmanager.domain.Employee;
import com.ikaansh.taskmanager.dto.EmployeeRequest;
import com.ikaansh.taskmanager.exception.ResourceNotFoundException;
import com.ikaansh.taskmanager.repository.EmployeeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createPersistsAndReturnsEmployee() {
        Employee employee = new Employee("Asha Rao", "asha@example.com", "Engineering");
        when(employeeRepository.save(org.mockito.ArgumentMatchers.any(Employee.class))).thenReturn(employee);

        var result = employeeService.create(new EmployeeRequest("Asha Rao", "asha@example.com", "Engineering"));

        assertEquals("asha@example.com", result.email());
        assertEquals("Engineering", result.department());
        verify(employeeRepository).save(org.mockito.ArgumentMatchers.any(Employee.class));
    }

    @Test
    void findByIdThrowsWhenEmployeeDoesNotExist() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.findById(99L));
    }

    @Test
    void updateChangesEmployeeFields() {
        Employee employee = new Employee("Asha Rao", "asha@example.com", "Engineering");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);

        var result = employeeService.update(1L, new EmployeeRequest("Asha M", "asha.m@example.com", "Platform"));

        assertEquals("Asha M", result.name());
        assertEquals("asha.m@example.com", result.email());
        assertEquals("Platform", result.department());
    }

    @Test
    void deleteRemovesExistingEmployee() {
        Employee employee = new Employee("Asha Rao", "asha@example.com", "Engineering");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.delete(1L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void deleteDoesNotRemoveMissingEmployee() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.delete(99L));
        verify(employeeRepository, never()).delete(org.mockito.ArgumentMatchers.any(Employee.class));
    }
}