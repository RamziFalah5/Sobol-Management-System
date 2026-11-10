package com.sobol.category.application;

import com.sobol.category.domain.Category;
import org.junit.jupiter.api.Test;
import java.util.*;
import static com.sobol.category.application.CategoryException.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

class CategoryServiceTest {
    static class MemoryRepository implements CategoryRepository {
        final Map<Integer, Category> rows = new LinkedHashMap<>();
        int sequence, writes;
        CategoryException failure;
        private void available() { if (failure != null) throw failure; }
        private void unique(String name, int currentId) {
            if (rows.values().stream().anyMatch(c -> c.id() != currentId && c.name().equals(name)))
                throw new CategoryException(DUPLICATE_NAME);
        }
        public List<Category> findAll() { available(); return new ArrayList<>(rows.values()); }
        public Category insert(String name, String description) {
            available(); writes++; unique(name, 0);
            var c = new Category(++sequence, name, description); rows.put(c.id(), c); return c;
        }
        public Optional<Category> update(int id, String name, String description) {
            available(); writes++;
            if (!rows.containsKey(id)) return Optional.empty();
            unique(name, id); var c = new Category(id, name, description); rows.put(id, c); return Optional.of(c);
        }
    }

    @Test void createsNormalizedImmutableCategoryAndListsSnapshots() {
        var repo = new MemoryRepository(); var service = new CategoryService(repo);
        Category created = service.createCategory("  Synthetic  ", "  Optional description  ");
        assertEquals(new Category(1, "Synthetic", "Optional description"), created);
        assertEquals(List.of(created), service.listCategories());
        assertThrows(UnsupportedOperationException.class, () -> service.listCategories().clear());
    }

    @Test void updatesOnlyTheRequestedIdentity() {
        var repo = new MemoryRepository(); var service = new CategoryService(repo);
        Category first = service.createCategory("First", ""), second = service.createCategory("Second", "Original");
        assertEquals(new Category(first.id(), "Updated", "Changed"), service.updateCategory(first.id(), " Updated ", " Changed "));
        assertEquals(second, repo.rows.get(second.id()));
    }

    @Test void rejectsInvalidNamesBeforePersistence() {
        var repo = new MemoryRepository(); var service = new CategoryService(repo);
        for (String name : Arrays.asList(null, "", "   ", "\u2003", "x".repeat(151))) {
            assertReason(INVALID_NAME, () -> service.createCategory(name, ""));
            assertReason(INVALID_NAME, () -> service.updateCategory(42, name, ""));
        }
        assertEquals(0, repo.writes);
    }

    @Test void nameLengthCountsDatabaseCharactersInsteadOfUtf16CodeUnits() {
        var repo = new MemoryRepository(); var service = new CategoryService(repo);
        String name = "\uD83D\uDCE6".repeat(150);
        assertEquals(name, service.createCategory(name, null).name());
        assertReason(INVALID_NAME, () -> service.createCategory(name + "x", ""));
        assertEquals("", service.listCategories().getFirst().description());
    }

    @Test void duplicateCreationAndUpdateRemainDistinctFromMissingAndStorageFailure() {
        var repo = new MemoryRepository(); var service = new CategoryService(repo);
        service.createCategory("First", ""); var second = service.createCategory("Second", "");
        assertReason(DUPLICATE_NAME, () -> service.createCategory("First", ""));
        assertReason(DUPLICATE_NAME, () -> service.updateCategory(second.id(), "First", ""));
        assertEquals("Second", repo.rows.get(second.id()).name());
    }

    @Test void invalidIdentityAndMissingPersistedIdentityAreDifferentOutcomes() {
        var repo = new MemoryRepository(); var service = new CategoryService(repo);
        for (int id : List.of(-1, 0)) assertReason(INVALID_ID, () -> service.updateCategory(id, "Synthetic", ""));
        assertEquals(0, repo.writes);
        assertReason(NOT_FOUND, () -> service.updateCategory(42, "Synthetic", ""));
    }

    @Test void unchangedExistingValuesAreSuccessfulAndDoNotCollideWithThemselves() {
        var repo = new MemoryRepository(); var service = new CategoryService(repo);
        var existing = service.createCategory("Synthetic", "Description");
        assertEquals(existing, service.updateCategory(existing.id(), existing.name(), existing.description()));
        assertEquals(1, repo.rows.size());
    }

    @Test void repositoryFailuresNeverBecomeEmptyListsOrSuccessfulWrites() {
        var repo = new MemoryRepository(); var service = new CategoryService(repo);
        repo.failure = new CategoryException(STORAGE_FAILURE);
        assertReason(STORAGE_FAILURE, service::listCategories);
        assertReason(STORAGE_FAILURE, () -> service.createCategory("Synthetic", ""));
        assertReason(STORAGE_FAILURE, () -> service.updateCategory(42, "Synthetic", ""));
        assertEquals(0, repo.writes);
    }

    @Test void domainRejectsInvalidPersistedDataWithoutAnyUiOrDatabase() {
        assertThrows(IllegalArgumentException.class, () -> new Category(0, "Synthetic", ""));
        assertThrows(IllegalArgumentException.class, () -> new Category(1, "", ""));
        assertEquals("", new Category(1, "Synthetic", null).description());
    }

    @Test void exactNameRuleAllowsDistinctCaseVariants() {
        var service = new CategoryService(new MemoryRepository());
        var first = service.createCategory("Case", "");
        var second = service.createCategory("case", "");
        assertNotEquals(first.id(), second.id());
        assertEquals(2, service.listCategories().size());
    }

    private void assertReason(CategoryException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        assertEquals(reason, assertThrows(CategoryException.class, action).reason());
    }
}
