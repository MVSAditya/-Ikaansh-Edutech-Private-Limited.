package com.ikaansh.taskmanager.service;

import com.ikaansh.taskmanager.domain.Employee;
import com.ikaansh.taskmanager.dto.EmployeeRequest;
import com.ikaansh.taskmanager.dto.EmployeeResponse;
import com.ikaansh.taskmanager.exception.ResourceNotFoundException;
import com.ikaansh.taskmanager.repository.EmployeeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public EmployeeResponse create(EmployeeRequest request) {
        Employee employee = new Employee(request.name(), request.email(), request.department());
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAll().stream().map(EmployeeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return EmployeeResponse.from(getEmployee(id));
    }

    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = getEmployee(id);
        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setDepartment(request.department());
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    public void delete(Long id) {
        employeeRepository.delete(getEmployee(id));
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " was not found."));
    }
}