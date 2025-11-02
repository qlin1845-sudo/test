package net.mooctest;

import java.time.LocalDate;

public final class ValidationUtil {
    private ValidationUtil() {}

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " must be non-blank");
        }
    }

    public static void requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }

    public static void requireNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new ValidationException(fieldName + " must be non-negative");
        }
    }

    public static void requireBetween(double value, double minInclusive, double maxInclusive, String fieldName) {
        if (value < minInclusive || value > maxInclusive) {
            throw new ValidationException(fieldName + " must be between " + minInclusive + " and " + maxInclusive);
        }
    }

    public static void requirePastOrPresent(LocalDate date, String fieldName) {
        if (date == null) {
            throw new ValidationException(fieldName + " must not be null");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException(fieldName + " must be in the past or present");
        }
    }
}


