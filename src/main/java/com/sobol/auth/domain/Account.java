package com.sobol.auth.domain;

import java.util.*;

/** Persisted identity/security state, without credential material. */
public record Account(long id, String login, String displayName, Status status, Role role, long authVersion) {
    public enum Status { PENDING, ACTIVE, DISABLED }
    public enum Role { MEMBER, ADMIN }
    public Account {
        if (id <= 0 || authVersion <= 0) throw new IllegalArgumentException("Invalid account identity.");
        login = normalizeLogin(login); displayName = name(displayName);
        Objects.requireNonNull(status); Objects.requireNonNull(role);
    }
    public static String normalizeLogin(String value) {
        if (value == null) throw new IllegalArgumentException("Invalid login identifier.");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !normalized.matches("[a-z0-9][a-z0-9._%+\\-]*@[a-z0-9](?:[a-z0-9.\\-]*[a-z0-9])?\\.[a-z]{2,63}"))
            throw new IllegalArgumentException("Invalid login identifier.");
        return normalized;
    }
    public static String name(String value) {
        if (value == null || value.trim().isBlank()) throw new IllegalArgumentException("Invalid display name.");
        String normalized = value.trim();
        if (normalized.codePointCount(0, normalized.length()) > 160) throw new IllegalArgumentException("Invalid display name.");
        return normalized;
    }
    @Override public String toString() { return "Account[identity redacted]"; }
}
