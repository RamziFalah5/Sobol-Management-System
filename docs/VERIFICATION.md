# Recovery verification — 2026-11-10

Verified locally on Windows 11 x64, Oracle JDK 22, Maven Wrapper 3.9.14.

| Check | Result |
| --- | --- |
| `mvnw.cmd -B -ntp clean verify` | PASS; 101 application Java sources compile with release 21; 8 non-desktop tests pass |
| `mvnw.cmd -B -ntp -Pdesktop-smoke clean verify` | PASS; 9 tests, 0 failures/errors/skips; includes original Main.start(Stage) showing/closing Signin |
| Connection safeguards | Tests force DB access off; absent settings, original schema, remote hosts, URL-embedded credentials rejected before a connection attempt |
| FXML contracts | All 53 XML files parse; declared controllers/handlers resolve; only documented injection gaps remain |
| Navigation | FieldAdministrators.fxml is the only unresolved named FXML destination; original unavailable feature |
| JAR resources | 53 FXML, 16 allowlisted non-personal PNGs, CSS, application classes; no SQL dump/private environment file |
| Source preservation | All 155 provenance entries match; beyond whitespace, Java changes limited to DBAuth and two packaged-icon lookup helpers |
| Historical preservation | All 378 original Eclipse files and 124 downloaded source files match private pre-recovery fingerprints |
| Staged review | Full indexed snapshot scanned, including private comparison against old DB credential; binary assets checked against approved digests; no prohibited content found |
| Whitespace | `git diff --cached --check` passes |

The desktop test harness runs JavaFX on the classpath and emits the JavaFX unnamed-module configuration warning. The final smoke run has no shutdown exception. The normal Maven JavaFX launcher supplies the JavaFX runtime separately.

No login/registration attempt, CRUD, SQL import, database creation, or original database access was performed. No claim of complete application operation is made. Database integration and the original defects in KNOWN-DEFECTS.md remain outside this verification.

GitHub initially rejected the unpublished baseline commit with GH007 (private author email). Repository-local identity was switched to the authenticated owner's GitHub noreply address using the verified account ID/login. Only the unpublished recovery commit identity was corrected; remote history and email privacy settings were not changed. No force-push is used.

Maven was selected to replace machine-specific Eclipse user-library paths with pinned dependency resolution and explicit resource packaging, while preserving the application package and historical library versions. References: [OpenJFX Maven setup](https://openjfx.io/openjfx-docs/), [JavaFX 24 runtime requirement](https://openjfx.io/highlights/24/).

## Phase 2 — isolated database foundation, 2026-11-10

The owner confirmed that MySQL development accounts/environment are not prepared and requested completion of artifacts/instructions only. Local MySQL93 service existence/running state was observed without opening a MySQL session. Historical credential rotation remains unverified. No original database access, development DDL, fixtures or database-backed DAO methods were executed.

| Check | Actual result |
| --- | --- |
| `mvnw.cmd -B -ntp clean verify` | PASS: 101 original application sources and 8 test/tooling sources compile; 14 offline tests pass, 0 failures/errors/skips. Surefire forces DB access off. |
| Offline safety tests | 6 DevDatabaseTest methods validate separate identities/credentials, confirmation/rotation requirements, inherited target restrictions, least-privilege grant parsing, 14 ordered DDL statements/digest and sanitized connection failure via an in-process failing JDBC driver. No network/database connection in these tests. |
| DDL provenance | All 14 normalized table definitions equal the reference DDL after removing historical AUTO_INCREMENT counters and applying explicit target qualification. All columns/indexes/constraints/table options are preserved. Original INSERT records are excluded. |
| Scope and packaging | `git diff HEAD -- src/main` is empty; the built JAR contains none of the new schema/setup/IT tooling. Full indexed snapshot review checks 197 files; no prohibited material found by baseline checks. |
| Explicit `-Pmysql-it verify` without rotation confirmation | EXPECTED BUILD FAILURE: CorePersistenceIT @BeforeAll throws `Required safety confirmation: SOBOL_CREDENTIAL_ROTATION_CONFIRMED` before any connection. Surefire 14 tests pass; Failsafe reports 1 class-initialization error, 0 test-method bodies executed. This verifies fail-closed invocation, not business behavior. |
| Real Integration Test methods | 17 compiled, **NOT EXECUTED**. No persistence, rollback, capacity, concurrency, fixture cleanup or SQL-engine acceptance result is available. |
| `-Pdb-setup test-compile exec:java` without rotation confirmation | EXPECTED BUILD FAILURE at the same named confirmation, before a JDBC connection or DDL. Initial command exposed a test-tool entry-point visibility error; making the tooling class public corrected it, and the repeated command reached the intended safety guard. |

The negative profile invocation explicitly removes rotation confirmation from its child process; it cannot proceed to network access even if other environment values later become available. Integration assertions were not loosened to hide original defects. Original DBAuth/DAO/Model/Controller and main resources remain unchanged. All new database tooling and SQL are in test scope and are excluded from the application JAR. See DEVELOPMENT-DATABASE.md for commands, synthetic data, limitations and the remaining live Phase 2 acceptance steps.
