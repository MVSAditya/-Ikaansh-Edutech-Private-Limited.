package com.ikaansh.taskmanager.repository;

import com.ikaansh.taskmanager.domain.Task;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    Page<Task> findByAssignedEmployeeId(Long employeeId, Pageable pageable);
    Optional<Task> findByIdAndAssignedEmployeeId(Long id, Long employeeId);
}