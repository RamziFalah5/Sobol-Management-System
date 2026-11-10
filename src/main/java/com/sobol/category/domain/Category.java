package com.sobol.category.domain;

/** Immutable business data. A persisted category always has a positive identity. */
public record Category(int id, String name, String description) {
    public Category {
        if (id <= 0) throw new IllegalArgumentException("Category identity must be positive.");
        name = normalizeName(name);
        description = normalizeDescription(description);
    }

    public static String normalizeName(String name) {
        if (name == null || name.trim().isBlank())
            throw new IllegalArgumentException("Category name is required.");
        String normalized = name.trim();
        if (normalized.codePointCount(0, normalized.length()) > 150)
            throw new IllegalArgumentException("Category name cannot exceed 150 characters.");
        return normalized;
    }

    public static String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }
}
