# PostgreSQL development safety and setup — Phase 6B

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

Set SOBOL_PG_URL to the reviewed explicit endpoint and SOBOL_PG_SSLROOTCERT to the trusted CA file; SOBOL_PG_USER=sobol_app and SOBOL_PG_MIGRATION_USER=sobol_migrator. Enable only that private process with SOBOL_PG_ENABLED=true. Leave **all** operation approvals unset until the respective operation is separately approved. Historical SOBOL_DB_* variables cannot activate a connection.

## Explicit migration gate

Before approval, privately verify exact database/schema/current+session identities, loopback, SSL, role flags/grants and destination objects. Review immutable `src/test/resources/db/postgresql/V1__reference_data.sql`, new `V2__accounts.sql` and both digests in docs/postgresql-migrations.json. The plan is three independent master-data CREATE TABLE statements (Category/Supplier/Customer), one independent account CREATE TABLE and Flyway history/bookkeeping. Existing complete V1 targets advance through V2; published V1 is not edited. It creates no database/schema/role and imports no records. PostgreSQL transactional DDL is expected for this migration; server behavior remains unexecuted. Unknown objects/partial schemas must stop; no automatic destructive recovery.

Only after explicit owner approval of that concrete migration operation, run in the configured private process:

```powershell
$env:SOBOL_PG_MIGRATION_APPROVED = 'sobol_dev/sobol'
.\mvnw.cmd -B -ntp -Ppostgres-migrate test-compile exec:java
Remove-Item Env:SOBOL_PG_MIGRATION_APPROVED
```

This is an instruction for a future approved operation, not a record of execution. The thin entry point validates configuration/session and recognized destination tables, then delegates versioning/checksums/locking/transactional migrations to Flyway. createSchemas=false, cleanDisabled=true, baselineOnMigrate=false, outOfOrder=false, target=2. No clean/repair/baseline command is exposed. Logs discard raw Flyway/driver diagnostics; outer errors are generic and require private diagnosis, not copy/paste of sensitive traces.

After successful migration, an approved administrator grants sobol_app SELECT/INSERT/UPDATE/DELETE on the **three named tables**, USAGE on their **three named identity sequences**, and SELECT on sobol.flyway_schema_history. No table/schema ownership, CREATE, TRUNCATE, role memberships or grant options. Do not grant against all future tables by default. Role/privilege administration itself is separately approved owner work. Verify effective grants privately before IT. Apply the account-specific restricted grants below; do not extend master-data CRUD grants to accounts.

## Explicit live-test gate

Review intended fixture operations, migration version, table constraints, app grants and cleanup before separately approving synthetic DML. Tests never run migrations and must reject missing/wrong targets. Then, and only then:

```powershell
$env:SOBOL_PG_IT_APPROVED = 'sobol_dev/sobol'
# Set only after separate approval of synthetic owner-account fixture DML/cleanup.
$env:SOBOL_PG_ACCOUNT_ADMIN_APPROVED = 'sobol_dev/sobol'
.\mvnw.cmd -B -ntp -Ppostgres-it verify
Remove-Item Env:SOBOL_PG_IT_APPROVED
Remove-Item Env:SOBOL_PG_ACCOUNT_ADMIN_APPROVED
```

The suite covers the migrated master-data slices and six new account contracts. Account IT additionally needs the separately approved owner-account DML described below, before its first connection. Historical 25 MySQL business scenarios are retained as non-executable reference, not silently renamed to PostgreSQL passes. No stock/receiving/sales/HR live correctness claim is made. No original personal records or password hashes are imported. Synthetic fixture cleanup must delete only IDs positively created by the suite; no TRUNCATE, schema reset or broad name-based deletion. Test failure and cleanup failure are reported distinctly without raw SQL diagnostics. Never weaken a correctness assertion to obtain green output.

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

## Phase 6B pgAdmin preparation and restricted account grants - proposal only

Keep the existing exact loopback/database/schema/TLS/role gates. pgAdmin 4 is not proof of PostgreSQL Server readiness. In the intended development server connection only, privately inspect Properties/Connection/SSL and confirm the installed version, numeric endpoint and trusted certificate covering 127.0.0.1. Use verify-full, not a relaxed mode. Do not open unrelated saved servers. After separate creation approval, use the approved sobol_dev database and sobol schema; inspect the owner and privileges in Properties. The new development login roles must have no elevated flags/memberships, distinct private secrets, no app ownership, no database CREATE/TEMP and no unrelated schema rights. Do not paste passwords into Query Tool/history/chat. Configure new-role credentials privately in the role dialog or another approved secret prompt.

After separately approved V1/V2 migration, inspect sobol.account columns, constraints, identity sequence and Flyway versions 1/2. Review effective privileges including PUBLIC and any defaults. The master-data tables keep their previously documented grants. **Do not grant table-level INSERT or UPDATE/DELETE on account, ALL TABLES/FUTURE TABLES, or security-column INSERT.** PostgreSQL adds privileges, so column restrictions do not cancel an existing broad table/PUBLIC grant: remove excessive grants only through a separately reviewed target-specific owner change. Column grants are supported by the [PostgreSQL GRANT reference](https://www.postgresql.org/docs/current/sql-grant.html).

The following is the concrete **future owner-reviewed grant proposal**, not SQL executed by this agent:

```sql
GRANT SELECT ON sobol.account TO sobol_app;
GRANT INSERT (login, display_name, phone, password_hash) ON sobol.account TO sobol_app;
GRANT USAGE ON SEQUENCE sobol.account_account_id_seq TO sobol_app;
```

Keep schema USAGE and Flyway history SELECT as documented. No WITH GRANT OPTION, role/status/version/timestamp/identity INSERT, account UPDATE/DELETE or grant through PUBLIC. PostgresDatabase additionally rejects effective account UPDATE/DELETE and INSERT on protected columns, including grants inherited via table/PUBLIC privileges. Metadata-query SQL and actual grants remain unverified until approved live acceptance. Reading hashes is necessary for this direct-JDBC implementation; a trusted server boundary is required before deploying to untrusted desktops (AUTHENTICATION.md).

## Owner account maintenance - separate approval, NOT EXECUTED

Read AUTHENTICATION.md first. Creation/migration approval is not account-maintenance approval. Review new account identity and intended action privately. Only then, in a private interactive terminal with the dedicated migrator credentials, verified TLS configuration and no output/input redirection:

```powershell
$env:SOBOL_PG_ACCOUNT_ADMIN_APPROVED = 'sobol_dev/sobol'
try {
    .\mvnw.cmd -B -ntp -Ppostgres-accounts test-compile exec:exec '-Dsobol.account.action=bootstrap-admin'
} finally {
    Remove-Item Env:SOBOL_PG_ACCOUNT_ADMIN_APPROVED
}
```

The pinned exec plugin starts a child Java process with inherited terminal I/O and the resolved test classpath; no new dependency/classpath plugin is required. The command refuses a missing Console and never substitutes echoed password input. Terminal behavior itself has not been tested here; if hidden input is unavailable, stop and resolve the terminal privately, never supply credentials as arguments. The confirmation prompt requires exactly `PROVISION sobol_dev/sobol`. bootstrap-admin prompts privately for login/name/new passphrase twice; no default account is created and any existing ADMIN blocks the action. Do not run this command through an automated agent or piped console.

For an opted-in registration request, separately review the exact account ID using pgAdmin **without selecting/copying password_hash**. Run the same gated command with `-Dsobol.account.action=approve-member`. It only approves that PENDING MEMBER and increments auth_version/updated_at. `disable` accepts a reviewed ID and protects the last active ADMIN. Neither action changes MEMBER to ADMIN. Authentication role ADMIN is an application value, not the PostgreSQL sobol_migrator role. Application startup needs only sobol_app credentials; unset owner credentials/approval in the app process. Disable registration again unless requests are deliberately desired.

Forgotten credentials, password/role changes and first-admin disaster recovery have no automatic reset path in Phase 6B. They require a concrete reviewed owner operation, fresh KDF encoding generated through a future safe tool and an auth_version increment. Never accept historical SHA-256, import old user records or weaken V2 to bypass provisioning. A commit/close failure is unconfirmed, not proof that an account was not created; inspect exact state privately before retry.

## Prepared authentication live matrix - six methods NOT EXECUTED

Both SOBOL_PG_IT_APPROVED and SOBOL_PG_ACCOUNT_ADMIN_APPROVED are required. Normal app connections test login/registration/constraints/denied escalation; owner connections only approve/change/delete positively created synthetic account IDs. Both configurations are checked before the first connection. The suite requires exact V1/V2 checksums/history and a suite advisory lock; it never creates or migrates schemas. Fixtures use random synthetic identifiers, real runtime-generated KDF values and no original records. No account hashes are printed or checked into fixtures. Cleanup deletes only IDs returned by this suite's successful account creates. An uncertain create before its returned ID can require separate manual investigation; no name-prefix cleanup is allowed.

| Method in PostgresAuthenticationIT | Intended evidence | Actual result |
| --- | --- | --- |
| pendingDefaultsDuplicateAndClosedRegistrationDoNotGrantAccess | PENDING MEMBER defaults, exact duplicate, closed registration, pending login rejection | NOT EXECUTED |
| ownerApprovalRealKdfLoginWrongPasswordAndLogout | Owner ID approval, real PBKDF2 credential round-trip and logout | NOT EXECUTED |
| ownerStatusRoleAndVersionChangesRevokeExistingSessions | Fresh security state rejects held handles | NOT EXECUTED |
| applicationRoleCannotPromoteActivateModifyOrDeleteAccounts | Real column/table permissions reject escalation/security-state writes | NOT EXECUTED |
| databaseRejectsNoncanonicalLoginLegacyHashAndInvalidSecurityState | Real CHECK constraints, with unconditional probe rollback | NOT EXECUTED |
| accountInsertRollbackAndTimestampTypesAreVerifiedByPostgres | Account rollback and OffsetDateTime mapping | NOT EXECUTED |

There are now **19 prepared live methods: 13 master-data + 6 authentication; zero executed**. This does not establish production login, concurrency, administrative locking, real TLS/privilege-query syntax or fixture cleanup acceptance. Historical MySQL credential revocation remains independently unconfirmed.
