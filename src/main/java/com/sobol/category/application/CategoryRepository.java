package com.sobol.category.application;

import com.sobol.category.domain.Category;
import java.util.List;
import java.util.Optional;

/** Persistence port: writes enforce unique names atomically and report typed storage/conflict failures. */
public interface CategoryRepository {
    List<Category> findAll();
    Category insert(String name, String description);

    /** Empty means missing; present means success, including unchanged existing values. */
    Optional<Category> update(int id, String name, String description);
}
