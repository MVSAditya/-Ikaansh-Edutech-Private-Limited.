CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_projects_name UNIQUE (name),
    INDEX idx_projects_status (status)
);

ALTER TABLE tasks ADD COLUMN project_id BIGINT NULL;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_project
    FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE SET NULL;
CREATE INDEX idx_tasks_project ON tasks (project_id);