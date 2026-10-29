package com.tapat.repository.sqlite;

import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;
import com.tapat.model.EligibilityStatus;
import com.tapat.repository.ApplicationRepository;
import com.tapat.util.Money;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SqliteApplicationRepository implements ApplicationRepository {
    private static final String SELECT = """
            SELECT a.id, a.reference_no, a.applicant_id, u.full_name AS applicant_name, a.program_id,
                   p.name AS program_name, a.status, a.household_income, a.purpose, a.requested_amount,
                   a.approved_amount, a.eligibility_status, a.eligibility_notes, a.duplicate_flag,
                   a.reviewer_id, a.review_notes, a.submitted_at, a.updated_at, a.claim_reference,
                   a.confirmed_at, c.full_name AS confirmed_by_name
            FROM applications a
            JOIN users u ON u.id = a.applicant_id
            JOIN programs p ON p.id = a.program_id
            LEFT JOIN users c ON c.id = a.confirmed_by
            """;
    private final Database database;

    public SqliteApplicationRepository(Database database) {
        this.database = database;
    }

    @Override
    public Application insert(Application draft) {
        String sql = """
                INSERT INTO applications (
                    reference_no, applicant_id, program_id, status, household_income, purpose, requested_amount,
                    approved_amount, eligibility_status, eligibility_notes, duplicate_flag, reviewer_id,
                    review_notes, submitted_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "TMP-" + UUID.randomUUID());
            statement.setInt(2, draft.getApplicantId());
            statement.setInt(3, draft.getProgramId());
            statement.setString(4, draft.getStatus().name());
            Sql.money(statement, 5, draft.getHouseholdIncome());
            statement.setString(6, draft.getPurpose());
            Sql.money(statement, 7, draft.getRequestedAmount());
            Sql.money(statement, 8, draft.getApprovedAmount());
            statement.setString(9, draft.getEligibilityStatus().name());
            Sql.text(statement, 10, draft.getEligibilityNotes());
            statement.setInt(11, draft.isDuplicateFlag() ? 1 : 0);
            Sql.integer(statement, 12, draft.getReviewerId());
            Sql.text(statement, 13, draft.getReviewNotes());
            Sql.dateTime(statement, 14, draft.getSubmittedAt());
            Sql.dateTime(statement, 15, draft.getUpdatedAt());
            statement.executeUpdate();
            int id;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
            }
            String reference = "TAPAT-" + draft.getSubmittedAt().getYear() + "-" + String.format("%05d", id);
            try (PreparedStatement update = database.connection().prepareStatement(
                    "UPDATE applications SET reference_no = ? WHERE id = ?")) {
                update.setString(1, reference);
                update.setInt(2, id);
                update.executeUpdate();
            }
            return findById(id).orElseThrow();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public void updateReview(int id, ApplicationStatus status, BigDecimal approvedAmount, Integer reviewerId, String reviewNotes) {
        String sql = """
                UPDATE applications
                SET status = ?, approved_amount = ?, reviewer_id = ?, review_notes = ?, updated_at = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, status.name());
            Sql.money(statement, 2, approvedAmount);
            Sql.integer(statement, 3, reviewerId);
            Sql.text(statement, 4, reviewNotes);
            Sql.dateTime(statement, 5, java.time.LocalDateTime.now());
            statement.setInt(6, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public void issueClaim(int id, String claimReference) {
        String sql = """
                UPDATE applications
                SET status = 'RELEASED', claim_reference = ?, updated_at = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, claimReference);
            Sql.dateTime(statement, 2, java.time.LocalDateTime.now());
            statement.setInt(3, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public void markVerified(int id, int officerId) {
        String sql = """
                UPDATE applications
                SET status = 'VERIFIED', confirmed_at = ?, confirmed_by = ?, updated_at = ?
                WHERE id = ?
                """;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            Sql.dateTime(statement, 1, now);
            statement.setInt(2, officerId);
            Sql.dateTime(statement, 3, now);
            statement.setInt(4, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    @Override
    public Optional<Application> findByClaim(String claimReference) {
        return list(SELECT + " WHERE a.claim_reference = ? COLLATE NOCASE", null, claimReference).stream().findFirst();
    }

    @Override
    public Optional<Application> findById(int id) {
        return one(SELECT + " WHERE a.id = ?", id);
    }

    @Override
    public List<Application> findByApplicant(int applicantId) {
        return list(SELECT + " WHERE a.applicant_id = ? ORDER BY a.submitted_at DESC", applicantId);
    }

    @Override
    public List<Application> findAll() {
        return list(SELECT + " ORDER BY a.submitted_at DESC", null);
    }

    @Override
    public List<Application> findByProgram(int programId) {
        return list(SELECT + " WHERE a.program_id = ? ORDER BY a.submitted_at DESC", programId);
    }

    @Override
    public int countApproved(int programId) {
        String sql = """
                SELECT COUNT(*) FROM applications
                WHERE program_id = ? AND status IN ('APPROVED', 'FOR_RELEASE', 'RELEASED', 'VERIFIED')
                """;
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, programId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    private Optional<Application> one(String sql, int id) {
        List<Application> found = list(sql, id, null);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    private List<Application> list(String sql, Integer id) {
        return list(sql, id, null);
    }

    private List<Application> list(String sql, Integer id, String claim) {
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            if (claim != null) {
                statement.setString(1, claim);
            } else if (id != null) {
                statement.setInt(1, id);
            }
            try (ResultSet result = statement.executeQuery()) {
                List<Application> applications = new ArrayList<>();
                while (result.next()) {
                    applications.add(map(result));
                }
                return applications;
            }
        } catch (SQLException ex) {
            throw Database.failure(ex);
        }
    }

    private Application map(ResultSet result) throws SQLException {
        return new Application(
                result.getInt("id"),
                result.getString("reference_no"),
                result.getInt("applicant_id"),
                result.getString("applicant_name"),
                result.getInt("program_id"),
                result.getString("program_name"),
                ApplicationStatus.valueOf(result.getString("status")),
                Money.load(result.getString("household_income")),
                result.getString("purpose"),
                Money.load(result.getString("requested_amount")),
                Money.load(result.getString("approved_amount")),
                EligibilityStatus.valueOf(result.getString("eligibility_status")),
                result.getString("eligibility_notes"),
                result.getInt("duplicate_flag") == 1,
                Sql.integer(result, "reviewer_id"),
                result.getString("review_notes"),
                Sql.dateTime(result, "submitted_at"),
                Sql.dateTime(result, "updated_at"),
                result.getString("claim_reference"),
                Sql.dateTime(result, "confirmed_at"),
                result.getString("confirmed_by_name"));
    }
}
