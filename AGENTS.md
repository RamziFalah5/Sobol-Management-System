# Sobol engineering and Git policy

## Scope and preservation
- Preserve the original application's business meaning. No unrelated redesign, framework upgrades, or new features without authorization.
- Historical Eclipse sources, downloaded archives, and private backups are evidence: never modify or publish them.
- Original source is under `src/main/java/application`; packaged resources are under `src/main/resources`.
- Read `docs/RECOVERY.md` and `docs/KNOWN-DEFECTS.md` before changing recovered behavior.
- Do not delegate to sub-agents unless the user explicitly authorizes delegation.

## Data and security
- Never stage or commit credentials, tokens, password hashes, personal/customer records, private SQL dumps, personal images, or local secrets.
- Prepare `.gitignore` before staging. Review the complete staged snapshot, not just the diff, using `tools/review_staged.py` and manual review.
- Database access is disabled by default. PostgreSQL development is restricted to numeric loopback `127.0.0.1`, database `sobol_dev`, schema `sobol`; legacy MySQL connections are retired; never connect to or modify the original `accounts` database.
- Use synthetic fixtures for tests. Never log connection credentials or private configuration.
- Phases 6A/6B authorize reviewed PostgreSQL V1/V2 migrations under `src/test/resources/db/postgresql`, allowlisted by `docs/postgresql-migrations.json` and `tools/review_staged.py`. Historical MySQL V001 is non-executable reference under `docs/reference/mysql`, checked against unchanged historical digests in `docs/schema-provenance.json`. Dumps and personal records remain prohibited. Database/schema creation, migrations and live tests require separate explicit owner approval; see `docs/POSTGRESQL-SETUP.md`. No startup migration or automatic schema repair/clean.
- Authentication account DML requires its own explicit owner approval; ordinary application credentials must have only SELECT and INSERT on the four registration columns of sobol.account, no security-state modification. See docs/AUTHENTICATION.md and docs/POSTGRESQL-SETUP.md. No seeded users, historical credential imports or development bypass.
- An ignore file does not protect already tracked secrets. Stop and remove secrets from the proposed tree before committing.

## Verification
- JDK 22 runs this baseline; compile with release 21. JavaFX 24.0.1 remains the recovered UI version. PostgreSQL JDBC and test-scoped Flyway versions are pinned in pom.xml; Connector/J is retired.
- Run `mvnw.cmd clean verify` on Windows (or `./mvnw clean verify`). Desktop smoke: add `-Pdesktop-smoke` on a machine with a graphical session.
- Run verification appropriate to each logical change before its commit. Do not claim functionality or a push that was not verified.
- Keep original defects distinct from recovery changes. Do not fix unrelated business defects in recovery commits.

## Permanent GitHub development policy
- Every successfully completed logical development task must be committed and pushed to the owner's authorized GitHub repository.
- Use descriptive Conventional Commits and logically scoped commits; never make artificial contribution-count commits.
- Verify the Git author email belongs to the owner's authenticated GitHub account before committing. Preserve owner identity; do not add fabricated authors or dates.
- Respect GitHub email privacy. Prefer the owner's GitHub-provided noreply identity, verified from their account or the authenticated numeric ID/login using GitHub's documented format. Never disable privacy protection to push. An unpublished local commit may have its identity corrected; published history must remain untouched.
- Preserve existing repository history. Never force-push, rewrite shared history, backdate commits, or manipulate contributions.
- Bring completed changes to the default branch (`main`) through its allowed branch/PR workflow. Respect protection rules.
- Before creating a new repository, obtain explicit approval. Do not change repository visibility without authorization.
- If authentication, author verification, remote setup, or approval is missing, finish safe local preparation and report the precise blocker; never claim success.
- After each verified push, report commit SHA, Conventional Commit message, branch, remote URL, and verification results.
- Verify the remote branch/commit after pushing. Stop after the authorized phase.
