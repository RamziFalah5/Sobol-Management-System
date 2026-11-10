package application;

public class BranchSession {
	private static int selectedBranchId;

	public static void setSelectedBranchId(int id) {
		selectedBranchId = id;
	}

	public static int getSelectedBranchId() {
		return selectedBranchId;
	}
}
