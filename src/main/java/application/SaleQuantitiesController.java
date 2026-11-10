package application;

import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.util.*;

public class SaleQuantitiesController {

    @FXML
    private Label lblTitle;
    @FXML
    private ComboBox<SalesDAO.EmployeeMini> cbEmployee;
    @FXML
    private TextField txtTotal;
    @FXML
    private TableView<ProductSalePickRow> table;
    @FXML
    private TableColumn<ProductSalePickRow, String> colName;
    @FXML
    private TableColumn<ProductSalePickRow, Number> colAvail;
    @FXML
    private TableColumn<ProductSalePickRow, Number> colPrice;
    @FXML
    private TableColumn<ProductSalePickRow, Number> colQty;

    private Customer customer;
    private final ObservableList<ProductSalePickRow> picked = FXCollections.observableArrayList();

    /* ===================== PUBLIC API ===================== */

    public void setData(Customer customer, List<ProductSalePickRow> selected) {
        this.customer = customer;
        this.picked.setAll(selected);

        lblTitle.setText("Quantities - Customer: " + customer.getFullName());

        loadEmployees();
        setupTable();
        computeTotal();
    }

    /* ===================== INIT ===================== */

    private void loadEmployees() {
        cbEmployee.getItems().setAll(SalesDAO.getSalesEmployees());
    }

    private void setupTable() {
        colName.setCellValueFactory(d -> d.getValue().productNameProperty());
        colAvail.setCellValueFactory(d -> d.getValue().availableQtyProperty());
        colPrice.setCellValueFactory(d -> d.getValue().sellingPriceProperty());

        // ✅ FIXED CELL FACTORY (NO OVERLAPPING)
        colQty.setCellFactory(tc -> new TableCell<>() {

            private final Spinner<Integer> spinner = new Spinner<>();

            {
                spinner.setEditable(false);
                spinner.setPrefWidth(120);

                spinner.valueProperty().addListener((obs, oldV, newV) -> {
                    ProductSalePickRow row =
                            getTableRow() == null ? null : getTableRow().getItem();
                    if (row != null) {
                        row.setPickedQty(newV);
                        computeTotal();
                    }
                });
            }

            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                ProductSalePickRow row = getTableRow().getItem();
                int max = Math.max(0, row.getAvailableQty());
                int start = row.getPickedQty();

                SpinnerValueFactory.IntegerSpinnerValueFactory vf =
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, max, start);

                spinner.setValueFactory(vf);
                setGraphic(spinner);
            }
        });

        table.setItems(picked);

        txtTotal.setEditable(false);
        txtTotal.setFocusTraversable(false);
    }

    /* ===================== TOTAL ===================== */

    private void computeTotal() {
        double total = 0.0;

        for (ProductSalePickRow r : picked) {
            total += r.getPickedQty() * r.getSellingPrice();
        }

        txtTotal.setText(String.format("%.2f", total));
    }

    /* ===================== ACTIONS ===================== */

    @FXML
    private void onComplete() {

        if (customer == null) {
            alert("Error", "No customer selected.");
            return;
        }

        SalesDAO.EmployeeMini emp = cbEmployee.getSelectionModel().getSelectedItem();
        if (emp == null) {
            alert("Validation", "Please select a sales employee (commission > 0).");
            return;
        }

        List<SalesDAO.OrderItemForm> items = new ArrayList<>();

        for (ProductSalePickRow r : picked) {
            int qty = r.getPickedQty();

            if (qty <= 0)
                continue;

            if (qty > r.getAvailableQty()) {
                alert(
                    "Stock",
                    "Not enough stock for: " + r.getProductName()
                    + "\nAvailable: " + r.getAvailableQty()
                    + " , Requested: " + qty
                );
                return;
            }

            items.add(new SalesDAO.OrderItemForm(r.getProductId(), qty));
        }

        if (items.isEmpty()) {
            alert("Validation", "Please set quantity for at least one product.");
            return;
        }

        boolean ok = SalesDAO.createOrder(customer.getCustomerId(), emp.id, items);

        if (!ok) {
            alert(
                "Error",
                "Failed to complete order.\n(Stock may have changed or DB error)"
            );
            return;
        }

        go("Sales.fxml");
    }

    @FXML
    private void onBack() {
        go("PickProductsForSale.fxml");
    }

    /* ===================== NAV ===================== */

    private void go(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxml));
            StackPane content =
                    (StackPane) table.getScene().getRoot().lookup("#contentArea");
            content.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===================== UTIL ===================== */

    private void alert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }
}
