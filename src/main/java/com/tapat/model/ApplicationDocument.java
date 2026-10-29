package com.tapat.model;

import java.time.LocalDateTime;

public class ApplicationDocument {
    private final int id;
    private final int applicationId;
    private final String documentName;
    private final String storedPath;
    private final LocalDateTime uploadedAt;

    public ApplicationDocument(int id, int applicationId, String documentName, String storedPath, LocalDateTime uploadedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.documentName = documentName;
        this.storedPath = storedPath;
        this.uploadedAt = uploadedAt;
    }

    public int getId() {
        return id;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
