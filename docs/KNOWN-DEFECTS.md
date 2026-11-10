# Original defects and unavailable functionality

These defects exist in the original source; recovery does not silently repair their business behavior.

| Area | Original evidence | Impact / verification boundary |
| --- | --- | --- |
| Daily attendance | DailyAttendance.fxml names DriverAttendanceController | Daily-worker navigation uses driver data/behavior. |
| Category edit | CategoryController.editCategory() does not call setCategory(); CategoryDAO ignores affected-row count | Selection is not passed; apparent success may update no row. |
| Shipment form | AddShipment.fxml lacks rbCash/rbCredit/cbMethod; onSave dereferences cbMethod | Save cannot complete without a separate approved fix. |
| Global search | System.fxml lacks searchField; onSearchClicked reads it | Null dereference. |
| Logout | ControllerSystem.logout() is empty | No logout behavior. |
| Product entry | AddProductController selects first branch before installing its warehouse listener | Initial warehouse list is not populated automatically. |
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

## Phase 2 database investigation (static evidence; no live defect reproduction)

The owner has not prepared isolated database accounts. Integration infrastructure is compiled, but its preflight stops before connecting. The following findings are source/schema conclusions, not observed MySQL test failures. No original application source was changed.

| Severity | Evidence | Impact / remediation direction for a separately approved phase |
| --- | --- | --- |
| High | ShipmentDAO.addShipment() computes currentQty + d.quantity without a positive-quantity check; inserts d.totalCost and caller branch/warehouse directly. V001 has independent FKs and no CHECK constraints. | Negative receipts, mismatched totals and inconsistent branch/warehouse location are not rejected at this persistence boundary. Validate centrally and define location/total invariants before changing behavior. UI validation alone does not establish the DAO invariant. |
| High | SalesDAO.createOrder(), ShipmentDAO.addShipment(): nonlocking SELECT then absolute stock UPDATE; ProductDAO.canFitInWarehouse() is separate from insertProduct()/updateProduct(). | Lost update/overselling/capacity race risks. Real concurrency tests are prepared, unexecuted. Define atomic stock/capacity operations and locking strategy; do not claim existing transactions resolve these races. |
| Medium | SalesDAO/ShipmentDAO/WarehouseDAO outer catch blocks are outside try-with-resources connection scope and contain no explicit rollback; AttendanceDAO batch methods explicitly rollback. | Failure-path consistency depends on connector close behavior for some operations. Verify injected connection/commit failures and make transaction failure handling explicit in Phase 3. This is not a claim that close currently commits partial work. |
| Medium | V001: product.branch_id and product.warehouse_id have separate FKs; employee.managed_branch_id has no FK; user_database.user_id has no FK. | FK-valid rows can still violate intended cross-field/domain consistency; orphan metadata is structurally permitted. Preserve historical DDL now; agree on intended invariants before adding constraints. |

The known empty-warehouse COUNT(*) and nonexistent-category update defects have correct-behavior Integration Tests tagged `original-defect`; they remain enabled. See DEVELOPMENT-DATABASE.md for coverage, unexecuted scenarios and setup prerequisites. No new runtime pass/fail claim is made for these defects.
