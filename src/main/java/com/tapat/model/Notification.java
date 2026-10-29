package com.tapat.model;

import java.time.LocalDateTime;

public class Notification {
    private final int id;
    private final int userId;
    private final String title;
    private final String message;
    private final boolean read;
    private final LocalDateTime createdAt;

    public Notification(int id, int userId, String title, String message, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
