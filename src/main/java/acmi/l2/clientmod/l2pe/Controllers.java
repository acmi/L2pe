package acmi.l2.clientmod.l2pe;

import static acmi.l2.clientmod.l2pe.Util.showException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.l2pe.view.SMView;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author PointerRage
 *
 */
public class Controllers {
	private final static Logger log = Logger.getLogger(Controllers.class.getName());
	
	private static MainWndController mainWndController;
	
	private static Stage importWndStage;
	private static ImportWndController importWndController;
	
	private static Stage nameWndStage;
	private static NameWndController nameWndController;
	
	private static Stage viewWndStage;
	private static SMView viewWndController;
	
	private Controllers() {
		throw new RuntimeException();
	}
	
	public static MainWndController getMainWndController() {
		return mainWndController;
	}
	
	public static void setMainWndController(MainWndController _mainWndController) {
		mainWndController = _mainWndController;
	}
	
	public static Stage getImportWndStage() {
		return importWndStage;
	}
	
	public static void setImportWndStage(Stage _importWndStage) {
		importWndStage = _importWndStage;
	}
	
	public static ImportWndController getImportWndController() {
		return importWndController;
	}
	
	public static void setImportWndController(ImportWndController _importWndController) {
		importWndController = _importWndController;
	}
	
	public static NameWndController getNameWndController() {
		return nameWndController;
	}
	
	public static void setNameWndController(NameWndController _nameWndController) {
		nameWndController = _nameWndController;
	}
	
	public static Stage getNameWndStage() {
		return nameWndStage;
	}
	
	public static void setNameWndStage(Stage _nameWndStage) {
		nameWndStage = _nameWndStage;
	}
	
	public static SMView getViewWndController() {
		return viewWndController;
	}
	
	public static void setViewWndController(SMView _viewWndController) {
		viewWndController = _viewWndController;
	}
	
	public static Stage getViewWndStage() {
		return viewWndStage;
	}
	
	public static void setViewWndStage(Stage _viewWndStage) {
		viewWndStage = _viewWndStage;
	}
	
	public static void setLoading(boolean value) {
		mainWndController.setVisibleLoading(value);
		if(importWndController != null) {
			importWndController.setVisibleLoading(value);
		}
		
		if(nameWndController != null) {
			nameWndController.setVisibleLoading(value);
		}
	}
	
	public static void die() {
		Platform.exit();
	}
	
	public static void showImportWnd() {
		Stage stage = getImportWndStage();
		if (stage != null) {
			if (!stage.isShowing()) {
				stage.show();
			}
			
			stage.requestFocus();
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(Controllers.class.getResource("importwnd.fxml"), null, new JavaFXBuilderFactory(), null, Charset.forName(FXMLLoader.DEFAULT_CHARSET_NAME));
			setImportWndStage(stage = new Stage());
			stage.setScene(new Scene(loader.load()));
			setImportWndController(loader.getController());
			stage.setTitle("L2PE Import Editor");
			stage.show();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to show importwnd", e);
			showException("Failed to show importwnd", e);
		}
	}
	
	public static void showNameWnd() {
		Stage stage = getNameWndStage();
		if (stage != null) {
			if (!stage.isShowing()) {
				stage.show();
			}
			
			stage.requestFocus();
			return;
		}

		try {
			FXMLLoader loader = new FXMLLoader(Controllers.class.getResource("namewnd.fxml"), null, new JavaFXBuilderFactory(), null, Charset.forName(FXMLLoader.DEFAULT_CHARSET_NAME));
			setNameWndStage(stage = new Stage());
			stage.setScene(new Scene(loader.load()));
			setNameWndController(loader.getController());
			stage.setTitle("L2PE Name Editor");
			stage.show();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to show namewnd", e);
			showException("Failed to show namewnd", e);
		}
	}
	
	public static void showMeshView() {
		Stage stage = getViewWndStage();
		
		if (stage != null) {
			if(!stage.isShowing()) {
				stage.show();
			}
			
			stage.requestFocus();
			return;
		}

		try {
			final FXMLLoader loader = new FXMLLoader(Controllers.class.getResource("view/smview.fxml"), null, new JavaFXBuilderFactory(), null, Charset.forName(FXMLLoader.DEFAULT_CHARSET_NAME));
			final Scene scene = new Scene(loader.load());
			setViewWndController(loader.getController());
			setViewWndStage(stage = new Stage());
			stage.setScene(scene);
			stage.setTitle("L2PE View");
			stage.show();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to view mesh", e);
			showException("Failed to view mesh", e);
		}
	}
	
	public static void showMesh(UnrealPackage.Entry<?> mesh) {
		showMeshView();
		
		final File packageFile;
		try {
			packageFile = Util.getPackageFile(mesh);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to view mesh", e);
			showException("Failed to view mesh", e);
			return;
		}

		final SMView controller = getViewWndController();
		try {
			controller.setStaticmesh(packageFile, mesh.getObjectFullName());
		} catch (UncheckedIOException e) {
			showException("Couldnt show mesh", e);
			return;
		}
	}
	
	public static void showTexture(UnrealPackage.Entry<?> tex) {
		showMeshView();
		
		final File packageFile;
		try {
			packageFile = Util.getPackageFile(tex);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to view mesh", e);
			showException("Package not found", e);
			return;
		}
		
		final SMView controller = getViewWndController();
		try {
			controller.setTexture(packageFile, tex.getObjectFullName());
		} catch(UncheckedIOException e) {
			showException("Couldnt show viewer", e);
			return;
		}
	}
}
