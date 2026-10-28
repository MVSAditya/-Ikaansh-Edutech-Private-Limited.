package com.ikaansh.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikaansh.taskmanager.domain.Project;
import com.ikaansh.taskmanager.domain.ProjectStatus;
import com.ikaansh.taskmanager.dto.ProjectRequest;
import com.ikaansh.taskmanager.exception.ResourceNotFoundException;
import com.ikaansh.taskmanager.repository.ProjectRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createPersistsProjectAndInitialStatus() {
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = projectService.create(new ProjectRequest("Website refresh", "Public site", ProjectStatus.PLANNING));

        assertEquals("Website refresh", result.name());
        assertEquals(ProjectStatus.PLANNING, result.status());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void updateAdvancesProjectLifecycle() {
        Project project = new Project("Website refresh", "Public site", ProjectStatus.PLANNING);
        when(projectRepository.findById(4L)).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        var result = projectService.update(4L,
                new ProjectRequest("Website refresh", "Public site", ProjectStatus.ACTIVE));

        assertEquals(ProjectStatus.ACTIVE, result.status());
    }

    @Test
    void findByIdRejectsMissingProject() {
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.findById(404L));
    }

    @Test
    void deleteRemovesProject() {
        Project project = new Project("Website refresh", "Public site", ProjectStatus.ACTIVE);
        when(projectRepository.findById(4L)).thenReturn(Optional.of(project));

        projectService.delete(4L);

        verify(projectRepository).delete(project);
    }
}