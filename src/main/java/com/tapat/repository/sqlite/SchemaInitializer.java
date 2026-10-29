package com.tapat.repository.sqlite;

import java.sql.Statement;

public class SchemaInitializer {
    private static final String[] STATEMENTS = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY,
                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                full_name TEXT NOT NULL,
                email TEXT,
                phone TEXT,
                address TEXT,
                role TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS programs (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                eligibility_summary TEXT NOT NULL,
                required_documents TEXT NOT NULL,
                instructions TEXT,
                deadline TEXT,
                max_assistance TEXT,
                income_limit TEXT,
                status TEXT NOT NULL,
                created_by INTEGER,
                created_at TEXT NOT NULL,
                FOREIGN KEY (created_by) REFERENCES users(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS applications (
                id INTEGER PRIMARY KEY,
                reference_no TEXT NOT NULL UNIQUE,
                applicant_id INTEGER NOT NULL,
                program_id INTEGER NOT NULL,
                status TEXT NOT NULL,
                household_income TEXT,
                purpose TEXT NOT NULL,
                requested_amount TEXT,
                approved_amount TEXT,
                eligibility_status TEXT NOT NULL,
                eligibility_notes TEXT,
                duplicate_flag INTEGER NOT NULL DEFAULT 0,
                reviewer_id INTEGER,
                review_notes TEXT,
                submitted_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                FOREIGN KEY (applicant_id) REFERENCES users(id),
                FOREIGN KEY (program_id) REFERENCES programs(id),
                FOREIGN KEY (reviewer_id) REFERENCES users(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS application_documents (
                id INTEGER PRIMARY KEY,
                application_id INTEGER NOT NULL,
                document_name TEXT NOT NULL,
                stored_path TEXT NOT NULL,
                uploaded_at TEXT NOT NULL,
                FOREIGN KEY (application_id) REFERENCES applications(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS fund_entries (
                id INTEGER PRIMARY KEY,
                program_id INTEGER NOT NULL,
                entry_type TEXT NOT NULL,
                amount TEXT NOT NULL,
                application_id INTEGER,
                remarks TEXT,
                recorded_by INTEGER NOT NULL,
                recorded_at TEXT NOT NULL,
                FOREIGN KEY (program_id) REFERENCES programs(id),
                FOREIGN KEY (application_id) REFERENCES applications(id),
                FOREIGN KEY (recorded_by) REFERENCES users(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS notifications (
                id INTEGER PRIMARY KEY,
                user_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                read_flag INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY,
                actor_id INTEGER,
                action TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                entity_id INTEGER,
                details TEXT,
                created_at TEXT NOT NULL
            )
            """
    };

    private SchemaInitializer() {
    }

    public static void apply(Database database) {
        try (Statement statement = database.connection().createStatement()) {
            for (String sql : STATEMENTS) {
                statement.execute(sql);
            }
            addColumn(statement, "users", "date_of_birth", "TEXT");
            addColumn(statement, "users", "barangay", "TEXT");
            addColumn(statement, "applications", "claim_reference", "TEXT");
            addColumn(statement, "applications", "confirmed_at", "TEXT");
            addColumn(statement, "applications", "confirmed_by", "INTEGER");
        } catch (Exception ex) {
            throw new com.tapat.exception.TapatException("Could not prepare the local database.", ex);
        }
    }

    private static void addColumn(java.sql.Statement statement, String table, String column, String type) {
        try {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (java.sql.SQLException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (!message.contains("duplicate column")) {
                throw new com.tapat.exception.TapatException("Could not prepare the local database.", ex);
            }
        }
    }
}
