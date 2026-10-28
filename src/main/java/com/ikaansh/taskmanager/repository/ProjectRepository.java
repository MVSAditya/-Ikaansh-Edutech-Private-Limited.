package com.ikaansh.taskmanager.repository;

import com.ikaansh.taskmanager.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}