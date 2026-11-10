package application;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.geometry.Side;

import java.io.File;
import java.util.List;

public class ViewWarehouseController {

	// Header
	@FXML
	private Label lblTitle;
	@FXML
	private Label lblSubTitle;

	// Circle (only one)
	@FXML
	private ProgressIndicator progress;
	@FXML
	private Label lblPercent;
	@FXML
	private Label lblUsed;
	@FXML
	private Label lblRemaining;
	@FXML
	private Label lblCapacity;

	// Cards
	@FXML
	private Label lblTotalProducts;
	@FXML
	private Label lblTotalQty;
	@FXML
	private Label lblOutOfStock;
	@FXML
	private Label lblLowStock;
	@FXML
	private Label lblAvgCartons;
	@FXML
	private Label lblMaxPallets;

	// Charts
	@FXML
	private PieChart pieCategory;

	@FXML
	private BarChart<String, Number> barTopProducts;
	@FXML
	private CategoryAxis barXAxis;
	@FXML
	private NumberAxis barYAxis;

	@FXML
	private LineChart<String, Number> lineAdded;
	@FXML
	private CategoryAxis lineXAxis;
	@FXML
	private NumberAxis lineYAxis;

	@FXML
	private BarChart<String, Number> barStockStatus;
	@FXML
	private CategoryAxis stockXAxis;
	@FXML
	private NumberAxis stockYAxis;

	// Table
	@FXML
	private TableView<WarehouseProductRow> table;
	@FXML
	private TableColumn<WarehouseProductRow, String> colImage;
	@FXML
	private TableColumn<WarehouseProductRow, String> colName;
	@FXML
	private TableColumn<WarehouseProductRow, Integer> colQty;
	@FXML
	private TableColumn<WarehouseProductRow, Integer> colCartons;
	@FXML
	private TableColumn<WarehouseProductRow, Integer> colPalletsUsed;

	@FXML
	private TextField txtTableSearch;
	@FXML
	private Label lblTableHint;

	private final ObservableList<WarehouseProductRow> master = FXCollections.observableArrayList();
	private FilteredList<WarehouseProductRow> filtered;

	private Warehouse warehouse;

	@FXML
	public void initialize() {
		setupTable();

		filtered = new FilteredList<>(master, p -> true);
		table.setItems(filtered);

		if (txtTableSearch != null) {
			txtTableSearch.textProperty().addListener((obs, o, n) -> applyTableFilter(n));
		}

		// Pie legend like your first screenshot
		if (pieCategory != null) {
			pieCategory.setLegendVisible(true);
			pieCategory.setLegendSide(Side.BOTTOM);
			pieCategory.setLabelsVisible(true);
			pieCategory.setLabelLineLength(12);
		}

		styleAxes();
	}

	public void setWarehouse(Warehouse w) {
		this.warehouse = w;

		WarehouseViewDAO.WarehouseInfo info = WarehouseViewDAO.getWarehouseInfo(w.getWarehouseId());

		String whName = (info != null && info.warehouseName != null && !info.warehouseName.isBlank())
				? info.warehouseName
				: w.getName();

		String brName = (info != null) ? info.branchName : "-";
		int cap = (info != null) ? info.capacity : w.getPalletCapacity();

		lblTitle.setText("Warehouse - " + safe(whName));
		lblSubTitle.setText("Branch: " + safe(brName) + "   •   Capacity: " + cap + " pallets");

		loadAll();
	}

	// ================= TABLE =================
	private void setupTable() {

		colName.setCellValueFactory(d -> d.getValue().nameProperty());
		colQty.setCellValueFactory(d -> d.getValue().quantityProperty().asObject());
		colCartons.setCellValueFactory(d -> d.getValue().cartonsPerPalletProperty().asObject());
		colPalletsUsed.setCellValueFactory(d -> d.getValue().palletsUsedProperty().asObject());

		colImage.setCellValueFactory(d -> d.getValue().imagePathProperty());
		colImage.setCellFactory(tc -> new TableCell<>() {
			private final ImageView iv = new ImageView();
			{
				iv.setFitWidth(48);
				iv.setFitHeight(48);
				iv.setPreserveRatio(true);
				setStyle("-fx-alignment:CENTER;");
			}

			@Override
			protected void updateItem(String path, boolean empty) {
				super.updateItem(path, empty);
				if (empty) {
					setGraphic(null);
					return;
				}

				if (path == null || path.isBlank() || !new File(path).exists()) {
					iv.setImage(null);
				} else {
					iv.setImage(new Image(new File(path).toURI().toString(), 48, 48, true, true));
				}
				setGraphic(iv);
			}
		});

		center(colQty);
		center(colCartons);
		center(colPalletsUsed);
		center(colName);
	}

	private <T> void center(TableColumn<WarehouseProductRow, T> col) {
		col.setCellFactory(tc -> {
			TableCell<WarehouseProductRow, T> cell = new TableCell<>() {
				@Override
				protected void updateItem(T item, boolean empty) {
					super.updateItem(item, empty);
					setText(empty || item == null ? "" : String.valueOf(item));
				}
			};
			cell.setStyle("-fx-alignment:CENTER;");
			return cell;
		});
	}

	private void applyTableFilter(String key) {
		String k = (key == null) ? "" : key.trim().toLowerCase();
		filtered.setPredicate(row -> k.isEmpty() || safe(row.getName()).toLowerCase().contains(k));

		if (lblTableHint != null)
			lblTableHint.setText(filtered.size() + " rows");
	}

	// ================= LOAD ALL =================
	private void loadAll() {
		if (warehouse == null)
			return;

		loadProducts();
		loadStatsAndCircle();
		loadCharts();
	}

	private void loadProducts() {
		List<WarehouseProductRow> rows = WarehouseViewDAO.getProductsForWarehouse(warehouse.getWarehouseId());
		master.setAll(rows);

		if (lblTableHint != null)
			lblTableHint.setText(master.size() + " rows");
		applyTableFilter(txtTableSearch == null ? "" : txtTableSearch.getText());
	}

	// ================= STATS + CIRCLE =================
	private void loadStatsAndCircle() {
		WarehouseViewDAO.Summary s = WarehouseViewDAO.getWarehouseSummary(warehouse.getWarehouseId());

		int capacity = (s != null) ? s.capacity : warehouse.getPalletCapacity();
		int used = (s != null) ? s.usedPallets
				: WarehouseViewDAO.getUsedPalletsForWarehouse(warehouse.getWarehouseId());

		if (capacity < 0)
			capacity = 0;
		if (used < 0)
			used = 0;

		int remaining = Math.max(0, capacity - used);
		double ratio = (capacity > 0) ? Math.min(1.0, (double) used / (double) capacity) : 0.0;

		progress.setProgress(ratio);

		int pct = (int) Math.round(ratio * 100.0);
		lblPercent.setText(pct + "%");
		lblUsed.setText("Used: " + used + " pallets");
		lblRemaining.setText("Remaining: " + remaining + " pallets");
		lblCapacity.setText("Capacity: " + capacity + " pallets");

		if (s != null) {
			lblTotalProducts.setText(String.valueOf(s.totalProducts));
			lblTotalQty.setText(String.valueOf(s.totalQuantity));
			lblOutOfStock.setText(String.valueOf(s.outOfStock));
			lblLowStock.setText(String.valueOf(s.lowStock));
			lblAvgCartons.setText(String.valueOf(s.avgCartonsPerPallet));
			lblMaxPallets.setText(String.valueOf(s.maxPalletsSingleProduct));
		}
	}

	// ================= CHARTS =================
	@SuppressWarnings("unchecked")
	private void loadCharts() {
		int wid = warehouse.getWarehouseId();

		// PIE: show legend + names
		pieCategory.getData().clear();
		List<WarehouseViewDAO.CategorySlice> slices = WarehouseViewDAO.getCategoryPalletDistribution(wid, 8);
		for (WarehouseViewDAO.CategorySlice cs : slices) {
			pieCategory.getData().add(new PieChart.Data(cs.categoryName + " (" + cs.palletsUsed + ")", cs.palletsUsed));
		}

		// BAR: top products
		barTopProducts.getData().clear();
		XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
		barSeries.setName("Pallets Used");

		List<WarehouseViewDAO.BarPoint> top = WarehouseViewDAO.getTopProductsByPallets(wid, 10);

		for (WarehouseViewDAO.BarPoint bp : top) {
			barSeries.getData().add(new XYChart.Data<>(bp.name, bp.value));
		}

		barTopProducts.getData().add(barSeries);

		// LINE: added last 30 days
		lineAdded.getData().clear();
		XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
		lineSeries.setName("Products Added");

		List<WarehouseViewDAO.TrendPoint> trend = WarehouseViewDAO.getProductsAddedTrend(wid, 30);

		for (WarehouseViewDAO.TrendPoint t : trend) {
			lineSeries.getData().add(new XYChart.Data<>(t.label, t.value));
		}

		lineAdded.getData().add(lineSeries);

		// STOCK STATUS: clear bar chart with legend
		barStockStatus.getData().clear();
		WarehouseViewDAO.StockStatus st = WarehouseViewDAO.getStockStatusCounts(wid);

		XYChart.Series<String, Number> sOut = new XYChart.Series<>();
		sOut.setName("Out");
		sOut.getData().add(new XYChart.Data<>("Out", st.out));

		XYChart.Series<String, Number> sLow = new XYChart.Series<>();
		sLow.setName("Low");
		sLow.getData().add(new XYChart.Data<>("Low", st.low));

		XYChart.Series<String, Number> sOk = new XYChart.Series<>();
		sOk.setName("OK");
		sOk.getData().add(new XYChart.Data<>("OK", st.ok));

		XYChart.Series<String, Number> sHigh = new XYChart.Series<>();
		sHigh.setName("High");
		sHigh.getData().add(new XYChart.Data<>("High", st.high));

		barStockStatus.getData().addAll(sOut, sLow, sOk, sHigh);

		// Apply colors + axis visibility after render
		Platform.runLater(() -> {
			styleAxes();
			styleBarSeriesOrange(barSeries); // top products bars = strong orange
			styleLineOrange(lineSeries); // line = orange
			styleStockColors(); // 4 colors statuses
		});
	}

	// ================= Axis Style =================
	private void styleAxes() {
		String axisStyle = "-fx-tick-label-fill:#546E7A; -fx-font-size:11px; -fx-font-weight:bold;";
		@SuppressWarnings("unused")
		String labelStyle = "-fx-text-fill:#263238; -fx-font-weight:bold; -fx-font-size:12px;";

		if (barXAxis != null) {
			barXAxis.setStyle(axisStyle);
			barXAxis.setLabel("Product");
			barXAxis.lookup(".axis-label");
		}
		if (barYAxis != null) {
			barYAxis.setStyle(axisStyle);
			barYAxis.setLabel("Pallets Used");
			barYAxis.lookup(".axis-label");
		}

		if (lineXAxis != null) {
			lineXAxis.setStyle(axisStyle);
			lineXAxis.setLabel("Date");
		}
		if (lineYAxis != null) {
			lineYAxis.setStyle(axisStyle);
			lineYAxis.setLabel("Products");
		}

		if (stockXAxis != null) {
			stockXAxis.setStyle(axisStyle);
			stockXAxis.setLabel("Status");
		}
		if (stockYAxis != null) {
			stockYAxis.setStyle(axisStyle);
			stockYAxis.setLabel("Products Count");
		}

		// give charts readable plot backgrounds and grid lines
		setChartPlotStyle(barTopProducts);
		setChartPlotStyle(lineAdded);
		setChartPlotStyle(barStockStatus);
	}

	private void setChartPlotStyle(Chart chart) {
		if (chart == null)
			return;
		chart.setStyle("-fx-background-color: transparent;");
		var plot = chart.lookup(".chart-plot-background");
		if (plot != null)
			plot.setStyle("-fx-background-color: white; -fx-background-insets: 0;");

		var h = chart.lookup(".chart-horizontal-grid-lines");
		if (h != null)
			h.setStyle("-fx-stroke: #E0E0E0;");

		var v = chart.lookup(".chart-vertical-grid-lines");
		if (v != null)
			v.setStyle("-fx-stroke: #EFEFEF;");
	}

	// ================= Colors =================
	private void styleBarSeriesOrange(XYChart.Series<String, Number> series) {
		if (series == null)
			return;
		for (XYChart.Data<String, Number> d : series.getData()) {
			if (d.getNode() != null) {
				d.getNode().setStyle("-fx-bar-fill: #f7931e;");
			} else {
				d.nodeProperty().addListener((obs, old, node) -> {
					if (node != null)
						node.setStyle("-fx-bar-fill: #f7931e;");
				});
			}
		}
	}

	private void styleLineOrange(XYChart.Series<String, Number> series) {
		if (series == null)
			return;

		if (series.getNode() != null) {
			var line = series.getNode().lookup(".chart-series-line");
			if (line != null)
				line.setStyle("-fx-stroke:#f7931e; -fx-stroke-width:3px;");
		} else {
			series.nodeProperty().addListener((obs, old, node) -> {
				if (node != null) {
					var line = node.lookup(".chart-series-line");
					if (line != null)
						line.setStyle("-fx-stroke:#f7931e; -fx-stroke-width:3px;");
				}
			});
		}

		// symbols
		for (XYChart.Data<String, Number> d : series.getData()) {
			if (d.getNode() != null) {
				d.getNode().setStyle("-fx-background-color:#f7931e, white;");
			} else {
				d.nodeProperty().addListener((obs, old, node) -> {
					if (node != null)
						node.setStyle("-fx-background-color:#f7931e, white;");
				});
			}
		}
	}

	private void styleStockColors() {
		// Out = red, Low = amber, OK = green, High = blue
		colorSeries(barStockStatus, 0, "#FF1744");
		colorSeries(barStockStatus, 1, "#FFAB00");
		colorSeries(barStockStatus, 2, "#00C853");
		colorSeries(barStockStatus, 3, "#2962FF");
	}

	private void colorSeries(BarChart<String, Number> chart, int seriesIndex, String color) {
		if (chart == null)
			return;
		if (seriesIndex >= chart.getData().size())
			return;

		XYChart.Series<String, Number> s = chart.getData().get(seriesIndex);
		for (XYChart.Data<String, Number> d : s.getData()) {
			if (d.getNode() != null) {
				d.getNode().setStyle("-fx-bar-fill: " + color + ";");
			} else {
				d.nodeProperty().addListener((obs, old, node) -> {
					if (node != null)
						node.setStyle("-fx-bar-fill: " + color + ";");
				});
			}
		}
	}

	// ================= BACK =================
	@FXML
	private void onBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Warehouses.fxml"));
			StackPane content = (StackPane) table.getScene().getRoot().lookup("#contentArea");
			content.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String safe(String s) {
		return (s == null) ? "" : s;
	}
}
