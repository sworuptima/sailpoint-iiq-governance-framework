-- HR Database Seed Data
-- This simulates an authoritative HR source that feeds identity data into IIQ.

CREATE TABLE IF NOT EXISTS departments (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    cost_center VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS identities (
    id              SERIAL PRIMARY KEY,
    employee_id     VARCHAR(20) NOT NULL UNIQUE,
    username        VARCHAR(50) NOT NULL UNIQUE,
    firstname       VARCHAR(100) NOT NULL,
    lastname        VARCHAR(100) NOT NULL,
    email           VARCHAR(200),
    title           VARCHAR(100),
    department_id   INTEGER REFERENCES departments(id),
    manager_id      INTEGER REFERENCES identities(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    start_date      DATE NOT NULL,
    end_date        DATE,
    location        VARCHAR(100),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS entitlements (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    display_name    VARCHAR(200),
    type            VARCHAR(50) NOT NULL DEFAULT 'role',
    description     TEXT,
    risk_level      VARCHAR(20) DEFAULT 'low'
);

CREATE TABLE IF NOT EXISTS identity_entitlements (
    id              SERIAL PRIMARY KEY,
    identity_id     INTEGER REFERENCES identities(id) ON DELETE CASCADE,
    entitlement_id  INTEGER REFERENCES entitlements(id) ON DELETE CASCADE,
    assigned_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    source          VARCHAR(50) DEFAULT 'birthright',
    UNIQUE(identity_id, entitlement_id)
);

-- Departments
INSERT INTO departments (name, cost_center) VALUES
    ('Engineering', 'CC-1000'),
    ('Sales', 'CC-2000'),
    ('Finance', 'CC-3000'),
    ('Human Resources', 'CC-4000'),
    ('Marketing', 'CC-5000'),
    ('IT', 'CC-6000');

-- Entitlements (matching the birthright config)
INSERT INTO entitlements (name, display_name, type, description, risk_level) VALUES
    ('Base Access', 'Base Access', 'role', 'Baseline access for all employees', 'low'),
    ('Email', 'Email', 'role', 'Corporate email access', 'low'),
    ('Company Intranet', 'Company Intranet', 'role', 'Internal company portal', 'low'),
    ('Engineering Tools', 'Engineering Tools', 'role', 'Development environment and build tools', 'medium'),
    ('GitHub Access', 'GitHub Access', 'role', 'Source code repository access', 'medium'),
    ('CI/CD Pipeline', 'CI/CD Pipeline', 'role', 'Continuous integration and deployment', 'medium'),
    ('CRM Access', 'CRM Access', 'role', 'Customer relationship management system', 'medium'),
    ('Sales Tools', 'Sales Tools', 'role', 'Sales enablement platform', 'low'),
    ('Lead Database', 'Lead Database', 'role', 'Sales lead database access', 'medium'),
    ('Finance Systems', 'Finance Systems', 'role', 'Financial management systems', 'high'),
    ('Reporting Tools', 'Reporting Tools', 'role', 'Financial reporting and analytics', 'medium'),
    ('Budget Portal', 'Budget Portal', 'role', 'Budget management portal', 'high'),
    ('HR Systems', 'HR Systems', 'role', 'Human resources information system', 'high'),
    ('Employee Portal Admin', 'Employee Portal Admin', 'role', 'Employee self-service portal administration', 'medium'),
    ('Benefits Platform', 'Benefits Platform', 'role', 'Benefits enrollment and management', 'medium'),
    ('Manager Dashboard', 'Manager Dashboard', 'role', 'Management reporting and team oversight', 'medium'),
    ('Approval Authority', 'Approval Authority', 'role', 'Ability to approve access requests', 'high'),
    ('Budget Access', 'Budget Access', 'role', 'View and manage departmental budgets', 'high'),
    ('Executive Reports', 'Executive Reports', 'role', 'Executive-level reporting access', 'high'),
    ('Marketing Platform', 'Marketing Platform', 'role', 'Marketing automation tools', 'low'),
    ('Analytics Dashboard', 'Analytics Dashboard', 'role', 'Business analytics dashboard', 'medium'),
    ('IT Service Desk', 'IT Service Desk', 'role', 'IT support ticketing system', 'low'),
    ('Infrastructure Access', 'Infrastructure Access', 'role', 'Infrastructure management tools', 'high'),
    ('Monitoring Tools', 'Monitoring Tools', 'role', 'System monitoring and alerting', 'medium');

-- Identities (managers first, then reports)
INSERT INTO identities (employee_id, username, firstname, lastname, email, title, department_id, manager_id, status, start_date, location) VALUES
    ('4503', 'mchen', 'Michael', 'Chen', 'mchen@toolkit.local', 'Manager', 1, NULL, 'active', '2019-03-15', 'San Francisco'),
    ('4511', 'rpatel', 'Raj', 'Patel', 'rpatel@toolkit.local', 'Manager', 2, NULL, 'active', '2018-06-01', 'New York'),
    ('4521', 'klee', 'Kevin', 'Lee', 'klee@toolkit.local', 'Director', 3, NULL, 'active', '2017-01-10', 'Chicago'),
    ('4531', 'dkim', 'David', 'Kim', 'dkim@toolkit.local', 'VP', 4, NULL, 'active', '2016-08-22', 'San Francisco');

INSERT INTO identities (employee_id, username, firstname, lastname, email, title, department_id, manager_id, status, start_date, location) VALUES
    ('4501', 'jdoe', 'John', 'Doe', 'jdoe@toolkit.local', 'Senior Developer', 1, 1, 'active', '2020-01-15', 'San Francisco'),
    ('4502', 'agarcia', 'Ana', 'Garcia', 'agarcia@toolkit.local', 'Developer', 1, 1, 'active', '2021-09-01', 'Remote'),
    ('4510', 'sthompson', 'Sarah', 'Thompson', 'sthompson@toolkit.local', 'Account Executive', 2, 2, 'active', '2022-03-10', 'New York'),
    ('4520', 'lwilson', 'Lisa', 'Wilson', 'lwilson@toolkit.local', 'Financial Analyst', 3, 3, 'active', '2021-06-15', 'Chicago'),
    ('4530', 'jmartinez', 'Julia', 'Martinez', 'jmartinez@toolkit.local', 'HR Specialist', 4, 4, 'active', '2023-01-05', 'San Francisco'),
    ('4540', 'enewman', 'Emily', 'Newman', 'enewman@toolkit.local', 'Developer', 1, 1, 'active', '2026-03-01', 'Remote');

-- Assign entitlements to existing employees
-- Global roles for everyone
INSERT INTO identity_entitlements (identity_id, entitlement_id, source) VALUES
    (1, 1, 'birthright'), (1, 2, 'birthright'), (1, 3, 'birthright'),
    (2, 1, 'birthright'), (2, 2, 'birthright'), (2, 3, 'birthright'),
    (3, 1, 'birthright'), (3, 2, 'birthright'), (3, 3, 'birthright'),
    (4, 1, 'birthright'), (4, 2, 'birthright'), (4, 3, 'birthright'),
    (5, 1, 'birthright'), (5, 2, 'birthright'), (5, 3, 'birthright'),
    (6, 1, 'birthright'), (6, 2, 'birthright'), (6, 3, 'birthright'),
    (7, 1, 'birthright'), (7, 2, 'birthright'), (7, 3, 'birthright'),
    (8, 1, 'birthright'), (8, 2, 'birthright'), (8, 3, 'birthright'),
    (9, 1, 'birthright'), (9, 2, 'birthright'), (9, 3, 'birthright');

-- Department roles
-- Engineering (mchen, jdoe, agarcia)
INSERT INTO identity_entitlements (identity_id, entitlement_id, source) VALUES
    (1, 4, 'birthright'), (1, 5, 'birthright'), (1, 6, 'birthright'),
    (5, 4, 'birthright'), (5, 5, 'birthright'), (5, 6, 'birthright'),
    (6, 4, 'birthright'), (6, 5, 'birthright'), (6, 6, 'birthright');

-- Sales (rpatel, sthompson)
INSERT INTO identity_entitlements (identity_id, entitlement_id, source) VALUES
    (2, 7, 'birthright'), (2, 8, 'birthright'), (2, 9, 'birthright'),
    (7, 7, 'birthright'), (7, 8, 'birthright'), (7, 9, 'birthright');

-- Finance (klee, lwilson)
INSERT INTO identity_entitlements (identity_id, entitlement_id, source) VALUES
    (3, 10, 'birthright'), (3, 11, 'birthright'), (3, 12, 'birthright'),
    (8, 10, 'birthright'), (8, 11, 'birthright'), (8, 12, 'birthright');

-- HR (dkim, jmartinez)
INSERT INTO identity_entitlements (identity_id, entitlement_id, source) VALUES
    (4, 13, 'birthright'), (4, 14, 'birthright'), (4, 15, 'birthright'),
    (9, 13, 'birthright'), (9, 14, 'birthright'), (9, 15, 'birthright');

-- Manager roles
INSERT INTO identity_entitlements (identity_id, entitlement_id, source) VALUES
    (1, 16, 'birthright'), (1, 17, 'birthright'),
    (2, 16, 'birthright'), (2, 17, 'birthright');

-- Director roles
INSERT INTO identity_entitlements (identity_id, entitlement_id, source) VALUES
    (3, 16, 'birthright'), (3, 17, 'birthright'), (3, 18, 'birthright'), (3, 19, 'birthright');

-- VP roles
INSERT INTO identity_entitlements (identity_id, entitlement_id, source) VALUES
    (4, 16, 'birthright'), (4, 17, 'birthright'), (4, 18, 'birthright'), (4, 19, 'birthright');

-- Emily Newman (new joiner) has NO entitlements yet — she's the joiner scenario test case

-- Create a view for easy querying
CREATE OR REPLACE VIEW identity_access_view AS
SELECT
    i.employee_id,
    i.username,
    i.firstname || ' ' || i.lastname AS full_name,
    d.name AS department,
    i.title,
    i.status,
    e.name AS entitlement,
    e.risk_level,
    ie.source,
    ie.assigned_date
FROM identities i
JOIN departments d ON i.department_id = d.id
LEFT JOIN identity_entitlements ie ON i.id = ie.identity_id
LEFT JOIN entitlements e ON ie.entitlement_id = e.id
ORDER BY i.username, e.name;
