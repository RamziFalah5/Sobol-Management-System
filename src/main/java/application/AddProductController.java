package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;

public class AddProductController {

	@FXML
	private ComboBox<Branch> cbBranch;
	@FXML
	private ComboBox<Warehouse> cbWarehouse;

	@FXML
	private TextField txtName, txtBaseUnit, txtCartonsPerPallet, txtQuantity, txtMin, txtMax, txtCost, txtSell;
	@FXML
	private TextArea txtDescription;
	@FXML
	private ImageView imgPreview;

	private String imagePath;

	@FXML
	public void initialize() {
		loadBranches();

		cbBranch.valueProperty().addListener((obs, oldB, newB) -> {
			if (newB == null) {
				cbWarehouse.getItems().clear();
				cbWarehouse.setValue(null);
				return;
			}
			loadWarehouses(newB.getBranchId());
		});
	}

	private void loadBranches() {
		try {
			cbBranch.setItems(FXCollections.observableArrayList(BranchDAO.getAllBranches()));
			cbBranch.setCellFactory(list -> new ListCell<>() {
				@Override
				protected void updateItem(Branch b, boolean empty) {
					super.updateItem(b, empty);
					setText(empty || b == null ? "" : b.getName());
				}
			});
			cbBranch.setButtonCell(new ListCell<>() {
				@Override
				protected void updateItem(Branch b, boolean empty) {
					super.updateItem(b, empty);
					setText(empty || b == null ? "" : b.getName());
				}
			});

			if (!cbBranch.getItems().isEmpty())
				cbBranch.getSelectionModel().selectFirst();

		} catch (Exception e) {
			e.printStackTrace();
			alert("Error", "Failed to load branches.");
		}
	}

	private void loadWarehouses(int branchId) {
		try {
			ObservableList<Warehouse> ws = FXCollections
					.observableArrayList(WarehouseDAO.getWarehousesByBranch(branchId));
			cbWarehouse.setItems(ws);

			cbWarehouse.setCellFactory(list -> new ListCell<>() {
				@Override
				protected void updateItem(Warehouse w, boolean empty) {
					super.updateItem(w, empty);
					setText(empty || w == null ? "" : w.getName());
				}
			});
			cbWarehouse.setButtonCell(new ListCell<>() {
				@Override
				protected void updateItem(Warehouse w, boolean empty) {
					super.updateItem(w, empty);
					setText(empty || w == null ? "" : w.getName());
				}
			});

			if (!ws.isEmpty())
				cbWarehouse.getSelectionModel().selectFirst();
			else
				cbWarehouse.setValue(null);

		} catch (Exception e) {
			e.printStackTrace();
			alert("Error", "Failed to load warehouses.");
		}
	}

	@FXML
	private void chooseImage() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Choose Product Image");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
		File f = fc.showOpenDialog(txtName.getScene().getWindow());
		if (f != null) {
			imagePath = f.getAbsolutePath();
			imgPreview.setImage(new Image(f.toURI().toString()));
		}
	}

	@FXML
	private void onAdd() {

		int categoryId = CategorySession.getSelectedCategoryId();

		Branch b = cbBranch.getValue();
		if (b == null) {
			alert("Validation", "Please select a branch.");
			return;
		}
		Warehouse w = cbWarehouse.getValue();
		if (w == null) {
			alert("Validation", "No warehouse available for this branch.");
			return;
		}

		String name = txtName.getText().trim();
		if (name.isEmpty()) {
			alert("Validation", "Name is required.");
			return;
		}

		if (ProductDAO.productNameExists(categoryId, name)) {
			alert("Duplicate", "Product name already exists in this category.");
			return;
		}

		Integer cartons = parseIntSafe(txtCartonsPerPallet, "Cartons per pallet");
		Integer qty = parseIntSafe(txtQuantity, "Quantity");
		Integer min = parseIntSafe(txtMin, "Min quantity");
		Integer max = parseIntSafe(txtMax, "Max quantity");
		Double cost = parseDoubleSafe(txtCost, "Cost price");
		Double sell = parseDoubleSafe(txtSell, "Selling price");

		if (cartons == null || qty == null || min == null || max == null || cost == null || sell == null)
			return;

		if (cartons <= 0) {
			alert("Validation", "Cartons per pallet must be greater than 0.");
			return;
		}
		if (qty < 0 || min < 0 || max < 0) {
			alert("Validation", "Quantities cannot be negative.");
			return;
		}
		if (min > max) {
			alert("Validation", "Min quantity cannot be greater than Max quantity.");
			return;
		}

		// ✅ capacity check (ceil pallets)
		int palletsNeeded = (int) Math.ceil(qty / (cartons * 1.0));
		if (!ProductDAO.canFitInWarehouse(w.getWarehouseId(), palletsNeeded, null)) {
			alert("Capacity", "Warehouse does not have enough space (pallet capacity).");
			return;
		}

		ProductDAO.ProductFormData d = new ProductDAO.ProductFormData();
		d.categoryId = categoryId;
		d.branchId = b.getBranchId();
		d.warehouseId = w.getWarehouseId();

		d.name = name;
		d.baseUnit = txtBaseUnit.getText().trim();
		d.cartonsPerPallet = cartons;
		d.quantity = qty;
		d.minQuantity = min;
		d.maxQuantity = max;
		d.costPrice = cost;
		d.sellingPrice = sell;
		d.description = txtDescription.getText().trim();
		d.imagePath = imagePath;

		if (ProductDAO.insertProduct(d)) {
			goBack();
		} else {
			alert("Error", "Failed to add product.");
		}
	}

	@FXML
	private void onCancel() {
		goBack();
	}

	private Integer parseIntSafe(TextField tf, String label) {
		String v = tf.getText().trim();
		if (v.isEmpty()) {
			alert("Validation", label + " is required.");
			return null;
		}
		try {
			return Integer.parseInt(v);
		} catch (NumberFormatException e) {
			alert("Validation", label + " must be a number.");
			return null;
		}
	}

	private Double parseDoubleSafe(TextField tf, String label) {
		String v = tf.getText().trim();
		if (v.isEmpty()) {
			alert("Validation", label + " is required.");
			return null;
		}
		try {
			return Double.parseDouble(v);
		} catch (NumberFormatException e) {
			alert("Validation", label + " must be a number.");
			return null;
		}
	}

	private void goBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Products.fxml"));
			StackPane contentArea = (StackPane) txtName.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
