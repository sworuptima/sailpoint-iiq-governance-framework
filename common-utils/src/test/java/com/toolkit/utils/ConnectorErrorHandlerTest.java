package com.toolkit.utils;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorErrorHandlerTest {

    @Test
    void testSuccessfulOperationReturnsResult() throws Exception {
        String result = ConnectorErrorHandler.executeWithRetry(() -> "success", 3, 100);
        assertEquals("success", result);
    }

    @Test
    void testTransientErrorRetriesAndSucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = ConnectorErrorHandler.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new ConnectException("Connection refused");
            }
            return "recovered";
        }, 3, 50);

        assertEquals("recovered", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void testNonTransientErrorFailsImmediately() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(SecurityException.class, () ->
            ConnectorErrorHandler.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new SecurityException("Access denied");
            }, 3, 50)
        );

        assertEquals(1, attempts.get());
    }

    @Test
    void testMaxRetriesExhaustedThrows() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(ConnectException.class, () ->
            ConnectorErrorHandler.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new ConnectException("Connection refused");
            }, 2, 50)
        );

        assertEquals(3, attempts.get()); // initial + 2 retries
    }

    @Test
    void testCategorizeConnectException() {
        assertEquals(ConnectorErrorHandler.ErrorCategory.TRANSIENT,
                ConnectorErrorHandler.categorizeError(new ConnectException("Connection refused")));
    }

    @Test
    void testCategorizeSocketTimeout() {
        assertEquals(ConnectorErrorHandler.ErrorCategory.TRANSIENT,
                ConnectorErrorHandler.categorizeError(new SocketTimeoutException("Read timed out")));
    }

    @Test
    void testCategorizeSecurityException() {
        assertEquals(ConnectorErrorHandler.ErrorCategory.PERMISSION,
                ConnectorErrorHandler.categorizeError(new SecurityException("Access denied")));
    }

    @Test
    void testCategorizeIllegalArgument() {
        assertEquals(ConnectorErrorHandler.ErrorCategory.CONFIGURATION,
                ConnectorErrorHandler.categorizeError(new IllegalArgumentException("Bad param")));
    }

    @Test
    void testCategorizeNotFoundMessage() {
        assertEquals(ConnectorErrorHandler.ErrorCategory.NOT_FOUND,
                ConnectorErrorHandler.categorizeError(new RuntimeException("Resource not found")));
    }

    @Test
    void testCategorizeUnknown() {
        assertEquals(ConnectorErrorHandler.ErrorCategory.UNKNOWN,
                ConnectorErrorHandler.categorizeError(new RuntimeException("Something weird")));
    }

    @Test
    void testCategorizeNull() {
        assertEquals(ConnectorErrorHandler.ErrorCategory.UNKNOWN,
                ConnectorErrorHandler.categorizeError(null));
    }

    @Test
    void testCategorizeCauseChain() {
        Exception wrapper = new RuntimeException("Wrapper",
                new ConnectException("Connection refused"));
        assertEquals(ConnectorErrorHandler.ErrorCategory.TRANSIENT,
                ConnectorErrorHandler.categorizeError(wrapper));
    }

    @Test
    void testBuildErrorReport() {
        Exception ex = new ConnectException("Connection refused");
        String report = ConnectorErrorHandler.buildErrorReport(ex, "ActiveDirectory", "getUser");

        assertTrue(report.contains("ActiveDirectory"));
        assertTrue(report.contains("getUser"));
        assertTrue(report.contains("TRANSIENT"));
        assertTrue(report.contains("Connection refused"));
    }

    @Test
    void testCalculateDelayIncreases() {
        long delay1 = ConnectorErrorHandler.calculateDelay(0, 1000);
        long delay2 = ConnectorErrorHandler.calculateDelay(1, 1000);
        long delay3 = ConnectorErrorHandler.calculateDelay(2, 1000);

        // Due to jitter, we check ranges rather than exact values
        assertTrue(delay1 >= 1000 && delay1 < 2000);
        assertTrue(delay2 >= 2000 && delay2 < 3000);
        assertTrue(delay3 >= 4000 && delay3 < 5000);
    }

    @Test
    void testCalculateDelayCapsAtMax() {
        long delay = ConnectorErrorHandler.calculateDelay(20, 1000);
        assertTrue(delay <= 30_000, "Delay should be capped at 30s, was " + delay);
    }
}
