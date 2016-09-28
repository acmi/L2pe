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

import static acmi.l2.clientmod.l2pe.Util.SAVE_DEFAULTS;
import static acmi.l2.clientmod.l2pe.Util.selectItem;
import static acmi.l2.clientmod.l2pe.Util.showException;
import static acmi.l2.clientmod.unreal.UnrealSerializerFactory.IS_STRUCT;
import static acmi.util.AutoCompleteComboBox.getSelectedItem;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import acmi.l2.clientmod.io.ObjectOutput;
import acmi.l2.clientmod.io.ObjectOutputStream;
import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.properties.control.PropertiesEditor;
import acmi.l2.clientmod.properties.control.skin.edit.ObjectEdit;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

public class MainWndController extends ControllerBase implements Initializable {
    private static final Logger log = Logger.getLogger(MainWndController.class.getName());

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
    private Button showNameWnd;
    @FXML
    private Button showImportWnd;
    @FXML
    private Button addExport;
    @FXML
    private Button save;
    @FXML
    private Button copy;
    @FXML 
    private Button delete;
	@FXML 
	private Button update;
    @FXML
    private CheckMenuItem showAllProperties;
    @FXML 
    private CheckMenuItem makeBackups;
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
    
    public boolean isBackupEnable() {
		return makeBackups.isSelected();
	}
    
    public void reselectEntry() {
		final UnrealPackage.ExportEntry selected = getEntry();
		if (selected != null) {
			Platform.runLater(() -> selectItem(entrySelector, selected));
		}
	}
    
    public void setVisibleLoading(boolean isLoading) {
		loading.setVisible(isLoading);
	}
    
    public File getSelectedPackage() {
		return packageSelector.getValue();
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
    	Controllers.setMainWndController(this);
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
                    
                    final ImportWndController iwc = Controllers.getImportWndController();
					if(iwc != null) {
						iwc.update(up);
					}
					
					final NameWndController nwc = Controllers.getNameWndController();
					if(nwc != null) {
						nwc.update(up);
					}
                }
            }, e -> {
                log.log(Level.SEVERE, e, () -> "Couldn't load: " + newValue);

                showException("Couldn't load: " + newValue, e);
            });
        });

        packageMenu.disableProperty().bind(packageSelected().not());
        entrySeparator.visibleProperty().bind(packageSelected());
        showNameWnd.visibleProperty().bind(packageSelected());
		showImportWnd.visibleProperty().bind(packageSelected());
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
        delete.visibleProperty().bind(entrySelected());
		update.visibleProperty().bind(entrySelected());
        
        makeBackups.setSelected(true);
        
        ObjectEdit.getInstance().addElement(context -> {
        	String type = ((acmi.l2.clientmod.unreal.core.ObjectProperty) context.getTemplate()).type.getFullName();
        	if(!type.equals("Engine.StaticMesh")) {
        		return null;
        	}
    	    
        	Button viewButton = new Button("View");
    	        viewButton.setMinWidth(Region.USE_PREF_SIZE);
    	        viewButton.setOnAction(e -> {
    	        	final ComboBox<UnrealPackage.Entry> cb = (ComboBox<UnrealPackage.Entry>) context.getEditorNode();
    	        	final UnrealPackage.Entry<?> entry = cb.getSelectionModel().getSelectedItem();
    	        	if(entry == null) {
    	        		return;
    	        	}
    	        	
    	        	Controllers.showMesh(entry);
    	        });
    	        return viewButton;
        });
        
        ObjectEdit.getInstance().addElement(context -> {
        	String type = ((acmi.l2.clientmod.unreal.core.ObjectProperty) context.getTemplate()).type.getFullName();
        	if(!type.equals("Engine.Texture")) {
        		return null;
        	}
    	    
        	Button viewButton = new Button("View");
    	        viewButton.setMinWidth(Region.USE_PREF_SIZE);
    	        viewButton.setOnAction(e -> {
    	        	final ComboBox<UnrealPackage.Entry> cb = (ComboBox<UnrealPackage.Entry>) context.getEditorNode();
    	        	final UnrealPackage.Entry<?> entry = cb.getSelectionModel().getSelectedItem();
    	        	if(entry == null) {
    	        		return;
    	        	}
    	        	
    	        	Controllers.showTexture(entry);
    	        });
    	        return viewButton;
        });
        
        loading.setVisible(false);
        
        final Map<String, String> namedParams = L2PE.getInstance().getParameters().getNamed();
		String value = namedParams.get("ini");
		if(value != null) {
			final File file = new File(value);
			if(!file.exists() || !file.isFile()) {
				showException("l2.ini not found", new Exception());
			}
			setIni(file);
		}
    }

    public void selectL2ini() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open l2.ini");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("L2.ini", "L2.ini"), new FileChooser.ExtensionFilter("All files", "*.*"));

		if (getInitialDirectory() != null && getInitialDirectory().exists() && getInitialDirectory().isDirectory())
			fileChooser.setInitialDirectory(getInitialDirectory());
		fileChooser.setInitialFileName("l2.ini");

		File selected = fileChooser.showOpenDialog(application.getStage());
		if (selected == null)
			return;
		
		setIni(selected);
	}
	
	private void setIni(File file) {
		setInitialDirectory(file.getParentFile());

		try {
			setEnvironment(Environment.fromIni(file));
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
                	Util.createBackup();
                	
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
        	Util.createBackup();
        	
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
        	Util.createBackup();
        	
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
    
    public void delete() {
		if (!isEntrySelected())
			return;

		UnrealPackage.ExportEntry selected = getSelectedItem(entrySelector);
		if (selected == null)
			return;

		execute(() -> {
			Util.createBackup();
			
			try (UnrealPackage up = new UnrealPackage(getUnrealPackage().getFile().openNewSession(false))) {
				up.updateExportTable(exportTable -> {
					exportTable.remove(selected);
					up.getFile().setPosition(up.getDataEndOffset().orElseThrow(IllegalStateException::new));
				});

				entrySelector.getSelectionModel().clearSelection();
				Platform.runLater(() -> setUnrealPackage(up));
				getEnvironment().markInvalid(up.getPackageName());
			}
		}, e -> {
			log.log(Level.SEVERE, e, () -> "Couldn't delete entry");
			showException("Couldn't delete entry", e);
		});
	}

	public void update() {
		UnrealPackage.ExportEntry selected = getSelectedItem(entrySelector);
		if (selected == null) {
			return;
		}

		execute(() -> {
			try (UnrealPackage up = new UnrealPackage(getUnrealPackage().getFile().openNewSession(false))) {
				Platform.runLater(() -> setUnrealPackage(up));
				getEnvironment().markInvalid(up.getPackageName());
			}

			Platform.runLater(() -> selectItem(entrySelector, selected));
		}, e -> {
			log.log(Level.SEVERE, e, () -> "Couldn't update entry");
			showException("Couldn't update entry", e);
		});
	}
	
	public void showImportWnd() {
		Controllers.showImportWnd();
	}
	
	public void showNameWnd() {
		Controllers.showNameWnd();
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
        Dialog<?> dialog = new Dialog<>();
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
    	Controllers.die();
    }
}
