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

import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.ObjectOutputStream;
import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.properties.control.PropertiesEditor;
import acmi.l2.clientmod.unreal.Environment;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.core.Class;
import acmi.l2.clientmod.unreal.core.Object;
import acmi.l2.clientmod.unreal.engine.Texture;
import acmi.l2.clientmod.unreal.properties.L2Property;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil;
import acmi.util.AutoCompleteComboBox;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static acmi.l2.clientmod.unreal.UnrealSerializerFactory.IS_STRUCT;
import static acmi.util.AutoCompleteComboBox.getSelectedItem;

public class Controller extends ControllerBase implements Initializable {
    private static final Logger log = Logger.getLogger(Controller.class.getName());

    private static final boolean SAVE_DEFAULTS = System.getProperty("L2pe.saveDefaults", "false").equalsIgnoreCase("true");
    private static final boolean SHOW_STACKTRACE = System.getProperty("L2pe.showStackTrace", "false").equalsIgnoreCase("true");

    @FXML
    private Menu packageMenu;
    @FXML
    private Menu entryMenu;
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
    private Button copy;
    @FXML
    private CheckMenuItem showAllProperties;
    @FXML
    private PropertiesEditor properties;
    @FXML
    private ProgressIndicator loading;

    private L2PE application;
    private ObjectProperty<File> initialDirectory = new SimpleObjectProperty<>(this, "initialDirectory");

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

    @Override
    protected void execute(Task task, Consumer<Exception> exceptionHandler) {
        super.execute(wrap(task), exceptionHandler);
    }

    private Task wrap(Task task) {
        return () -> {
            Platform.runLater(() -> loading.setVisible(true));

            try {
                task.run();
            } finally {
                Platform.runLater(() -> loading.setVisible(false));
            }
        };
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setInitialDirectory(new File(L2PE.getPrefs().get("initialDirectory", System.getProperty("user.dir"))));
        initialDirectoryProperty().addListener((observable, oldVal, newVal) -> {
            if (newVal != null)
                L2PE.getPrefs().put("initialDirectory", newVal.getPath());
        });

        properties.editableOnlyProperty().bind(showAllProperties.selectedProperty().not());
        properties.serializerProperty().bind(serializerFactoryProperty());
        properties.unrealPackageProperty().bind(unrealPackageProperty());
        packagesProperty().addListener((Observable observable) -> {
            folderSelector.getSelectionModel().clearSelection();
            folderSelector.getItems().clear();
            folderSelector.getItems().addAll(getPackages().keySet()
                    .stream()
                    .sorted((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()))
                    .collect(Collectors.toList()));
        });

        folderSeparator.visibleProperty().bind(environmentSelected());
        folderSelector.visibleProperty().bind(environmentSelected());
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
        packageSelector.setConverter(name);
        folderSelector.getSelectionModel().selectedIndexProperty().addListener((observable) -> {
            packageSelector.getSelectionModel().clearSelection();
            packageSelector.getItems().clear();

            if (folderSelector.getSelectionModel().getSelectedIndex() < 0)
                return;

            packageSelector.getItems().addAll(getPackages().get(getSelectedItem(folderSelector)));

            AutoCompleteComboBox.autoCompleteComboBox(packageSelector, AutoCompleteComboBox.AutoCompleteMode.CONTAINING);
        });
        packageSelector.getSelectionModel().selectedIndexProperty().addListener((observable) -> {
            entrySelector.getSelectionModel().clearSelection();
            entrySelector.getItems().clear();

            setUnrealPackage(null);

            File newValue = getSelectedItem(packageSelector);

            if (newValue == null)
                return;

            execute(() -> {
                try (UnrealPackage up = new UnrealPackage(newValue, true)) {
                    Platform.runLater(() -> setUnrealPackage(up));
                }
            }, e -> {
                log.log(Level.SEVERE, e, () -> "Couldn't load: " + newValue);

                showException("Couldn't load: " + newValue, e);
            });
        });

        packageMenu.disableProperty().bind(packageSelected().not());
        entrySeparator.visibleProperty().bind(packageSelected());
        addName.visibleProperty().bind(packageSelected());
        addImport.visibleProperty().bind(packageSelected());
        addExport.visibleProperty().bind(packageSelected());
        entrySelector.visibleProperty().bind(packageSelected());
        unrealPackageProperty().addListener((observable, oldValue, newValue) -> {
            entrySelector.getSelectionModel().clearSelection();
            entrySelector.getItems().clear();

            if (newValue == null)
                return;

            entrySelector.getItems().addAll(newValue.getExportTable()
                    .stream()
                    .sorted((e1, e2) -> e1.getObjectFullName().compareToIgnoreCase(e2.getObjectFullName()))
                    .collect(Collectors.toList())
            );

            AutoCompleteComboBox.autoCompleteComboBox(entrySelector, AutoCompleteComboBox.AutoCompleteMode.CONTAINING);
        });

        entrySelector.getSelectionModel().selectedIndexProperty().addListener(observable -> {
            setEntry(getSelectedItem(entrySelector));
        });
        entryProperty().addListener((observable, oldValue, newValue) -> {
            setObject(null);

            if (newValue == null)
                return;

            execute(() -> setObject(getSerializerFactory().getOrCreateObject(newValue)), e -> {
                log.log(Level.SEVERE, e, () -> "Couldn't load entry");

                showException("Couldn't load entry", e);
            });
        });
        objectProperty().addListener((observable, oldValue, newValue) -> {
            properties.setStructName(null);
            properties.setPropertyList(null);

            if (newValue == null)
                return;

            properties.setStructName(newValue.entry != null && newValue.entry.getObjectClass() == null ?
                    newValue.entry.getObjectSuperClass().getObjectFullName() :
                    newValue.getClassFullName());
            properties.setPropertyList(FXCollections.observableList(newValue.properties));
        });
        entryMenu.disableProperty().bind(entrySelected().not());
        save.visibleProperty().bind(entrySelected());
        copy.visibleProperty().bind(Bindings.createBooleanBinding(() -> canCopy(getObject()), objectProperty()));

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
            log.log(Level.SEVERE, e, () -> "Couldn't load L2.ini");

            showException("Couldn't load L2.ini", e);
            return;
        }

        execute(() -> getSerializerFactory().getOrCreateObject("Engine.Actor", IS_STRUCT), e -> {
            log.log(Level.SEVERE, e, () -> "Couldn't load Engine.Actor");

            showException("Couldn't load Engine.Actor", e);
        });
    }

    public void addName() {
        if (!isPackageSelected())
            return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create name entry");
        dialog.setHeaderText(null);
        dialog.setContentText("Name string:");
        dialog.showAndWait()
                .ifPresent(name -> execute(() -> {
                            try (UnrealPackage up = new UnrealPackage(getUnrealPackage().getFile().openNewSession(false))) {
                                up.addNameEntries(name);
                                Platform.runLater(() -> setUnrealPackage(up));

                                getEnvironment().markInvalid(up.getPackageName());
                            }
                        }, e -> {
                            log.log(Level.SEVERE, e, () -> "Couldn't add name entry");

                            showException("Couldn't add name entry", e);
                        }
                ));
    }

    public void addImport() {
        if (!isPackageSelected())
            return;

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
                .ifPresent(nameClass -> execute(() -> {
                    try (UnrealPackage up = new UnrealPackage(getUnrealPackage().getFile().openNewSession(false))) {
                        up.addImportEntries(Collections.singletonMap(nameClass.getKey(), nameClass.getValue()));
                        Platform.runLater(() -> setUnrealPackage(up));

                        getEnvironment().markInvalid(up.getPackageName());
                    }
                }, e -> {
                    log.log(Level.SEVERE, e, () -> "Couldn't add import entry");

                    showException("Couldn't add import entry", e);
                }));
    }

    public void addExport() {
        if (!isPackageSelected())
            return;

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
        dialog.getDialogPane().getButtonTypes().addAll(objType, classType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return new String[]{dialogButton.getText(), name.getText(), clazz.getText()};
            }
            return null;
        });

        dialog.showAndWait()
                .ifPresent(nameClass -> execute(() -> {
                    try (UnrealPackage up = new UnrealPackage(getUnrealPackage().getFile().openNewSession(false))) {
                        String objName = nameClass[1];
                        int flags = UnrealPackage.DEFAULT_OBJECT_FLAGS;
                        String objClass = nameClass[2];
                        switch (nameClass[0]) {
                            case "Class": {
                                Util.createClass(getSerializerFactory(), up, objName, objClass, flags, Collections.emptyList());
                                break;
                            }
                            case "Object":
                            default: {
                                Util.createObject(getSerializerFactory(), up, objName, objClass, flags, hasStack.isSelected(), Collections.emptyList());
                                break;
                            }
                        }
                        Platform.runLater(() -> setUnrealPackage(up));

                        getEnvironment().markInvalid(up.getPackageName());
                    }
                }, e -> {
                    log.log(Level.SEVERE, e, () -> "Couldn't add export entry");

                    showException("Couldn't add export entry", e);
                }));
    }

    public void save() {
        if (!isEntrySelected())
            return;

        UnrealPackage.ExportEntry selected = getSelectedItem(entrySelector);

        if (selected == null)
            return;

        execute(() -> {
            try (UnrealPackage up = new UnrealPackage(getUnrealPackage().getFile().openNewSession(false))) {
                UnrealPackage.ExportEntry entry = up.getExportTable().get(selected.getIndex());
                Object object = getSerializerFactory().getOrCreateObject(entry);
                if (!SAVE_DEFAULTS)
                    PropertiesUtil.removeDefaults(object.properties, entry.getObjectClass() == null ? entry.getObjectSuperClass().getObjectFullName() : entry.getFullClassName(), getSerializerFactory(), selected.getUnrealPackage());
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
                setUnrealPackage(up);
                entrySelector.getSelectionModel().select(entry);
            }
        }, e -> {
            log.log(Level.SEVERE, e, () -> "Couldn't save entry");

            showException("Couldn't save entry", e);
        });
    }

    private static boolean canCopy(Object object) {
        return object != null &&
                ((object.getClass() == Object.class && (object.unreadBytes == null || object.unreadBytes.length == 0)) ||
                        (object instanceof Class && ((Class) object).child == null));
    }

    public void copy() {
        if (!isEntrySelected())
            return;

        UnrealPackage.ExportEntry selected = getSelectedItem(entrySelector);

        if (selected == null)
            return;

        Object object = getObject();
        if (!canCopy(object))
            return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Copy export entry");
        dialog.setHeaderText(null);
        dialog.setContentText("New name:");
        dialog.showAndWait().ifPresent(name -> execute(() -> {
            try (UnrealPackage up = new UnrealPackage(getUnrealPackage().getFile().openNewSession(false))) {
                if (object.getClass() == Object.class) {
                    up.addExportEntry(name,
                            Optional.ofNullable(selected.getObjectClass()).map(UnrealPackage.Entry::getObjectFullName).orElse(null),
                            Optional.ofNullable(selected.getObjectSuperClass()).map(UnrealPackage.Entry::getObjectFullName).orElse(null),
                            selected.getObjectRawDataExternally(),
                            selected.getObjectFlags());

                } else if (object instanceof Class) {
                    List<L2Property> properties = new ArrayList<>(object.properties);
                    PropertiesUtil.removeDefaults(properties, selected.getObjectSuperClass().getObjectFullName(), getSerializerFactory(), selected.getUnrealPackage());
                    Util.createClass(getSerializerFactory(), up, name, selected.getObjectSuperClass().getObjectFullName(), selected.getObjectFlags(), properties);
                }

                getEnvironment().markInvalid(up.getPackageName());
            }

            Platform.runLater(() -> {
                int selectedPackage = packageSelector.getSelectionModel().getSelectedIndex();
                packageSelector.getSelectionModel().clearSelection();
                packageSelector.getSelectionModel().select(selectedPackage);
            });
        }, e -> {
            log.log(Level.SEVERE, e, () -> "Couldn't copy entry");

            showException("Couldn't copy entry", e);
        }));
    }

    public void exportProperties() {
        if (!isEntrySelected())
            return;

        if (getObject() == null)
            return;

        execute(() -> {
            CharSequence text = Decompiler.decompileProperties(getObject(), getSerializerFactory(), 0);
            if (text.length() != 0) {
                Platform.runLater(() -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save properties");
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Text files", "*.txt"),
                            new FileChooser.ExtensionFilter("All files", "*.*"));
                    fileChooser.setInitialFileName(getEntry().getObjectInnerFullName());

                    File selected = fileChooser.showSaveDialog(application.getStage());
                    if (selected == null)
                        return;

                    execute(() -> {
                        try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(selected)), "UTF-8")) {
                            writer.write(text.toString());
                        }
                    }, e -> {
                        log.log(Level.SEVERE, e, () -> "Couldn't save properties text");

                        showException("Couldn't save properties text", e);
                    });
                });
            }
        }, e -> {
            log.log(Level.SEVERE, e, () -> "Couldn't generate properties text");

            showException("Couldn't generate properties text", e);
        });
    }

    public void about() {
        Dialog dialog = new Dialog();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("About");

        Label name = new Label("L2pe");
        Label version = new Label("Version: " + application.getApplicationVersion());
        Label jre = new Label("JRE: " + System.getProperty("java.version"));
        Label jvm = new Label("JVM: " + System.getProperty("java.vm.name") + " by " + System.getProperty("java.vendor"));
        Hyperlink link = new Hyperlink("GitHub");
        link.setOnAction(event -> application.getHostServices().showDocument("https://github.com/acmi/L2pe"));

        VBox content = new VBox(name, version, jre, jvm, link);
        VBox.setMargin(jre, new Insets(10, 0, 0, 0));
        VBox.setMargin(link, new Insets(10, 0, 0, 0));

        DialogPane pane = new DialogPane();
        pane.setContent(content);
        pane.getButtonTypes().addAll(ButtonType.OK);
        dialog.setDialogPane(pane);

        dialog.showAndWait();
    }

    public void exit() {
        Platform.exit();
    }

    private void showException(String text, Throwable ex) {
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
                //noinspection ThrowableResultOfMethodCallIgnored
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
}
