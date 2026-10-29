package com.tapat.repository;

import com.tapat.model.AuditLog;

public interface AuditRepository {
    void insert(Integer actorId, String action, String entityType, Integer entityId, String details);

    java.util.List<AuditLog> findRecent(int limit);

    java.util.List<AuditLog> findFor(String entityType, int entityId);
}
