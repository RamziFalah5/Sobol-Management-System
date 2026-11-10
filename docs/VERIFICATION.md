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

## Phase 3A — first functional repairs, 2026-11-10

Started on a clean `main` at `a34abdbe2b96c68cd1477ed3974e82f48c5a288e`, matching remote main. The authorized application changes are limited to DailyAttendance.fxml and DailyAttendanceController, CategoryController, EditCategoryController and AddProductController. All DAOs, DBAuth, models, historical migration and database tooling remain unchanged. Historical Eclipse sources/private backups were not modified.

| Check | Actual result |
| --- | --- |
| `mvnw.cmd -B -ntp clean verify` | PASS: 17 tests, 0 failures/errors/skips; includes 3 new ControllerRepairContractTest cases. All 101 application sources compile. |
| `mvnw.cmd -B -ntp -Pdesktop-smoke verify` | PASS: 32 tests, 0 failures/errors/skips; includes the existing sign-in startup smoke and all 14 new ControllerRepairsDesktopTest cases. |
| Daily attendance | Actual FXML loads the daily controller with all injections. Synthetic existing false/true/null states become false/true/true. A rendered checkbox is toggled and the correct rows/date reach the synthetic save boundary. Save success/failure and cancel paths are exercised. |
| Category editing | Actual category card selection handler and Edit button load the actual editor FXML with ID/name/description. Save retains ID 42 despite later synthetic session/model/display changes to ID 99. Missing/stale selection, invalid initialization, null description, blank/duplicate names, persistence-failure response and cancel are exercised. |
| Initial product warehouse | Actual AddProduct.fxml/controller initialize with synthetic branches; initial warehouse selection, changing branches, empty/null branches, no branches, failed lookup and stale/out-of-list values are exercised before product persistence. |
| Isolation | Surefire keeps SOBOL_DB_ENABLED=false. Tests override only package-visible DAO/UI boundaries with artificial data; no MySQL connection, setup/import or SQL write was performed. The 17 real MySQL IT methods were not invoked. |

The first desktop run exposed a test-fixture setup error: Category.fxml's ScrollPane content was not attached to the Scene until its skin/layout was initialized. The fixture now applies CSS/layout and asserts scene attachment before navigation. The original assertions remain intact; the complete repeated desktop run passes.

Desktop output includes the existing JavaFX unnamed-module warning and the deliberately injected `Synthetic warehouse lookup failure` stack trace from the original error handler. That artificial error verifies clearing stale state and produces no test failure. These are not MySQL errors. The default build remains display-free; graphical tests are explicitly enabled by desktop-smoke, which uses separate JVMs to preserve JavaFX lifecycle isolation for the existing startup test and the new controller tests.

Verification stops at the controller-to-DAO boundary for repaired CRUD flows. Real attendance save, category duplicate/update queries, product inserts/capacity queries and complete return-screen data reloads require the isolated development database. The outstanding Phase 2 live setup/credential-rotation requirements remain; no database-backed behavior is claimed as verified. CategoryDAO's zero-affected-row success defect is explicitly still open.

## Phase 4A — independently verified Category groundwork, 2026-11-10

The starting main commit is da26deb4ae5161aa77cbd02d68fa8a1c92a200b6, with a clean working tree. The first scoped change introduces five production classes under com.sobol.category: an immutable domain Category, application CategoryService/CategoryRepository/CategoryException, and JdbcCategoryRepository with an injected connection supplier. Existing controllers/DAO remain unchanged in this groundwork commit; their migration is the next scoped change in this phase.

`mvnw.cmd -B -ntp clean verify` passes **38 tests**, 0 failures/errors/skips, compiling 106 application sources. Nine new service/domain tests use an in-memory repository. Twelve new repository tests use scripted in-process JDBC proxies, not a SQL engine: generated identity, rollback, missing target, 0/1 update counts on an existing locked target, unique conflict, connection/commit/rollback failure, resource closure and invalid persisted data. Driver messages/causes are not exposed through application failures.

The update implementation reads the target with SELECT FOR UPDATE inside its own transaction before writing. Zero changed rows is accepted only after existence is established under the lock. Connector/J's found-versus-changed row modes are accounted for; live MySQL behavior, locking and SQL-engine acceptance remain **unverified**. No database connection or schema operation occurred. Controller migration and final architecture documentation follow in the second scoped Phase 4A change.

## Phase 4A — Category vertical slice completed, 2026-11-10

The groundwork was committed/pushed as 4157fd882bf612b01586823a9a9405da0e31d4fe after normal verification (38 tests) and desktop verification (53 tests), both with no failures/errors/skips. Its remote main SHA was verified. The second scoped change connects all three Category controllers to the service, supplies a single CategoryModule composition root, maps domain records to the existing JavaFX Category view model, removes the unused legacy DAO, and documents the implemented architecture.

| Check | Actual result |
| --- | --- |
| `mvnw.cmd -B -ntp clean verify` | PASS: **41 tests**, 0 failures/errors/skips, 106 application sources and 13 test/tooling sources compile. |
| `mvnw.cmd -B -ntp -Pdesktop-smoke verify` | PASS: **62 tests**, 0 failures/errors/skips; includes existing startup smoke and all 20 controller regressions. |
| Category service/domain | 9 tests: creation, normalization, invalid/overlong/Unicode names, exact update ID, duplicate create/update, invalid versus missing ID, unchanged existing values, repository failure and immutable snapshots. No JavaFX/MySQL dependency in the production inner layers. |
| Scripted JDBC contracts | 12 tests: list mapping/resource closure, generated-key creation, absent key rollback, missing target/no UPDATE, locked existing target with returned counts 0 and 1, duplicate create/update, sanitized connection failure, commit/rollback failure and unexpected counts/corrupt records. Proxies do not execute SQL or emulate InnoDB. |
| JavaFX/service delegation | Actual Category/Add/Edit FXML is loaded with the real CategoryService and an in-memory repository. Creation, validation/conflicts/errors, missing/unchanged saves, exact selected ID after UI/session mutation, filtering, Add/Edit/Products navigation and read failure are verified. Only UI feedback/return callbacks and the legacy Products destination's data loading are substituted. |
| Phase 3A preservation | All original 14 desktop regression scenarios remain and pass; Category assertions now observe service/repository outcomes instead of the removed DAO preflight internals. Rendered daily checkbox/default policy and product warehouse selection regressions are unchanged. |
| Architecture | 3 source/mapping boundary tests pass. `jdeps --ignore-missing-deps -verbose:package target/classes/com/sobol/category` confirms inward dependencies and no JavaFX/legacy presentation imports in the new core. Domain/application require only java.base; java.sql is confined to infrastructure. |
| Integration suite | All original 17 methods retained; the missing-category method now verifies NOT_FOUND for a valid deleted owned ID. Two Category cases add unchanged-update success and create/unique-conflict round trips. **19 live IT methods compiled, not executed.** |

All 53 FXML/resources remain at their existing paths and the three Category FXML contents are unchanged. CategorySession and its Product consumers remain presentation compatibility boundaries. DBAuth, SalesDAO, ShipmentDAO, ProductDAO, historical V001, Phase 2 safety tooling and unrelated Phase 3A production repairs are unchanged. There was no MySQL connection, SQL import, schema operation or private-backup modification. JavaFX's existing unnamed-module warning and deliberately injected synthetic warehouse failure diagnostic remain expected test output.

The missing/unchanged distinction is verified with scripted 0/1 counts and application/controller tests, not an observed server result. Live Connector/J row counts, collation, SELECT FOR UPDATE behavior, deadlock/disconnect outcomes and the expanded IT cleanup need the still-unprepared sobol_dev environment. Commit failure is explicitly reported as unconfirmed; tests do not prove that a lost response means the server rolled back. No unrelated defect test was skipped or weakened to obtain a green build.

## Phase 4B — independently verified Supplier groundwork, 2026-11-10

Started on clean main at 20b8206f7e166d72575194f31db742b7eee3ffaa, matching remote main. Five Supplier core files introduce immutable identity/contact details, typed application outcomes, service/repository contracts and prepared JDBC persistence. Historical required name/phone, email regex, optional-null behavior, all seven VARCHAR limits and descending-ID list order are preserved/centralized. No schema or existing production caller changes in this groundwork commit; the complete caller migration follows as the second scoped unit.

`mvnw.cmd -B -ntp clean verify`: **67 tests**, 0 failures/errors/skips; 111 production sources compile. `mvnw.cmd -B -ntp -Pdesktop-smoke verify`: **88 tests**, 0 failures/errors/skips. Ten new service/domain tests and sixteen new scripted JDBC tests cover normalization, all length boundaries, required/optional fields, historical email format, duplicates, invalid/missing identity, unchanged update, immutable snapshots, prepared mapping/SQL NULL, generated keys, resource closure, unexpected counts, connection/read/commit/rollback failures and safe diagnostics. Existing Category/desktop tests remain unchanged and pass.

No MySQL session, schema setup or live Integration Test ran. JDBC scripts validate calls/outcomes only; server SQL acceptance, collation and locking remain unverified. Reads own no explicit transaction. Create requires a transaction to validate generated identity before commit; update requires a locked existence check to distinguish missing from unchanged values. Recovery/provenance records remain untouched.
