# Getting Started

How to build, test, and explore the IIQ Governance Framework without a SailPoint license.

## Prerequisites

| Tool | Version | Required? |
|------|---------|-----------|
| JDK | 11+ | Yes |
| Maven | 3.8+ | Yes |
| Git | 2.x | Yes |
| Docker + Docker Compose | 20+ / v2+ | Optional (for integration testing) |

## Clone and Build

```bash
git clone https://github.com/sworuptima/sailpoint-iiq-governance-framework.git
cd sailpoint-iiq-governance-framework
mvn clean install
```

This compiles all modules and runs all tests. You should see `BUILD SUCCESS` with all tests passing.

## Run Tests

```bash
# Run all tests
mvn test

# Run tests for a specific module
cd mock-context && mvn test
cd common-utils && mvn test
cd rules/lifecycle && mvn test

# Run a specific test class
mvn test -pl rules/lifecycle -Dtest=JoinerBirthrightAccessTest
```

## Understanding the Mock Layer

The biggest barrier to SailPoint rule development is the need for a live IIQ instance. This toolkit eliminates that barrier with a mock context layer.

Instead of:
```java
import sailpoint.object.Identity;     // requires proprietary JAR
import sailpoint.api.SailPointContext; // requires running IIQ
```

You use:
```java
import com.toolkit.mock.MockIdentity;
import com.toolkit.mock.MockSailPointContext;
```

The mock classes mirror the SailPoint API methods that rules actually use. When you're ready to deploy to production, the same logic works — just swap the imports and pass the real `SailPointContext`.

## Your First Rule Test

Let's walk through `JoinerBirthrightAccessTest`:

### 1. Set up the mock context

```java
MockSailPointContext context = new MockSailPointContext();

// Register the roles that exist in our system
context.addBundle(new MockBundle("Base Access"));
context.addBundle(new MockBundle("Engineering Tools"));
context.addBundle(new MockBundle("GitHub Access"));
```

### 2. Create a test identity

```java
MockIdentity identity = new MockIdentity("jdoe")
    .setAttribute("department", "Engineering")
    .setAttribute("title", "Developer");
context.addIdentity(identity);
```

### 3. Run the rule

```java
JoinerBirthrightAccess rule = new JoinerBirthrightAccess(config);
MockProvisioningPlan plan = rule.execute(context, identity);
```

### 4. Assert the results

```java
List<String> assignedRoles = extractAddedRoles(plan);
assertTrue(assignedRoles.contains("Base Access"));
assertTrue(assignedRoles.contains("Engineering Tools"));
assertTrue(assignedRoles.contains("GitHub Access"));
```

The rule read the department ("Engineering"), looked up the matching roles in the config, and built a provisioning plan to assign them.

## Using Docker for Integration Testing

The Docker environment provides real LDAP and database services seeded with sample data.

### Start the environment

```bash
cd docker
docker-compose up -d
```

### What's running

- **OpenLDAP** (port 389) — 10 sample users, 8 groups matching birthright roles
- **PostgreSQL** (port 5432) — HR database with identity, department, and entitlement tables
- **phpLDAPadmin** (port 6443) — Web UI for browsing LDAP data

### Verify the data

```bash
# Check LDAP users
docker exec toolkit-ldap ldapsearch -x -H ldap://localhost \
    -b "ou=People,dc=toolkit,dc=local" \
    -D "cn=admin,dc=toolkit,dc=local" -w admin \
    "(objectClass=inetOrgPerson)" uid cn departmentNumber

# Check database
docker exec -it toolkit-postgres psql -U toolkit -d iiq_toolkit \
    -c "SELECT username, department, title FROM identity_access_view GROUP BY username, department, title;"
```

### Stop the environment

```bash
docker-compose down      # stop containers
docker-compose down -v   # stop and remove data
```

## Customizing Rules

### Change birthright mappings

Edit `rules/lifecycle/src/main/resources/joiner-birthright-config.json`:

```json
{
  "departmentRoleMappings": {
    "Your Department": ["Role A", "Role B"]
  }
}
```

### Change mover behavior

Edit `rules/lifecycle/src/main/resources/mover-rebalance-config.json`:

```json
{
  "sensitiveRoles": ["Your Sensitive Role"],
  "alwaysCertifyOnDepartmentChange": false
}
```

### Test your changes

```bash
mvn test -pl rules/lifecycle
```

## Creating a New Rule

Use the lifecycle rules as a template:

1. **Create the Java class** — extend the pattern in `JoinerBirthrightAccess.java`
2. **Create a config file** — externalize business logic into JSON
3. **Create a test class** — use `MockSailPointContext` to simulate scenarios
4. **Create an XML template** — wrap the logic in BeanShell for IIQ import
5. **Update the module README** — document business context, technical design, and usage

## FAQ

**Do I need a SailPoint license?**
No. The mock context layer replaces the SailPoint API for development and testing. You only need a license when deploying to a real IIQ instance.

**Can I deploy these rules directly to IIQ?**
Yes. The XML templates in `xml-templates/` are ready to import into IIQ. The BeanShell source mirrors the Java logic. You'll need to adjust application names and role names to match your environment.

**What Java version should I use?**
Java 11 is the target. SailPoint IIQ 8.x runs on Java 11, so this ensures compatibility.

**How do I add a new department?**
Edit the config JSON file and add the department-to-role mapping. No code changes needed. Run `mvn test` to verify.

**Can I use these with SailPoint IdentityNow (ISC)?**
The patterns and logic translate, but the API is different. IdentityNow uses REST APIs and transforms instead of BeanShell rules. The governance concepts (birthright, mover, certification) are the same.
