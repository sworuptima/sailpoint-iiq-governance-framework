# Contributing

PRs are welcome. This project is meant to be useful to the SailPoint community, and I'd rather have good contributions than write everything myself.

## Ground Rules

- Every rule needs tests. No exceptions. If it can't be tested against the mock context, rethink the design.
- Business logic goes in config, not code. If you're writing `if (department.equals("Engineering"))` in Java, you're doing it wrong. Put it in a JSON config map.
- Document the "why," not just the "what." A non-IIQ person should be able to read your README and understand the business problem the rule solves.

## Setup

```bash
git clone https://github.com/sworuptima/sailpoint-iiq-governance-framework.git
cd sailpoint-iiq-governance-framework
mvn clean install
```

Requirements: JDK 11+, Maven 3.8+. Docker optional for integration testing.

## Submitting Changes

1. Fork and branch from `main`
2. Write your code and tests
3. Run `mvn clean test` — everything must pass
4. Open a PR with a clear description of what the rule does and what business scenario it addresses

## Rule Contribution Checklist

Every new rule should include:

- [ ] Java implementation in `rules/{module}/src/`
- [ ] JSON config file in `src/main/resources/` (if the rule has configurable behavior)
- [ ] JUnit tests with realistic scenarios, null handling, and edge cases
- [ ] XML template in `xml-templates/` with BeanShell source (no generics — BeanShell doesn't support them)
- [ ] README section covering: business context, config parameters, Mermaid flow diagram, how to test
- [ ] Passing build (`mvn clean test`)

## Code Style

- Java 11 target (IIQ 8.x compat)
- Null-safe everything — use `SafeAttributeUtils` instead of raw `getAttribute()` calls
- Descriptive test names: `testEngineeringToSales`, not `test1`
- Keep methods short. If a rule method is over 50 lines, it's doing too much.

## Commit Messages

[Conventional commits](https://www.conventionalcommits.org/):

```
feat(lifecycle): add LeaverGracefulDisable rule
fix(mock-context): handle null attribute in MockIdentity
docs(scenarios): add mover scenario walkthrough
```

## Questions

Open a GitHub Issue. I'll respond when I can.
