package com.aiusage.util;

import java.text.NumberFormat;
import java.util.Locale;

public class CostFormatter {
    private static final NumberFormat USD_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    public static String format(double amount) {
        return USD_FORMAT.format(amount);
    }

    public static String formatDetailed(double amount) {
        return String.format("%s (%.4f)", USD_FORMAT.format(amount), amount);
    }
}
