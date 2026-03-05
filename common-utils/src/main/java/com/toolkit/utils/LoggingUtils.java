package com.toolkit.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standardized logging utilities for IIQ rules.
 * Provides a consistent log format across all rules: each message includes
 * the rule name, identity being processed, and the action taken.
 *
 * Format: [RuleName] identity=identityName action=actionName detail=message
 */
public final class LoggingUtils {

    private LoggingUtils() {
        // Static utility class
    }

    /**
     * Returns a Logger instance for the given rule name.
     */
    public static Logger getLogger(String ruleName) {
        return LoggerFactory.getLogger(ruleName);
    }

    /**
     * Logs a rule entry event at INFO level.
     */
    public static void logRuleEntry(Logger log, String ruleName, String identityName) {
        log.info("[{}] Processing identity: {}", ruleName, identityName);
    }

    /**
     * Logs a rule exit event at INFO level with a result summary.
     */
    public static void logRuleExit(Logger log, String ruleName, String identityName, String result) {
        log.info("[{}] Completed identity: {}, result: {}", ruleName, identityName, result);
    }

    /**
     * Logs a specific action taken during rule execution.
     */
    public static void logAction(Logger log, String ruleName, String identityName,
                                 String action, String detail) {
        log.info("[{}] identity={} action={} detail={}", ruleName, identityName, action, detail);
    }

    /**
     * Logs a warning during rule execution.
     */
    public static void logWarning(Logger log, String ruleName, String identityName,
                                  String action, String detail) {
        log.warn("[{}] identity={} action={} detail={}", ruleName, identityName, action, detail);
    }

    /**
     * Logs an error during rule execution with exception details.
     */
    public static void logError(Logger log, String ruleName, String identityName,
                                String action, Throwable t) {
        log.error("[{}] identity={} action={} error={}", ruleName, identityName, action,
                  t.getMessage(), t);
    }

    /**
     * Builds a formatted context string for use in custom log messages.
     */
    public static String buildContext(String ruleName, String identityName) {
        return "[" + ruleName + "] identity=" + identityName;
    }
}
