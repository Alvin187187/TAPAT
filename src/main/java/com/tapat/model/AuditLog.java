package com.tapat.model;

import java.time.LocalDateTime;

public class AuditLog {
    private final int id;
    private final Integer actorId;
    private final String action;
    private final String entityType;
    private final Integer entityId;
    private final String details;
    private final LocalDateTime createdAt;

    public AuditLog(int id, Integer actorId, String action, String entityType, Integer entityId,
                    String details, LocalDateTime createdAt) {
        this.id = id;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public Integer getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
