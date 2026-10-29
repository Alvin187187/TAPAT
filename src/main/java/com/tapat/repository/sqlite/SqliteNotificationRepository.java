package com.tapat.repository.sqlite;

import com.tapat.model.Notification;
import com.tapat.repository.NotificationRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SqliteNotificationRepository implements NotificationRepository {
    private final Database database;

    public SqliteNotificationRepository(Database database) {
        this.database = database;
    }

    @Override
    public void insert(int userId, String title, String message) {
        String sql = """
                INSERT INTO notifications (user_id, title, message, read_flag, created_at)
                VALUES (?, ?, ?, 0, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, title);
            statement.setString(3, message);
            Sql.dateTime(statement, 4, LocalDateTime.now());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public List<Notification> findByUser(int userId) {
        String sql = """
                SELECT id, user_id, title, message, read_flag, created_at
                FROM notifications WHERE user_id = ? ORDER BY created_at DESC, id DESC
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                List<Notification> notifications = new ArrayList<>();
                while (result.next()) {
                    notifications.add(new Notification(
                            result.getInt("id"),
                            result.getInt("user_id"),
                            result.getString("title"),
                            result.getString("message"),
                            result.getInt("read_flag") == 1,
                            Sql.dateTime(result, "created_at")));
                }
                return notifications;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public void markRead(int id, int userId) {
        String sql = "UPDATE notifications SET read_flag = 1 WHERE id = ? AND user_id = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public void markAllRead(int userId) {
        String sql = "UPDATE notifications SET read_flag = 1 WHERE user_id = ? AND read_flag = 0";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_flag = 0";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public boolean existsWithTitle(int userId, String title) {
        String sql = "SELECT 1 FROM notifications WHERE user_id = ? AND title = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setString(2, title);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }
}
