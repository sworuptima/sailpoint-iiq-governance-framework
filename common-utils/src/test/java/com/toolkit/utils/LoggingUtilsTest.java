package com.toolkit.utils;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LoggingUtilsTest {

    @Test
    void testGetLogger() {
        Logger logger = LoggingUtils.getLogger("TestRule");
        assertNotNull(logger);
        assertEquals("TestRule", logger.getName());
    }

    @Test
    void testBuildContext() {
        String context = LoggingUtils.buildContext("JoinerRule", "jdoe");
        assertEquals("[JoinerRule] identity=jdoe", context);
    }

    @Test
    void testBuildContextWithNullIdentity() {
        String context = LoggingUtils.buildContext("JoinerRule", null);
        assertEquals("[JoinerRule] identity=null", context);
    }

    @Test
    void testLogMethodsDoNotThrow() {
        Logger logger = LoggingUtils.getLogger("TestRule");

        assertDoesNotThrow(() -> LoggingUtils.logRuleEntry(logger, "TestRule", "jdoe"));
        assertDoesNotThrow(() -> LoggingUtils.logRuleExit(logger, "TestRule", "jdoe", "success"));
        assertDoesNotThrow(() -> LoggingUtils.logAction(logger, "TestRule", "jdoe", "addRole", "Base Access"));
        assertDoesNotThrow(() -> LoggingUtils.logWarning(logger, "TestRule", "jdoe", "skipRole", "already assigned"));
        assertDoesNotThrow(() -> LoggingUtils.logError(logger, "TestRule", "jdoe", "provision",
                new RuntimeException("test error")));
    }

    @Test
    void testLogMethodsWithNullValues() {
        Logger logger = LoggingUtils.getLogger("TestRule");

        assertDoesNotThrow(() -> LoggingUtils.logRuleEntry(logger, "TestRule", null));
        assertDoesNotThrow(() -> LoggingUtils.logAction(logger, "TestRule", null, null, null));
    }
}
