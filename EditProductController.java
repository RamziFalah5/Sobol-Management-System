package application;

import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;

public class EditProductController {

	@FXML
	private Label lblId;

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

	private int productId;
	private int categoryId;
	private String imagePath;

	// old values for capacity logic
	private int oldWarehouseId;

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

	public void setProduct(Product p) {
		productId = p.getProductId();
		categoryId = p.getCategoryId();
		imagePath = p.getImagePath();

		oldWarehouseId = p.getWarehouseId();

		lblId.setText("ID: " + productId);

		txtName.setText(p.getName());
		txtBaseUnit.setText(p.getBaseUnit());
		txtCartonsPerPallet.setText(String.valueOf(p.getCartonsPerPallet()));
		txtQuantity.setText(String.valueOf(p.getQuantity()));
		txtMin.setText(String.valueOf(p.getMinQuantity()));
		txtMax.setText(String.valueOf(p.getMaxQuantity()));
		txtCost.setText(String.valueOf(p.getCostPrice()));
		txtSell.setText(String.valueOf(p.getSellingPrice()));
		txtDescription.setText(p.getDescription());

		if (imagePath != null && !imagePath.isBlank()) {
			File f = new File(imagePath);
			if (f.exists())
				imgPreview.setImage(new Image(f.toURI().toString()));
		}

		// ✅ select branch, then warehouses, then select warehouse
		selectBranchAndWarehouse(p.getBranchId(), p.getWarehouseId());
	}

	private void selectBranchAndWarehouse(int branchId, int warehouseId) {
		for (Branch b : cbBranch.getItems()) {
			if (b.getBranchId() == branchId) {
				cbBranch.setValue(b);
				break;
			}
		}
		loadWarehouses(branchId);

		for (Warehouse w : cbWarehouse.getItems()) {
			if (w.getWarehouseId() == warehouseId) {
				cbWarehouse.setValue(w);
				break;
			}
		}
	}

	private void loadBranches() {
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
	}

	private void loadWarehouses(int branchId) {
		cbWarehouse.setItems(FXCollections.observableArrayList(WarehouseDAO.getWarehousesByBranch(branchId)));

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
	private void onSave() {

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

		if (ProductDAO.productNameExistsForEdit(categoryId, name, productId)) {
			alert("Duplicate", "Another product already uses this name in this category.");
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

		int palletsNeeded = (int) Math.ceil(qty / (cartons * 1.0));

		// ✅ exclude current product from used pallets
		if (!ProductDAO.canFitInWarehouse(w.getWarehouseId(), palletsNeeded, productId)) {
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

		if (ProductDAO.updateProduct(productId, d)) {
			goBack();
		} else {
			alert("Error", "Failed to update product.");
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
