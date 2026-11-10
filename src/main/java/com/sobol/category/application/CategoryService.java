package com.sobol.category.application;

import com.sobol.category.domain.Category;
import java.util.List;
import java.util.Objects;
import static com.sobol.category.application.CategoryException.Reason.*;

/** Category use cases; no UI state, connection handling or presentation dependencies. */
public final class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<Category> listCategories() { return List.copyOf(repository.findAll()); }

    public Category createCategory(String name, String description) {
        return repository.insert(validName(name), Category.normalizeDescription(description));
    }

    public Category updateCategory(int id, String name, String description) {
        if (id <= 0) throw new CategoryException(INVALID_ID);
        return repository.update(id, validName(name), Category.normalizeDescription(description))
                .orElseThrow(() -> new CategoryException(NOT_FOUND));
    }

    private String validName(String name) {
        try { return Category.normalizeName(name); }
        catch (IllegalArgumentException failure) { throw new CategoryException(INVALID_NAME); }
    }
}
