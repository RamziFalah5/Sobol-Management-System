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
