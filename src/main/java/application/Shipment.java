package application;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Shipment {

	private final IntegerProperty shipmentId = new SimpleIntegerProperty();
	private final IntegerProperty productId = new SimpleIntegerProperty();
	private final IntegerProperty supplierId = new SimpleIntegerProperty();
	private final IntegerProperty branchId = new SimpleIntegerProperty();
	private final IntegerProperty warehouseId = new SimpleIntegerProperty();

	private final StringProperty productName = new SimpleStringProperty();
	private final StringProperty supplierName = new SimpleStringProperty();
	private final StringProperty branchName = new SimpleStringProperty();
	private final StringProperty warehouseName = new SimpleStringProperty();

	private final IntegerProperty quantity = new SimpleIntegerProperty();
	private final DoubleProperty unitCost = new SimpleDoubleProperty();
	private final DoubleProperty totalCost = new SimpleDoubleProperty();

	private final ObjectProperty<LocalDate> shipmentDate = new SimpleObjectProperty<>();
	private final StringProperty paymentType = new SimpleStringProperty();
	private final StringProperty shippingMethod = new SimpleStringProperty();
	private final StringProperty referenceNo = new SimpleStringProperty();
	private final StringProperty notes = new SimpleStringProperty();

	public Shipment() {
	}

	public Shipment(int shipmentId, int productId, int supplierId, int branchId, int warehouseId, String productName,
			String supplierName, String branchName, String warehouseName, int quantity, double unitCost,
			double totalCost, LocalDate shipmentDate, String paymentType, String shippingMethod, String referenceNo,
			String notes) {

		this.shipmentId.set(shipmentId);
		this.productId.set(productId);
		this.supplierId.set(supplierId);
		this.branchId.set(branchId);
		this.warehouseId.set(warehouseId);

		this.productName.set(productName);
		this.supplierName.set(supplierName);
		this.branchName.set(branchName);
		this.warehouseName.set(warehouseName);

		this.quantity.set(quantity);
		this.unitCost.set(unitCost);
		this.totalCost.set(totalCost);

		this.shipmentDate.set(shipmentDate);
		this.paymentType.set(paymentType);
		this.shippingMethod.set(shippingMethod);
		this.referenceNo.set(referenceNo);
		this.notes.set(notes);
	}

	public int getShipmentId() {
		return shipmentId.get();
	}

	public int getProductId() {
		return productId.get();
	}

	public int getSupplierId() {
		return supplierId.get();
	}

	public int getBranchId() {
		return branchId.get();
	}

	public int getWarehouseId() {
		return warehouseId.get();
	}

	public String getProductName() {
		return productName.get();
	}

	public String getSupplierName() {
		return supplierName.get();
	}

	public String getBranchName() {
		return branchName.get();
	}

	public String getWarehouseName() {
		return warehouseName.get();
	}

	public int getQuantity() {
		return quantity.get();
	}

	public double getUnitCost() {
		return unitCost.get();
	}

	public double getTotalCost() {
		return totalCost.get();
	}

	public LocalDate getShipmentDate() {
		return shipmentDate.get();
	}

	public String getPaymentType() {
		return paymentType.get();
	}

	public String getShippingMethod() {
		return shippingMethod.get();
	}

	public String getReferenceNo() {
		return referenceNo.get();
	}

	public String getNotes() {
		return notes.get();
	}

	public IntegerProperty shipmentIdProperty() {
		return shipmentId;
	}

	public StringProperty productNameProperty() {
		return productName;
	}

	public StringProperty supplierNameProperty() {
		return supplierName;
	}

	public StringProperty branchNameProperty() {
		return branchName;
	}

	public StringProperty warehouseNameProperty() {
		return warehouseName;
	}

	public IntegerProperty quantityProperty() {
		return quantity;
	}

	public DoubleProperty unitCostProperty() {
		return unitCost;
	}

	public DoubleProperty totalCostProperty() {
		return totalCost;
	}

	public ObjectProperty<LocalDate> shipmentDateProperty() {
		return shipmentDate;
	}

	public StringProperty paymentTypeProperty() {
		return paymentType;
	}

	public StringProperty shippingMethodProperty() {
		return shippingMethod;
	}

	public StringProperty referenceNoProperty() {
		return referenceNo;
	}
}
