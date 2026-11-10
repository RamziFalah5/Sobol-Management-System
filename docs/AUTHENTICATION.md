# PostgreSQL authentication and shell - Phase 6B

Implemented and tested offline; live PostgreSQL acceptance is pending. No database connection, provisioning, migration, live test or historical credential/user import was executed. This document supersedes the Phase 6A statement that login/logout are unimplemented. It does not declare the desktop production-ready.

## Architecture and boundary

```text
Main -> AuthModule.Context -> Signin.fxml / Main1.fxml -> Controller1
   -> AuthenticationService -> AccountRepository <- JdbcAccountRepository -> PostgresDatabase
   -> PasswordHasher <- Pbkdf2PasswordHasher
   -> LoginThrottle + SessionManager -> immutable identity + private handle
Authenticated ControllerSystem -> Home / Category / Supplier / Customer
   -> session-capturing composition roots -> existing services/repositories
   -> require(same handle) before each data connection -> PostgresDatabase
```

domain/application depend only inward and on the JDK. JavaFX, concrete composition and JDBC stay outside. Authentication JDBC uses bound parameters, owned connections/resources, an explicit registration transaction and exact PostgreSQL `uq_account_login` write-phase conflict classification. Commit-response loss and post-commit close failure are unconfirmed storage outcomes; there is no automatic retry. Safe errors discard raw driver causes and suppressed diagnostics.

This is a controlled desktop foundation, **not a trusted server authorization boundary**. A desktop user who controls the executable or shared `sobol_app` database credentials can bypass client-side session checks and read/write whatever the database role permits, including account hashes through its necessary SELECT privilege. Do not distribute these credentials to untrusted clients or deploy this design across enterprises as-is. A future trusted service boundary and centrally enforced authorization would address that threat; neither is introduced here.

## Passwords and account policy

- Standard JCE `PBKDF2WithHmacSHA256`: 600,000 iterations, independent SecureRandom 16-byte salt, 32-byte derived key. Encoded parameters are versioned. Strict algorithm/version/work-factor/size parsing rejects legacy SHA-256, unknown versions and attacker-supplied expensive parameters. Verification uses MessageDigest.isEqual. No historical hash compatibility or automatic rehash/import.
- PBKDF2 is justified here by the maintained JDK implementation and no new native/runtime dependency. The work factor follows the [OWASP password storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html); this is not a FIPS certification claim. Argon2id remains a future option, not an installed framework. Algorithm/parameter changes need an explicit version and migration/schema-policy review, since V2 constrains the accepted encoding.
- New passwords require 15-128 Unicode code points, cannot be whitespace-only, retain spaces, and have no forced upper/lower/digit composition. The KDF input is bounded to 256 UTF-16 units. Login never trims passwords. Character arrays are wiped and PasswordFields cleared; JavaFX/JCE/String copies and encoded hash Strings cannot be guaranteed erased from JVM memory.
- Login is a trimmed, lower-case ASCII email-style identifier, at most 254 characters. Case variants refer to one login. Gmail-only and artificial name-with-digit rules are retired for new accounts. Display name: trimmed, required, at most 160 Unicode code points. Phone: optional, at most 60 code points. This is deliberately a new account policy, not a silent change to business master-data validation.
- Registration is **closed by default**. Only exact `SOBOL_REGISTRATION_ENABLED=true` in the private application process enables requests. The UI and repository cannot choose role/status: every request is `PENDING MEMBER`. It never starts a session. New and duplicate identifiers get the same success message; failures never overwrite an existing account. Email ownership verification, password reset/change, MFA, account-management UI and security audit-event storage are not implemented.
- Wrong passwords, unknown identifiers, pending and disabled accounts share a generic login failure. Unknown valid identifiers perform a dummy KDF. This reduces enumeration differences but is not a formal constant-time endpoint guarantee. A process-wide shared throttle allows 5 requests per identifier/300 seconds and 10 requests globally/60 seconds, using bounded identifier storage. Registration shares the budget. Logout does not reset it. Restarting another process can bypass this local budget; distributed enforcement requires a trusted backend. See [OWASP authentication guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).

## V2 account schema

`src/test/resources/db/postgresql/V2__accounts.sql` adds only `sobol.account`. Published V1 is unchanged; Flyway target is 2 and both digests are allowlisted. No users, passwords, defaults for an administrator, tenant mapping or per-user database are seeded.

| Column | Contract |
| --- | --- |
| account_id | Positive bigint, GENERATED ALWAYS AS IDENTITY, PK |
| login | Required VARCHAR(254), C collation, lower-case/trimmed CHECK, UNIQUE uq_account_login |
| display_name / phone | Required trimmed VARCHAR(160) / optional VARCHAR(60) |
| password_hash | Required VARCHAR(256); CHECK allows only versioned PBKDF2 v1/600000 with expected Base64 lengths |
| status | PENDING default, ACTIVE or DISABLED, CHECK |
| role | MEMBER default or ADMIN, CHECK |
| auth_version | Positive bigint, default 1 |
| created_at / updated_at | Required TIMESTAMP WITH TIME ZONE, CURRENT_TIMESTAMP defaults |

The PK/UNIQUE indexes support the only account lookups; no speculative status/role index or tenant table is added. There is no FK to unrelated business entities. SQL CHECKs cover storage integrity; service email/password validation is stricter. There is no automatic updated_at/version trigger: every authorized security-state/password maintenance operation must explicitly increment auth_version and update updated_at. Delivered approve/disable tooling does so. All future maintenance writers must follow the same rule. Role/status are also compared directly at session checks.

## Sessions and authorized navigation

SessionManager begins a session only after a valid KDF result and a fresh ACTIVE-account read. It retains account ID, display name, role, auth_version, private handle and lifecycle timestamps, not email/phone/password/hash/JDBC credentials. Handles use private construction and object identity; they are not serialized bearer tokens. Logout increments a generation to invalidate an in-flight verification and clears state. A stale handle cannot invalidate a newer identity.

Both ACTIVE MEMBER and ACTIVE ADMIN are authorized for the **same three master-data modules**. ADMIN does not confer additional desktop actions yet. Pending/disabled/anonymous users are denied. There is no claim of branch-level permissions, per-operation RBAC or row-level isolation.

Idle timeout is 15 minutes; absolute lifetime is 8 hours. Navigation and every protected repository connection re-read account status, role and auth_version. The shell also polls at 15-second intervals in the authentication worker without extending idle time; actual response latency includes queue/network scheduling. Deletion, disabling, changed role/version, expiry or account-storage failure invalidates access. Revocation is checked at these boundaries and does **not** atomically cancel a business transaction already executing. Some existing navigation/CRUD checks still perform synchronous I/O on JavaFX; moving whole use cases to workers is separate follow-up work.

Controller1 runs KDF/login/registration outside the production JavaFX thread, clears passwords immediately, disables the submitting button and refuses form switching while a request is pending. Late callbacks from a closed/replaced scene cannot navigate or invalidate a newer verified identity. No direct UI SQL or PasswordUtil call remains. PasswordUtil is retained unused as historical source, not an active credential verifier.

ControllerSystem rejects anonymous direct FXML loading; the old switchToScene1 entry also requires a session. Home displays only the verified display name, role and three module buttons. Logout clears business-selection compatibility state, protected content and owned dialogs, replaces the Scene with Signin and stops polling. Hiding the protected window invalidates its handle. Old retained forms cannot open data connections after logout or under a different user.

Sales, Inventory, Receiving, HR, Fleet, Reports and Settings remain unavailable with clear feedback; their source and 53 FXML files remain. Category -> Products and Supplier -> Shipments remain blocked, Customer View remains disabled, and no Customer -> Sales activation is added. Search is explicitly unavailable and no longer dereferences/logs absent input. No financial/dashboard statistics are fabricated.

## Explicit owner provisioning - NOT EXECUTED

After separately approved target creation, TLS/least-privilege setup and V1/V2 migration, use the **test-classpath only** `com.sobol.database.PostgresAccounts` from a private interactive Java terminal. This command is not shipped as an application action. It requires the existing dedicated `sobol_migrator` credentials plus a separate exact `SOBOL_PG_ACCOUNT_ADMIN_APPROVED=sobol_dev/sobol`; migration approval does not grant account-DML approval. It verifies the destination, exact V1/V2 history and Flyway checksums, and performs no DDL/migration/repair.

Actions: `bootstrap-admin` privately prompts for a new login/name/passphrase/confirmation and creates ACTIVE ADMIN **only if no ADMIN exists at all**, including disabled administrators. `approve-member` accepts a privately reviewed ID and changes only PENDING MEMBER to ACTIVE, increasing version/timestamp. `disable` takes a reviewed ID and cannot disable the last active ADMIN. Operations serialize using a table lock, check affected rows and own an explicit transaction. No passwords/hashes/user details are printed. Duplicate/close/rollback/commit uncertainty fails generically; do not blindly retry. Forgotten/disabled-first-admin recovery is a separately reviewed owner operation, not an automatic bootstrap exception.

Do not type a hash/password into pgAdmin Query Tool, shell arguments, Maven properties or chat. `Console.readPassword` hides input; lack of an interactive Console fails closed without an echoed fallback. See [Java Console](https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/io/Console.html). Instructions to construct the tooling classpath and run it after approval are in POSTGRESQL-SETUP.md. This process and every live assertion remain unexecuted.

## Verification limits

Offline service/session/JDBC fault tests and real-FXML tests use synthetic repositories. They prove application contracts, not PostgreSQL SQL acceptance, TLS, actual constraints/grants, transactional locking or a complete database login. Prepared PostgresAuthenticationIT adds six live methods; the existing 13 PostgresPersistenceIT methods remain. See VERIFICATION.md for actual results and POSTGRESQL-SETUP.md for owner gates. Historical MySQL credential rotation/revocation remains unconfirmed and independent of this work.
