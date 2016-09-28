package acmi.l2.clientmod.l2pe;

import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.HasStack;
import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.Standalone;
import static acmi.l2.clientmod.unreal.UnrealSerializerFactory.IS_STRUCT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.ObjectOutputStream;
import acmi.l2.clientmod.io.RandomAccess;
import acmi.l2.clientmod.io.RandomAccessFile;
import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.properties.L2Property;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class Util {
	private Util() {
		throw new RuntimeException();
	}
	
	public static final boolean SAVE_DEFAULTS = System.getProperty("L2pe.saveDefaults", "false").equalsIgnoreCase("true");
	public static final boolean SHOW_STACKTRACE = System.getProperty("L2pe.showStackTrace", "false").equalsIgnoreCase("true");
	
    public static void createClass(UnrealSerializerFactory serializer, UnrealPackage up, String objName, String objSuperClass, int flags, List<L2Property> properties) {
        flags |= Standalone.getMask();

        Stream.of(up.getPackageName(), "System")
                .filter(s -> up.nameReference(s) < 0)
                .forEach(up::addNameEntries);
        if (up.objectReferenceByName("Core.Object", IS_STRUCT) == 0)
            up.addImportEntries(Collections.singletonMap("Core.Object", "Core.Class"));
        up.addExportEntry(
                objName,
                null,
                objSuperClass,
                new byte[0],
                flags);
        UnrealPackage.ExportEntry entry = up.getExportTable().get(up.getExportTable().size() - 1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput<UnrealRuntimeContext> dataOutput = new ObjectOutputStream<>(baos, up.getFile().getCharset(), serializer, new UnrealRuntimeContext(entry, serializer));
        dataOutput.writeCompactInt(up.objectReferenceByName(objSuperClass, IS_STRUCT));
        dataOutput.writeCompactInt(0);
        dataOutput.writeCompactInt(0);
        dataOutput.writeCompactInt(0);
        dataOutput.writeCompactInt(up.nameReference(entry.getObjectName().getName()));
        dataOutput.writeCompactInt(0);
        dataOutput.writeInt(-1);
        dataOutput.writeInt(-1);
        dataOutput.writeInt(0);
        dataOutput.writeLong(0x0080000000000040L);
        dataOutput.writeLong(-1L);
        dataOutput.writeShort(-1);
        dataOutput.writeInt(0);
        dataOutput.writeInt(0x00000212);
        dataOutput.writeBytes(new byte[16]);
        dataOutput.writeCompactInt(2);
        dataOutput.writeCompactInt(entry.getObjectReference());
        dataOutput.writeInt(1);
        dataOutput.writeInt(0);
        dataOutput.writeCompactInt(entry.getObjectSuperClass().getObjectReference());
        dataOutput.writeInt(1);
        dataOutput.writeInt(0);
        Set<String> packages = new HashSet<>(Arrays.asList("Core", "Engine", up.getPackageName()));
        dataOutput.writeCompactInt(packages.size());
        for (String packageName : packages)
            dataOutput.writeCompactInt(up.nameReference(packageName));
        dataOutput.writeCompactInt(up.objectReferenceByName("Core.Object", IS_STRUCT));
        dataOutput.writeCompactInt(up.nameReference("System"));
        dataOutput.writeCompactInt(0);
        PropertiesUtil.writeProperties(dataOutput, properties);

        entry.setObjectRawData(baos.toByteArray());
    }

    public static void createObject(UnrealSerializerFactory serializer, UnrealPackage up, String objName, String objClass, int flags, boolean hasStack, List<L2Property> properties) {
        if (hasStack)
            flags |= HasStack.getMask();

        up.addExportEntry(
                objName,
                objClass,
                null,
                new byte[0],
                flags);
        UnrealPackage.ExportEntry entry = up.getExportTable().get(up.getExportTable().size() - 1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput<UnrealRuntimeContext> dataOutput = new ObjectOutputStream<>(baos, up.getFile().getCharset(), serializer, new UnrealRuntimeContext(entry, serializer));
        if (hasStack) {
            int classRef = up.objectReferenceByName(objClass, IS_STRUCT);
            if (classRef == 0) {
                up.addImportEntries(Collections.singletonMap(objClass, "Core.Class"));
                classRef = up.objectReferenceByName(objClass, IS_STRUCT);
            }
            dataOutput.writeCompactInt(classRef);
            dataOutput.writeCompactInt(classRef);
            dataOutput.writeLong(-1);
            dataOutput.writeInt(0);
            dataOutput.writeCompactInt(-1);
        }
        PropertiesUtil.writeProperties(dataOutput, properties);

        entry.setObjectRawData(baos.toByteArray());
    }
    
    public static void createBackup() {
		final MainWndController mainWnd = Controllers.getMainWndController();
		if(!mainWnd.isBackupEnable() || !mainWnd.isPackageSelected()) {
			return;
		}
		
		final RandomAccess ra = mainWnd.getUnrealPackage().getFile();
		if (ra instanceof RandomAccessFile) {
			final RandomAccessFile raf = (RandomAccessFile) ra;
			final File source = new File(raf.getPath());
			final String time = DateTimeFormatter.ofPattern("yyyy_MM_dd-HH_mm_ss").format(LocalDateTime.now());
			String path = source.getPath();
			path = path.substring(0, path.lastIndexOf(File.separatorChar));
			final File backup = new File(path, source.getName() + "_" + time);

			try {
				Files.copy(source.toPath(), backup.toPath());
			} catch(IOException e) {
				showException("Failed to create backup", e);
			}
		}
	}
	
	public static File getPackageFile(UnrealPackage.Entry<?> entry) throws IOException {
		final MainWndController mainWnd = Controllers.getMainWndController();
		final File root = mainWnd.getInitialDirectory().getParentFile();
		final String entryPath = entry.getObjectFullName();
		final String packageName = entryPath.substring(0, entryPath.indexOf('.'));
		
		return Files.walk(root.toPath())
			.filter(p -> p.toFile().isFile())
			.map(p -> p.toFile())
			.filter(f -> f.getName().contains(".") && f.getName().substring(0, f.getName().lastIndexOf('.')).equalsIgnoreCase(packageName))
			.findAny()
			.orElseThrow(IOException::new);
	}
	
	public static void showException(String text, Throwable ex) {
        Platform.runLater(() -> {
            if (SHOW_STACKTRACE) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText(text);

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                String exceptionText = sw.toString();

                Label label = new Label("Exception stacktrace:");

                TextArea textArea = new TextArea(exceptionText);
                textArea.setEditable(false);
                textArea.setWrapText(true);

                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);

                GridPane expContent = new GridPane();
                expContent.setMaxWidth(Double.MAX_VALUE);
                expContent.add(label, 0, 0);
                expContent.add(textArea, 0, 1);

                alert.getDialogPane().setExpandableContent(expContent);

                alert.showAndWait();
            } else {
                Throwable t = getTop(ex);

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(t.getClass().getSimpleName());
                alert.setHeaderText(text);
                alert.setContentText(t.getMessage());

                alert.showAndWait();
            }
        });
    }
    
	private static Throwable getTop(Throwable t) {
        while (t.getCause() != null)
            t = t.getCause();
        return t;
    }
	
	public static <T> void sort(ListView<T> list) {
		Platform.runLater(() -> Collections.sort(list.getItems(), new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o1.toString().compareTo(o2.toString());
			}
		}));
	}
	
	public static <T> void selectItem(ComboBox<T> comboBox, T select) {
    	for(T item : comboBox.getItems()) {
    		if(!item.equals(select)) {
    			continue;
    		}
    		
    		comboBox.getSelectionModel().select(item);
    		break;
    	}
    }
}
