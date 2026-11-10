# Development database ? PostgreSQL transition

Phase 6A supersedes the historical MySQL setup. Do not run db-setup/mysql-it or historical instructions. The old runner, 25 IT methods, fixtures, barrier and V001 are reference-only under [reference/mysql](reference/mysql/README.md), outside executable Maven paths. Legacy DBAuth refuses all connections; no MySQL driver is shipped.

- [Canonical target model, decisions, ERD and dependency closures](POSTGRESQL-MODEL.md)
- [PostgreSQL configuration, private preparation and separate approval gates](POSTGRESQL-SETUP.md)
- [Actually executed verification](VERIFICATION.md)

Category/Supplier/Customer persistence contracts are migrated and verified offline in this phase. Full application cutover is not complete; stock/sales/HR/security/reporting remain legacy and unavailable. No automatic migration runs on application startup. Owner confirmed PostgreSQL is not prepared: no connection, DDL, fixture or live IT is authorized/executed. Historical MySQL credential rotation remains unconfirmed and separately outstanding.
