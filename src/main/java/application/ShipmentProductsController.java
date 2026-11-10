package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.File;

public class ShipmentProductsController {

	@FXML
	private TableView<ProductPickRow> table;

	@FXML
	private TableColumn<ProductPickRow, String> colImage;
	@FXML
	private TableColumn<ProductPickRow, String> colName;
	@FXML
	private TableColumn<ProductPickRow, String> colCategory;
	@FXML
	private TableColumn<ProductPickRow, String> colBranch;
	@FXML
	private TableColumn<ProductPickRow, String> colWarehouse;
	@FXML
	private TableColumn<ProductPickRow, Number> colQty;
	@FXML
	private TableColumn<ProductPickRow, Number> colCartons;
	@FXML
	private TableColumn<ProductPickRow, Number> colCost;

	@FXML
	private TextField txtSearch;

	private final ObservableList<ProductPickRow> list = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		colName.setCellValueFactory(d -> d.getValue().productNameProperty());
		colCategory.setCellValueFactory(d -> d.getValue().categoryNameProperty());
		colBranch.setCellValueFactory(d -> d.getValue().branchNameProperty());
		colWarehouse.setCellValueFactory(d -> d.getValue().warehouseNameProperty());
		colQty.setCellValueFactory(d -> d.getValue().quantityProperty());
		colCartons.setCellValueFactory(d -> d.getValue().cartonsPerPalletProperty());
		colCost.setCellValueFactory(d -> d.getValue().costPriceProperty());

		colImage.setCellValueFactory(d -> d.getValue().imagePathProperty());
		colImage.setCellFactory(tc -> new TableCell<>() {

			private final ImageView iv = new ImageView();

			{
				iv.setFitWidth(45);
				iv.setFitHeight(45);
				iv.setPreserveRatio(true);
				setStyle("-fx-alignment: CENTER;");
			}

			@Override
			protected void updateItem(String path, boolean empty) {
				super.updateItem(path, empty);
				if (empty) {
					setGraphic(null);
					return;
				}

				try {
					if (path == null || path.isBlank()) {
						iv.setImage(null);
					} else {
						File f = new File(path);
						if (f.exists())
							iv.setImage(new Image(f.toURI().toString(), 45, 45, true, true));
						else
							iv.setImage(null);
					}
					setGraphic(iv);
				} catch (Exception e) {
					setGraphic(null);
				}
			}
		});

		load();

		txtSearch.textProperty().addListener((obs, o, n) -> filter(n));
	}

	private void load() {
		list.setAll(ShipmentDAO.getAllProductsForShipment());
		table.setItems(list);
	}

	private void filter(String key) {
		if (key == null || key.isBlank()) {
			table.setItems(list);
			return;
		}

		String k = key.toLowerCase();
		ObservableList<ProductPickRow> filtered = FXCollections.observableArrayList();

		for (ProductPickRow p : list) {
			String n = p.getProductName() == null ? "" : p.getProductName().toLowerCase();
			String c = p.getCategoryName() == null ? "" : p.getCategoryName().toLowerCase();

			if (n.contains(k) || c.contains(k)) {
				filtered.add(p);
			}
		}

		table.setItems(filtered);
	}

	@FXML
	private void onSelect() {

		ProductPickRow sel = table.getSelectionModel().getSelectedItem();
		if (sel == null) {
			alert("Select Product", "Please select a product first.");
			return;
		}

		ShipmentSession.setSelectedProduct(sel);
		go("AddShipment.fxml");
	}

	@FXML
	private void onBack() {
		ShipmentSession.clear();
		go("Imports.fxml");
	}

	private void go(String fxml) {
		try {
			Parent view = FXMLLoader.load(getClass().getResource(fxml));
			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
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
