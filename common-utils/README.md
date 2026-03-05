# Common Utilities

Shared helper methods used across all IIQ governance rules. These utilities address the most common pain points in SailPoint rule development: null pointer exceptions from missing attributes, inconsistent date handling, unstructured logging, and brittle connector error handling.

## Modules

### SafeAttributeUtils

Null-safe attribute access that works with both mock and real SailPoint objects via reflection.

```java
// Never throws NPE, even if identity or attribute is null
String dept = SafeAttributeUtils.getStringAttribute(identity, "department", "Unknown");
boolean isVip = SafeAttributeUtils.getBooleanAttribute(identity, "vipFlag", false);
List<String> groups = SafeAttributeUtils.getListAttribute(identity, "memberOf");
```

### DateUtils

Date comparison and formatting utilities backed by `java.time` for correctness.

```java
long daysSinceHire = DateUtils.daysSince(identity.getAttribute("startDate"));
boolean isRecent = DateUtils.isWithinDays(lastLoginDate, 30);
String formatted = DateUtils.formatShortDate(certificationDueDate);
```

### LoggingUtils

Standardized structured logging for rules. Every log entry includes the rule name and identity being processed.

```java
Logger log = LoggingUtils.getLogger("JoinerBirthrightAccess");
LoggingUtils.logRuleEntry(log, "JoinerBirthrightAccess", identity.getName());
LoggingUtils.logAction(log, "JoinerBirthrightAccess", identity.getName(),
    "assignRole", "Engineering Tools");
```

### ConnectorErrorHandler

Retry logic with exponential backoff for connector operations, plus error categorization.

```java
String result = ConnectorErrorHandler.executeWithRetry(() -> {
    return connector.getObject("user", username);
}, 3, 1000);
```

## Building

```bash
cd common-utils
mvn clean test
```
