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

public class BranchesController {

	@FXML
	private GridPane gridBranches;
	@FXML
	private ScrollPane scrollBranches;
	@FXML
	private TextField txtSearch;
	@FXML
	private Label lblStats;
	@FXML
	private Label lblEmpty;
	@FXML
	private Label lblSelected;

	private final ObservableList<Branch> branchList = FXCollections.observableArrayList();
	private Branch selectedBranch = null;

	// ألوان فاقعة
	private final String[] palette = { "#FF3D00", "#2962FF", "#00C853", "#D500F9", "#00B0FF", "#FFAB00", "#00BFA5",
			"#F50057", "#651FFF", "#1DE9B6", "#FF1744", "#76FF03" };

	private Image branchIconImage = null;

	@FXML
	public void initialize() {

		branchIconImage = loadBranchIconSmart();

		loadBranches();
		buildGrid(branchList);

		txtSearch.textProperty().addListener((obs, o, n) -> filterBranches(n));
	}

	// ================= ICON LOADER =================
	private Image loadBranchIconSmart() {

		String[] resourceCandidates = { "../../../../OneDrive/Pictures/DataBases/Branches.png",
				"/OneDrive/Pictures/DataBases/Branches.png", "/images/Branches.png", "/application/Branches.png" };

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

			Path f1 = Paths.get(home, "OneDrive", "Pictures", "DataBases", "Branches.png");
			if (Files.exists(f1))
				return new Image(f1.toUri().toString(), true);

			Path f2 = Paths.get(home, "Pictures", "DataBases", "Branches.png");
			if (Files.exists(f2))
				return new Image(f2.toUri().toString(), true);

		} catch (Exception ignored) {
		}

		return null;
	}

	// ================= LOAD =================
	private void loadBranches() {
		branchList.setAll(BranchDAO.getAllBranches());

		lblStats.setText(branchList.size() + " branches");
		lblEmpty.setVisible(branchList.isEmpty());

		updateSelectedLabel();
	}

	// ================= SEARCH =================
	private void filterBranches(String keyword) {

		if (keyword == null || keyword.isBlank()) {
			buildGrid(branchList);
			lblStats.setText(branchList.size() + " branches");
			lblEmpty.setVisible(branchList.isEmpty());
			return;
		}

		String k = keyword.toLowerCase().trim();
		ObservableList<Branch> filtered = FXCollections.observableArrayList();

		for (Branch b : branchList) {
			String name = safe(b.getName()).toLowerCase();
			String city = safe(b.getCity()).toLowerCase();

			if (name.contains(k) || city.contains(k))
				filtered.add(b);
		}

		buildGrid(filtered);
		lblStats.setText(filtered.size() + " results");
		lblEmpty.setVisible(filtered.isEmpty());
	}

	// ================= GRID =================
	private void buildGrid(List<Branch> list) {

		gridBranches.getChildren().clear();

		int cols = 3;
		int col = 0, row = 0;
		int colorIndex = 0;

		for (Branch b : list) {
			Button card = createBranchCard(b, palette[colorIndex % palette.length]);
			gridBranches.add(card, col, row);

			col++;
			if (col == cols) {
				col = 0;
				row++;
			}
			colorIndex++;
		}
	}

	// ================= CARD =================
	private Button createBranchCard(Branch b, String accentColor) {

		ImageView icon = new ImageView();
		icon.setFitHeight(56);
		icon.setFitWidth(56);
		icon.setPreserveRatio(true);
		if (branchIconImage != null)
			icon.setImage(branchIconImage);

		Label name = new Label(safe(b.getName()));
		name.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:white;");

		// ✅ سطر 1: City + Warehouses
		Label metaLine1 = new Label(safe(b.getCity()) + "  •  Warehouses: " + b.getWarehousesCount());
		metaLine1.setStyle("-fx-font-size:12px; -fx-text-fill: rgba(255,255,255,0.92);");

		// ✅ سطر 2: Capacity لوحده (عشان ما يختفي)
		Label metaLine2 = new Label("Capacity: " + b.getTotalCapacity());
		metaLine2.setStyle("-fx-font-size:12px; -fx-text-fill: rgba(255,255,255,0.92);");

		VBox box = new VBox(6, icon, name, metaLine1, metaLine2);
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

		btn.setStyle(isSelected(b) ? selectedStyle : baseStyle);

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
			if (!isSelected(b))
				btn.setStyle(styleHover(accentColor));
		});

		btn.setOnMouseExited(e -> {
			stIn.stop();
			stOut.playFromStart();
			btn.setStyle(isSelected(b) ? selectedStyle : baseStyle);
		});

		// ✅ Mouse behavior (مثل Warehouses):
		// Single LEFT: select only
		// Double LEFT: open warehouses
		btn.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {

				if (e.getClickCount() == 1) {
					selectBranch(b);
					rebuildForSelection();
					return;
				}

				if (e.getClickCount() == 2) {
					selectBranch(b);
					rebuildForSelection();
					openWarehouses(b);
				}
			}
		});

		// ✅ Right click: select only (لأجل Edit)
		btn.setOnContextMenuRequested(e -> {
			selectBranch(b);
			rebuildForSelection();
		});

		return btn;
	}

	private void selectBranch(Branch b) {
		selectedBranch = b;
		updateSelectedLabel();
	}

	private void rebuildForSelection() {
		String k = txtSearch.getText();
		if (k == null || k.isBlank())
			buildGrid(branchList);
		else
			filterBranches(k);
	}

	private boolean isSelected(Branch b) {
		return selectedBranch != null && selectedBranch.getBranchId() == b.getBranchId();
	}

	private void updateSelectedLabel() {
		if (lblSelected == null)
			return;

		if (selectedBranch == null)
			lblSelected.setText("Selected: none");
		else
			lblSelected.setText("Selected: " + safe(selectedBranch.getName()));
	}

	private void openWarehouses(Branch b) {
		BranchSession.setSelectedBranchId(b.getBranchId());

		try {
			Parent view = FXMLLoader.load(getClass().getResource("Warehouses.fxml"));
			StackPane contentArea = (StackPane) gridBranches.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);
		} catch (Exception ex) {
			ex.printStackTrace();
			alert("Warehouses", "Cannot open Warehouses screen.");
		}
	}

	// ================= ADD =================
	@FXML
	private void addBranch() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("AddBranch.fxml"));
			StackPane contentArea = (StackPane) gridBranches.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ================= EDIT =================
	@FXML
	private void editBranch() {

		if (selectedBranch == null) {
			alert("Edit Branch", "Please select a branch first (Right-click to select).");
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("EditBranch.fxml"));
			Parent view = loader.load();

			EditBranchController controller = loader.getController();
			controller.setBranch(selectedBranch);

			StackPane contentArea = (StackPane) gridBranches.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
