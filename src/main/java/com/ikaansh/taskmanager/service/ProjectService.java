package com.ikaansh.taskmanager.service;

import com.ikaansh.taskmanager.domain.Project;
import com.ikaansh.taskmanager.dto.ProjectRequest;
import com.ikaansh.taskmanager.dto.ProjectResponse;
import com.ikaansh.taskmanager.exception.ResourceNotFoundException;
import com.ikaansh.taskmanager.repository.ProjectRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project(request.name(), request.description(), request.status());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        return projectRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        return ProjectResponse.from(getProject(id));
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getProject(id);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(request.status());
        return ProjectResponse.from(projectRepository.save(project));
    }

    public void delete(Long id) {
        projectRepository.delete(getProject(id));
    }

    private Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " was not found."));
    }
}