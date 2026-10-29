package com.tapat.repository.sqlite;

import com.tapat.model.FundEntry;
import com.tapat.model.FundEntryType;
import com.tapat.repository.FundRepository;
import com.tapat.util.Money;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SqliteFundRepository implements FundRepository {
    private static final String SELECT = """
            SELECT f.id, f.program_id, p.name AS program_name, f.entry_type, f.amount, f.application_id,
                   a.reference_no AS application_reference, au.full_name AS applicant_name, f.remarks,
                   f.recorded_by, ru.full_name AS recorded_by_name, f.recorded_at
            FROM fund_entries f
            JOIN programs p ON p.id = f.program_id
            LEFT JOIN applications a ON a.id = f.application_id
            LEFT JOIN users au ON au.id = a.applicant_id
            JOIN users ru ON ru.id = f.recorded_by
            """;
    private final Database database;

    public SqliteFundRepository(Database database) {
        this.database = database;
    }

    @Override
    public FundEntry insert(int programId, FundEntryType entryType, BigDecimal amount, Integer applicationId,
                            String remarks, int recordedBy) {
        String sql = """
                INSERT INTO fund_entries (program_id, entry_type, amount, application_id, remarks, recorded_by, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, programId);
            statement.setString(2, entryType.name());
            Sql.money(statement, 3, amount);
            Sql.integer(statement, 4, applicationId);
            Sql.text(statement, 5, remarks);
            statement.setInt(6, recordedBy);
            Sql.dateTime(statement, 7, LocalDateTime.now());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getInt(1));
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public List<FundEntry> findAll() {
        try (Statement statement = database.connection().createStatement();
             ResultSet result = statement.executeQuery(SELECT + " ORDER BY f.recorded_at DESC, f.id DESC")) {
            return collect(result);
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public BigDecimal sum(int programId, FundEntryType entryType) {
        String sql = "SELECT amount FROM fund_entries WHERE program_id = ? AND entry_type = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, programId);
            statement.setString(2, entryType.name());
            try (ResultSet result = statement.executeQuery()) {
                BigDecimal total = Money.ZERO;
                while (result.next()) {
                    total = total.add(Money.zeroIfNull(Money.load(result.getString("amount"))));
                }
                return total;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public BigDecimal sumDisbursedForApplication(int applicationId) {
        String sql = "SELECT amount FROM fund_entries WHERE application_id = ? AND entry_type = 'DISBURSEMENT'";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, applicationId);
            try (ResultSet result = statement.executeQuery()) {
                BigDecimal total = Money.ZERO;
                while (result.next()) {
                    total = total.add(Money.zeroIfNull(Money.load(result.getString("amount"))));
                }
                return total;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    private FundEntry findById(int id) throws SQLException {
        try (PreparedStatement statement = database.connection().prepareStatement(SELECT + " WHERE f.id = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Saved fund entry was not found.");
                }
                return map(result);
            }
        }
    }

    private List<FundEntry> collect(ResultSet result) throws SQLException {
        List<FundEntry> entries = new ArrayList<>();
        while (result.next()) {
            entries.add(map(result));
        }
        return entries;
    }

    private FundEntry map(ResultSet result) throws SQLException {
        return new FundEntry(
                result.getInt("id"),
                result.getInt("program_id"),
                result.getString("program_name"),
                FundEntryType.valueOf(result.getString("entry_type")),
                Money.load(result.getString("amount")),
                Sql.integer(result, "application_id"),
                result.getString("application_reference"),
                result.getString("applicant_name"),
                result.getString("remarks"),
                result.getInt("recorded_by"),
                result.getString("recorded_by_name"),
                Sql.dateTime(result, "recorded_at"));
    }
}
