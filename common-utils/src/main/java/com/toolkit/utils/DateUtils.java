package com.toolkit.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * Date formatting, comparison, and calculation utilities for IIQ rules.
 * Uses java.time internally for correctness while accepting/returning
 * java.util.Date for compatibility with the SailPoint API.
 */
public final class DateUtils {

    private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private DateUtils() {
        // Static utility class
    }

    /**
     * Returns the number of days between two dates (signed).
     * Positive if 'to' is after 'from', negative otherwise.
     */
    public static long daysBetween(Date from, Date to) {
        if (from == null || to == null) {
            return 0;
        }
        LocalDate fromLocal = toLocalDate(from);
        LocalDate toLocal = toLocalDate(to);
        return ChronoUnit.DAYS.between(fromLocal, toLocal);
    }

    /**
     * Returns the number of days from the given date to today.
     * Positive if the date is in the past.
     */
    public static long daysSince(Date date) {
        if (date == null) {
            return 0;
        }
        return daysBetween(date, new Date());
    }

    /**
     * Returns the number of days from today to the given date.
     * Positive if the date is in the future.
     */
    public static long daysUntil(Date date) {
        if (date == null) {
            return 0;
        }
        return daysBetween(new Date(), date);
    }

    /**
     * Checks whether the given date is within N days of today (past or future).
     */
    public static boolean isWithinDays(Date date, int days) {
        if (date == null) {
            return false;
        }
        long diff = Math.abs(daysBetween(new Date(), date));
        return diff <= days;
    }

    /**
     * Formats a Date using the specified pattern. Returns null if the date is null.
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null || pattern == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * Formats a Date in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss'Z').
     */
    public static String formatISODate(Date date) {
        return formatDate(date, ISO_FORMAT);
    }

    /**
     * Formats a Date as yyyy-MM-dd.
     */
    public static String formatShortDate(Date date) {
        return formatDate(date, "yyyy-MM-dd");
    }

    /**
     * Parses a date string using the specified pattern. Returns null on failure.
     */
    public static Date parseDate(String dateStr, String pattern) {
        if (dateStr == null || pattern == null) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Adds (or subtracts if negative) the specified number of days to a date.
     */
    public static Date addDays(Date date, int days) {
        if (date == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    /**
     * Returns true if the given date is before today (midnight comparison).
     */
    public static boolean isBeforeToday(Date date) {
        if (date == null) {
            return false;
        }
        return toLocalDate(date).isBefore(LocalDate.now());
    }

    /**
     * Returns true if the given date is after today (midnight comparison).
     */
    public static boolean isAfterToday(Date date) {
        if (date == null) {
            return false;
        }
        return toLocalDate(date).isAfter(LocalDate.now());
    }

    private static LocalDate toLocalDate(Date date) {
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
