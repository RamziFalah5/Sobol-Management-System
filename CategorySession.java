package application;

public class CategorySession {

	private static int selectedCategoryId = -1;
	private static String selectedCategoryName = null;

	// ================== Set ==================
	public static void setSelectedCategory(int id, String name) {
		selectedCategoryId = id;
		selectedCategoryName = name;
	}

	// ================== Get ==================
	public static int getSelectedCategoryId() {
		return selectedCategoryId;
	}

	public static String getSelectedCategoryName() {
		return selectedCategoryName;
	}

	// ================== State ==================
	public static boolean hasSelectedCategory() {
		return selectedCategoryId != -1;
	}

	// ================== Clear ==================
	public static void clear() {
		selectedCategoryId = -1;
		selectedCategoryName = null;
	}
}
