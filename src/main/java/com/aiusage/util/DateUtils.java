package com.aiusage.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateUtils {
    private static final DateTimeFormatter MONTH_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM yyyy");

    public static String formatMonth(LocalDate date) {
        return date.format(MONTH_FORMATTER);
    }

    public static String formatMonthYear(int year, int month) {
        return LocalDate.of(year, month, 1).format(MONTH_FORMATTER);
    }

    public static LocalDate getCurrentMonthStart() {
        return LocalDate.now().withDayOfMonth(1);
    }

    public static LocalDate getPreviousMonthStart() {
        return getCurrentMonthStart().minusMonths(1);
    }

    public static String getShortMonthName(int month) {
        return java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }
}
