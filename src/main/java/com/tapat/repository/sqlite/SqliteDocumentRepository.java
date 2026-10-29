package com.tapat.repository.sqlite;

import com.tapat.model.ApplicationDocument;
import com.tapat.repository.DocumentRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SqliteDocumentRepository implements DocumentRepository {
    private final Database database;

    public SqliteDocumentRepository(Database database) {
        this.database = database;
    }

    @Override
    public void insert(int applicationId, String documentName, String storedPath) {
        String sql = """
                INSERT INTO application_documents (application_id, document_name, stored_path, uploaded_at)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, applicationId);
            statement.setString(2, documentName);
            statement.setString(3, storedPath);
            Sql.dateTime(statement, 4, LocalDateTime.now());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public List<ApplicationDocument> findByApplication(int applicationId) {
        String sql = """
                SELECT id, application_id, document_name, stored_path, uploaded_at
                FROM application_documents WHERE application_id = ? ORDER BY document_name
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, applicationId);
            try (ResultSet result = statement.executeQuery()) {
                List<ApplicationDocument> documents = new ArrayList<>();
                while (result.next()) {
                    documents.add(new ApplicationDocument(
                            result.getInt("id"),
                            result.getInt("application_id"),
                            result.getString("document_name"),
                            result.getString("stored_path"),
                            Sql.dateTime(result, "uploaded_at")));
                }
                return documents;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }
}
