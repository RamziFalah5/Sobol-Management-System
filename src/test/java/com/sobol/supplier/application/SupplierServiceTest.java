package com.sobol.supplier.application;

import com.sobol.supplier.domain.Supplier;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.sobol.supplier.application.SupplierException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

class SupplierServiceTest {
    static class Memory implements SupplierRepository {
        final Map<Integer, Supplier> rows = new LinkedHashMap<>();
        int sequence, writes;
        SupplierException failure;
        private void available() { if (failure != null) throw failure; }
        private void unique(Supplier.Details d, int id) {
            if (rows.values().stream().anyMatch(s -> s.id() != id && s.details().name().equalsIgnoreCase(d.name())))
                throw new SupplierException(DUPLICATE_NAME);
        }
        public List<Supplier> findAll() { available(); return new ArrayList<>(rows.values()); }
        public Supplier insert(Supplier.Details d) {
            available(); writes++; unique(d, 0);
            var s = new Supplier(++sequence, d); rows.put(s.id(), s); return s;
        }
        public Optional<Supplier> update(int id, Supplier.Details d) {
            available(); writes++;
            if (!rows.containsKey(id)) return Optional.empty();
            unique(d, id); var s = new Supplier(id, d); rows.put(id, s); return Optional.of(s);
        }
    }

    @Test void creationNormalizesEveryFieldAndListsImmutableSnapshots() {
        var r = new Memory(); var s = new SupplierService(r);
        var created = s.createSupplier(" Synthetic ", " +000 ext 1 ", " test@example.invalid ", " City ", " Address ", " Contact ", " Notes ");
        assertEquals(new Supplier(1, new Supplier.Details("Synthetic", "+000 ext 1", "test@example.invalid", "City", "Address", "Contact", "Notes")), created);
        var snapshot = s.listSuppliers(); assertEquals(List.of(created), snapshot);
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        r.rows.clear(); assertEquals(1, snapshot.size());
    }

    @Test void optionalNullAndBlankValuesBecomeNullWithoutInventedPhoneOrEmailUniqueness() {
        var r = new Memory(); var s = new SupplierService(r);
        var first = s.createSupplier("First", " extension ", " ", null, "", "\u2003", null);
        assertEquals(new Supplier.Details("First", "extension", null, null, null, null, null), first.details());
        assertNotEquals(first.id(), s.createSupplier("Second", "extension", null, null, null, null, null).id());
        s.createSupplier("Third", "extension", "same@host", null, null, null, null);
        s.createSupplier("Fourth", "extension", "same@host", null, null, null, null);
        assertEquals(4, r.rows.size());
    }

    @Test void requiredNameAndPhoneAreRejectedBeforeRepositoryAccess() {
        var r = new Memory(); var s = new SupplierService(r);
        for (String value : Arrays.asList(null, "", "  ", "\u2003")) {
            reason(INVALID_NAME, () -> s.createSupplier(value, "000", null, null, null, null, null));
            reason(INVALID_PHONE, () -> s.updateSupplier(42, "Synthetic", value, null, null, null, null, null));
        }
        assertEquals(0, r.writes);
    }

    @Test void historicalEmailFormatIsPreserved() {
        var r = new Memory(); var s = new SupplierService(r);
        for (String email : List.of("plain", "x@", "@host", "x y@host", "x@host/path"))
            reason(INVALID_EMAIL, () -> s.createSupplier("Synthetic", "000", email, null, null, null, null));
        assertEquals(0, r.writes);
        assertEquals("a+b._-@host", s.createSupplier("Synthetic", "000", "a+b._-@host", null, null, null, null).details().email());
    }

    @Test void allSchemaLengthBoundariesCountUnicodeCodePoints() {
        var r = new Memory(); var s = new SupplierService(r);
        String[] values = {"\uD83D\uDCE6".repeat(150), "p".repeat(30), "a".repeat(148) + "@h",
                "c".repeat(100), "a".repeat(255), "n".repeat(150), "t".repeat(400)};
        assertEquals(values[0], create(s, values).details().name());
        var reasons = new SupplierException.Reason[]{INVALID_NAME, INVALID_PHONE, INVALID_EMAIL, INVALID_CITY, INVALID_ADDRESS, INVALID_CONTACT_PERSON, INVALID_NOTES};
        for (int i = 0; i < values.length; i++) {
            String[] invalid = values.clone(); invalid[i] += "x";
            reason(reasons[i], () -> create(s, invalid));
            reason(reasons[i], () -> s.updateSupplier(1, invalid[0], invalid[1], invalid[2], invalid[3], invalid[4], invalid[5], invalid[6]));
        }
        assertEquals(1, r.writes);
    }

    @Test void updatesOnlyTheRequestedIdentityAndAllowsUnchangedValues() {
        var r = new Memory(); var s = new SupplierService(r);
        var first = s.createSupplier("First", "000", null, null, null, null, null);
        var second = s.createSupplier("Second", "111", null, null, null, null, null);
        assertEquals(first, s.updateSupplier(first.id(), "First", "000", null, null, null, null, null));
        var updated = s.updateSupplier(first.id(), " Updated ", " 222 ", null, null, null, null, null);
        assertEquals(first.id(), updated.id()); assertEquals("Updated", updated.details().name());
        assertEquals(second, r.rows.get(second.id()));
    }

    @Test void duplicateWritesDoNotBecomeStorageFailuresOrOverwriteOtherRows() {
        var r = new Memory(); var s = new SupplierService(r);
        var first = s.createSupplier("First", "000", null, null, null, null, null);
        var second = s.createSupplier("Second", "000", null, null, null, null, null);
        reason(DUPLICATE_NAME, () -> s.createSupplier("first", "000", null, null, null, null, null));
        reason(DUPLICATE_NAME, () -> s.updateSupplier(second.id(), "First", "000", null, null, null, null, null));
        assertEquals(List.of(first, second), s.listSuppliers());
    }

    @Test void invalidIdentityAndMissingTargetAreDifferent() {
        var r = new Memory(); var s = new SupplierService(r);
        for (int id : List.of(-1, 0)) reason(INVALID_ID, () -> s.updateSupplier(id, "Synthetic", "000", null, null, null, null, null));
        assertEquals(0, r.writes);
        reason(NOT_FOUND, () -> s.updateSupplier(42, "Synthetic", "000", null, null, null, null, null));
    }

    @Test void repositoryOutagesNeverBecomeEmptyResultsOrSuccessfulWrites() {
        var r = new Memory(); var s = new SupplierService(r); r.failure = new SupplierException(STORAGE_FAILURE);
        reason(STORAGE_FAILURE, s::listSuppliers);
        reason(STORAGE_FAILURE, () -> s.createSupplier("Synthetic", "000", null, null, null, null, null));
        reason(STORAGE_FAILURE, () -> s.updateSupplier(42, "Synthetic", "000", null, null, null, null, null));
        assertEquals(0, r.writes);
    }

    @Test void domainCannotContainInvalidPersistedIdentityOrDetails() {
        var details = new Supplier.Details("Synthetic", "000", null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> new Supplier(0, details));
        assertThrows(NullPointerException.class, () -> new Supplier(1, null));
        assertThrows(Supplier.InvalidField.class, () -> new Supplier.Details("", "000", null, null, null, null, null));
    }

    private Supplier create(SupplierService s, String[] v) { return s.createSupplier(v[0], v[1], v[2], v[3], v[4], v[5], v[6]); }
    private void reason(SupplierException.Reason expected, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(SupplierException.class, action); assertEquals(expected, failure.reason()); assertNull(failure.getCause());
    }
}
