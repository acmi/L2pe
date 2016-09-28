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

import static acmi.l2.clientmod.l2pe.Util.createBackup;
import static acmi.l2.clientmod.l2pe.Util.showException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import acmi.l2.clientmod.io.UnrealPackage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

/**
 * @author PointerRage
 *
 */
public class ImportWndController implements Initializable {
	private final static Logger log = Logger.getLogger(ImportWndController.class.getName());
	private ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "L2pe ImportWndExecutor") {
		{
			setDaemon(true);
		}
	});

	@FXML private Button addImport;
	@FXML private Button editImport;
	@FXML private Button deleteImport;
	@FXML private Button sortImports;
	
	@FXML private ListView<UnrealPackage.ImportEntry> imports;
	@FXML private ProgressIndicator loading;
	
	private boolean isNeedSort = false;

	public ImportWndController() {
	}
	
	public void setVisibleLoading(boolean value) {
		loading.setVisible(value);
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		imports.setCellFactory(list -> new ListCell<UnrealPackage.ImportEntry>() {
			@Override
			protected void updateItem(UnrealPackage.ImportEntry e, boolean bln) {
				super.updateItem(e, bln);
				if(e == null) {
					return;
				}
				
				setText(e.getObjectFullName() + " [" + e.getFullClassName() + "]");
			}
		});
		imports.setOnMouseClicked(this::importAction);
		
		update(null);
	}

	public synchronized void update(UnrealPackage _up) {
		final MainWndController mainWnd = Controllers.getMainWndController();
		
		final UnrealPackage up;
		if(_up == null) {
			if (!mainWnd.isPackageSelected()) {
				return;
			}
			
			up = mainWnd.getUnrealPackage();
		} else {
			up = _up;
		}
		
		executor.execute(() -> {
			loading.setVisible(true);
			try {
				imports.getSelectionModel().clearSelection();
				Platform.runLater(() -> imports.getItems().setAll(up.getImportTable()));
				sortIfNeeded();
			} finally {
				loading.setVisible(false);
			}
		});
	}
	
	private void importAction(MouseEvent ev) {
		if(ev.getClickCount() < 2) {
			return;
		}
		
		final MainWndController mainWnd = Controllers.getMainWndController();
		if (!mainWnd.isPackageSelected()) {
			return;
		}
		
		final UnrealPackage.ImportEntry selected = imports.getSelectionModel().getSelectedItem();
		if(selected == null) {
			return;
		}
		
		if(selected.getFullClassName().equals("Engine.Texture")) {
			Controllers.showTexture(selected);
		} else if(selected.getFullClassName().equals("Engine.StaticMesh")) {
			Controllers.showMesh(selected);
		} else if(selected.getFullClassName().equals("Engine.ColorModifier")) {
			File packageFile;
			try {
				packageFile = Util.getPackageFile(selected);
			} catch (IOException e) {
				log.log(Level.SEVERE, "Failed to view mesh", e);
				showException("Failed to view mesh", e);
				return;
			}
			
			Controllers.showMeshView();
			Controllers.getViewWndController().setColorModifier(packageFile, selected.getObjectFullName());
		}
	}

	public void addImport() {
		final MainWndController mainWnd = Controllers.getMainWndController();

		if (!mainWnd.isPackageSelected())
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

		dialog.showAndWait().ifPresent(nameClass -> executor.execute(() -> {
			Controllers.setLoading(true);

			createBackup();
			try {
				try (UnrealPackage up = new UnrealPackage(mainWnd.getUnrealPackage().getFile().openNewSession(false))) {
					up.addImportEntries(Collections.singletonMap(nameClass.getKey(), nameClass.getValue()));
					Platform.runLater(() -> mainWnd.setUnrealPackage(up));
					mainWnd.getEnvironment().markInvalid(up.getPackageName());
					Platform.runLater(() -> update(up));
				}

				mainWnd.reselectEntry();
			} catch (Exception e) {
				log.log(Level.SEVERE, e, () -> "Couldn't add import entry");
				showException("Couldn't add import entry", e);
			} finally {
				Controllers.setLoading(false);
			}
		}));
	}
	
	public void editImport() {
		final MainWndController mainWnd = Controllers.getMainWndController();

		if (!mainWnd.isPackageSelected()) {
			return;
		}
		
		final UnrealPackage.ImportEntry selected = imports.getSelectionModel().getSelectedItem();
		if(selected == null) {
			return;
		}

		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle("Create import entry");
		dialog.setHeaderText(null);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		TextField textName = new TextField();
		textName.setPromptText("Package.Name");
		textName.setText(selected.getObjectFullName());
		
		grid.add(new Label("Name:"), 0, 0);
		grid.add(textName, 1, 0);

		dialog.getDialogPane().setContent(grid);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == ButtonType.OK) {
				return textName.getText();
			}
			return null;
		});

		dialog.showAndWait().ifPresent(name -> executor.execute(() -> {
			Controllers.setLoading(true);

			createBackup();
			try {
				try (UnrealPackage up = new UnrealPackage(mainWnd.getUnrealPackage().getFile().openNewSession(false))) {
					final int index = up.getImportTable().indexOf(selected);
					if(index < 0) {
						throw new Exception("Negative index!");
					}
					up.renameImport(index, name);
					Platform.runLater(() -> mainWnd.setUnrealPackage(up));
					mainWnd.getEnvironment().markInvalid(up.getPackageName());
					Platform.runLater(() -> update(up));
				}

				mainWnd.reselectEntry();
			} catch (Exception e) {
				log.log(Level.SEVERE, e, () -> "Couldn't add import entry");
				showException("Couldn't add import entry", e);
			} finally {
				Controllers.setLoading(false);
			}
		}));
	}

	public void deleteImport() {
		final MainWndController mainWnd = Controllers.getMainWndController();
		
		final UnrealPackage.ImportEntry selected = imports.getSelectionModel().getSelectedItem();
		if(selected == null) {
			return;
		}

		executor.execute(() -> {
			Controllers.setLoading(true);

			createBackup();

			try {
				final File selectedFile = Controllers.getMainWndController().getSelectedPackage();
				if (selectedFile == null) {
					return;
				}

				try (UnrealPackage up = new UnrealPackage(selectedFile, false)) {
					up.updateImportTable(table -> table.remove(selected));
					Platform.runLater(() -> mainWnd.setUnrealPackage(up));
					mainWnd.getEnvironment().markInvalid(up.getPackageName());
					Platform.runLater(() -> update(up));
				} catch (Exception e) {
					log.log(Level.SEVERE, "Failed to delete selected import entry", e);
					showException("Failed to delete selected import entry", e);
				}
				
				mainWnd.reselectEntry();
			} finally {
				Controllers.setLoading(false);
			}
		});
	}
	
	public void sortImports() {
		isNeedSort = !isNeedSort;
		sortIfNeeded();
		if(!isNeedSort) {
			Platform.runLater(() -> update(null));
		}
	}
	
	private void sortIfNeeded() {
		if(!isNeedSort) {
			return;
		}
		
		Util.sort(imports);
	}
}
