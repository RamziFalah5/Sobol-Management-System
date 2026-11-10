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

## Phase 4B — complete Supplier caller migration, 2026-11-10

Groundwork commit 31420634b09e7221f3c594d2a762629ce3af6113 was verified, security-reviewed, pushed to main and matched against remote main. The completed caller migration replaces SupplierDAO throughout production and CorePersistenceIT, adds SupplierModule plus detached JavaFX mapping, preserves original FXML and shipment persistence, and documents both implemented slices in ARCHITECTURE.md.

| Check | Actual result |
| --- | --- |
| `mvnw.cmd -B -ntp clean verify` | PASS: **71 tests**, 0 failures/errors/skips; all **111 production** and **17 test/tooling** Java sources compile. |
| `mvnw.cmd -B -ntp -Pdesktop-smoke verify` | PASS: **107 tests**, 0 failures/errors/skips, including original startup smoke and all 20 existing controller regressions. |
| New Supplier offline coverage | **45 tests**: 10 service/domain, 16 JDBC, 4 architecture/mapping and 15 real-FXML desktop regressions. The first 30 run without a display; the 15 desktop cases join the explicit profile. All pre-existing tests are retained unchanged except the Supplier caller in CorePersistenceIT, whose round-trip assertion is preserved and strengthened with returned-ID checking. |
| Supplier JavaFX flows | Actual Suppliers/AddSupplier/EditSupplier FXML tests cover column bindings, name/phone/city search, empty selection, Add/Edit navigation, all contact prefills, normalized creation, validation, duplicate/storage feedback, missing target, unchanged update, cancel without a write, invalid setter reset, and selected ID preservation after model/display ID mutation. Forms substitute modal feedback/return callbacks; return-screen database reload is not live-verified. |
| Shipment compatibility | Actual ViewSupplierShipments FXML tests verify selected supplier title/ID, all 11 column values, quantity/cost/date data, count/total, empty data and actual Back-to-Suppliers navigation. Only legacy ShipmentDAO read delegates are replaced. Actual AddShipment FXML runs the production Supplier loader with the real service and fake repository, verifying first/changed selection and IDs, distinct empty/error outcomes and stale-state clearing. Its unrelated static Branch/Warehouse initialization is isolated by the fixture; complete shipment initialization/save is not claimed. |
| Architecture/dependencies | 4 new Supplier boundary/mapping tests and the 3 existing Category architecture tests pass. Compiled `jdeps --ignore-missing-deps -verbose:package` checks for each slice show java.base in inner layers and java.sql confined to Infrastructure, without reverse/JavaFX/legacy presentation dependencies. |
| Resources and scope | All 53 FXML parse/resolve according to existing tests, with the same documented gaps. JAR includes 53 FXML/16 approved PNGs; no SQL/test tooling/private configuration. Baseline comparison confirms all main resources, Category source/tests, DBAuth, pom.xml, V001, protected DAOs and recovery/provenance records unchanged. |
| Integration suite | All prior 19 test methods retained and compiled, including Customer assertions and original defect tests. Three Supplier cases added: unchanged update; generated creation/SQL NULL/read mapping and duplicate insert/update; deleted owned ID -> NOT_FOUND. **22 methods compiled, NOT EXECUTED against MySQL.** |

Surefire forces SOBOL_DB_ENABLED=false in every verification fork. No MySQL connection, setup/import, original accounts access, credential read or private-backup modification was performed. Existing JavaFX unnamed-module warnings and the deliberate synthetic warehouse error diagnostic remain expected output. No test assertion or safety restriction was weakened to obtain these results. Real SQL acceptance, collation/UNIQUE behavior, locking, commit ambiguity and live fixture cleanup still need separately prepared/authorized sobol_dev verification. Full staged scans and whitespace review are required for both scoped commits; see Git history for their exact snapshots.

## Phase 4C — Customer dependency closure and verified groundwork, 2026-11-10

Baseline: clean main a8b2f64b9c8fdf763cfff20ec6c59a2ccb629716, matched to origin/main. Before caller migration, the complete active CustomerDAO closure is CustomersController (list), AddCustomerController (prechecks/insert), EditCustomerController (prechecks/update), PickCustomerForSaleController (list), and CorePersistenceIT (round trip/email conflict). Customer view state also flows through PickProductsForSaleController.setCustomer -> SaleQuantitiesController.setData -> unchanged SalesDAO.createOrder(customerId,...). SalesController opens the picker; ControllerSystem and Dashboard navigate to Customers/Sales. DashboardDAO/ReportsDAO aggregates and SalesDAO joins query customer independently and never call CustomerDAO. SyntheticFixture owns Customer/sales fixture IDs and exact cleanup; it is retained.

Customers.fxml has Add/Edit and a disabled View button; it has no direct start-sale action. The actual existing route is Sales -> New Order -> PickCustomerForSale -> PickProductsForSale. No new Customer-to-Sales button/global selection is planned. The migration will test detached list/picker state and selected identity at the actual sales handoff, preserving existing FXML and navigation. Product selection currently retains a mutable Customer reference: the authorized compatibility fix will snapshot that identity before further handoff, without changing SalesDAO or sales calculations.

Five Customer core files and two test files are added in this independent groundwork unit; legacy callers remain unchanged until the next unit. `mvnw.cmd -B -ntp clean verify` passes **106 tests**; `mvnw.cmd -B -ntp -Pdesktop-smoke verify` passes **142 tests**, both with 0 failures/errors/skips. All 116 application sources and 19 test/tooling sources compile. New coverage: 12 service/domain tests and 23 scripted JDBC contracts. Existing Category/Supplier/UI tests pass unchanged. No MySQL connection or live IT occurred.

Customer-specific contracts preserve all V001 lengths, the historical permissive email predicate (`contains @`, a later dot), blank email -> SQL NULL, and null versus empty city/address/notes. Name/non-null email uniqueness is authoritative at write time. The JDBC implementation recognizes only the two known trailing constraint names in 1062 write diagnostics; missing/unrecognized/localized diagnostics become safe STORAGE_FAILURE, never a guessed name conflict. Duplicate values cannot spoof the trailing key, and read/commit errors are not classified as uniqueness failures. This limited diagnostic parsing follows the documented ER_DUP_ENTRY key-name format; actual driver/server diagnostics still require live acceptance. Reference: [MySQL Server Error Reference](https://dev.mysql.com/doc/mysql-errors/8.0/en/server-error-reference.html).

## Phase 4C — complete Customer migration and Sales compatibility, 2026-11-10

Groundwork 842fa092da3e316d311382d331b0ae5d6b178f54 was security-reviewed, committed/pushed on main and matched to the remote. The second unit migrates all active CRUD/lookup consumers and the original Integration Test, deletes CustomerDAO, adds explicit composition/view mapping, and stabilizes Customer identity through the existing sales UI handoff. No temporary DAO adapter remains.

| Check | Actual final result |
| --- | --- |
| `mvnw.cmd -B -ntp clean verify` | PASS: **113 tests**, 0 failures/errors/skips; **116 production** and **21 test/tooling** Java sources compile. |
| `mvnw.cmd -B -ntp -Pdesktop-smoke verify` | PASS: **165 tests**, 0 failures/errors/skips; includes startup smoke, all 20 existing controller repairs and 15 Supplier desktop tests unchanged. |
| New Customer coverage | **58 offline tests**: 12 service/domain, 25 scripted JDBC, 5 architecture/mapping and 16 JavaFX regressions. The 42 display-free tests run normally; 16 additional graphical cases require desktop-smoke. These are not live MySQL tests. |
| CRUD JavaFX | Actual Customers/Add/Edit FXML covers descending repository order, all table columns, name/phone/email filtering, missing/invalid selection, all prefills, selected ID mutation, required fields/email/schema limits, distinct name/email conflict feedback, missing/unchanged updates, read/write errors and Cancel. Add/Edit success and Cancel navigate to and reload actual Customers.fxml using the same real service/fake repository. |
| Customer-to-Sales | Management list selection is isolated; fixture switches the content area to actual Sales.fxml, fires New Order, loads actual PickCustomerForSale and Select Customer -> actual PickProductsForSale. Mutation during destination loading and after setCustomer, changed source selection and stale management selection cannot change the chosen ID/name. Actual Select Quantity hands the correct detached Customer/product to SaleQuantities.fxml's controller boundary. Actual Back navigates to a fresh picker then Sales. Legacy order/product reads and SaleQuantities.setData are substituted; no sales employee lookup, financial calculation or order write is executed. The unchanged global menu/Dashboard navigation is not dynamically tested here. |
| JDBC safety | Prepared parameter/NULL/empty mapping, generated keys, 0/1 locked update counts, missing target, duplicate name/email, known qualified/unqualified keys, unknown/misleading diagnostics, read/connection/commit/rollback failures and resource closure pass. Final exceptions have no raw driver causes/suppressed diagnostics in the injected close-failure tests. |
| Architecture | 5 Customer tests plus all existing Category/Supplier contracts pass. Separate compiled jdeps checks for all three slices confirm inward dependencies; java.sql is confined to Infrastructure, with no JavaFX/legacy imports in the new inner layers. These tests are not JVM-enforced module isolation. |
| Integration suite | All prior **22 methods retained**, with Customer round-trip/email rejection re-expressed through the new service and unchanged-data checking added. Three Customer cases add unchanged/missing updates, create/read/null/empty mapping and both name/email conflicts on create/update. **25 IT methods compile and remain NOT EXECUTED.** |
| Scope/resources | Source comparison to a8b2f64 confirms Category/Supplier production/tests, all main resources, DBAuth, protected DAOs, SaleQuantitiesController, V001, build/dependency configuration and recovery/provenance records unchanged. JAR checks retain 53 FXML/16 approved PNGs and exclude retired CustomerDAO, SQL/test tooling and private configuration. |

The first complete UI runs passed 111 normal/163 desktop tests before two additional JDBC-close cases were introduced. A focused 25-case repository run then intentionally reproduced one failure: the duplicate error could retain a raw suppressed close exception and remained classified as DUPLICATE_EMAIL. The repository now recreates only a safe typed reason after its connection scope closes, classifying close failures as STORAGE_FAILURE. Assertions were not weakened. Both full final commands above were rerun after this correction and passed. A close failure after a successful scripted commit is explicitly treated as unconfirmed; rollback is not claimed to reverse a commit.

Surefire enforces SOBOL_DB_ENABLED=false. No MySQL login, schema setup, SQL import, real data, original accounts access or backup modification occurred. Existing JavaFX unnamed-module warnings and the deliberate synthetic warehouse failure diagnostic remain expected output. Actual server constraint diagnostics/localization, collation, locking, row counts, commit outcomes and fixture cleanup still require the separately prepared secure sobol_dev environment. Resource-close coverage has not been broadened to the older slices during this Customer-only phase. Full indexed security/whitespace reviews apply to both logical commits; Git records their exact snapshots.

## Phase 5A — narrowly scoped repository security hardening, 2026-11-10

Starting baseline: clean main c1a4d33b9ed04599e237b2099249a88926d8ace4, matching remote main. Changes in this logical unit are limited to JdbcCategoryRepository, JdbcSupplierRepository, their existing scripted tests and these verification/defect notes. No architecture, business rule, SQL statement, schema, dependency, UI or development-connection guard changes.

Eight additional test methods in each repository suite inject connection/statement/result close errors, combined rollback/close errors, misleading non-write vendor codes and lost commit responses before/after a scripted commit. The initial targeted run executed 44 methods: Category 20 with 8 failures; Supplier 24 with 6 failures; total **14 failures, 0 errors/skips**. Failures confirmed raw suppressed driver diagnostics surviving on typed application exceptions and broad 1062 translation outside actual writes. This was controlled in-process JDBC injection, not a live server failure or observed credential disclosure.

Both repositories now recreate only safe typed outcomes after the outer connection scope closes. A suppressed close error yields STORAGE_FAILURE without raw causes/suppressed exceptions. Only executeUpdate on the actual INSERT/UPDATE classifies code 1062 as DUPLICATE_NAME; read, generated-key/resource close, commit and connection errors remain storage failures. Successful duplicate-name behavior, generated keys, missing/unchanged updates and all prior assertions are preserved. No shared repository framework or new transaction policy was introduced.

| Check | Actual result |
| --- | --- |
| `mvnw.cmd -B -ntp clean verify` | PASS: **129 tests**, 0 failures/errors/skips; 116 production and 21 test/tooling sources compile. |
| `mvnw.cmd -B -ntp -Pdesktop-smoke verify` | PASS: **181 tests**, 0 failures/errors/skips; includes all unchanged Category/Supplier/Customer UI and application startup tests. |
| Repository contracts | Category **20**, Supplier **24**, Customer **25** pass. All prior tests remain; 16 new Category/Supplier methods verify diagnostic isolation and phase-specific failure handling. |
| Commit ambiguity | Scripts model a lost response with commit recorded or not recorded, for insert and update. Both return unconfirmed STORAGE_FAILURE and attempt rollback without retry; a rollback attempt does not reverse the script's recorded commit. Close failure after completed commit also remains unconfirmed. No live transaction outcome is inferred. |
| Architecture | Existing architecture/FXML contracts pass. Separate compiled jdeps checks for all three slices retain inward dependencies and java.sql only in Infrastructure. |
| Database boundary | All verification forks keep SOBOL_DB_ENABLED=false. The 25 live IT methods compile unchanged and were not invoked. No database connection, DDL/import or real data was used. |

Existing JavaFX unnamed-module warnings and the synthetic warehouse lookup failure diagnostic remain expected. Architecture documentation is unchanged because the layer/package/dependency structure did not change. Live database activation/acceptance status is a separate documentation unit, not a consequence of these offline passes.

## Phase 5A — activation readiness and live acceptance deferred, 2026-11-10

The independently verified hardening was committed/pushed as d4365a9f2b72dbfe5583f0f39701f1e63aa9dcb0 and matched against remote main after full indexed review (226 files) and whitespace/owner-identity checks. This documentation unit records actual environment observations and the owner-controlled boundary; it does not claim completed database activation.

| Observation | Measured status / classification |
| --- | --- |
| Git baseline | Started on clean main c1a4d33b9ed04599e237b2099249a88926d8ace4 matching remote; no published history rewrite. |
| Rotation/accounts | Owner explicitly answered: rotation unconfirmed; development accounts not ready. Environment/readiness blocker, not an application test failure. No confirmation was set and no account/password was changed. |
| Service/tooling | MySQL93 reports Running. Known-path mysql.exe --no-defaults --version reports client 9.3.0. Workbench executable exists. mysql/mysqlsh were not found on PATH. This command opened no SQL connection and read no default client options. |
| TCP endpoint | Get-NetTCPConnection queries for 3306/3307/33060 and the MySQL93 service PID returned no listener. Port/transport is unverified; this does not establish a server SQL/configuration defect. No setting/service change or permission bypass was attempted. |
| Process environment | SOBOL_DB_ENABLED, URL, USER, PASSWORD, SETUP_USER, SETUP_PASSWORD, ROTATION_CONFIRMED, SETUP_CONFIRM and IT_CONFIRM were all absent. Only presence booleans were output; no environment secret/value was read into the report. |
| Schema/TLS/grants | NOT INSPECTED: no SQL login occurred. sobol_dev existence/version/emptiness, effective application identity, grant scope and encrypted session remain unknown. The historical accounts database was not connected to or inspected. |
| Setup and live IT | Neither db-setup nor mysql-it was invoked. All 25 CorePersistenceIT method bodies remain NOT EXECUTED; no fixtures or cleanup ran. No live pass/fail result, migration failure or stock/concurrency reproduction is available. |
| Offline baseline | 129 normal and 181 desktop tests pass with 0 failures/errors/skips. All 25 IT methods compile unchanged. Compiled jdeps checks pass for Category/Supplier/Customer; FXML checks pass with documented original gaps. JAR inspection confirms 53 FXML/16 approved PNGs and no SQL/test tooling/private environment files. |
| Scope | Compared with the Phase 4C baseline, DBAuth, all development tooling/fixtures/IT assertions, business DAOs, UI/resources, Customer core, build/dependencies, V001 and recovery/provenance are unchanged. No architecture modification, inventory/sales refactoring or historical backup access occurred. |

DEVELOPMENT-DATABASE.md now records NOT EXECUTED on every one of the 25 IT rows and separates their intended evidence from measured results. The next owner action is private historical-credential rotation/revocation, followed by separate loopback accounts/new distinct credentials and least-privilege schema grants, with no historical default schema selected. Configure only a private process and require encrypted 127.0.0.1 transport; never paste secrets into chat. Schema-state/grant/TLS review and explicit approval for the existing 14-table V001 + tooling history operation must precede the setup confirmation. Partial/unknown destinations cannot be reset automatically. Phase 5A is stopped at that boundary; real MySQL acceptance remains incomplete.

## Phase 6A foundation ? offline, 2026-11-10

Started clean main 8ba996702d838a785a2f2f30e13ca8ce18d5a479. Owner confirmed PostgreSQL is not prepared; continue implementation/offline tests only. No database connection, DDL, migration or live test was run. Historical MySQL rotation remains unconfirmed.

Foundation clean verify: 134 tests, zero failures/errors/skips, 118 production sources and 20 test/tooling sources compile. The old 25-method MySQL IT suite, runner, fixture/barrier and associated six MySQL tooling tests move intact to non-executable reference; four obsolete DBAuth configuration cases are replaced with equivalent PostgreSQL boundary coverage. Existing slice/security/FXML tests are retained. Nine PostgresDatabase tests, one structured-error test and four Flyway/migration plan tests run without SQL. Flyway configuration loads without connecting. An initial Java lambda capture compile error and an unsupported Flyway logger name were corrected before the green run; a real supported LogCreator now discards raw diagnostics. No failed assertion was removed.

Foundation desktop verification initially encountered NoClassDefFoundError/ClassNotFoundException in two Surefire forks despite class files being present. A clean desktop rerun passed 185 tests before the extra destination-state regression; no assertion/configuration was weakened. Cause of the transient fork error is unconfirmed. Final foundation desktop verify passed 186 tests, zero failures/errors/skips, including the added destination-state regression.

## Phase 6A Category slice

clean verify passes 137 tests with zero failures/errors/skips; 118 production/21 test-tooling sources compile. Category JDBC 21, service 10 and architecture 4 tests pass. Existing resource/close/rollback/ambiguous-commit assertions remain, now injecting structured PostgreSQL protocol errors. Zero matched rows after a locked existence read is deliberately a PostgreSQL storage failure; unchanged values still match one row. New tests reject wrong schema/table/constraint/state and verify exact-case name policy and identity-column requests. The actual Category FXML selection/edit tests remain; the inventory navigation assertion now verifies the explicit unavailable boundary. Four PostgresPersistenceIT methods compile and are NOT EXECUTED. Desktop verify passes 189 tests with zero failures/errors/skips.

## Phase 6A Supplier slice

clean verify passes 140 tests with zero failures/errors/skips. Supplier JDBC 25, service 11 and architecture 5 pass; the 15 real-FXML Supplier tests preserve CRUD/null/identity coverage and verify the explicit unavailable shipment route. Isolated historical shipment view mappings remain tested via synthetic delegates, not a live receiving claim. All prior safe resource/rollback/commit tests now use structured PostgreSQL errors, with additional unknown-constraint/wrong-table/schema/state rejection. The three added Supplier live methods bring PostgresPersistenceIT to seven methods, all NOT EXECUTED. Desktop verify passes 192 tests with zero failures/errors/skips.
