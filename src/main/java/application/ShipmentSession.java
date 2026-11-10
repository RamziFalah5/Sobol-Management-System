package application;

public class ShipmentSession {

	private static ProductPickRow selectedProduct;

	public static void setSelectedProduct(ProductPickRow p) {
		selectedProduct = p;
	}

	public static ProductPickRow getSelectedProduct() {
		return selectedProduct;
	}

	public static void clear() {
		selectedProduct = null;
	}
}
