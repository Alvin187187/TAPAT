package com.tapat.repository;

import com.tapat.model.Notification;

import java.util.List;

public interface NotificationRepository {
    void insert(int userId, String title, String message);

    List<Notification> findByUser(int userId);

    void markRead(int id, int userId);

    void markAllRead(int userId);

    int countUnread(int userId);

    boolean existsWithTitle(int userId, String title);
}
