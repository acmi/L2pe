package acmi.l2.clientmod.l2pe;

import static acmi.l2.clientmod.l2pe.Util.createBackup;
import static acmi.l2.clientmod.l2pe.Util.showException;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import acmi.l2.clientmod.io.UnrealPackage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;

/**
 * @author PointerRage
 *
 */
public class NameWndController implements Initializable {
	private final static Logger log = Logger.getLogger(NameWndController.class.getName());
	private ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "L2pe NameWndExecutor") {
		{
			setDaemon(true);
		}
	});

	@FXML private Button addName;
	@FXML private Button editName;
	@FXML private Button deleteName;
	@FXML private Button sortName;
	
	@FXML private ListView<UnrealPackage.NameEntry> names;
	@FXML private ProgressIndicator loading;
	
	private boolean isNeedSort = false;

	public NameWndController() {
	}
	
	public void setVisibleLoading(boolean value) {
		loading.setVisible(value);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		update(null);
	}
	
	public void update(UnrealPackage _up) {
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
				names.getSelectionModel().clearSelection();
				Platform.runLater(() -> names.getItems().setAll(up.getNameTable()));
				sortIfNeeded();
			} finally {
				loading.setVisible(false);
			}
		});
	}
	
	public void addName() {
		final MainWndController mainWnd = Controllers.getMainWndController();
		if (!mainWnd.isPackageSelected()) {
			return;
		}

		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Create name entry");
		dialog.setHeaderText(null);
		dialog.setContentText("Name string:");
		dialog.showAndWait().ifPresent(name -> executor.execute(() -> {
			Controllers.setLoading(true);
			
			createBackup();

			try {
				try (UnrealPackage up = new UnrealPackage(mainWnd.getUnrealPackage().getFile().openNewSession(false))) {
					up.addNameEntries(name);
					Platform.runLater(() -> mainWnd.setUnrealPackage(up));
					mainWnd.getEnvironment().markInvalid(up.getPackageName());
					Platform.runLater(() -> update(up));
				}
	
				mainWnd.reselectEntry();
			} catch(Exception e) {
				log.log(Level.SEVERE, e, () -> "Couldn't add name entry");
				showException("Couldn't add name entry", e);
			} finally {
				Controllers.setLoading(false);
			}
		}));
	}
	
	public void editName() {
		final MainWndController mainWnd = Controllers.getMainWndController();
		if (!mainWnd.isPackageSelected()) {
			return;
		}
		
		final UnrealPackage.NameEntry selected = names.getSelectionModel().getSelectedItem();
		if(selected == null) {
			return;
		}

		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Create name entry");
		dialog.setHeaderText(null);
		dialog.setContentText("Name string:");
		dialog.showAndWait().ifPresent(name -> executor.execute(() -> {
			Controllers.setLoading(true);
			
			createBackup();

			try {
				try (UnrealPackage up = new UnrealPackage(mainWnd.getUnrealPackage().getFile().openNewSession(false))) {
					up.updateNameEntry(selected.getIndex(), name, selected.getFlags());
					Platform.runLater(() -> mainWnd.setUnrealPackage(up));
					mainWnd.getEnvironment().markInvalid(up.getPackageName());
					Platform.runLater(() -> update(up));
				}
	
				mainWnd.reselectEntry();
			} catch(Exception e) {
				log.log(Level.SEVERE, e, () -> "Couldn't edit name entry");
				showException("Couldn't edit name entry", e);
			} finally {
				Controllers.setLoading(false);
			}
		}));
	}
	
	public void deleteName() {
		final MainWndController mainWnd = Controllers.getMainWndController();
		if (!mainWnd.isPackageSelected())
			return;
		
		final UnrealPackage.NameEntry selected = names.getSelectionModel().getSelectedItem();
		if(selected == null) {
			return;
		}

		executor.execute(() -> {
			Controllers.setLoading(true);
			
			createBackup();

			try {
				try (UnrealPackage up = new UnrealPackage(mainWnd.getUnrealPackage().getFile().openNewSession(false))) {
					up.updateNameTable(table -> table.remove(selected.getIndex()));
					Platform.runLater(() -> mainWnd.setUnrealPackage(up));
					mainWnd.getEnvironment().markInvalid(up.getPackageName());
					Platform.runLater(() -> update(up));
				}
	
				mainWnd.reselectEntry();
			} catch(Exception e) {
				log.log(Level.SEVERE, e, () -> "Couldn't delete name entry");
				showException("Couldn't delete name entry", e);
			} finally {
				Controllers.setLoading(false);
			}
		});
	}
	
	public void sortName() {
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
		
		Util.sort(names);
	}
}
