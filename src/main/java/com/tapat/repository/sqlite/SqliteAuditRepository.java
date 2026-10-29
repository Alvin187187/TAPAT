package com.tapat.repository.sqlite;

import com.tapat.model.AuditLog;
import com.tapat.repository.AuditRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SqliteAuditRepository implements AuditRepository {
    private final Database database;

    public SqliteAuditRepository(Database database) {
        this.database = database;
    }

    @Override
    public void insert(Integer actorId, String action, String entityType, Integer entityId, String details) {
        String sql = """
                INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            Sql.integer(statement, 1, actorId);
            statement.setString(2, action);
            statement.setString(3, entityType);
            Sql.integer(statement, 4, entityId);
            Sql.text(statement, 5, details);
            Sql.dateTime(statement, 6, LocalDateTime.now());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        String sql = """
                SELECT id, actor_id, action, entity_type, entity_id, details, created_at
                FROM audit_logs ORDER BY created_at DESC, id DESC LIMIT ?
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                List<AuditLog> logs = new ArrayList<>();
                while (result.next()) {
                    logs.add(new AuditLog(
                            result.getInt("id"),
                            Sql.integer(result, "actor_id"),
                            result.getString("action"),
                            result.getString("entity_type"),
                            Sql.integer(result, "entity_id"),
                            result.getString("details"),
                            Sql.dateTime(result, "created_at")));
                }
                return logs;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public List<AuditLog> findFor(String entityType, int entityId) {
        String sql = """
                SELECT id, actor_id, action, entity_type, entity_id, details, created_at
                FROM audit_logs
                WHERE entity_type = ? AND entity_id = ?
                ORDER BY created_at, id
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, entityType);
            statement.setInt(2, entityId);
            try (ResultSet result = statement.executeQuery()) {
                List<AuditLog> logs = new ArrayList<>();
                while (result.next()) {
                    logs.add(new AuditLog(
                            result.getInt("id"),
                            Sql.integer(result, "actor_id"),
                            result.getString("action"),
                            result.getString("entity_type"),
                            Sql.integer(result, "entity_id"),
                            result.getString("details"),
                            Sql.dateTime(result, "created_at")));
                }
                return logs;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }
}
