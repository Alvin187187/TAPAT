package com.tapat.repository;

import com.tapat.model.ApplicationDocument;

import java.util.List;

public interface DocumentRepository {
    void insert(int applicationId, String documentName, String storedPath);

    List<ApplicationDocument> findByApplication(int applicationId);
}
