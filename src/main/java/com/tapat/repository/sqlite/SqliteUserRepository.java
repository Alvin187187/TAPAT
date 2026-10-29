package com.tapat.repository.sqlite;

import com.tapat.model.LoginRecord;
import com.tapat.model.Role;
import com.tapat.model.User;
import com.tapat.model.UserFactory;
import com.tapat.repository.UserRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteUserRepository implements UserRepository {
    private final Database database;

    public SqliteUserRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<LoginRecord> findLogin(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new LoginRecord(map(result), result.getString("password_hash"), result.getString("salt")));
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
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
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public User insert(String username, String passwordHash, String salt, String fullName, String email,
                       String phone, String address, Role role) {
        String sql = """
                INSERT INTO users (username, password_hash, salt, full_name, email, phone, address, role, active, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, salt);
            statement.setString(4, fullName);
            Sql.text(statement, 5, email);
            Sql.text(statement, 6, phone);
            Sql.text(statement, 7, address);
            statement.setString(8, role.name());
            Sql.dateTime(statement, 9, LocalDateTime.now());
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
    public List<User> findAll() {
        return query("SELECT * FROM users ORDER BY full_name");
    }

    @Override
    public List<User> findByRole(Role role) {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, role.name());
            try (ResultSet result = statement.executeQuery()) {
                return collect(result);
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Statement statement = database.connection().createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public java.util.List<com.tapat.model.PersonRecord> people() {
        String sql = "SELECT id, full_name, phone, address, date_of_birth, barangay FROM users WHERE role = 'APPLICANT'";
        try (Statement statement = database.connection().createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            java.util.List<com.tapat.model.PersonRecord> people = new ArrayList<>();
            while (result.next()) {
                people.add(new com.tapat.model.PersonRecord(
                        result.getInt("id"),
                        result.getString("full_name"),
                        result.getString("phone"),
                        result.getString("address"),
                        Sql.date(result, "date_of_birth"),
                        result.getString("barangay")));
            }
            return people;
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public void saveProfile(int userId, java.time.LocalDate dateOfBirth, String barangay) {
        String sql = "UPDATE users SET date_of_birth = ?, barangay = ? WHERE id = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            Sql.date(statement, 1, dateOfBirth);
            Sql.text(statement, 2, barangay);
            statement.setInt(3, userId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    private List<User> query(String sql) {
        try (Statement statement = database.connection().createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return collect(result);
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    private List<User> collect(ResultSet result) throws SQLException {
        List<User> users = new ArrayList<>();
        while (result.next()) {
            users.add(map(result));
        }
        return users;
    }

    private User map(ResultSet result) throws SQLException {
        return UserFactory.create(
                result.getInt("id"),
                result.getString("username"),
                result.getString("full_name"),
                result.getString("email"),
                result.getString("phone"),
                result.getString("address"),
                result.getInt("active") == 1,
                Role.fromStored(result.getString("role")));
    }
}
