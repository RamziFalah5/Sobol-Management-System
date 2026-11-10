package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.File;

public class ProductsController {

	@FXML
	private Label lblTitle;

	@FXML
	private TableView<Product> productsTable;

	@FXML
	private TableColumn<Product, Product> colStatus;
	@FXML
	private TableColumn<Product, String> colImage;

	// ❌ حذف colId
	@FXML
	private TableColumn<Product, String> colName;
	@FXML
	private TableColumn<Product, String> colBranch;
	@FXML
	private TableColumn<Product, String> colWarehouse;

	@FXML
	private TableColumn<Product, Number> colQty;
	@FXML
	private TableColumn<Product, Number> colMin;
	@FXML
	private TableColumn<Product, Number> colMax;
	@FXML
	private TableColumn<Product, Number> colPrice;
	@FXML
	private TableColumn<Product, Number> colPallet;

	@FXML
	private TextField txtSearch;
	@FXML
	private Button btnSearch;

	private ObservableList<Product> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		String catName = CategorySession.getSelectedCategoryName();
		lblTitle.setText("Products - " + catName);

		colName.setCellValueFactory(d -> d.getValue().nameProperty());
		colBranch.setCellValueFactory(d -> d.getValue().branchNameProperty());
		colWarehouse.setCellValueFactory(d -> d.getValue().warehouseNameProperty());

		colQty.setCellValueFactory(d -> d.getValue().quantityProperty());
		colMin.setCellValueFactory(d -> d.getValue().minQuantityProperty());
		colMax.setCellValueFactory(d -> d.getValue().maxQuantityProperty());
		colPrice.setCellValueFactory(d -> d.getValue().sellingPriceProperty());
		colPallet.setCellValueFactory(d -> d.getValue().cartonsPerPalletProperty());

		// ✅ center alignment لكل الأعمدة (بدون CSS ملف)
		centerColumn(colName);
		centerColumn(colBranch);
		centerColumn(colWarehouse);
		centerColumn(colQty);
		centerColumn(colMin);
		centerColumn(colMax);
		centerColumn(colPrice);
		centerColumn(colPallet);

		// image
		colImage.setCellValueFactory(d -> d.getValue().imagePathProperty());
		colImage.setCellFactory(col -> new TableCell<>() {
			private final ImageView iv = new ImageView();
			{
				iv.setFitWidth(55);
				iv.setFitHeight(55);
				iv.setPreserveRatio(true);
				setStyle("-fx-alignment: CENTER;");
			}

			@Override
			protected void updateItem(String path, boolean empty) {
				super.updateItem(path, empty);
				if (empty || path == null || path.isBlank()) {
					setGraphic(null);
				} else {
					try {
						File f = new File(path);
						if (f.exists())
							iv.setImage(new Image(f.toURI().toString()));
						else
							iv.setImage(null);
						setGraphic(iv);
					} catch (Exception e) {
						setGraphic(null);
					}
				}
			}
		});

		// status circle
		colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue()));
		colStatus.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(Product p, boolean empty) {
				super.updateItem(p, empty);
				if (empty || p == null) {
					setGraphic(null);
					return;
				}
				int q = p.getQuantity();
				int min = p.getMinQuantity();
				int max = p.getMaxQuantity();

				Circle c = new Circle(7);
				if (q < min)
					c.setStyle("-fx-fill: #D32F2F;");
				else if (q <= max)
					c.setStyle("-fx-fill: #FB8C00;");
				else
					c.setStyle("-fx-fill: #2E7D32;");

				setStyle("-fx-alignment: CENTER;");
				setGraphic(c);
			}
		});

		load();

		txtSearch.textProperty().addListener((obs, oldText, newText) -> filterProducts(newText));
	}

	private <T> void centerColumn(TableColumn<Product, T> col) {
		col.setCellFactory(tc -> new TableCell<>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : item.toString());
				setStyle("-fx-alignment: CENTER;");
			}
		});
	}

	private void load() {
		int catId = CategorySession.getSelectedCategoryId();
		list.setAll(ProductDAO.getProductsByCategory(catId));
		productsTable.setItems(list);
	}

	private Product selectedOrAlert(String title) {
		Product p = productsTable.getSelectionModel().getSelectedItem();
		if (p == null) {
			alert(title, "Please select a product first.");
		}
		return p;
	}

	@FXML
	private void openAddProduct() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("AddProduct.fxml"));
			StackPane contentArea = (StackPane) productsTable.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void openEditProduct() {
		Product p = selectedOrAlert("Edit Product");
		if (p == null)
			return;

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditProduct.fxml"));
			Parent view = loader.load();

			EditProductController controller = loader.getController();
			controller.setProduct(p);

			StackPane contentArea = (StackPane) productsTable.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void openViewProduct() {
		Product p = selectedOrAlert("View Product");
		if (p == null)
			return;

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ViewProduct.fxml"));
			Parent view = loader.load();

			ViewProductController controller = loader.getController();
			controller.setProduct(p);

			StackPane contentArea = (StackPane) productsTable.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void deleteProduct() {
		Product p = selectedOrAlert("Delete Product");
		if (p == null)
			return;

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("Confirm Delete");
		confirm.setHeaderText(null);
		confirm.setContentText("Delete product: " + p.getName() + " ?");
		if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
			return;

		if (ProductDAO.deleteProduct(p.getProductId()))
			load();
	}

	@FXML
	private void onSearch() {
		String keyword = txtSearch.getText().trim().toLowerCase();
		if (keyword.isEmpty()) {
			productsTable.setItems(list);
			return;
		}
		ObservableList<Product> filtered = FXCollections.observableArrayList();
		for (Product p : list) {
			if (p.getName().toLowerCase().contains(keyword))
				filtered.add(p);
		}
		productsTable.setItems(filtered);
	}

	private void filterProducts(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			productsTable.setItems(list);
			return;
		}
		String lower = keyword.toLowerCase();
		ObservableList<Product> filtered = FXCollections.observableArrayList();
		for (Product p : list) {
			if (p.getName().toLowerCase().contains(lower))
				filtered.add(p);
		}
		productsTable.setItems(filtered);
	}

	private void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
