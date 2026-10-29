package com.tapat.repository.sqlite;

import com.tapat.model.Program;
import com.tapat.model.ProgramCategory;
import com.tapat.model.ProgramStatus;
import com.tapat.repository.ProgramRepository;
import com.tapat.util.Money;
import com.tapat.util.Text;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteProgramRepository implements ProgramRepository {
    private static final String SELECT = """
            SELECT id, name, category, description, eligibility_summary, required_documents, instructions,
                   deadline, max_assistance, income_limit, status, created_by, created_at
            FROM programs
            """;
    private final Database database;

    public SqliteProgramRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<Program> findAll() {
        return query(SELECT + " ORDER BY name");
    }

    @Override
    public List<Program> findOpen() {
        return query(SELECT + " WHERE status = 'OPEN' ORDER BY deadline, name");
    }

    @Override
    public Optional<Program> findById(int id) {
        String sql = SELECT + " WHERE id = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public Program insert(Program program) {
        String sql = """
                INSERT INTO programs (name, category, description, eligibility_summary, required_documents,
                    instructions, deadline, max_assistance, income_limit, status, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, program);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getInt(1)).orElseThrow();
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public void update(Program program) {
        String sql = """
                UPDATE programs
                SET name = ?, category = ?, description = ?, eligibility_summary = ?, required_documents = ?,
                    instructions = ?, deadline = ?, max_assistance = ?, income_limit = ?, status = ?, created_by = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            bind(statement, program);
            statement.setInt(12, program.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    private void bind(PreparedStatement statement, Program program) throws SQLException {
        statement.setString(1, program.getName());
        statement.setString(2, program.getCategory().name());
        statement.setString(3, program.getDescription());
        statement.setString(4, program.getEligibilitySummary());
        statement.setString(5, Text.joinLines(program.getRequiredDocuments()));
        Sql.text(statement, 6, program.getInstructions());
        Sql.date(statement, 7, program.getDeadline());
        Sql.money(statement, 8, program.getMaxAssistance());
        Sql.money(statement, 9, program.getIncomeLimit());
        statement.setString(10, program.getStatus().name());
        Sql.integer(statement, 11, program.getCreatedBy());
        if (program.getId() == 0) {
            Sql.dateTime(statement, 12, program.getCreatedAt());
        }
    }

    private List<Program> query(String sql) {
        try (Statement statement = database.connection().createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<Program> programs = new ArrayList<>();
            while (result.next()) {
                programs.add(map(result));
            }
            return programs;
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    private Program map(ResultSet result) throws SQLException {
        return new Program(
                result.getInt("id"),
                result.getString("name"),
                ProgramCategory.valueOf(result.getString("category")),
                result.getString("description"),
                result.getString("eligibility_summary"),
                Text.lines(result.getString("required_documents")),
                result.getString("instructions"),
                Sql.date(result, "deadline"),
                Money.load(result.getString("max_assistance")),
                Money.load(result.getString("income_limit")),
                ProgramStatus.valueOf(result.getString("status")),
                Sql.integer(result, "created_by"),
                Sql.dateTime(result, "created_at"));
    }
}
