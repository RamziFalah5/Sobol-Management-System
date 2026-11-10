package com.sobol.supplier.application;

import com.sobol.supplier.domain.Supplier;
import java.util.List;
import java.util.Objects;
import static com.sobol.supplier.application.SupplierException.Reason.*;

/** Supplier use cases shared by CRUD forms and incoming-shipment lookup. */
public final class SupplierService {
    private final SupplierRepository repository;
    public SupplierService(SupplierRepository repository) { this.repository = Objects.requireNonNull(repository); }
    public List<Supplier> listSuppliers() { return List.copyOf(repository.findAll()); }

    public Supplier createSupplier(String name, String phone, String email, String city, String address,
                                   String contactPerson, String notes) {
        return repository.insert(validate(name, phone, email, city, address, contactPerson, notes));
    }

    public Supplier updateSupplier(int id, String name, String phone, String email, String city, String address,
                                   String contactPerson, String notes) {
        if (id <= 0) throw new SupplierException(INVALID_ID);
        return repository.update(id, validate(name, phone, email, city, address, contactPerson, notes))
                .orElseThrow(() -> new SupplierException(NOT_FOUND));
    }

    private Supplier.Details validate(String name, String phone, String email, String city, String address,
                                      String contactPerson, String notes) {
        try { return new Supplier.Details(name, phone, email, city, address, contactPerson, notes); }
        catch (Supplier.InvalidField failure) {
            throw new SupplierException(switch (failure.field()) {
                case NAME -> INVALID_NAME;
                case PHONE -> INVALID_PHONE;
                case EMAIL -> INVALID_EMAIL;
                case CITY -> INVALID_CITY;
                case ADDRESS -> INVALID_ADDRESS;
                case CONTACT_PERSON -> INVALID_CONTACT_PERSON;
                case NOTES -> INVALID_NOTES;
            });
        }
    }
}
