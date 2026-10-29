package com.tapat.repository.sqlite;

import com.tapat.exception.TapatException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database implements AutoCloseable {
    private final Connection connection;

    private Database(Connection connection) {
        this.connection = connection;
    }

    public static Database open(Path databaseFile) {
        try {
            if (databaseFile.getParent() != null) {
                Files.createDirectories(databaseFile.getParent());
            }
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return new Database(connection);
        } catch (Exception ex) {
            throw new TapatException("Could not open the local database.", ex);
        }
    }

    public Connection connection() {
        return connection;
    }

    public void transaction(Runnable work) {
        try {
            connection.setAutoCommit(false);
            try {
                work.run();
                connection.commit();
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new TapatException("Could not save the financial record.", ex);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ex) {
            throw new TapatException("Could not close the local database.", ex);
        }
    }

    static TapatException failure(SQLException ex) {
        ex.printStackTrace();
        return new TapatException("The database could not complete that request.", ex);
    }

    static String money(BigDecimal amount) {
        return com.tapat.util.Money.store(amount);
    }
}
