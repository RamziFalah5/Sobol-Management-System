package application;

import com.sobol.category.application.CategoryService;
import com.sobol.database.PostgresDatabase;
import com.sobol.category.infrastructure.JdbcCategoryRepository;

/** Category composition root for legacy FXML no-argument controller construction. */
final class CategoryModule {
    private CategoryModule() { }
    static CategoryService service() {
        return new CategoryService(new JdbcCategoryRepository(PostgresDatabase::get));
    }
}
