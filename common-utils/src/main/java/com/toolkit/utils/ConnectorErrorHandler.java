package com.toolkit.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Error handling and retry utilities for connector operations.
 * Provides exponential backoff with jitter for transient failures
 * and error categorization to determine whether a retry is appropriate.
 */
public final class ConnectorErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorErrorHandler.class);
    private static final long MAX_DELAY_MS = 30_000;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_DELAY_MS = 1_000;

    private ConnectorErrorHandler() {
        // Static utility class
    }

    /**
     * Functional interface for operations that can be retried.
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Categories of errors that determine retry behavior.
     */
    public enum ErrorCategory {
        /** Transient failure — safe to retry (network timeout, temporary unavailability) */
        TRANSIENT,
        /** Configuration error — do not retry, fix the configuration */
        CONFIGURATION,
        /** Permission denied — do not retry, escalate to administrator */
        PERMISSION,
        /** Resource not found — do not retry */
        NOT_FOUND,
        /** Unknown error — may or may not be retryable */
        UNKNOWN
    }

    /**
     * Executes an operation with retry logic using exponential backoff and jitter.
     * Only TRANSIENT errors trigger retries. All other error categories fail immediately.
     *
     * @param operation     the operation to execute
     * @param maxRetries    maximum number of retry attempts
     * @param initialDelayMs base delay in milliseconds before first retry
     * @return the result of the operation
     * @throws Exception if all retries are exhausted or a non-transient error occurs
     */
    public static <T> T executeWithRetry(RetryableOperation<T> operation,
                                         int maxRetries,
                                         long initialDelayMs) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                ErrorCategory category = categorizeError(e);

                if (category != ErrorCategory.TRANSIENT) {
                    LOG.error("Non-transient error ({}), failing immediately: {}",
                              category, e.getMessage());
                    throw e;
                }

                if (attempt < maxRetries) {
                    long delay = calculateDelay(attempt, initialDelayMs);
                    LOG.warn("Transient error on attempt {}/{}, retrying in {}ms: {}",
                             attempt + 1, maxRetries + 1, delay, e.getMessage());
                    Thread.sleep(delay);
                }
            }
        }

        throw lastException;
    }

    /**
     * Executes an operation with default retry settings (3 retries, 1s initial delay).
     */
    public static <T> T executeWithRetry(RetryableOperation<T> operation) throws Exception {
        return executeWithRetry(operation, DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY_MS);
    }

    /**
     * Categorizes an exception to determine whether the operation should be retried.
     */
    public static ErrorCategory categorizeError(Throwable t) {
        if (t == null) {
            return ErrorCategory.UNKNOWN;
        }

        // Network/connectivity issues are transient
        if (t instanceof ConnectException || t instanceof SocketTimeoutException) {
            return ErrorCategory.TRANSIENT;
        }
        if (t instanceof java.io.IOException) {
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Connection reset") ||
                                msg.contains("Connection refused") ||
                                msg.contains("timed out"))) {
                return ErrorCategory.TRANSIENT;
            }
        }

        // Security/permission issues
        if (t instanceof SecurityException || t instanceof java.security.AccessControlException) {
            return ErrorCategory.PERMISSION;
        }

        // Configuration issues
        if (t instanceof IllegalArgumentException || t instanceof ClassNotFoundException) {
            return ErrorCategory.CONFIGURATION;
        }

        // Check the message for common patterns
        String message = t.getMessage();
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("not found") || lower.contains("no such")) {
                return ErrorCategory.NOT_FOUND;
            }
            if (lower.contains("unauthorized") || lower.contains("forbidden") ||
                lower.contains("access denied")) {
                return ErrorCategory.PERMISSION;
            }
            if (lower.contains("timeout") || lower.contains("unavailable") ||
                lower.contains("temporarily")) {
                return ErrorCategory.TRANSIENT;
            }
        }

        // Check cause chain
        if (t.getCause() != null && t.getCause() != t) {
            return categorizeError(t.getCause());
        }

        return ErrorCategory.UNKNOWN;
    }

    /**
     * Builds a structured error report for logging or audit purposes.
     */
    public static String buildErrorReport(Throwable t, String connectorName, String operation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error Report\n");
        sb.append("  Connector: ").append(connectorName).append("\n");
        sb.append("  Operation: ").append(operation).append("\n");
        sb.append("  Category:  ").append(categorizeError(t)).append("\n");
        sb.append("  Message:   ").append(t.getMessage()).append("\n");
        if (t.getCause() != null) {
            sb.append("  Cause:     ").append(t.getCause().getMessage()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Calculates the delay for exponential backoff with jitter.
     * Formula: min(maxDelay, initialDelay * 2^attempt + random(0, initialDelay))
     */
    static long calculateDelay(int attempt, long initialDelayMs) {
        long exponentialDelay = initialDelayMs * (1L << attempt);
        long jitter = ThreadLocalRandom.current().nextLong(0, initialDelayMs);
        return Math.min(MAX_DELAY_MS, exponentialDelay + jitter);
    }
}
