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

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class L2PE extends Application {
    private static final Logger log = Logger.getLogger(L2PE.class.getName());

    private Stage stage;

    Stage getStage() {
        return stage;
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        String version = readAppVersion();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        stage.setScene(new Scene(loader.load()));

        Controller controller = loader.getController();
        controller.setApplication(this);
        stage.titleProperty().bind(Bindings.createStringBinding(() ->
                (controller.getEnvironment() != null ? controller.getEnvironment().getStartDir().getAbsolutePath() + " - " : "") + "L2PE " + version, controller.environmentProperty()));

        stage.show();
    }

    private String readAppVersion() throws IOException, URISyntaxException {
        try (JarFile jarFile = new JarFile(Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toFile())) {
            Manifest manifest = jarFile.getManifest();
            return manifest.getMainAttributes().getValue("Version");
        } catch (FileNotFoundException ignore) {
        } catch (IOException | URISyntaxException e) {
            log.log(Level.WARNING, "version info load error", e);
        }
        return "";
    }

    public static Preferences getPrefs() {
        return Preferences.userRoot().node("l2pe");
    }

    public static void main(String[] args) {
        try (InputStream is = L2PE.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            System.err.println("Couldn't load logging.properties");
        }

        launch(args);
    }
}
