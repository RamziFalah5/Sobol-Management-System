package application;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class WarehousesController {

	@FXML
	private GridPane gridWarehouses;
	@FXML
	private ScrollPane scrollWarehouses;
	@FXML
	private TextField txtSearch;
	@FXML
	private Label lblStats;
	@FXML
	private Label lblEmpty;
	@FXML
	private Label lblSelected;

	private final ObservableList<Warehouse> warehouseList = FXCollections.observableArrayList();
	private Warehouse selectedWarehouse = null;

	private int branchId;

	private final String[] palette = { "#FF3D00", "#2962FF", "#00C853", "#D500F9", "#00B0FF", "#FFAB00", "#00BFA5",
			"#F50057", "#651FFF", "#1DE9B6", "#FF1744", "#76FF03" };

	private Image warehouseIcon = null;

	@FXML
	public void initialize() {

		branchId = BranchSession.getSelectedBranchId();

		warehouseIcon = loadWarehouseIconSmart();

		loadWarehouses();
		buildGrid(warehouseList);

		txtSearch.textProperty().addListener((obs, o, n) -> filter(n));
	}

	// ================= ICON LOADER =================
	private Image loadWarehouseIconSmart() {

		String[] resourceCandidates = { "/application/icons/warehouse.png", "../../../../OneDrive/Pictures/DataBases/werhouse.png",
				"/OneDrive/Pictures/DataBases/werhouse.png", "/images/werhouse.png", "/application/werhouse.png" };

		for (String p : resourceCandidates) {
			try {
				URL u = getClass().getResource(p);
				if (u != null)
					return new Image(u.toExternalForm(), true);
			} catch (Exception ignored) {
			}
		}

		try {
			String home = System.getProperty("user.home");

			Path f1 = Paths.get(home, "OneDrive", "Pictures", "DataBases", "werhouse.png");
			if (Files.exists(f1))
				return new Image(f1.toUri().toString(), true);

			Path f2 = Paths.get(home, "Pictures", "DataBases", "werhouse.png");
			if (Files.exists(f2))
				return new Image(f2.toUri().toString(), true);

		} catch (Exception ignored) {
		}

		return null;
	}

	// ================= LOAD =================
	private void loadWarehouses() {
		warehouseList.clear();
		warehouseList.addAll(WarehouseDAO.getWarehousesByBranch(branchId));

		lblStats.setText(warehouseList.size() + " warehouses");
		lblEmpty.setVisible(warehouseList.isEmpty());

		updateSelectedLabel();
	}

	// ================= SEARCH =================
	private void filter(String key) {

		if (key == null || key.isBlank()) {
			buildGrid(warehouseList);
			lblStats.setText(warehouseList.size() + " warehouses");
			lblEmpty.setVisible(warehouseList.isEmpty());
			return;
		}

		String k = key.toLowerCase().trim();
		ObservableList<Warehouse> filtered = FXCollections.observableArrayList();

		for (Warehouse w : warehouseList) {
			String name = safe(w.getName()).toLowerCase();
			if (name.contains(k))
				filtered.add(w);
		}

		buildGrid(filtered);
		lblStats.setText(filtered.size() + " results");
		lblEmpty.setVisible(filtered.isEmpty());
	}

	// ================= GRID =================
	private void buildGrid(List<Warehouse> list) {

		gridWarehouses.getChildren().clear();

		int cols = 3;
		int col = 0, row = 0;
		int colorIndex = 0;

		for (Warehouse w : list) {

			String accent = palette[colorIndex % palette.length];
			Button card = createWarehouseCard(w, accent);

			gridWarehouses.add(card, col, row);

			col++;
			if (col == cols) {
				col = 0;
				row++;
			}

			colorIndex++;
		}
	}

	// ================= CARD =================
	private Button createWarehouseCard(Warehouse w, String accentColor) {

		ImageView icon = new ImageView();
		icon.setFitHeight(62);
		icon.setFitWidth(62);
		icon.setPreserveRatio(true);
		if (warehouseIcon != null)
			icon.setImage(warehouseIcon);

		Label name = new Label(safe(w.getName()));
		name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

		Label cap = new Label("Pallet Capacity: " + w.getPalletCapacity());
		cap.setStyle("-fx-font-size: 12.5px; -fx-text-fill: rgba(255,255,255,0.92);");

		VBox box = new VBox(10, icon, name, cap);
		box.setStyle("-fx-alignment: center; -fx-padding: 14 12 14 12;");

		Button btn = new Button();
		btn.setGraphic(box);
		btn.setMinHeight(160);
		btn.setPrefHeight(160);
		btn.setPrefWidth(260);
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setFocusTraversable(false);

		String baseStyle = styleBase(accentColor);
		String selectedStyle = styleSelected(accentColor);

		btn.setStyle(isSelected(w) ? selectedStyle : baseStyle);

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
			if (!isSelected(w))
				btn.setStyle(styleHover(accentColor));
		});

		btn.setOnMouseExited(e -> {
			stIn.stop();
			stOut.playFromStart();
			btn.setStyle(isSelected(w) ? selectedStyle : baseStyle);
		});

		// ✅ Mouse clicks:
		// - Single LEFT: select only
		// - Double LEFT: select + view
		btn.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {

				// single click: select فقط
				if (e.getClickCount() == 1) {
					selectWarehouse(w);
					rebuildForSelection();
					return;
				}

				// double click: view
				if (e.getClickCount() == 2) {
					selectWarehouse(w);
					rebuildForSelection();
					viewWarehouse();
				}
			}
		});

		// ✅ Right click: select only (لأجل Edit)
		btn.setOnContextMenuRequested(e -> {
			selectWarehouse(w);
			rebuildForSelection();
		});

		return btn;
	}

	private void selectWarehouse(Warehouse w) {
		selectedWarehouse = w;
		updateSelectedLabel();
	}

	private void rebuildForSelection() {
		String k = txtSearch.getText();
		if (k == null || k.isBlank())
			buildGrid(warehouseList);
		else
			filter(k);
	}

	private boolean isSelected(Warehouse w) {
		return selectedWarehouse != null && selectedWarehouse.getWarehouseId() == w.getWarehouseId();
	}

	private void updateSelectedLabel() {
		if (lblSelected == null)
			return;

		if (selectedWarehouse == null)
			lblSelected.setText("Selected: none");
		else
			lblSelected.setText("Selected: " + safe(selectedWarehouse.getName()));
	}

	// ================= STYLES =================
	private String styleBase(String c) {
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

	// ================= ADD =================
	@FXML
	private void addWarehouse() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("AddWarehouse.fxml"));
			StackPane content = (StackPane) gridWarehouses.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ================= EDIT =================
	@FXML
	private void editWarehouse() {

		if (selectedWarehouse == null) {
			alert("Edit Warehouse", "Please select a warehouse first (Right-click to select).");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditWarehouse.fxml"));
			Parent view = loader.load();

			EditWarehouseController controller = loader.getController();
			controller.setWarehouse(selectedWarehouse);

			StackPane content = (StackPane) gridWarehouses.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ================= VIEW =================
	@FXML
	private void viewWarehouse() {

		if (selectedWarehouse == null) {
			alert("View Warehouse", "Please select a warehouse to view.");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ViewWarehouse.fxml"));
			Parent view = loader.load();

			ViewWarehouseController controller = loader.getController();
			controller.setWarehouse(selectedWarehouse);

			StackPane content = (StackPane) gridWarehouses.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String safe(String s) {
		return (s == null) ? "" : s;
	}

	private void alert(String t, String m) {
		Alert a = new Alert(Alert.AlertType.INFORMATION);
		a.setTitle(t);
		a.setHeaderText(null);
		a.setContentText(m);
		a.showAndWait();
	}
}
