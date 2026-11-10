# Sobol Management System

Recovered JavaFX desktop application for inventory, sales, purchasing, branches, employees, and vehicles. This is a recovery baseline, not a production-ready release.

## Build

- JDK **22** (historical runtime compatibility); source/target **21**.
- Maven Wrapper **3.9.14**.
- JavaFX **24.0.1**, PostgreSQL JDBC **42.7.14**; test-scoped Flyway **13.12.1**.
- Windows graphical session for the optional desktop smoke test. Maven resolves platform-specific JavaFX artifacts.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Pdesktop-smoke verify
.\mvnw.cmd javafx:run
```

On Linux/macOS use `./mvnw`. The JAR is a thin artifact: use the Maven JavaFX launcher with resolved dependencies, not `java -jar`.

Database access is **disabled by default**. The sign-in window opens offline. PostgreSQL transition is partial: authentication, inventory, sales, procurement, HR, fleet, dashboard and reporting remain legacy and unavailable. There is no new login bypass and no claim of full production startup/CRUD readiness.

## Development database boundary

The owner's PostgreSQL decision supersedes MySQL setup. Only explicit numeric loopback `127.0.0.1:<port>/sobol_dev`, schema `sobol`, dedicated roles and verified TLS are permitted. No database/schema creation, migration or live test is executed without separate explicit approval. Ordinary verification disables both database boundaries.

See [target model and ERD](docs/POSTGRESQL-MODEL.md), [setup and approval gates](docs/POSTGRESQL-SETUP.md), and config/development.env.example. The example has no credentials and is not auto-loaded. New opt-in profiles are postgres-migrate and postgres-it; do not run them before approval. The MySQL driver/profiles/custom runner are retired. Historical schema/test source remains non-executable reference, not the canonical design.

Existing GitHub history previously contained embedded database credentials. Removing them from this tree does not revoke them or remove old commits. The owner must rotate/revoke the historical credentials. Shared history has not been rewritten.

## Recovery and limitations

- [Recovery provenance and validation](docs/RECOVERY.md)
- [Implemented Category, Supplier and Customer architecture and module boundaries](docs/ARCHITECTURE.md)
- [Original defects and unavailable features](docs/KNOWN-DEFECTS.md)
- [Engineering/Git policy](AGENTS.md)

The recovered snapshot contained 101 original Java files and 53 FXML screens. Phase 3A repaired three UI workflows; Phases 4A/4B/4C replace CategoryDAO, SupplierDAO and CustomerDAO with layered services/repositories and independent domain models. Supplier shipment lookup and Customer sales selection use the new services; ShipmentDAO and SalesDAO transactions remain unchanged. Customer selection is detached before the sales handoff. Controllers/resource paths remain compatible, all 53 FXML files are retained, and historical evidence/provenance remains preserved. Other business modules retain their legacy implementation. `FieldAdministrators.fxml` was not found and was not reconstructed. Settings is an original static UI.

Before every commit, run appropriate verification and review the complete staged tree:

```powershell
python -B tools/review_staged.py
git diff --cached --check
git diff --cached --stat
```

Personal images, private dumps, original archives, IDE state, and build output do not belong in Git. Push every successfully completed logical task using the authorized default-branch workflow; never force-push.
