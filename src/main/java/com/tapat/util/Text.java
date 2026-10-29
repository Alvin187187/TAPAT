package com.tapat.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Text {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private Text() {
    }

    public static String date(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    public static String closes(LocalDate deadline) {
        if (deadline == null) {
            return "has no deadline";
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        if (days < 0) {
            return "closed on " + date(deadline);
        }
        if (days == 0) {
            return "closes today";
        }
        if (days == 1) {
            return "closes tomorrow";
        }
        return "closes in " + days + " days, on " + date(deadline);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static List<String> lines(String raw) {
        List<String> items = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return items;
        }
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    public static String joinLines(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return String.join("\n", items);
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
