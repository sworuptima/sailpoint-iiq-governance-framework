# Docker Demo Environment

A local test environment with OpenLDAP and PostgreSQL, seeded with sample identity data that matches the toolkit's rule configurations.

## Prerequisites

- Docker Engine 20+
- Docker Compose v2+

## Quick Start

```bash
cd docker
docker-compose up -d
```

## Services

| Service | Port | Credentials | Purpose |
|---------|------|-------------|---------|
| OpenLDAP | 389 (LDAP), 636 (LDAPS) | `cn=admin,dc=toolkit,dc=local` / `admin` | Directory service with sample users and groups |
| phpLDAPadmin | 6443 (HTTPS) | Same as LDAP admin | Web-based LDAP browser |
| PostgreSQL | 5432 | `toolkit` / `toolkit` (db: `iiq_toolkit`) | HR database with identity and entitlement data |

## Seed Data

### LDAP (init.ldif)

- **Base DN**: `dc=toolkit,dc=local`
- **OUs**: `People`, `Groups`
- **10 users** across 4 departments (Engineering, Sales, Finance, HR)
- **8 groups** matching birthright roles (Base Access, Engineering Tools, GitHub Access, CRM Access, Sales Tools, Finance Systems, HR Systems, Manager Dashboard)

### PostgreSQL (init.sql)

- **departments** table — 6 departments with cost centers
- **identities** table — 10 employees with manager relationships, hire dates, locations
- **entitlements** table — 24 role definitions with risk levels
- **identity_entitlements** table — current role assignments
- **identity_access_view** — convenient view joining all tables

### Sample Users

| Username | Name | Department | Title | Notes |
|----------|------|------------|-------|-------|
| mchen | Michael Chen | Engineering | Manager | Engineering team lead |
| jdoe | John Doe | Engineering | Senior Developer | Standard employee |
| agarcia | Ana Garcia | Engineering | Developer | Standard employee |
| rpatel | Raj Patel | Sales | Manager | Sales team lead |
| sthompson | Sarah Thompson | Sales | Account Executive | Standard employee |
| klee | Kevin Lee | Finance | Director | Finance director |
| lwilson | Lisa Wilson | Finance | Financial Analyst | Standard employee |
| dkim | David Kim | Human Resources | VP | HR VP |
| jmartinez | Julia Martinez | Human Resources | HR Specialist | Standard employee |
| enewman | Emily Newman | Engineering | Developer | **New joiner** (no roles assigned) |

### Testing Scenarios

- **Joiner**: Emily Newman (`enewman`) has no entitlements — run JoinerBirthrightAccess to see what gets assigned
- **Mover**: Move `lwilson` from Finance to Engineering — run MoverAccessRebalance to see role changes
- **Access query**: Use the `identity_access_view` to inspect current state

## Verifying Seed Data

### LDAP

```bash
# List all users
docker exec toolkit-ldap ldapsearch -x -H ldap://localhost -b "ou=People,dc=toolkit,dc=local" -D "cn=admin,dc=toolkit,dc=local" -w admin "(objectClass=inetOrgPerson)" uid cn departmentNumber

# List all groups
docker exec toolkit-ldap ldapsearch -x -H ldap://localhost -b "ou=Groups,dc=toolkit,dc=local" -D "cn=admin,dc=toolkit,dc=local" -w admin "(objectClass=groupOfNames)" cn member
```

### PostgreSQL

```bash
# Connect to database
docker exec -it toolkit-postgres psql -U toolkit -d iiq_toolkit

# View all identities with their access
SELECT * FROM identity_access_view;

# Find users with high-risk entitlements
SELECT * FROM identity_access_view WHERE risk_level = 'high';

# Find Emily Newman (new joiner with no access)
SELECT * FROM identity_access_view WHERE username = 'enewman';
```

## Stopping

```bash
docker-compose down
```

To remove volumes and start fresh:

```bash
docker-compose down -v
```
