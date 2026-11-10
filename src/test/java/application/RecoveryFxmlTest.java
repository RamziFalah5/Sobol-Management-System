package application;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import javafx.fxml.FXML;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Static contract checks never instantiate controllers or open a connection. */
class RecoveryFxmlTest {
    private static final Path RESOURCES = Path.of("src/main/resources/application");

    @Test void allOriginalFxmlControllersHandlersAndImageResourcesResolve() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        int count = 0;
        try (var files = Files.list(RESOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                count++;
                var doc = factory.newDocumentBuilder().parse(file.toFile());
                String name = doc.getDocumentElement().getAttributeNS("http://javafx.com/fxml/1", "controller");
                Class<?> controller = name.isEmpty() ? null : Class.forName(name, false, getClass().getClassLoader());
                var nodes = doc.getElementsByTagName("*");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element node = (Element) nodes.item(i);
                    var attrs = node.getAttributes();
                    for (int k = 0; k < attrs.getLength(); k++) {
                        var attr = attrs.item(k);
                        if (attr.getNodeName().startsWith("on") && attr.getNodeValue().startsWith("#")) {
                            assertNotNull(controller, file + " has events but no controller");
                            String handler = attr.getNodeValue().substring(1);
                            assertTrue(java.util.Arrays.stream(controller.getDeclaredMethods())
                                    .anyMatch(m -> m.getName().equals(handler)), file + " missing handler " + handler);
                        }
                    }
                    if (node.hasAttribute("url") && node.getAttribute("url").startsWith("@")) {
                        String relative = node.getAttribute("url").substring(1);
                        assertNotNull(getClass().getResource("/application/" + relative), file + " missing image resource");
                    }
                }
            }
        }
        assertEquals(53, count);
        assertNotNull(getClass().getResource("/images/user.png"));
        assertNotNull(getClass().getResource("/truck2.png"));
    }

    @Test void onlyDocumentedOriginalNavigationTargetRemainsUnavailable() throws Exception {
        Set<String> missing = new HashSet<>();
        Pattern fxml = Pattern.compile("\"([A-Za-z][A-Za-z0-9]*\\.fxml)\"");
        try (var sources = Files.list(Path.of("src/main/java/application"))) {
            for (Path file : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                var matcher = fxml.matcher(Files.readString(file));
                while (matcher.find()) {
                    if (getClass().getResource("/application/" + matcher.group(1)) == null) missing.add(matcher.group(1));
                }
            }
        }
        assertEquals(Set.of("FieldAdministrators.fxml"), missing);
    }

    @Test void injectionGapsAreLimitedToDocumentedOriginalDefectsAndSharedSigninFields() throws Exception {
        Set<String> gaps = new HashSet<>();
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var files = Files.list(RESOURCES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                var doc = factory.newDocumentBuilder().parse(file.toFile());
                String name = doc.getDocumentElement().getAttributeNS("http://javafx.com/fxml/1", "controller");
                if (name.isEmpty()) continue;
                Set<String> ids = new HashSet<>();
                var nodes = doc.getElementsByTagName("*");
                for (int i = 0; i < nodes.getLength(); i++) {
                    ids.add(((Element) nodes.item(i)).getAttributeNS("http://javafx.com/fxml/1", "id"));
                }
                var controller = Class.forName(name, false, getClass().getClassLoader());
                for (var field : controller.getDeclaredFields()) {
                    if (field.isAnnotationPresent(FXML.class) && !ids.contains(field.getName())) {
                        gaps.add(file.getFileName() + ":" + field.getName());
                    }
                }
            }
        }
        assertEquals(Set.of("Signin.fxml:name", "Signin.fxml:phone", "System.fxml:searchField",
                "AddShipment.fxml:rbCash", "AddShipment.fxml:rbCredit", "AddShipment.fxml:cbMethod"), gaps);
    }
}
