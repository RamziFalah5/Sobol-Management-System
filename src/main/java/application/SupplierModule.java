package application;

import com.sobol.supplier.application.SupplierService;
import com.sobol.database.PostgresDatabase;
import com.sobol.supplier.infrastructure.JdbcSupplierRepository;

/** Explicit Supplier composition for no-argument FXML controllers, including shipment lookup. */
final class SupplierModule {
    private SupplierModule() { }
    static SupplierService service() {
        return new SupplierService(new JdbcSupplierRepository(PostgresDatabase::get));
    }
}
