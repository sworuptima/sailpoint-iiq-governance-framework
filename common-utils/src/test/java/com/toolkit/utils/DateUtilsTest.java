package com.toolkit.utils;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void testDaysBetweenKnownDates() {
        Date from = createDate(2025, 1, 1);
        Date to = createDate(2025, 1, 11);

        assertEquals(10, DateUtils.daysBetween(from, to));
        assertEquals(-10, DateUtils.daysBetween(to, from));
    }

    @Test
    void testDaysBetweenSameDate() {
        Date date = createDate(2025, 6, 15);
        assertEquals(0, DateUtils.daysBetween(date, date));
    }

    @Test
    void testDaysBetweenWithNull() {
        assertEquals(0, DateUtils.daysBetween(null, new Date()));
        assertEquals(0, DateUtils.daysBetween(new Date(), null));
    }

    @Test
    void testDaysSince() {
        Date tenDaysAgo = DateUtils.addDays(new Date(), -10);
        long result = DateUtils.daysSince(tenDaysAgo);
        assertTrue(result >= 9 && result <= 11, "Expected ~10 days, got " + result);
    }

    @Test
    void testDaysUntil() {
        Date tenDaysFromNow = DateUtils.addDays(new Date(), 10);
        long result = DateUtils.daysUntil(tenDaysFromNow);
        assertTrue(result >= 9 && result <= 11, "Expected ~10 days, got " + result);
    }

    @Test
    void testIsWithinDays() {
        Date threeDaysAgo = DateUtils.addDays(new Date(), -3);
        assertTrue(DateUtils.isWithinDays(threeDaysAgo, 5));
        assertFalse(DateUtils.isWithinDays(threeDaysAgo, 1));
        assertFalse(DateUtils.isWithinDays(null, 5));
    }

    @Test
    void testFormatDate() {
        Date date = createDate(2025, 3, 15);
        assertEquals("2025-03-15", DateUtils.formatDate(date, "yyyy-MM-dd"));
        assertEquals("03/15/2025", DateUtils.formatDate(date, "MM/dd/yyyy"));
    }

    @Test
    void testFormatDateNull() {
        assertNull(DateUtils.formatDate(null, "yyyy-MM-dd"));
        assertNull(DateUtils.formatDate(new Date(), null));
    }

    @Test
    void testFormatShortDate() {
        Date date = createDate(2025, 3, 15);
        assertEquals("2025-03-15", DateUtils.formatShortDate(date));
    }

    @Test
    void testParseDate() {
        Date parsed = DateUtils.parseDate("2025-03-15", "yyyy-MM-dd");
        assertNotNull(parsed);

        String roundtrip = DateUtils.formatDate(parsed, "yyyy-MM-dd");
        assertEquals("2025-03-15", roundtrip);
    }

    @Test
    void testParseDateInvalid() {
        assertNull(DateUtils.parseDate("not-a-date", "yyyy-MM-dd"));
        assertNull(DateUtils.parseDate(null, "yyyy-MM-dd"));
        assertNull(DateUtils.parseDate("2025-03-15", null));
    }

    @Test
    void testAddDays() {
        Date date = createDate(2025, 1, 28);
        Date result = DateUtils.addDays(date, 5);
        assertEquals("2025-02-02", DateUtils.formatDate(result, "yyyy-MM-dd"));
    }

    @Test
    void testAddDaysNegative() {
        Date date = createDate(2025, 3, 1);
        Date result = DateUtils.addDays(date, -1);
        assertEquals("2025-02-28", DateUtils.formatDate(result, "yyyy-MM-dd"));
    }

    @Test
    void testAddDaysNull() {
        assertNull(DateUtils.addDays(null, 5));
    }

    @Test
    void testIsBeforeToday() {
        Date yesterday = DateUtils.addDays(new Date(), -1);
        Date tomorrow = DateUtils.addDays(new Date(), 1);

        assertTrue(DateUtils.isBeforeToday(yesterday));
        assertFalse(DateUtils.isBeforeToday(tomorrow));
        assertFalse(DateUtils.isBeforeToday(null));
    }

    @Test
    void testIsAfterToday() {
        Date yesterday = DateUtils.addDays(new Date(), -1);
        Date tomorrow = DateUtils.addDays(new Date(), 1);

        assertTrue(DateUtils.isAfterToday(tomorrow));
        assertFalse(DateUtils.isAfterToday(yesterday));
        assertFalse(DateUtils.isAfterToday(null));
    }

    private Date createDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
