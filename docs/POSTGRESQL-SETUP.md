# PostgreSQL development safety and setup — Phase 6A

## Current status and authority

Owner response: **Not prepared; continue implementation and offline tests.** No PostgreSQL connection, target inspection, database/schema/role creation, migration, fixture write or cleanup has been authorized/executed. pgAdmin is a client; its presence is not server readiness evidence. No PostgreSQL server version, TCP endpoint, certificate or privilege state has been verified. Historical MySQL credential revocation/rotation remains an independent outstanding security task; changing database direction does not revoke it.

Only numeric `127.0.0.1:<explicit-port>`, database `sobol_dev`, schema `sobol` are accepted. Historical accounts and all unrelated databases are off-limits. No default target/credentials, URL options, remote/failover hosts, MySQL URLs or credential-bearing URLs are accepted. Use newly generated development-only secrets. Do not paste secrets, certificates containing private keys, environment dumps or driver diagnostics into chat.

## Owner preparation — proposal only, not executed approval

1. Privately confirm a supported PostgreSQL 16–18 server installation/service and the numeric loopback endpoint. Use pgAdmin connection properties to avoid unrelated saved server connections. Do not install/upgrade/reconfigure servers implicitly. Stop if server or encrypted transport is unavailable.
2. Review a concrete plan for a **new empty** dedicated sobol_dev database and sobol schema, plus sobol_migrator and sobol_app login roles. Database/schema/account creation requires separate explicit owner approval; none has been given. Unknown or partially used targets must not be dropped/reset/repaired.
3. After approval, an authorized local administrator prepares only that target and two distinct new secrets using private prompts. Neither role may have SUPERUSER, CREATEDB, CREATEROLE, REPLICATION, BYPASSRLS or membership in other roles. App must not own the database, schema, tables or sequences. Migrator owns only sobol schema/objects, has CONNECT on sobol_dev and no database CREATE; app has CONNECT and schema USAGE. Revoke default PUBLIC CREATE/TEMP and unrelated privileges where appropriate **only on the approved target**. Do not change unrelated databases/server settings. The guard checks role flags, membership, effective database CREATE/TEMP, expected schema CREATE, identity, numeric endpoint, negotiated SSL, creation rights outside sobol, application object ownership and selected excess/cross-schema table privileges; a full privilege review by the owner remains necessary, including PUBLIC/inherited grants and access to other schemas/databases.
4. Review and configure trusted TLS. JDBC fixes sslmode=verify-full and requires an explicit absolute path to a trusted CA certificate. Server certificate must cover IP 127.0.0.1. Do not change TLS to require/prefer/disable or add driver URL hooks to proceed. No CA/private-key generation or server configuration is executed by the agent. Missing trust/certificate support is a setup blocker.
5. Set process-only settings from config/development.env.example in a private terminal. The example is not an automatic loader. Prompt for each password privately; do not use CLI password arguments, Maven properties, shell history literals, debug output or committed local config. Environment strings are plaintext within the private process, not an enterprise secret manager. Variables in another shell do not change this agent's existing environment.

Example private prompt helper (owner-run only):

```powershell
function Set-SobolPrivateValue([string]$Name) {
    $secureValue = Read-Host $Name -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        [Environment]::SetEnvironmentVariable($Name,
            [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer), 'Process')
    } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}
Set-SobolPrivateValue 'SOBOL_PG_PASSWORD'
Set-SobolPrivateValue 'SOBOL_PG_MIGRATION_PASSWORD'
```

Set SOBOL_PG_URL to the reviewed explicit endpoint and SOBOL_PG_SSLROOTCERT to the trusted CA file; SOBOL_PG_USER=sobol_app and SOBOL_PG_MIGRATION_USER=sobol_migrator. Enable only that private process with SOBOL_PG_ENABLED=true. Leave **both** operation approvals unset until the respective operation is separately approved. Historical SOBOL_DB_* variables cannot activate a connection.

## Explicit migration gate

Before approval, privately verify exact database/schema/current+session identities, loopback, SSL, role flags/grants and destination objects. Review `src/test/resources/db/postgresql/V1__reference_data.sql` and its digest in docs/postgresql-migrations.json. The plan is exactly three independent master-data CREATE TABLE statements (Category/Supplier/Customer) plus Flyway's own history table and migration bookkeeping. It creates no database/schema/role and imports no records. PostgreSQL transactional DDL is expected for this migration; server behavior remains unexecuted. Unknown objects/partial schemas must stop; no automatic destructive recovery.

Only after explicit owner approval of that concrete migration operation, run in the configured private process:

```powershell
$env:SOBOL_PG_MIGRATION_APPROVED = 'sobol_dev/sobol'
.\mvnw.cmd -B -ntp -Ppostgres-migrate test-compile exec:java
Remove-Item Env:SOBOL_PG_MIGRATION_APPROVED
```

This is an instruction for a future approved operation, not a record of execution. The thin entry point validates configuration/session and recognized destination tables, then delegates versioning/checksums/locking/transactional migrations to Flyway. createSchemas=false, cleanDisabled=true, baselineOnMigrate=false, outOfOrder=false, target=1. No clean/repair/baseline command is exposed. Logs discard raw Flyway/driver diagnostics; outer errors are generic and require private diagnosis, not copy/paste of sensitive traces.

After successful migration, an approved administrator grants sobol_app SELECT/INSERT/UPDATE/DELETE on the **three named tables**, USAGE on their **three named identity sequences**, and SELECT on sobol.flyway_schema_history. No table/schema ownership, CREATE, TRUNCATE, role memberships or grant options. Do not grant against all future tables by default. Role/privilege administration itself is separately approved owner work. Verify effective grants privately before IT.

## Explicit live-test gate

Review intended fixture operations, migration version, table constraints, app grants and cleanup before separately approving synthetic DML. Tests never run migrations and must reject missing/wrong targets. Then, and only then:

```powershell
$env:SOBOL_PG_IT_APPROVED = 'sobol_dev/sobol'
.\mvnw.cmd -B -ntp -Ppostgres-it verify
Remove-Item Env:SOBOL_PG_IT_APPROVED
```

The suite is limited to migrated master-data slices. Historical 25 MySQL business scenarios are retained as non-executable reference, not silently renamed to PostgreSQL passes. No stock/receiving/sales/HR live correctness claim is made. No original personal records or password hashes are imported. Synthetic fixture cleanup must delete only IDs positively created by the suite; no TRUNCATE, schema reset or broad name-based deletion. Test failure and cleanup failure are reported distinctly without raw SQL diagnostics. Never weaken a correctness assertion to obtain green output.

Offline clean verify/desktop-smoke force SOBOL_PG_ENABLED=false and SOBOL_DB_ENABLED=false in test JVMs. They compile live IT/tooling but do not execute them. Live migration/schema/constraints/CRUD/rollback/decimal probes remain NOT EXECUTED until the gates above are met. FK and inventory/financial workflow tests are deferred with their absent entities, not passing V1 coverage.

## Prepared live matrix - no methods executed

All 13 methods in PostgresPersistenceIT are **NOT EXECUTED**. There are zero live passes/failures/fixture writes/cleanup runs. Test presence and compilation do not establish PostgreSQL acceptance. The suite holds a dedicated advisory lock and checks Flyway version/checksum before DML. Cleanup confirms absence of each positively owned ID; commit-response loss before an identity is returned can still leave an unconfirmed synthetic row requiring a separately reviewed private investigation. Never retry a create automatically or broaden deletion to a name/prefix.

| Method | Intended evidence | Actual result |
| --- | --- | --- |
| schemaAndIdentityMatchOnlyTheImplementedMasterData | Exact target and master-data table set | NOT EXECUTED |
| categoryCreateReadUpdateAndUnchangedValuesUseTheRealRepository | Category identity/read/update/unchanged | NOT EXECUTED |
| categoryExactDuplicateRollsBackButCaseVariantsRemainDistinct | Category exact UNIQUE and rejected-update preservation | NOT EXECUTED |
| deletedCategoryUpdateReportsMissingWithoutCreatingAnotherRow | Category missing target | NOT EXECUTED |
| supplierCrudUnchangedAndNullFieldsUseTheRealRepository | Supplier identity/contacts/nulls/unchanged | NOT EXECUTED |
| supplierUniqueNameRejectsConflictsWithoutAddingContactUniqueness | Supplier exact UNIQUE and nonunique contacts | NOT EXECUTED |
| deletedSupplierUpdateReportsMissing | Supplier missing target | NOT EXECUTED |
| customerSameNameCrudAndUnchangedUpdatesPreserveDistinctIdentities | Customer repeated names and ID-safe CRUD | NOT EXECUTED |
| customerExactEmailConflictsRollBackAndCaseVariantsRemainDistinct | Customer exact optional-email UNIQUE | NOT EXECUTED |
| deletedCustomerUpdateReportsMissing | Customer missing target | NOT EXECUTED |
| databaseEnforcesRequiredLengthsAndIdentityWithoutInventedForeignKeys | Required/CHECK/length/ALWAYS identity; absence of deferred FK entities | NOT EXECUTED |
| failingSecondStatementRollsBackTheFirstSyntheticWrite | Real transaction abort/rollback after prior write | NOT EXECUTED |
| postgresDecimalAndTimestampProbeUsesExactJdbcTypes | BigDecimal NUMERIC expression and OffsetDateTime timestamp probe only | NOT EXECUTED |

The decimal probe is not a migrated financial workflow. No FK-dependent operational entity exists in V1: positive FK/RESTRICT/posted-document tests, inventory concurrency, monetary rounding/tax/settlement and attendance persistence are deferred with their complete slices. Constraint probes use rollback/savepoints even if invalid data is unexpectedly accepted. Offline fake ResultSets do not verify privilege-query SQL against PostgreSQL.
