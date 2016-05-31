/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.l2pe;

import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.properties.control.PropertiesEditor;
import acmi.l2.clientmod.unreal.Environment;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.core.Class;
import acmi.l2.clientmod.unreal.core.Object;
import acmi.l2.clientmod.unreal.engine.Texture;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil;
import acmi.util.AutoCompleteComboBox;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.HasStack;
import static acmi.l2.clientmod.unreal.UnrealSerializerFactory.IS_STRUCT;

public class Controller implements Initializable {
    @FXML
    private Separator folderSeparator;
    @FXML
    private ComboBox<File> folderSelector;
    @FXML
    private Separator packageSeparator;
    @FXML
    private ComboBox<File> packageSelector;
    @FXML
    private Separator entrySeparator;
    @FXML
    private ComboBox<UnrealPackage.ExportEntry> entrySelector;
    @FXML
    private Button addName;
    @FXML
    private Button addImport;
    @FXML
    private Button addExport;
    @FXML
    private Button save;
    @FXML
    private PropertiesEditor properties;
    @FXML
    private ProgressIndicator loading;

    private L2PE application;
    private ObjectProperty<File> initialDirectory = new SimpleObjectProperty<>(this, "initialDirectory");
    private ObjectProperty<Environment> environment = new SimpleObjectProperty<>(this, "environment");
    private ObjectProperty<UnrealSerializerFactory> serializerFactory = new SimpleObjectProperty<>(this, "serializerFactory");
    private MapProperty<File, List<File>> packages = new SimpleMapProperty<>(this, "packages");
    private ObjectProperty<UnrealPackage> unrealPackage = new SimpleObjectProperty<>(this, "unrealPackage");

    public void setApplication(L2PE application) {
        this.application = application;
    }

    public File getInitialDirectory() {
        return initialDirectory.get();
    }

    public ObjectProperty<File> initialDirectoryProperty() {
        return initialDirectory;
    }

    public void setInitialDirectory(File initialDirectory) {
        this.initialDirectory.set(initialDirectory);
    }

    public Environment getEnvironment() {
        return environment.get();
    }

    public ObjectProperty<Environment> environmentProperty() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment.set(environment);
    }

    public UnrealSerializerFactory getSerializerFactory() {
        return serializerFactory.get();
    }

    public ReadOnlyObjectProperty<UnrealSerializerFactory> serializerFactoryProperty() {
        return serializerFactory;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setInitialDirectory(new File(L2PE.getPrefs().get("initialDirectory", System.getProperty("user.dir"))));
        initialDirectoryProperty().addListener((observable, oldVal, newVal) -> {
            if (newVal != null)
                L2PE.getPrefs().put("initialDirectory", newVal.getPath());
        });

        environmentProperty().addListener(observable -> {
            packages.setValue(FXCollections.observableMap(getEnvironment().listFiles()
                    .collect(Collectors.groupingBy(File::getParentFile))));
        });
        serializerFactory.bind(Bindings.createObjectBinding(() -> getEnvironment() != null ? new UnrealSerializerFactory(getEnvironment()) : null, environmentProperty()));
        properties.serializerProperty().bind(serializerFactoryProperty());
        properties.unrealPackageProperty().bind(unrealPackage);
        packages.addListener((Observable observable) -> {
            folderSelector.getSelectionModel().clearSelection();
            folderSelector.getItems().clear();
            folderSelector.getItems().addAll(packages.keySet()
                    .stream()
                    .sorted((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()))
                    .collect(Collectors.toList()));
        });

        BooleanBinding environmentSelected = Bindings.createBooleanBinding(() -> Objects.nonNull(getEnvironment()), environmentProperty());
        folderSeparator.visibleProperty().bind(environmentSelected);
        folderSelector.visibleProperty().bind(environmentSelected);
        AutoCompleteComboBox.autoCompleteComboBox(folderSelector, AutoCompleteComboBox.AutoCompleteMode.CONTAINING);
        StringConverter<File> name = new StringConverter<File>() {
            @Override
            public String toString(File object) {
                return object == null ? null : object.getName();
            }

            @Override
            public File fromString(String string) {
                throw new RuntimeException("unsupported");
            }
        };
        folderSelector.setConverter(name);

        BooleanBinding folderSelected = Bindings.createBooleanBinding(() -> Objects.nonNull(getSelectedItem(folderSelector)), folderSelector.getSelectionModel().selectedIndexProperty());
        packageSeparator.visibleProperty().bind(folderSelected);
        packageSelector.visibleProperty().bind(folderSelected);
        AutoCompleteComboBox.autoCompleteComboBox(packageSelector, AutoCompleteComboBox.AutoCompleteMode.CONTAINING);
        packageSelector.setConverter(name);
        folderSelector.getSelectionModel().selectedIndexProperty().addListener((observable) -> {
            packageSelector.getSelectionModel().clearSelection();
            packageSelector.getItems().clear();

            if (folderSelector.getSelectionModel().getSelectedIndex() < 0)
                return;

            packageSelector.getItems().addAll(packages.get(getSelectedItem(folderSelector)));
        });
        packageSelector.getSelectionModel().selectedIndexProperty().addListener((observable) -> {
            entrySelector.getSelectionModel().clearSelection();
            entrySelector.getItems().clear();

            unrealPackage.setValue(null);

            File newValue = getSelectedItem(packageSelector);

            if (newValue == null)
                return;

            try (UnrealPackage up = new UnrealPackage(newValue, true)) {
                unrealPackage.setValue(up);
            } catch (Exception e) {
                showException("Couldn't load: " + newValue, e);
            }
        });

        BooleanBinding packageSelected = Bindings.createBooleanBinding(() -> Objects.nonNull(unrealPackage.get()), unrealPackage);
        entrySeparator.visibleProperty().bind(packageSelected);
        addName.visibleProperty().bind(packageSelected);
        addImport.visibleProperty().bind(packageSelected);
        addExport.visibleProperty().bind(packageSelected);
        entrySelector.visibleProperty().bind(packageSelected);
        AutoCompleteComboBox.autoCompleteComboBox(entrySelector, AutoCompleteComboBox.AutoCompleteMode.CONTAINING);
        unrealPackage.addListener((observable, oldValue, newValue) -> {
            entrySelector.getSelectionModel().clearSelection();
            entrySelector.getItems().clear();

            if (newValue == null)
                return;

            entrySelector.getItems().addAll(newValue.getExportTable()
                    .stream()
                    .sorted((e1, e2) -> e1.getObjectFullName().compareToIgnoreCase(e2.getObjectFullName()))
                    .collect(Collectors.toList())
            );
        });
        entrySelector.getSelectionModel().selectedIndexProperty().addListener((observable) -> {
            properties.setStructName(null);
            properties.setPropertyList(null);

            UnrealPackage.ExportEntry newValue = getSelectedItem(entrySelector);

            if (newValue == null)
                return;

            new Thread(() -> {
                Platform.runLater(() -> loading.setVisible(true));
                try {
                    Object obj = getSerializerFactory().getOrCreateObject(newValue);

                    properties.setStructName(obj.getClassFullName());
                    properties.setPropertyList(FXCollections.observableList(obj.properties));
                } catch (Exception e) {
                    showException("Couldn't load entry", e);
                } finally {
                    Platform.runLater(() -> loading.setVisible(false));
                }
            }).start();
        });

        BooleanBinding entrySelected = Bindings.createBooleanBinding(() -> Objects.nonNull(getSelectedItem(entrySelector)), entrySelector.getSelectionModel().selectedIndexProperty());
        save.visibleProperty().bind(entrySelected);

        loading.setVisible(false);
    }

    public void selectL2ini() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open l2.ini");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("L2.ini", "L2.ini"),
                new FileChooser.ExtensionFilter("All files", "*.*"));

        if (getInitialDirectory() != null &&
                getInitialDirectory().exists() &&
                getInitialDirectory().isDirectory())
            fileChooser.setInitialDirectory(getInitialDirectory());

        File selected = fileChooser.showOpenDialog(application.getStage());
        if (selected == null)
            return;

        setInitialDirectory(selected.getParentFile());

        try {
            setEnvironment(Environment.fromIni(selected));
        } catch (Exception e) {
            showException("Couldn't load L2.ini", e);
            return;
        }

        new Thread(() -> {
            Platform.runLater(() -> loading.setVisible(true));
            try {
                getSerializerFactory().getOrCreateObject("Engine.Actor", IS_STRUCT);
            } catch (Exception e) {
                showException("Couldn't load Engine.Actor", e);
            } finally {
                Platform.runLater(() -> loading.setVisible(false));
            }
        }).start();
    }

    public void addName() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create name entry");
        dialog.setHeaderText(null);
        dialog.setContentText("Name string:");
        dialog.showAndWait()
                .ifPresent(name -> {
                            try (UnrealPackage up = new UnrealPackage(unrealPackage.get().getFile().openNewSession(false))) {
                                up.addNameEntries(name);
                                unrealPackage.set(up);
                            } catch (Exception e) {
                                showException("Couldn't add name entry", e);
                            }
                        }
                );
    }

    public void addImport() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Create import entry");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField name = new TextField();
        name.setPromptText("Package.Name");
        TextField clazz = new TextField();
        clazz.setPromptText("Core.Class");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Class:"), 0, 1);
        grid.add(clazz, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Pair<>(name.getText(), clazz.getText());
            }
            return null;
        });

        dialog.showAndWait()
                .ifPresent(nameClass -> {
                            try (UnrealPackage up = new UnrealPackage(unrealPackage.get().getFile().openNewSession(false))) {
                                up.addImportEntries(Collections.singletonMap(nameClass.getKey(), nameClass.getValue()));
                                unrealPackage.set(up);
                            } catch (Exception e) {
                                showException("Couldn't add import entry", e);
                            }
                        }
                );
    }

    public void addExport() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Create package entry");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField name = new TextField();
        name.setPromptText("Package.Name");
        TextField clazz = new TextField();
        clazz.setPromptText("Core.Class");
        CheckBox hasStack = new CheckBox("HasStack");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Class:"), 0, 1);
        grid.add(clazz, 1, 1);
        grid.add(hasStack, 0, 2);

        ButtonType objType = new ButtonType("Object", ButtonBar.ButtonData.OK_DONE);
        ButtonType classType = new ButtonType("Class", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(objType/*, classType*/, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return new String[]{dialogButton.getText(), name.getText(), clazz.getText()};
            }
            return null;
        });

        dialog.showAndWait()
                .ifPresent(nameClass -> {
                            try (UnrealPackage up = new UnrealPackage(unrealPackage.get().getFile().openNewSession(false))) {
                                String objName = nameClass[1];
                                String objClass = null;
                                String objSuperClass = null;
                                byte[] data;
                                int flags = UnrealPackage.DEFAULT_OBJECT_FLAGS;
                                switch (nameClass[0]) {
                                    case "Class":
                                        objSuperClass = nameClass[2];
                                        data = null; //TODO
                                        break;
                                    case "Object":
                                    default:
                                        objClass = nameClass[2];
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        DataOutput dataOutput = new DataOutputStream(baos, null);
                                        if (hasStack.isSelected()) {
                                            flags |= HasStack.getMask();

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
                                        dataOutput.writeCompactInt(up.nameReference("None"));
                                        data = baos.toByteArray();
                                        break;
                                }
                                up.addExportEntry(
                                        objName,
                                        objClass,
                                        objSuperClass,
                                        data,
                                        flags);
                                unrealPackage.set(up);
                            } catch (Exception e) {
                                showException("Couldn't add export entry", e);
                            }
                        }
                );
    }

    public void save() {
        UnrealPackage.ExportEntry selected = getSelectedItem(entrySelector);

        if (selected == null)
            return;

        new Thread(() -> {
            Platform.runLater(() -> loading.setVisible(true));
            try (UnrealPackage up = new UnrealPackage(unrealPackage.get().getFile().openNewSession(false))) {
                UnrealPackage.ExportEntry entry = up.getExportTable().get(selected.getIndex());
                Object object = getSerializerFactory().getOrCreateObject(entry);
                PropertiesEditor.removeDefaults(object.properties, getSerializerFactory(), selected.getUnrealPackage());
                UnrealRuntimeContext context = new UnrealRuntimeContext(entry, getSerializerFactory());
                {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutput<UnrealRuntimeContext> objectOutput = new ObjectOutputStream<>(baos, up.getFile().getCharset(), getSerializerFactory(), context);
                    objectOutput.write(object);
                    if (object instanceof Class)
                        PropertiesUtil.writeProperties(objectOutput, object.properties);
                    if (object.unreadBytes != null && object.unreadBytes.length > 0)
                        objectOutput.writeBytes(object.unreadBytes);
                    entry.setObjectRawData(baos.toByteArray());
                }
                if (object instanceof Texture) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutput<UnrealRuntimeContext> objectOutput = new ObjectOutputStream<>(baos, up.getFile().getCharset(), entry.getOffset(), getSerializerFactory(), context);
                    objectOutput.write(object);
                    if (object.unreadBytes != null && object.unreadBytes.length > 0)
                        objectOutput.writeBytes(object.unreadBytes);
                    entry.setObjectRawData(baos.toByteArray());
                }
                unrealPackage.set(up);
                entrySelector.getSelectionModel().select(entry);
            } catch (Exception e) {
                showException("Couldn't save entry", e);
            } finally {
                Platform.runLater(() -> loading.setVisible(false));
            }
        }).start();
    }

    private void showException(String text, Throwable ex) {
        Platform.runLater(() -> {
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
        });
    }

    private static <T> T getSelectedItem(ComboBox<T> comboBox) {
        int index = comboBox.getSelectionModel().getSelectedIndex();
        return index < 0 ? null : comboBox.getItems().get(index);
    }
}
