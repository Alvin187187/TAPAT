package com.tapat.service.report;

import com.tapat.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface Report {
    String getTitle();

    List<String> columns();

    List<List<String>> rows();

    default Map<String, Integer> chartSlices() {
        return Map.of();
    }

    default String toCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append(line(columns()));
        for (List<String> row : rows()) {
            csv.append('\n').append(line(row));
        }
        return csv.toString();
    }

    private static String line(List<String> cells) {
        List<String> escaped = new ArrayList<>();
        for (String cell : cells) {
            escaped.add(Text.csv(cell));
        }
        return String.join(",", escaped);
    }
}
