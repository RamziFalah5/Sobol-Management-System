package application;

import java.util.List;

import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class CategoryController {

	@FXML
	private GridPane categoryGrid;
	@FXML
	private ScrollPane scrollCategories;
	@FXML
	private TextField txtSearch;
	@FXML
	private Label lblStats;
	@FXML
	private Label lblEmpty;
	@FXML
	private Label lblSelected;

	private final ObservableList<Category> categoryList = FXCollections.observableArrayList();
	private Category selectedCategory = null;

	// ألوان فاقعة “أسطورية”
	private final String[] palette = { "#FF3D00", "#2962FF", "#00C853", "#D500F9", "#00B0FF", "#FFAB00", "#00BFA5",
			"#F50057", "#651FFF", "#1DE9B6", "#FF1744", "#76FF03" };

	@FXML
	public void initialize() {

		loadCategories();
		buildGrid(categoryList);

		txtSearch.textProperty().addListener((obs, o, n) -> filterCategories(n));
	}

	// ================= LOAD =================
	private void loadCategories() {
		categoryList.setAll(CategoryDAO.getAllCategories());

		lblStats.setText(categoryList.size() + " categories");
		lblEmpty.setVisible(categoryList.isEmpty());

		if (CategorySession.hasSelectedCategory()) {
			// حاول نمسك المختار من السيشن (إذا موجود)
			int sid = CategorySession.getSelectedCategoryId();
			for (Category c : categoryList) {
				if (c.getId() == sid) {
					selectedCategory = c;
					break;
				}
			}
		}

		updateSelectedLabel();
	}

	// ================= SEARCH =================
	private void filterCategories(String keyword) {

		if (keyword == null || keyword.isBlank()) {
			buildGrid(categoryList);
			lblStats.setText(categoryList.size() + " categories");
			lblEmpty.setVisible(categoryList.isEmpty());
			return;
		}

		String k = keyword.toLowerCase().trim();
		ObservableList<Category> filtered = FXCollections.observableArrayList();

		for (Category c : categoryList) {
			String name = safe(c.nameProperty().get()).toLowerCase();
			String desc = safe(c.descriptionProperty().get()).toLowerCase();

			if (name.contains(k) || desc.contains(k))
				filtered.add(c);
		}

		buildGrid(filtered);
		lblStats.setText(filtered.size() + " results");
		lblEmpty.setVisible(filtered.isEmpty());
	}

	// ================= GRID =================
	private void buildGrid(List<Category> list) {

		categoryGrid.getChildren().clear();

		int cols = 3;
		int col = 0, row = 0;
		int colorIndex = 0;

		for (Category c : list) {

			String accent = palette[colorIndex % palette.length];
			Button card = createCategoryCard(c, accent);

			categoryGrid.add(card, col, row);

			col++;
			if (col == cols) {
				col = 0;
				row++;
			}

			colorIndex++;
		}
	}

	// ================= CARD =================
	private Button createCategoryCard(Category c, String accentColor) {

		Label name = new Label(safe(c.nameProperty().get()));
		name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

		Label desc = new Label(safe(c.descriptionProperty().get()));
		desc.setWrapText(true);
		desc.setMaxWidth(220);
		desc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: rgba(255,255,255,0.92);");

		VBox box = new VBox(8, name, desc);
		box.setStyle("-fx-alignment: center-left; -fx-padding: 14 14 14 14;");

		Button btn = new Button();
		btn.setGraphic(box);
		btn.setMinHeight(140);
		btn.setPrefHeight(140);
		btn.setPrefWidth(260);
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setFocusTraversable(false);

		String baseStyle = styleBase(accentColor);
		String selectedStyle = styleSelected(accentColor);

		if (isSelected(c))
			btn.setStyle(selectedStyle);
		else
			btn.setStyle(baseStyle);

		// Hover Zoom
		ScaleTransition stIn = new ScaleTransition(Duration.millis(120), btn);
		stIn.setToX(1.03);
		stIn.setToY(1.03);

		ScaleTransition stOut = new ScaleTransition(Duration.millis(120), btn);
		stOut.setToX(1.00);
		stOut.setToY(1.00);

		btn.setOnMouseEntered(e -> {
			stOut.stop();
			stIn.playFromStart();
			if (!isSelected(c))
				btn.setStyle(styleHover(accentColor));
		});

		btn.setOnMouseExited(e -> {
			stIn.stop();
			stOut.playFromStart();
			if (isSelected(c))
				btn.setStyle(selectedStyle);
			else
				btn.setStyle(baseStyle);
		});

		// ✅ Left click: Select + Open Products (مثل Branches)
		btn.setOnAction(e -> {
			selectCategory(c);
			rebuildForSelection();
			openProducts();
		});

		// ✅ Right click: Select only (لأجل Edit)
		btn.setOnContextMenuRequested(e -> {
			selectCategory(c);
			rebuildForSelection();
		});

		return btn;
	}

	private void selectCategory(Category c) {
		selectedCategory = c;
		CategorySession.setSelectedCategory(c.getId(), safe(c.nameProperty().get()));
		updateSelectedLabel();
	}

	private void rebuildForSelection() {
		String k = txtSearch.getText();
		if (k == null || k.isBlank())
			buildGrid(categoryList);
		else
			filterCategories(k);
	}

	private boolean isSelected(Category c) {
		return selectedCategory != null && selectedCategory.getId() == c.getId();
	}

	private void updateSelectedLabel() {
		if (lblSelected == null)
			return;

		if (selectedCategory == null)
			lblSelected.setText("Selected: none");
		else
			lblSelected.setText("Selected: " + safe(selectedCategory.nameProperty().get()));
	}

	// ================= STYLES =================
	private String styleBase(String c) {
		// تدرّج + ظل = “كرت فخم”
		return """
				-fx-background-radius: 18;
				-fx-border-radius: 18;
				-fx-background-color: linear-gradient(to bottom right, %s, derive(%s, -18%%));
				-fx-border-color: rgba(255,255,255,0.35);
				-fx-border-width: 1.2;
				-fx-cursor: hand;
				-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0.25, 0, 8);
				""".formatted(c, c);
	}

	private String styleHover(String c) {
		return """
				-fx-background-radius: 18;
				-fx-border-radius: 18;
				-fx-background-color: linear-gradient(to bottom right, derive(%s, -6%%), derive(%s, -22%%));
				-fx-border-color: rgba(255,255,255,0.55);
				-fx-border-width: 1.4;
				-fx-cursor: hand;
				-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 20, 0.28, 0, 10);
				""".formatted(c, c);
	}

	private String styleSelected(String c) {
		return """
				-fx-background-radius: 18;
				-fx-border-radius: 18;
				-fx-background-color: linear-gradient(to bottom right, %s, derive(%s, -24%%));
				-fx-border-color: white;
				-fx-border-width: 2.8;
				-fx-cursor: hand;
				-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 22, 0.30, 0, 12);
				""".formatted(c, c);
	}

	// ================= NAVIGATION =================
	@FXML
	private void openAddCategory() {
		loadView("AddCategory.fxml");
	}

	@FXML
	private void editCategory() {
		if (selectedCategory == null && !CategorySession.hasSelectedCategory()) {
			showInfo("Edit Category", "Please select a category first (Right-click to select).");
			return;
		}
		loadView("EditCategory.fxml");
	}

	private void openProducts() {
		loadView("Products.fxml");
	}

	private void loadView(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));
			StackPane contentArea = (StackPane) categoryGrid.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showInfo(String title, String msg) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(title);
		a.setHeaderText(null);
		a.setContentText(msg);
		a.showAndWait();
	}

	private String safe(String s) {
		return (s == null) ? "" : s;
	}
}
