package com.sobol.supplier.application;

import com.sobol.supplier.domain.Supplier;
import java.util.List;
import java.util.Optional;

/** Unique names are enforced at write time; failures must never become empty results or booleans. */
public interface SupplierRepository {
    List<Supplier> findAll();
    Supplier insert(Supplier.Details details);
    /** Empty means missing; present includes a successful unchanged existing row. */
    Optional<Supplier> update(int id, Supplier.Details details);
}
