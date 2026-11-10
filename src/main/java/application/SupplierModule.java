package application;

import com.sobol.supplier.application.SupplierService;
import com.sobol.database.PostgresDatabase;
import com.sobol.supplier.infrastructure.JdbcSupplierRepository;

/** Explicit Supplier composition for no-argument FXML controllers, including shipment lookup. */
final class SupplierModule {
    private SupplierModule() { }
    static SupplierService service() {
        var access = AuthModule.protect(PostgresDatabase::get);
        return new SupplierService(new JdbcSupplierRepository(access::open));
    }
}
