package com.tapat.util;

public class NameSimilarity {
    private NameSimilarity() {
    }

    public static int percent(String left, String right) {
        String a = Text.normalizeName(left);
        String b = Text.normalizeName(right);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        if (a.equals(b)) {
            return 100;
        }
        int distance = distance(a, b);
        int longest = Math.max(a.length(), b.length());
        return (int) Math.round(100.0 * (longest - distance) / longest);
    }

    public static String digits(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isDigit(character)) {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private static int distance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
