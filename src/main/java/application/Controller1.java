package application;

import java.io.IOException;
import java.sql.*;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class Controller1 {

	@FXML
	private TextField name; // موجود في Register و Signin
	@FXML
	private TextField email; // موجود في Register فقط
	@FXML
	private TextField phone; // موجود في Register فقط
	@FXML
	private TextField password; // موجود في Register و Signin

	// ================== تنقّل عام ==================
	private void go(ActionEvent e, String fxml) throws IOException {
		Parent root = FXMLLoader.load(getClass().getResource(fxml));
		Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
		Scene scene = new Scene(root, 480, 640);
		if (getClass().getResource("application.css") != null) {
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		}
		stage.setScene(scene);
		stage.centerOnScreen();
		stage.show();
	}

	private void go2(ActionEvent e, String fxml) throws IOException {
		Parent root = FXMLLoader.load(getClass().getResource(fxml));
		Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
		Scene scene = new Scene(root, 1180, 760);
		if (getClass().getResource("application.css") != null) {
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		}
		stage.setScene(scene);
		stage.centerOnScreen();
		stage.show();
	}

	@FXML
	public void switchToScene1(ActionEvent e) throws IOException {
		System.out.println(";;;;;;");
		go2(e, "System.fxml");
	}

	@FXML
	public void switchToScene2(ActionEvent e) throws IOException {
		go(e, "Signin.fxml");
	}

	@FXML
	public void switchToScene3(ActionEvent e) throws IOException {
		go(e, "Main1.fxml");
	}

	// ================== Register (Main1.fxml) ==================
	@FXML
	public void onRegister(ActionEvent e) {
		String nm = safe(name);
		String em = safe(email);
		String ph = safe(phone);
		String pw = safe(password);

		// تحقّق الحقول الفارغة أولاً
		if (nm.isEmpty() || em.isEmpty() || ph.isEmpty() || pw.isEmpty()) {
			info("Validation", "الرجاء تعبئة جميع الحقول: الاسم، الإيميل، الهاتف، كلمة السر.");
			return;
		}

		// تحقّق صيغة الاسم
		if (!isValidName(nm)) {
			info("Validation", "الاسم يجب أن يتكوّن من أحرف وأرقام، لا يبدأ برقم، ويحتوي حرفاً ورقماً على الأقل.");
			return;
		}

		// تحقّق صيغة الإيميل
		if (!isValidEmail(em)) {
			info("Validation", "صيغة الإيميل يجب أن تكون @gmail.com ولا تبدأ برقم.");
			return;
		}

		// تحقّق رقم الهاتف
		if (!isValidPhone(ph)) {
			info("Validation", "رقم الهاتف يجب أن يكون أرقاماً فقط ويبدأ بـ 05.");
			return;
		}

		// تحقّق قوة كلمة السر
		if (!isStrongPassword(pw)) {
			info("Validation", "كلمة السر يجب أن تحتوي على حرف صغير وحرف كبير ورقم على الأقل.");
			return;
		}

		String hash = PasswordUtil.sha256(pw);

		String sql = "INSERT INTO user_account(name,email,phone,password_hash) VALUES(?,?,?,?)";
		try (Connection c = DBAuth.get();
				PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			ps.setString(1, nm);
			ps.setString(2, em);
			ps.setString(3, ph);
			ps.setString(4, hash);
			ps.executeUpdate();

			info("Success", "تم إنشاء الحساب بنجاح. يمكنك تسجيل الدخول الآن.");
			go(e, "Signin.fxml"); // ارجاعك لشاشة الدخول بعد النجاح

		} catch (SQLIntegrityConstraintViolationException dup) {
			info("Error", "هذا الإيميل مسجّل من قبل.");
		} catch (Exception ex) {
			ex.printStackTrace();
			info("Error", "فشل إنشاء الحساب.");
		}
	}

	// ================== Login (Signin.fxml) ==================
	// المطلوب: الاسم + كلمة السر (كما طلبت)
	@FXML
	public void onLogin(ActionEvent e) {
		String em = safe(email); // ← استخدم حقل الإيميل
		String pw = safe(password);

		if (em.isEmpty() || pw.isEmpty()) {
			info("Validation", "الرجاء إدخال الإيميل وكلمة السر.");
			return;
		}

		// لو بدك تتأكد من صيغة الإيميل كمان في شاشة الدخول:
		if (!em.matches("^[A-Za-z][A-Za-z0-9._%+-]*@gmail\\.com$")) {
			info("Validation", "صيغة الإيميل يجب أن تكون @gmail.com ولا تبدأ برقم.");
			return;
		}

		String sql = "SELECT user_id, password_hash FROM user_account WHERE email=? LIMIT 1";
		try (Connection c = DBAuth.get(); PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, em);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					info("Error", "لا يوجد حساب بهذا الإيميل.");
					return;
				}
				String hash = rs.getString("password_hash");

				if (!PasswordUtil.sha256(pw).equals(hash)) {
					info("Error", "كلمة السر غير صحيحة.");
					return;
				}

				go2(e, "System.fxml"); // نجاح
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			info("Error", "فشل تسجيل الدخول.");
		}
	}

	private void info(String title, String msg) {
		show(Alert.AlertType.INFORMATION, title, msg);
	}

//	private void warn(String title, String msg) {
//		show(Alert.AlertType.WARNING, title, msg);
//	}
//
//	private void error(String title, String msg) {
//		show(Alert.AlertType.ERROR, title, msg);
//	}

	private void show(Alert.AlertType type, String title, String msg) {
		Alert a = new Alert(type);
		a.setTitle(title);
		a.setHeaderText(null);
		a.setContentText(msg);
		a.showAndWait();
	}

	private String safe(TextField tf) {
		return (tf == null || tf.getText() == null) ? "" : tf.getText().trim();
	}

	// يبدأ بحرف، أحرف وأرقام فقط، ويحتوي حرفاً واحداً على الأقل ورقماً واحداً على
	// الأقل
	private boolean isValidName(String s) {
		return s != null && s.matches("^(?!\\d)(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9]+$");
	}

	// Gmail فقط ولا يبدأ برقم
	private boolean isValidEmail(String s) {
		return s != null && s.matches("^(?!\\d)[A-Za-z][A-Za-z0-9._%+-]*@gmail\\.com$");
	}

	// أرقام فقط وتبدأ بـ 05 (بدون تقييد طول محدّد)
	private boolean isValidPhone(String s) {
		return s != null && s.matches("^05\\d+$");
	}

	// على الأقل حرف صغير وحرف كبير ورقم (بدون حدّ طول محدّد)
	private boolean isStrongPassword(String s) {
		return s != null && s.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
	}

}
