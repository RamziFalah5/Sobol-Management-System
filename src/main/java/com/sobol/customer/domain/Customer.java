package com.sobol.customer.domain;

import java.util.Objects;

/** Immutable persisted identity and contact details; no presentation or persistence dependencies. */
public record Customer(int id, Details details) {
    public Customer {
        if (id <= 0) throw new IllegalArgumentException("Customer identity must be positive.");
        Objects.requireNonNull(details);
    }

    public enum Field { NAME, PHONE, EMAIL, CITY, ADDRESS, NOTES }

    /** Safe field identity, never the submitted value. */
    public static final class InvalidField extends IllegalArgumentException {
        private final Field field;
        private InvalidField(Field field) { super("Invalid customer field: " + field); this.field = field; }
        public Field field() { return field; }
    }

    /** Shared invariants for persisted data and write requests. Blank email remains SQL NULL; other optional blanks preserve historical empty strings. */
    public record Details(String fullName, String phone, String email, String city, String address, String notes) {
        public Details {
            fullName = normalize(fullName, 150, true, Field.NAME);
            phone = normalize(phone, 30, true, Field.PHONE);
            email = normalize(email, 180, false, Field.EMAIL);
            if (email != null && !(email.contains("@") && email.contains(".") && email.indexOf("@") < email.lastIndexOf(".")))
                throw new InvalidField(Field.EMAIL);
            city = normalize(city, 100, false, Field.CITY);
            address = normalize(address, 220, false, Field.ADDRESS);
            notes = normalize(notes, 400, false, Field.NOTES);
        }

        private static String normalize(String value, int max, boolean required, Field field) {
            String normalized = value == null ? null : value.trim();
            if (normalized == null) {
                if (required) throw new InvalidField(field);
                return null;
            }
            if (normalized.isBlank() && (required || field == Field.EMAIL)) {
                if (required) throw new InvalidField(field);
                return null;
            }
            if (normalized.codePointCount(0, normalized.length()) > max) throw new InvalidField(field);
            return normalized;
        }
    }
}
