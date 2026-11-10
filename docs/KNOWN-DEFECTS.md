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
