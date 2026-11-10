package application;

import javafx.fxml.FXML;
import org.junit.jupiter.api.Test;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Display-free regression checks; controller behavior is covered by desktop-smoke. */
class ControllerRepairContractTest {
    private Document document(String name) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var in = getClass().getResourceAsStream("/application/" + name)) {
            assertNotNull(in);
            return factory.newDocumentBuilder().parse(in);
        }
    }

    private Map<String, Element> ids(Document doc) {
        Map<String, Element> ids = new HashMap<>();
        var nodes = doc.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element e = (Element) nodes.item(i);
            if (e.hasAttributeNS("http://javafx.com/fxml/1", "id"))
                assertNull(ids.put(e.getAttributeNS("http://javafx.com/fxml/1", "id"), e), "Duplicate fx:id");
        }
        return ids;
    }

    @Test void dailyAttendanceUsesDailyControllerAndProvidesEveryInjectedField() throws Exception {
        var doc = document("DailyAttendance.fxml");
        assertEquals(DailyAttendanceController.class.getName(), doc.getDocumentElement()
                .getAttributeNS("http://javafx.com/fxml/1", "controller"));
        var ids = ids(doc);
        for (var field : DailyAttendanceController.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(FXML.class)) assertTrue(ids.containsKey(field.getName()), field.getName());
        }
        assertFalse(ids.containsKey("colBranch"));
        assertFalse(ids.containsKey("btnAnchor"));
        assertEquals("#onSave", ids.get("btnSave").getAttribute("onAction"));
        assertEquals("true", ids.get("table").getAttribute("editable"));
        assertEquals("false", ids.get("colName").getAttribute("editable"));
    }

    @Test void dailyTableAndCheckboxColumnUseDailyAttendanceRowProperties() throws Exception {
        var table = (ParameterizedType) DailyAttendanceController.class.getDeclaredField("table").getGenericType();
        assertEquals(DailyAttendanceRow.class, table.getActualTypeArguments()[0]);
        var present = (ParameterizedType) DailyAttendanceController.class.getDeclaredField("colPresent").getGenericType();
        assertArrayEquals(new Class<?>[]{DailyAttendanceRow.class, Boolean.class}, present.getActualTypeArguments());
        var row = new DailyAttendanceRow(101, "Synthetic worker", false);
        row.presentProperty().set(true);
        assertTrue(row.isPresent());
        assertEquals(101, row.getEmployeeId());
    }

    @Test void categoryEditorKeepsItsIdDisplaySeparateFromEditableValues() throws Exception {
        var doc = document("EditCategory.fxml");
        assertEquals(EditCategoryController.class.getName(), doc.getDocumentElement()
                .getAttributeNS("http://javafx.com/fxml/1", "controller"));
        var ids = ids(doc);
        assertEquals("Label", ids.get("lblCategoryId").getTagName());
        assertEquals("TextField", ids.get("txtName").getTagName());
        assertEquals("TextArea", ids.get("txtDescription").getTagName());
    }
}
