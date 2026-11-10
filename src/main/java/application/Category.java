package application;

import javafx.beans.property.*;

/** Mutable JavaFX view state; the immutable business model lives in com.sobol.category.domain. */
public class Category {

	private IntegerProperty id = new SimpleIntegerProperty();
	private StringProperty name = new SimpleStringProperty();
	private StringProperty description = new SimpleStringProperty();

	public Category(int id, String name, String description) {
		this.id.set(id);
		this.name.set(name);
		this.description.set(description);
	}

	public static Category fromDomain(com.sobol.category.domain.Category category) {
		return new Category(category.id(), category.name(), category.description());
	}

	public int getId() {
		return id.get();
	}

	public IntegerProperty idProperty() {
		return id;
	}

	public StringProperty nameProperty() {
		return name;
	}

	public StringProperty descriptionProperty() {
		return description;
	}
}
