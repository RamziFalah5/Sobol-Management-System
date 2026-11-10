# Isolated MySQL foundation — Phase 2

## Observed status, 2026-11-10

The local Windows service `MySQL93` is running and MySQL Server 9.3 is installed. A running service does not prove TCP access, usable credentials or permissions. No MySQL login, server configuration change, schema creation, historical import or database-backed DAO execution was performed in this phase. The owner confirmed that the development accounts/environment are not prepared and requested tooling/instructions only. Old credential rotation is **unverified and outstanding**. Do not set the rotation confirmation until the owner has actually rotated/revoked the compromised historical credentials.

The implemented foundation compiles and has offline tests. Live DDL execution, grant compatibility, fixture creation/cleanup and all 17 Integration Test methods remain unverified. Phase 2's live acceptance criteria are incomplete. Never interpret the expected preflight failure as successful persistence testing.

## Architecture and historical fidelity

```text
normal clean verify -> Surefire -> DB forcibly disabled
db-setup + confirmations -> test-scope DevDatabase -> setup account
                         -> 127.0.0.1 / no default schema
                         -> explicitly qualified sobol_dev DDL only
mysql-it verify + confirmations -> Failsafe -> limited sobol_app
                               -> validate DATABASE(), CURRENT_USER(), SHOW GRANTS
                               -> validate V001 history, acquire development lock
                               -> per-method artificial fixtures -> original DAOs -> scoped cleanup
```

`src/test/resources/db/migration/V001__historical_schema.sql` contains exactly the 14 historical CREATE TABLE definitions. Only FK creation order, explicit schema qualification and removal of table-level historical AUTO_INCREMENT counters differ from the reference DDL. Column types, nullability, defaults, primary keys, all unique/ordinary indexes, FK names/actions, InnoDB and utf8mb4_0900_ai_ci are preserved. `docs/schema-provenance.json` records the reviewed migration digest and each normalized historical definition digest. No original INSERT, account record, password hash, personal data, DROP, USE, server setting or FK disabling is present.

Creation order: branch, category, customer, supplier, user_account, user_database, vehicle, warehouse, employee, product, attendance, import_shipment, sales_order, sales_order_item. There are **15 historical FKs**. The historical user_database table has no FK; managed_branch_id has no FK; product/import branch and warehouse are independently referenced; no CHECK constraints enforce positive stock, capacity, rates or totals; duplicate branch-name indexes remain. None of these limitations was silently repaired.

Versioning is intentionally small: one immutable V001 resource and one extra **tooling** table `_sobol_dev_schema_history` (version, SHA-256 checksum, installation time). This is 14 business tables plus one infrastructure table, not a business redesign. Setup refuses partial, unknown or checksum-mismatched states. Re-running a completed setup verifies the history instead of recreating tables. It does not overwrite records or provide reset/repair commands. Future schema changes need separately reviewed V002 tooling/approval; do not edit installed V001.

MySQL DDL implicitly commits. A sequence of CREATE statements is **not** one rollbackable transaction. A failure can leave a partial development schema: the runner stops, does not mark V001 installed, and refuses automatic recovery. Inspect only sobol_dev and agree on a bounded repair before further DDL. History checksum checks resource/version integrity; it is **not** a complete detector of subsequent manual column/index changes. The live metadata test checks table/FK counts; complete live metadata drift verification remains a follow-up. [MySQL atomic DDL documentation](https://dev.mysql.com/doc/refman/8.0/en/atomic-ddl.html).

## Safe local preparation (owner-controlled; not executed)

1. Rotate/revoke credentials exposed in historical source/history. Record completion privately without recording either old/new values. Do not try the old credential or access the historical database to prove rotation.
2. Use the existing local MySQL service; no server configuration change is required by this tooling. Confirm it accepts **127.0.0.1** on the selected port using an administrative tool privately. Do not select a historical default schema or import the original dump. A numeric address is mandatory for the new tools; DBAuth itself is unchanged.
3. In a private administrative session, create **new** loopback-only accounts `sobol_setup` and `sobol_app`, with different newly generated passwords. Use the account administration UI/private secure mechanism; never put password-bearing CREATE USER commands in shell history, Git, screenshots or reports. Do not reuse/alter historical accounts. Do not grant roles or global privileges to sobol_app.
4. The administrator may issue these password-free grants for those newly created accounts. They grant no access to other databases and no GRANT OPTION. Match the exact loopback host used for the new account; the runner accepts localhost or 127.0.0.1 account host identities. Do not use `%`.

```sql
GRANT CREATE, SELECT, INSERT, REFERENCES ON `sobol\_dev`.* TO 'sobol_setup'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `sobol\_dev`.* TO 'sobol_app'@'localhost';
```

The escaped underscore is deliberate: database grants can otherwise match additional schema names. The runner rejects unescaped schema grants, DDL/admin/global grants, roles and GRANT OPTION for the application identity. SHOW GRANTS output is checked in memory and never logged. This conservative allowlist can reject additional legitimate grants; inspect locally instead of weakening it or granting more privileges. No user creation/grant SQL is executed by the runner. Setup/application accounts must already exist. [MySQL GRANT documentation](https://dev.mysql.com/doc/refman/8.4/en/grant.html).

5. Configure only the process environment, without printing it. The `.example` file is documentation, not an environment loader. In a private PowerShell session:

```powershell
$env:SOBOL_DB_ENABLED = 'true'
$env:SOBOL_DB_URL = 'jdbc:mysql://127.0.0.1:3306/sobol_dev?sslMode=REQUIRED&serverTimezone=UTC'
$env:SOBOL_DB_USER = 'sobol_app'
$env:SOBOL_DB_SETUP_USER = 'sobol_setup'

function Set-SobolPrivateEnvironment([string]$Name) {
    $secureValue = Read-Host $Name -AsSecureString
    $privatePointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        [Environment]::SetEnvironmentVariable($Name,
            [Runtime.InteropServices.Marshal]::PtrToStringBSTR($privatePointer), 'Process')
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($privatePointer)
        $secureValue.Dispose()
    }
}
Set-SobolPrivateEnvironment 'SOBOL_DB_PASSWORD'
Set-SobolPrivateEnvironment 'SOBOL_DB_SETUP_PASSWORD'
# Only AFTER the owner confirms actual rotation/revocation:
$env:SOBOL_CREDENTIAL_ROTATION_CONFIRMED = 'true'
$env:SOBOL_DB_SETUP_CONFIRM = 'sobol_dev'
```

Do not pass passwords as Maven properties/CLI arguments. Do not use Maven debug output, environment dumps or print configuration objects. The example requires the server's existing TLS support, which remains unverified. If unavailable, stop and record that setup blocker; do not change server configuration or enable public-key retrieval/unsafe URL hooks to make authentication pass. New caching_sha2_password accounts may require a secure transport for initial authentication. `REQUIRED` provides encryption, not full certificate/hostname verification; this numeric-loopback development configuration is not an enterprise TLS deployment. Environment values exist as plaintext in the process as required by JDBC; this is development configuration, not an enterprise secret manager.

6. Review V001, ensure the target is empty or already installed by this runner, then explicitly run:

```powershell
.\mvnw.cmd -B -ntp -Pdb-setup test-compile exec:java
```

This separate command opens a server connection without a default database, verifies the authenticated setup identity, takes `sobol_dev.foundation`, creates only sobol_dev and its versioned tables if empty, and verifies limited application access. URI validation, explicit target/rotation confirmations, separate credentials and numeric loopback are checked **before** any connection. Missing prerequisites stop execution. A connection failure is sanitized; after connection, setup error messages are deliberately generic to avoid leaking driver metadata. Inspect permissions/version privately. Connection timeouts are bounded in tooling. Do not add the setup profile to ordinary builds or application startup.

7. Remove setup credentials from the process before using the app/test account:

```powershell
Remove-Item Env:SOBOL_DB_SETUP_PASSWORD, Env:SOBOL_DB_SETUP_USER, Env:SOBOL_DB_SETUP_CONFIRM -ErrorAction SilentlyContinue
$env:SOBOL_IT_CONFIRM = 'sobol_dev'
.\mvnw.cmd -B -ntp -Pmysql-it verify
```

Failsafe does not initialize schemas or silently skip a missing MySQL environment. Surefire still runs with DB access off; Failsafe inherits the explicitly supplied environment in a separate JVM. Parallel JUnit execution is disabled because a controlled driver timing seam temporarily wraps the real MySQL driver. A MySQL named lock serializes cooperating setup/test runs; it does not lock out the desktop application or other SQL clients. Close the desktop application and avoid manual development writes during tests.

8. Afterward remove application password and confirmations from the process, or close the private shell. Never copy Maven reports containing private environment/debug output into Git. This suite is designed to emit only artificial fixture diagnostics, and DBAuth sanitizes connection failures.

## Synthetic fixtures and cleanup

`SyntheticFixture` seeds each test with fixed semantic values and date 2026-01-15, generated PKs, and an artificial `SOBOL_IT_<namespace>` marker. The namespace prevents unique-key collisions between cases; generated IDs/namespaces are not expected to be identical across executions. No real contact details are used: example.invalid and all-zero phone values are artificial.

Fixtures cover all 14 business tables: branches; main/empty/near-capacity warehouses (100/3/10 pallets); categories; quantities 20/3/0/81 with 10 cartons per pallet and min/max 5/100; supplier/import; customer/two-item sale; active daily/hourly worker, inactive worker, driver and sales representative; assigned/free vehicles; daily/hourly/driver attendance; user_account plus user_database metadata. The artificial login hash is computed at runtime and never printed/stored in Git. user_database metadata does not create another database. Seed order totals equal their item totals; starting stock represents stock after the historical synthetic seed events, not a new inventory ledger.

Before fixture writes, the invalid-ID rejection-test sentinels are checked for absence; an occupied sentinel stops the suite instead of modifying an unrelated row. After each method, cleanup uses exact newly generated root IDs and their fixture-owned descendants in FK-safe order, inside one transaction. There is no global DELETE, LIKE-prefix sweep, TRUNCATE, DROP or FK disabling. On partial seed failure cleanup is attempted immediately. A cleanup failure is reported as an infrastructure error and rolls back cleanup; manual investigation must use that run's known IDs, never a global reset. Cleanup cannot guarantee recovery after process kill/server loss, and does not reset AUTO_INCREMENT sequences. Do not attach unrelated development rows to fixture roots while a test runs. Live cleanup behavior remains unverified.

## Integration coverage and interpretation

All methods below are compiled but **NOT EXECUTED against MySQL** in Phase 2:

| Methods in CorePersistenceIT | Intended evidence |
| --- | --- |
| dedicatedConnectionAndHistoricalSchemaAreAvailable | Effective application identity/grants, V001, 15 total tables, 15 FKs |
| branchCreationWarehouseUpdatesAndAggregatesPersist | Original BranchDAO/WarehouseDAO insert/read/update/delete and branch aggregates |
| productDaoPreservesRelationsPricesAndThresholds | ProductDAO insert/read/update/delete; relationships, prices, min/max |
| capacityUsesRoundedPalletsAndExcludesCurrentProduct | Pallet rounding, capacity limits, low/zero stock buckets |
| emptyWarehouseHasZeroProducts | Correct expected result for known COUNT(*) defect |
| supplierAndCustomerDaoRoundTripsAndUniqueEmail | SupplierDAO/CustomerDAO update/read, duplicate email rejection |
| shipmentCommitsHeaderAndInventoryTogether | ShipmentDAO insertion, stock increment, supplier totals |
| shipmentRejectsOverflowAndInvalidForeignKeyWithoutChanges | Capacity rejection and failed-FK path; stock/header unchanged |
| multiItemSaleUsesCurrentPricesAndDeductsStock | SalesDAO order/items, stored DECIMAL totals, stock deductions |
| failedSecondSalesItemRollsBackFirstItemHeaderAndStock | Failure after first item changed stock; header/items/stock atomicity, invalid quantity |
| employeeInsertionDriverAssignmentAndSoftDeletePersist | EmployeeDAO creation, Worker/GeneralWorker/TruckDriver retrieval, VehicleDAO availability, SalesRepDAO deactivation |
| dailyHourlyAndDriverAttendanceUpsertWithoutDuplicates | AttendanceDAO daily/hourly/driver round trips and unique-day upsert |
| attendanceBatchRollsBackWhenSecondEmployeeDoesNotExist | Batch rollback on invalid second FK |
| databaseEnforcesUniqueKeysAndForeignKeys | Duplicate branch/vehicle assignment, invalid FK, RESTRICT/CASCADE, synthetic account mapping |
| nonexistentCategoryUpdateMustReportFailure | Correct affected-row expectation for known CategoryDAO defect |
| concurrentSalesMustNotOversellTheSameStock | Two actual SalesDAO calls synchronized after buffered nonlocking product SELECT; only one sale of 8 may consume stock of 10 |
| concurrentCapacityChecksMustNotAllowWarehouseOverflow | Two real capacity-check/insert callers approve 2 pallets against the same capacity of 3 |

There are 17 Integration Test methods. Their source presence and successful compilation do not establish execution or successful persistence.

Known-correctness tests carry `original-defect` tags but are **not excluded**. They are expected to expose original defects; a red test must be classified with evidence, not weakened or converted into a skip. Preflight failure prevents all method bodies from running; a JUnit class-initialization error is not 17 failed business scenarios.

The stock timing seam changes only scheduling/JDBC delegation inside the IT JVM; original SQL, transactions and results remain real. It relies on Connector/J's default buffered result sets and the current product-read query shape; a changed query must cause an explicit seam assertion failure. A future correct locking strategy may need a different synchronization point, since forcing both readers past a locking SELECT can itself deadlock the harness. Timeouts/instrumentation errors are test-infrastructure failures, not evidence of overselling. Capacity race outcomes require at least one actual insertion. No passing race test proves global race freedom.

## Findings and remaining work

- **Verified statically:** SalesDAO/ShipmentDAO read stock without FOR UPDATE then overwrite an absolute quantity; ProductDAO separates capacity approval from insert; WarehouseDAO accepts UI-provided stale deltas. Lost updates, overselling and capacity/aggregate drift are risks, not newly observed runtime results.
- **Verified statically:** SalesDAO and ShipmentDAO use transactions and explicit rollback for selected rejection paths, but their outer exception paths rely on connection-close behavior rather than explicit rollback. Attendance batch methods explicitly roll back. The live suite tests sales/attendance mid-operation rejection and shipment pre-update failure; injected connection/commit failures and shipment failure after header insertion remain additional coverage needs.
- **Verified statically:** ShipmentDAO accepts caller-supplied totals/locations and has no positive-quantity validation; schema has no corresponding CHECK. No negative-quantity or inconsistent-total fix was made.
- **Financial boundary:** Tests compare persisted DECIMAL values for multi-item cent-valued orders; they do not prove correctness over all monetary ranges, rounding rules, commissions, taxes, returns or a complete ledger. Original Java uses double; business rounding policy remains undefined.
- **Untested:** All live SQL syntax/grants/fixtures/DAO workflows; simultaneous import/sale and import/import races; disconnect/deadlock/commit-failure recovery; complete metadata drift; app login/authorization/UI integration; full DashboardDAO/ReportsDAO analytics. Fixture presence does not verify their consumers.

## Completing Phase 2 before Phase 3

Provide the new accounts and private process configuration, confirm actual historical credential rotation, run guarded setup once, then run mysql-it. Record per-method pass/fail/error, SQLState/category without private values, and cleanup results. Separate infrastructure/test-harness defects from original application defects. Commit measured results separately when available. Only afterward scope Phase 3 fixes: stock/capacity transaction boundaries and affected-row correctness first, then independently approved UI/authentication/financial work. No Phase 3 behavior change is included here.
