# Sobol Management System

Recovered JavaFX desktop application for inventory, sales, purchasing, branches, employees, and vehicles. This is a recovery baseline, not a production-ready release.

## Build

- JDK **22** (historical runtime compatibility); source/target **21**.
- Maven Wrapper **3.9.14**.
- JavaFX **24.0.1**, MySQL Connector/J **9.3.0**.
- Windows graphical session for the optional desktop smoke test. Maven resolves platform-specific JavaFX artifacts.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -Pdesktop-smoke verify
.\mvnw.cmd javafx:run
```

On Linux/macOS use `./mvnw`. The JAR is a thin artifact: use the Maven JavaFX launcher with resolved dependencies, not `java -jar`.

Database access is **disabled by default**. The sign-in window can open without a database. Login, registration, dashboards, and CRUD require an independently prepared development database. Do not enable them against historical/production data.

## Development database boundary

Ordinary builds never create or connect to a database. Explicit opt-in tooling can initialize only **`sobol_dev`**, from a sanitized historical schema with no historical records. Setup and Integration Tests require numeric loopback `127.0.0.1`, separate limited application credentials and safety confirmations. The original schema is rejected. See [development database instructions](docs/DEVELOPMENT-DATABASE.md) and `config/development.env.example`; neither contains credentials or is auto-loaded.

Supply credentials privately through the process environment only after preparing an isolated database and least-privilege user. Set `SOBOL_DB_ENABLED=true` explicitly to opt in. Never commit local configuration or paste credentials into commands, reports, logs, issues, or pull requests. Surefire unit/contract tests force database access off. Failsafe Integration Tests run only with `-Pmysql-it verify` after the documented preflight; they never create schemas and fail clearly if setup is unavailable.

Existing GitHub history previously contained embedded database credentials. Removing them from this tree does not revoke them or remove old commits. The owner must rotate/revoke the historical credentials. Shared history has not been rewritten.

## Recovery and limitations

- [Recovery provenance and validation](docs/RECOVERY.md)
- [Original defects and unavailable features](docs/KNOWN-DEFECTS.md)
- [Engineering/Git policy](AGENTS.md)

All 101 original Java files and 53 FXML screens are represented. Business algorithms were preserved; only the connection boundary and portable resource resolution changed. `FieldAdministrators.fxml` was not found and was not reconstructed. Settings is an original static UI.

Before every commit, run appropriate verification and review the complete staged tree:

```powershell
python -B tools/review_staged.py
git diff --cached --check
git diff --cached --stat
```

Personal images, private dumps, original archives, IDE state, and build output do not belong in Git. Push every successfully completed logical task using the authorized default-branch workflow; never force-push.
