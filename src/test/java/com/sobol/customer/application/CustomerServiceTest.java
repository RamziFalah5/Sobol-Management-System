package com.sobol.customer.application;

import com.sobol.customer.domain.Customer;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.sobol.customer.application.CustomerException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceTest {
    static class Memory implements CustomerRepository {
        final Map<Integer, Customer> rows = new LinkedHashMap<>();
        int sequence, writes; CustomerException failure;
        private void available() { if (failure != null) throw failure; }
        private void unique(Customer.Details d, int id) {
            for (var row : rows.values()) if (row.id() != id) {
                if (row.details().fullName().equalsIgnoreCase(d.fullName())) throw new CustomerException(DUPLICATE_NAME);
                if (d.email() != null && d.email().equalsIgnoreCase(row.details().email())) throw new CustomerException(DUPLICATE_EMAIL);
            }
        }
        public List<Customer> findAll() { available(); return new ArrayList<>(rows.values()); }
        public Customer insert(Customer.Details d) { available(); writes++; unique(d, 0); var c = new Customer(++sequence, d); rows.put(c.id(), c); return c; }
        public Optional<Customer> update(int id, Customer.Details d) {
            available(); writes++; if (!rows.containsKey(id)) return Optional.empty();
            unique(d, id); var c = new Customer(id, d); rows.put(id, c); return Optional.of(c);
        }
    }
    @Test void creationNormalizesAllFieldsAndReturnsImmutableSnapshots() {
        var r = new Memory(); var s = new CustomerService(r);
        var c = s.createCustomer(" Synthetic ", " +000 ext 1 ", " test@example.invalid ", " City ", " Address ", " Notes ");
        assertEquals(new Customer(1, new Customer.Details("Synthetic", "+000 ext 1", "test@example.invalid", "City", "Address", "Notes")), c);
        var snapshot = s.listCustomers(); assertEquals(List.of(c), snapshot);
        assertThrows(UnsupportedOperationException.class, snapshot::clear); r.rows.clear(); assertEquals(1, snapshot.size());
    }
    @Test void historicalOptionalNullVersusEmptyStringSemanticsArePreserved() {
        var s = new CustomerService(new Memory());
        var nulls = s.createCustomer("Nulls", "000", null, null, null, null).details();
        assertEquals(new Customer.Details("Nulls", "000", null, null, null, null), nulls);
        var blanks = s.createCustomer("Blanks", "000", " ", " ", "", "  ").details();
        assertEquals(new Customer.Details("Blanks", "000", null, "", "", ""), blanks);
        assertEquals("\u2003", s.createCustomer("Unicode", "000", "\u2003", "\u2003", null, null).details().city());
    }
    @Test void requiredFieldsAreValidatedBeforeAnyWrite() {
        var r = new Memory(); var s = new CustomerService(r);
        for (String invalid : Arrays.asList(null, "", " ", "\u2003")) {
            reason(INVALID_NAME, () -> s.createCustomer(invalid, "000", null, null, null, null));
            reason(INVALID_PHONE, () -> s.updateCustomer(42, "Synthetic", invalid, null, null, null, null));
        }
        assertEquals(0, r.writes);
    }
    @Test void preservesTheHistoricalPermissiveEmailPredicate() {
        var r = new Memory(); var s = new CustomerService(r); int i = 0;
        for (String accepted : List.of("@.", "a b@c.d", "x@@host.", "valid@example.invalid"))
            assertEquals(accepted, s.createCustomer("Accepted " + (++i), "000", accepted, null, null, null).details().email());
        int before = r.writes;
        for (String rejected : List.of("plain", "x@host", "x.host@", "x.y@host"))
            reason(INVALID_EMAIL, () -> s.createCustomer("Rejected", "000", rejected, null, null, null));
        assertEquals(before, r.writes);
    }
    @Test void allSchemaBoundariesCountCharactersAndRejectOverlongCreateAndUpdate() {
        var r = new Memory(); var s = new CustomerService(r);
        String[] v = {"\uD83D\uDCE6".repeat(150), "p".repeat(30), "a".repeat(177) + "@.h", "c".repeat(100), "a".repeat(220), "n".repeat(400)};
        assertEquals(v[0], create(s, v).details().fullName());
        var reasons = new CustomerException.Reason[]{INVALID_NAME, INVALID_PHONE, INVALID_EMAIL, INVALID_CITY, INVALID_ADDRESS, INVALID_NOTES};
        for (int i = 0; i < v.length; i++) {
            String[] invalid = v.clone(); invalid[i] += "x";
            reason(reasons[i], () -> create(s, invalid));
            reason(reasons[i], () -> s.updateCustomer(1, invalid[0], invalid[1], invalid[2], invalid[3], invalid[4], invalid[5]));
        }
        assertEquals(1, r.writes);
    }
    @Test void updateTargetsOnlyItsOwnIdentityAndNormalizesEveryChangedField() {
        var r = new Memory(); var s = new CustomerService(r);
        var first = s.createCustomer("First", "000", null, null, null, null);
        var second = s.createCustomer("Second", "111", null, null, null, null);
        var updated = s.updateCustomer(first.id(), " Updated ", " 222 ", " new@example.invalid ", " City ", " Address ", " Notes ");
        assertEquals(new Customer(first.id(), new Customer.Details("Updated", "222", "new@example.invalid", "City", "Address", "Notes")), updated);
        assertEquals(second, r.rows.get(second.id()));
    }
    @Test void duplicateNameCreateAndUpdateAreDistinctAndDoNotOverwrite() {
        var r = new Memory(); var s = new CustomerService(r);
        var first = s.createCustomer("First", "000", null, null, null, null);
        var second = s.createCustomer("Second", "000", null, null, null, null);
        reason(DUPLICATE_NAME, () -> s.createCustomer("first", "000", null, null, null, null));
        reason(DUPLICATE_NAME, () -> s.updateCustomer(second.id(), "First", "000", null, null, null, null));
        assertEquals(List.of(first, second), s.listCustomers());
    }
    @Test void duplicateNonNullEmailCreateAndUpdateAreDistinctAndDoNotOverwrite() {
        var r = new Memory(); var s = new CustomerService(r);
        var first = s.createCustomer("First", "000", "shared@example.invalid", null, null, null);
        var second = s.createCustomer("Second", "000", null, null, null, null);
        reason(DUPLICATE_EMAIL, () -> s.createCustomer("Third", "000", "SHARED@example.invalid", null, null, null));
        reason(DUPLICATE_EMAIL, () -> s.updateCustomer(second.id(), "Second", "000", first.details().email(), null, null, null));
        assertEquals(List.of(first, second), s.listCustomers());
    }
    @Test void nullEmailAndRepeatedPhoneAreAllowedAndUnchangedUpdatesSucceed() {
        var s = new CustomerService(new Memory());
        var first = s.createCustomer("First", "extension", null, "", "", "");
        var second = s.createCustomer("Second", "extension", " ", null, null, null);
        assertNotEquals(first.id(), second.id());
        assertEquals(first, s.updateCustomer(first.id(), "First", "extension", null, "", "", ""));
        var withEmail = s.createCustomer("Email", "extension", "same@example.invalid", null, null, null);
        assertEquals(withEmail, s.updateCustomer(withEmail.id(), "Email", "extension", "same@example.invalid", null, null, null));
    }
    @Test void invalidIdentityAndMissingPersistedTargetRemainDistinct() {
        var r = new Memory(); var s = new CustomerService(r);
        for (int id : List.of(-1, 0)) reason(INVALID_ID, () -> s.updateCustomer(id, "Synthetic", "000", null, null, null, null));
        assertEquals(0, r.writes); reason(NOT_FOUND, () -> s.updateCustomer(42, "Synthetic", "000", null, null, null, null));
    }
    @Test void storageOutagesNeverBecomeEmptyListsOrSuccessfulWrites() {
        var r = new Memory(); var s = new CustomerService(r); r.failure = new CustomerException(STORAGE_FAILURE);
        reason(STORAGE_FAILURE, s::listCustomers);
        reason(STORAGE_FAILURE, () -> s.createCustomer("Synthetic", "000", null, null, null, null));
        reason(STORAGE_FAILURE, () -> s.updateCustomer(42, "Synthetic", "000", null, null, null, null)); assertEquals(0, r.writes);
    }
    @Test void invalidPersistedIdentityAndDetailsCannotEnterDomain() {
        var d = new Customer.Details("Synthetic", "000", null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> new Customer(0, d));
        assertThrows(NullPointerException.class, () -> new Customer(1, null));
        assertThrows(Customer.InvalidField.class, () -> new Customer.Details("Synthetic", "", null, null, null, null));
    }
    private Customer create(CustomerService s, String[] v) { return s.createCustomer(v[0], v[1], v[2], v[3], v[4], v[5]); }
    private void reason(CustomerException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        var failure = assertThrows(CustomerException.class, action); assertEquals(reason, failure.reason()); assertNull(failure.getCause());
    }
}
