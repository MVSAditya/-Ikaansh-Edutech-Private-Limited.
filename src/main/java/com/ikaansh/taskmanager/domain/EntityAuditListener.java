package com.ikaansh.taskmanager.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityAuditListener {

    private static final Logger logger = LoggerFactory.getLogger(EntityAuditListener.class);

    @jakarta.persistence.PostPersist
    public void onCreate(Object entity) {
        logEvent("create", entity);
    }

    @jakarta.persistence.PostUpdate
    public void onUpdate(Object entity) {
        logEvent("update", entity);
    }

    @jakarta.persistence.PostRemove
    public void onDelete(Object entity) {
        logEvent("delete", entity);
    }

    private void logEvent(String action, Object entity) {
        if (entity instanceof Employee employee) {
            logger.info("audit action={} entity=employee id={}", action, employee.getId());
        } else if (entity instanceof Project project) {
            logger.info("audit action={} entity=project id={}", action, project.getId());
        } else if (entity instanceof Task task) {
            logger.info("audit action={} entity=task id={}", action, task.getId());
        }
    }
}