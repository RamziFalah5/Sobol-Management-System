package com.sobol.supplier.domain;

import java.util.Objects;

/** Immutable persisted identity and contact details; no presentation or persistence dependencies. */
public record Supplier(int id, Details details) {
    public Supplier {
        if (id <= 0) throw new IllegalArgumentException("Supplier identity must be positive.");
        Objects.requireNonNull(details);
    }

    public enum Field { NAME, PHONE, EMAIL, CITY, ADDRESS, CONTACT_PERSON, NOTES }

    /** Safe field identity, never the submitted value. */
    public static final class InvalidField extends IllegalArgumentException {
        private final Field field;
        private InvalidField(Field field) { super("Invalid supplier field: " + field); this.field = field; }
        public Field field() { return field; }
    }

    /** Shared invariants for persisted data and write requests. Blank optional fields remain SQL NULL. */
    public record Details(String name, String phone, String email, String city, String address,
                          String contactPerson, String notes) {
        public Details {
            name = normalize(name, 150, true, Field.NAME);
            phone = normalize(phone, 30, true, Field.PHONE);
            email = normalize(email, 150, false, Field.EMAIL);
            if (email != null && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
                throw new InvalidField(Field.EMAIL);
            city = normalize(city, 100, false, Field.CITY);
            address = normalize(address, 255, false, Field.ADDRESS);
            contactPerson = normalize(contactPerson, 150, false, Field.CONTACT_PERSON);
            notes = normalize(notes, 400, false, Field.NOTES);
        }

        private static String normalize(String value, int max, boolean required, Field field) {
            String normalized = value == null ? null : value.trim();
            if (normalized == null || normalized.isBlank()) {
                if (required) throw new InvalidField(field);
                return null;
            }
            if (normalized.codePointCount(0, normalized.length()) > max) throw new InvalidField(field);
            return normalized;
        }
    }
}
