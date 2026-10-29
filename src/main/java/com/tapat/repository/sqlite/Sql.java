package com.tapat.repository.sqlite;

import com.tapat.util.Money;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

class Sql {
    private Sql() {
    }

    static void text(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    static void money(PreparedStatement statement, int index, BigDecimal value) throws SQLException {
        text(statement, index, Money.store(value));
    }

    static void integer(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    static void date(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        text(statement, index, value == null ? null : value.toString());
    }

    static void dateTime(PreparedStatement statement, int index, LocalDateTime value) throws SQLException {
        text(statement, index, value == null ? null : value.withNano(0).toString());
    }

    static Integer integer(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    static LocalDate date(ResultSet result, String column) throws SQLException {
        String value = result.getString(column);
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    static LocalDateTime dateTime(ResultSet result, String column) throws SQLException {
        String value = result.getString(column);
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }
}
