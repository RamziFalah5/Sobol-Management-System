# Recovery baseline provenance

## Historical evidence

The private Eclipse project `javaTestSC` is the source of recovery. Its 123 files shared with the downloaded GitHub snapshot are byte-for-byte identical. It contains 101 Java sources, 53 FXML files, and application.css under `src/application`. The two downloaded Sobol ZIPs match the GitHub main snapshot `cb0f2c10ed3c779828511649d09e14a203c811c3` and do not contain the missing files.

Private verified archives of the Eclipse project, the downloaded working source, and both original ZIPs are stored outside this repository with restricted filesystem access. Their private manifest is not committed. The original Eclipse project, downloaded source, and archives are left untouched.

`recovery-provenance.json` maps every original package file to its new destination, records source digests (except the credential-bearing DBAuth source), and describes recovery changes. Recovered-file digests use LF line endings. `resource-provenance.json` allowlists the exact non-personal binary assets approved for Git.

## Files absent from the downloaded snapshot

The following 32 original files were recovered as one dependency closure:

- Product: Product.java, ProductDAO.java, ProductPickRow.java, ProductsController.java, Products.fxml.
- Imports: Shipment.java, ShipmentDAO.java, ShipmentSession.java, ShipmentProductsController.java, ShipmentProducts.fxml.
- Sales: SalesOrder.java, SalesItem.java, SalesDAO.java, PickProductsForSaleController.java, ProductSalePickRow.java, SaleQuantitiesController.java, SaleQuantities.fxml, SalesController.java, Sales.fxml.
- Sales representatives: SalesRepRow.java, SalesRepDAO.java, SalesRepresentative.java, SalesRepsController.java, SalesReps.fxml, SalesAttendanceController.java, SalesAttendance.fxml.
- Reports: ReportsController.java, ReportsDAO.java, Reports.fxml.
- Settings: SettingsController.java, Settings.fxml.
- Authentication: Signin.fxml.

## Essential recovery changes

1. Move Java to `src/main/java/application`; FXML/CSS to `src/main/resources/application`. Package names and FXML controller names remain unchanged.
2. Replace embedded database credentials with explicit environment configuration. No default credentials, URL, or connection attempt. Restrict this baseline to loopback `sobol_dev`. Sanitize connection-failure messages. Historical credentials must be rotated by the owner.
3. Package 14 original icons from the historical icon collection and the original truck2.png. Replace external FXML icon paths with classpath paths.
4. For missing workers/hourly-workers/sales-manager graphics, use the existing generic Employee icon. For missing `/images/user.png`, package the same generic non-personal icon. These are documented visual fallbacks, not recovered personal photos. Branch/warehouse icon helpers prefer packaged icons (including the correctly spelled warehouse.png).
5. Add a pinned Maven build, safety/contract tests, documentation, and permanent AGENTS.md policy. Normalize text line endings to LF and remove trailing/invalid indentation whitespace without changing business algorithms.

No SQL dump, account/customer records, employee photos, local Eclipse configuration, original credential, binary dependency JAR, or compiled application class is added to Git.

## Historical build evidence

- Eclipse source root `src`, output `bin`, UTF-8, Java source/compliance/target 21.
- Eclipse JavaFx user library and Main launch configuration point to SDK 24.0.1; SDK version file confirms 24.0.1+4.
- Connector/J 9.3.0 is confirmed by the original JAR manifest.
- Prior class files target Java 21. They were not executed or decompiled for recovery.
- Maven replaces machine-specific Eclipse library references. JDK 22 runs Maven/JavaFX; compiler release remains 21. Build plugins and dependency versions are pinned. No enterprise framework is introduced.

## Validation boundaries

`clean verify` compiles all recovered source and runs configuration/FXML contract tests. `-Pdesktop-smoke verify` additionally shows and closes the original sign-in stage with DB disabled. Static FXML checks resolve controllers, handlers, and packaged images but do not prove every initialization path or CRUD workflow works. Known original defects are recorded separately.

No original database is contacted, no dump is imported, and no development schema is provisioned during recovery verification. SQL compatibility was assessed statically against the supplied dump. Production operation, database integration, inventory concurrency, and complete feature acceptance remain unverified.

See [VERIFICATION.md](VERIFICATION.md) for the checks actually executed and their results.
