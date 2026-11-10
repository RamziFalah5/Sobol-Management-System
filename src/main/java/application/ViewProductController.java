package application;

import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.File;

public class ViewProductController {

	@FXML
	private Circle statusCircle;
	@FXML
	private ImageView imgPreview;

	@FXML
	private Label lblId, lblName, lblBranch, lblWarehouse, lblUnit, lblPallet, lblQty, lblMin, lblMax, lblCost, lblSell,
			lblDesc;

	public void setProduct(Product p) {

		lblId.setText("ID: " + p.getProductId());

		lblName.setText("Name: " + p.getName());
		lblBranch.setText("Branch: " + p.getBranchName());
		lblWarehouse.setText("Warehouse: " + p.getWarehouseName());

		lblUnit.setText("Base Unit: " + p.getBaseUnit());
		lblPallet.setText("Cartons/Pallet: " + p.getCartonsPerPallet());
		lblQty.setText("Quantity: " + p.getQuantity());
		lblMin.setText("Min: " + p.getMinQuantity());
		lblMax.setText("Max: " + p.getMaxQuantity());
		lblCost.setText("Cost Price: " + p.getCostPrice());
		lblSell.setText("Selling Price: " + p.getSellingPrice());
		lblDesc.setText("Description: " + p.getDescription());

		int q = p.getQuantity();
		int min = p.getMinQuantity();
		int max = p.getMaxQuantity();

		if (q < min)
			statusCircle.setStyle("-fx-fill: #D32F2F;");
		else if (q <= max)
			statusCircle.setStyle("-fx-fill: #FB8C00;");
		else
			statusCircle.setStyle("-fx-fill: #2E7D32;");

		String path = p.getImagePath();
		if (path != null && !path.isBlank()) {
			File f = new File(path);
			if (f.exists())
				imgPreview.setImage(new Image(f.toURI().toString()));
		}
	}

	@FXML
	private void onBack() {
		try {
			Parent view = FXMLLoader.load(getClass().getResource("Products.fxml"));
			StackPane contentArea = (StackPane) statusCircle.getScene().getRoot().lookup("#contentArea");
			contentArea.getChildren().setAll(view);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
