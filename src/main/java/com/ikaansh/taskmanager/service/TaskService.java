package com.ikaansh.taskmanager.service;

import com.ikaansh.taskmanager.domain.Employee;
import com.ikaansh.taskmanager.domain.Project;
import com.ikaansh.taskmanager.domain.Task;
import com.ikaansh.taskmanager.domain.TaskPriority;
import com.ikaansh.taskmanager.domain.TaskStatus;
import com.ikaansh.taskmanager.dto.TaskRequest;
import com.ikaansh.taskmanager.dto.TaskResponse;
import com.ikaansh.taskmanager.exception.ResourceNotFoundException;
import com.ikaansh.taskmanager.repository.EmployeeRepository;
import com.ikaansh.taskmanager.repository.ProjectRepository;
import com.ikaansh.taskmanager.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, EmployeeRepository employeeRepository,
                       ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
    }

    public TaskResponse create(TaskRequest request) {
        Employee employee = getEmployee(request.assignedEmployeeId());
        Task task = new Task(request.title(), request.description(), request.status(), request.priority(),
                request.dueDate(), employee);
        task.setProject(getProject(request.projectId()));
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(Pageable pageable, boolean canViewAll, String email,
                                      TaskStatus status, TaskPriority priority, Long projectId) {
        Long employeeId = canViewAll ? null : getEmployeeByEmail(email).getId();
        Specification<Task> specification = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (employeeId != null) {
                predicates.add(builder.equal(root.get("assignedEmployee").get("id"), employeeId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(builder.equal(root.get("priority"), priority));
            }
            if (projectId != null) {
                predicates.add(builder.equal(root.get("project").get("id"), projectId));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Page<Task> tasks = taskRepository.findAll(specification, pageable);
        return tasks.map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id, boolean canViewAll, String email) {
        Task task;
        if (canViewAll) {
            task = taskRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Task " + id + " was not found."));
        } else {
            Long employeeId = getEmployeeByEmail(email).getId();
            task = taskRepository.findByIdAndAssignedEmployeeId(id, employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task " + id + " was not found."));
        }
        return TaskResponse.from(task);
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + id + " was not found."));
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setProject(getProject(request.projectId()));
        task.setAssignedEmployee(getEmployee(request.assignedEmployeeId()));
        return TaskResponse.from(taskRepository.save(task));
    }

    public void delete(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + id + " was not found."));
        taskRepository.delete(task);
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee " + id + " was not found."));
    }

    private Employee getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee for account " + email + " was not found."));
    }

    private Project getProject(Long id) {
        if (id == null) {
            return null;
        }
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " was not found."));
    }
}