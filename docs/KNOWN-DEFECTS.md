# Original defects and unavailable functionality

Outstanding original defects are listed below. Verified Phase 3A/4A/4B/4C changes are recorded separately with their verification boundaries.

| Area | Original evidence | Impact / verification boundary |
| --- | --- | --- |
| Shipment form | AddShipment.fxml lacks rbCash/rbCredit/cbMethod; onSave dereferences cbMethod | Save cannot complete without a separate approved fix. |
| Global search | System.fxml lacks searchField; onSearchClicked reads it | Null dereference. |
| Logout | ControllerSystem.logout() is empty | No logout behavior. |
| Attendance | Driver/Sales attendance initialize all present and save per row; hourly cells accumulate listeners | Existing absence can be overwritten; partial saves/row cross-updates are possible. |
| Stock concurrency | SalesDAO/ShipmentDAO read quantities without row locks, then set an absolute value | Transaction exists but lost updates/overselling are possible. |
| Capacity | ProductDAO checks capacity separately from writes; warehouse edits use stale UI deltas | Concurrent writes and aggregate drift are possible. |
| Warehouse summary | COUNT(*) with LEFT JOIN counts an empty warehouse as one product | Incorrect card value. |
| Authentication | Unsalted SHA-256, no authorization/session; registration password uses TextField | Recovery only secures the DB configuration boundary, not the complete authentication design. |
| Original history | Embedded database credentials existed in earlier public commits | Rotate/revoke them; history is preserved without force-push or rewriting. |
| Field administrators | FieldAdministrators.fxml and its controller not found | Keep unavailable; do not fabricate an original implementation. |
| Settings | Original FXML explicitly static, no controller/events; unused SettingsController prints only | Save/update/theme/account controls are not functional. |
| Reports | ReportsController leaves sales analytics for later | Inventory/import/attendance reports exist; complete sales/financial reporting does not. |
| View/social actions | Multiple worker View actions are placeholders; social buttons have no handlers | Do not present these as completed features. |

Recovery-only resource substitutions are listed in RECOVERY.md and the provenance manifest. They are not business fixes. Database-backed runtime behavior remains unverified until a separately authorized isolated schema and synthetic fixtures are prepared.

## Verified Phase 3A repairs — 2026-11-10

| Original defect | Repair | Verified evidence and remaining boundary |
| --- | --- | --- |
| DailyAttendance.fxml used DriverAttendanceController, driver fields and title; the intended daily controller requires lblDate/btnSave and editable Boolean cells. | Bind DailyAttendanceController; provide its date/save fields and Name/Present columns; enable checkbox editing. Preserve GeneralWorkerDAO daily-worker source, existing attendance restoration, missing-record default present, loaded date and AttendanceDAO.saveDaily batch delegate. | Real FXMLLoader/JavaFX tests verify saved absence/presence/default, a rendered checkbox changing DailyAttendanceRow, save-date/row handoff, failure staying on screen and cancel without saving. Actual MySQL persistence is unverified. |
| CategoryController.editCategory() opened EditCategory.fxml without passing the selected Category. | Load the editor with an FXMLLoader instance and call setCategory before displaying it. Require a valid selection from loaded categories; reject a stale session-only ID. EditCategoryController rejects uninitialized/invalid targets, captures the selected ID and normalizes nullable description. | Real FXML tests verify selection-to-form navigation, prefilled values, retained ID even if session/model/display ID changes afterward, blank/duplicate validation, save failure and cancel. CategoryDAO remains unchanged; live SQL and the affected-row defect remain unverified/unfixed. |
| AddProductController selected its first branch before installing the warehouse listener and could retain a previous warehouse after lookup failure. | Install the listener first, clear/disable warehouse state before loading another branch, select the first available warehouse, leave empty/null/failed branches unselected, and reject an out-of-branch or unloaded warehouse before persistence. | Real FXML tests verify initial selection, branch changes, empty/null branches, zero branches, lookup failure and stale values. Existing numerical/stock/capacity validation and ProductDAO are unchanged. No product database write was executed. |

Package-visible DAO/UI delegates permit synthetic controller tests without enabling DBAuth or changing application architecture. Default delegates still call the original static DAOs. The desktop profile runs test classes in separate JVMs because JavaFX cannot restart after Platform.exit. No original test was removed, weakened or disabled; the new graphical tests join the existing opt-in desktop profile, and new display-free contracts run in ordinary verification.

## Phase 4A Category replacement — implemented and verified offline

The earlier Category persistence finding is repaired in the replacement JdbcCategoryRepository and its service-backed UI; CategoryDAO is retired. The repository locks the selected target in a transaction, returns missing if absent, and accepts 0/1 update counts only after existence has been established. Existing unchanged values therefore succeed instead of being confused with a missing record. UNIQUE conflicts and storage failures are separate typed outcomes; controllers stay on the form on missing/conflict/failure rather than falsely reporting success.

Evidence: 9 CategoryServiceTest cases, 12 scripted JdbcCategoryRepositoryTest cases, 3 architecture/mapping tests, and 20 desktop controller regressions (including the original daily/product repairs). Category tests now inject a real service with a fake repository instead of overriding DAO operations. Deleted-after-opening, unchanged-save, creation, duplicate/invalid input, storage/read failure, selection ID retention, search and Products navigation are exercised. FXML files and the unrelated Phase 3A repairs are unchanged.

This is an offline-verified implementation correction, **not live MySQL verification**. The missing-category Integration Test is retained and migrated to the new service with a valid, owned ID deleted before saving; two additional real-MySQL Category tests are prepared. All 19 live IT methods remain unexecuted. Driver row-count mode, actual locking, collation, commit outcomes and cleanup still require isolated MySQL verification. See ARCHITECTURE.md and VERIFICATION.md. The Phase 3A statements about the then-unmodified CategoryDAO are historical status, superseded by this section.

## Phase 2 database investigation (static evidence; no live defect reproduction)

The owner has not prepared isolated database accounts. Integration infrastructure is compiled, but its preflight stops before connecting. The following findings are source/schema conclusions, not observed MySQL test failures. No original application source was changed.

| Severity | Evidence | Impact / remediation direction for a separately approved phase |
| --- | --- | --- |
| High | ShipmentDAO.addShipment() computes currentQty + d.quantity without a positive-quantity check; inserts d.totalCost and caller branch/warehouse directly. V001 has independent FKs and no CHECK constraints. | Negative receipts, mismatched totals and inconsistent branch/warehouse location are not rejected at this persistence boundary. Validate centrally and define location/total invariants before changing behavior. UI validation alone does not establish the DAO invariant. |
| High | SalesDAO.createOrder(), ShipmentDAO.addShipment(): nonlocking SELECT then absolute stock UPDATE; ProductDAO.canFitInWarehouse() is separate from insertProduct()/updateProduct(). | Lost update/overselling/capacity race risks. Real concurrency tests are prepared, unexecuted. Define atomic stock/capacity operations and locking strategy; do not claim existing transactions resolve these races. |
| Medium | SalesDAO/ShipmentDAO/WarehouseDAO outer catch blocks are outside try-with-resources connection scope and contain no explicit rollback; AttendanceDAO batch methods explicitly rollback. | Failure-path consistency depends on connector close behavior for some operations. Verify injected connection/commit failures and make transaction failure handling explicit in Phase 3. This is not a claim that close currently commits partial work. |
| Medium | V001: product.branch_id and product.warehouse_id have separate FKs; employee.managed_branch_id has no FK; user_database.user_id has no FK. | FK-valid rows can still violate intended cross-field/domain consistency; orphan metadata is structurally permitted. Preserve historical DDL now; agree on intended invariants before adding constraints. |

The empty-warehouse COUNT(*) defect still has an enabled correct-behavior Integration Test. The nonexistent-category regression remains enabled through the Phase 4A service/repository replacement described above; its historical tag does not exclude it. See DEVELOPMENT-DATABASE.md for coverage, unexecuted scenarios and setup prerequisites. No live MySQL pass/fail claim is made.

## Phase 4B Supplier replacement — implemented and verified offline

Original SupplierDAO.getAllSuppliers() returned an empty/partial list after errors; nameExists/nameExistsForEdit returned false after errors, and boolean writes collapsed duplicate/storage outcomes. updateSupplier required a positive affected count, conflating an unchanged existing record with failure in changed-row mode. Add/Edit duplicated validation and did not enforce schema lengths before writing.

The production replacement uses SupplierService/JdbcSupplierRepository for all prior DAO consumers, including AddShipmentController lookup and the retained combined Supplier/Customer Integration Test. SupplierDAO is removed. Validation is centralized, preserves the historical email regex and optional-null behavior, and checks all schema lengths without new phone/email uniqueness rules. Locked existence verification distinguishes missing from unchanged updates; UNIQUE conflicts and storage failures produce distinct safe typed feedback. Selection IDs are captured independently of mutable display state. Evidence: 10 service/domain, 16 scripted JDBC, 4 architecture/mapping and 15 real-FXML tests pass. Existing Category tests remain unchanged and pass.

These are offline-verified application/JDBC-call contracts, not live server results. Three additional Supplier IT cases compile; all 22 live methods remain unexecuted. Supplier rows with invalid historical contact data produce an explicit storage error and are not auto-repaired. No schema/data migration occurred. ViewSupplierShipmentsController still uses unchanged ShipmentDAO reads, whose silent failure/empty results and separate count/total snapshots remain legacy debt. AddShipment's missing payment/method controls and save defect remain outstanding; only Supplier lookup compatibility was migrated and tested. Stock/capacity, authentication and unrelated defects above are unchanged.

## Phase 4C Customer replacement — implemented and verified offline

Original CustomerDAO masked read/precheck failures as empty/false, collapsed name/email/storage write failures into booleans, and required positive update counts even for unchanged existing rows. Add/Edit repeated permissive email checks and omitted schema lengths. PickProductsForSaleController retained the picker Customer object directly, so later mutation of that view model could change the Customer ID eventually handed to SaleQuantities.

CustomerService/JdbcCustomerRepository now serve all four production callers and the original IT caller; CustomerDAO is retired. Required/name/phone/email/length rules are centralized while preserving the historical email predicate and null/empty semantics. Locked existence checks distinguish missing from unchanged updates. The two database UNIQUE keys are classified separately from known write-error key tokens; unknown diagnostics fail closed rather than guessing. Driver diagnostics are not exposed in UI errors, causes or suppressed close exceptions. Picker snapshots before loading and product-picker immutable Customer state preserve selected identity independently of mutable source/display state.

Evidence: 12 service/domain, 25 scripted JDBC, 5 architecture/mapping and 16 real-FXML tests pass. The suppressed-close regression was first reproduced and then fixed without weakening assertions. Three new IT cases compile; all 25 live methods remain unexecuted. Source/GUI claims do not prove actual MySQL SQL acceptance, collation, diagnostic format/localization, locking or cleanup. Legacy data outside form invariants may produce an explicit storage error and is not auto-repaired.

Customers.fxml still has a disabled View action and no direct start-sale action. Existing Sales -> New Order -> Customer picker -> Product picker navigation is retained; no global Customer selection or new purchasing/sales feature was added. SaleQuantitiesController and SalesDAO calculations/transactions remain unchanged and unverified against a live database. Existing back-navigation state loss beyond the tested product/customer picker boundary remains legacy behavior. Synchronous UI I/O and older slices' resource-close exception coverage remain follow-up work, not completed fixes in Phase 4C.

## Phase 5A repository security corrections — verified offline

| Finding | Evidence and impact | Correction and actual verification |
| --- | --- | --- |
| Medium: typed Category/Supplier failures could retain raw suppressed JDBC connection-close diagnostics. | JdbcCategoryRepository.insert/update and JdbcSupplierRepository.insert/update threw typed exceptions inside the owned Connection scope. A later close exception was attached to that application failure. Injected duplicate-plus-close and rollback-plus-close tests observed one suppressed diagnostic where none was permitted. Logging the whole exception could disclose driver metadata; no real credential disclosure was observed. | Recreate safe typed outcomes after the outer scope closes; close failures become STORAGE_FAILURE. Regression checks assert null cause, no suppressed exceptions and no synthetic private diagnostic in the message. |
| Medium: non-write failures could be misclassified as duplicate names. | Category translated vendor 1062 in general SQL paths; Supplier translated it in rollback regardless of the failing phase. Tests injected the code into read/close/commit paths and reproduced DUPLICATE_NAME rather than an unconfirmed storage result. These are synthetic fault paths, not observed MySQL commit error codes. | Classify duplicate only at INSERT/UPDATE executeUpdate. Resource/read/commit failures remain safe storage failures. |

Eight new methods per suite bring Category JDBC coverage to 20 and Supplier to 24; Customer's 25 JDBC contracts remain unchanged and pass. The initial focused run had 14 expected regression failures; after correction, clean verify passes 129 and desktop-smoke passes 181 tests, with no failures/errors/skips. Tests include commit response loss before/after a scripted commit and post-commit close failure, without retry or a claim that rollback proves the write did not commit.

Only the two repositories, their tests and supporting notes changed. Business DAOs, UI, validation, locks/transaction boundaries, DBAuth, V001 and historical evidence remain unchanged. Sales/stock/capacity findings above remain source-based risks until their live tests are authorized and executed. No live database acceptance is established by this hardening.
