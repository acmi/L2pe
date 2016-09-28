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
package acmi.l2.clientmod.l2pe.view;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public class SMView implements Initializable {
    @FXML
    private Pane root;
    @FXML
    private SubScene view3dScene;
    @FXML
    private View3D view3dController;
    @FXML
    private GridPane properties;
    @FXML
    private Label vertex;
    @FXML
    private Label triangles;
    @FXML
    private Label sections;
    @FXML
    private Label materials;

    public void setStaticmesh(File packageFile, String obj) {
        view3dController.setStaticmesh(packageFile, obj);
    }
    
    public void setTexture(File packageFile, String obj) {
        view3dController.setTexture(packageFile, obj);
    }
    
    public void setColorModifier(File packageFile, String objName) {
    	view3dController.setColorModifier(packageFile, objName);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        view3dScene.widthProperty().bind(root.widthProperty());
        view3dScene.heightProperty().bind(root.heightProperty());
        view3dScene.setCamera(view3dController.getCamera());

        properties.setVisible(true);
        vertex.textProperty().bind(view3dController.pointsProperty().asString());
        triangles.textProperty().bind(view3dController.trianglesProperty().asString());
        sections.textProperty().bind(Bindings.createStringBinding(() -> view3dController.getMaterials() == null ? "0" : String.valueOf(view3dController.getMaterials().size()), view3dController.materialsProperty()));
        materials.textProperty().bind(Bindings.createStringBinding(() -> view3dController.getMaterials() == null ? "" : view3dController.getMaterials().stream().filter(Objects::nonNull).distinct().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining("\n")), view3dController.materialsProperty()));
    }

    public void onMousePressed(MouseEvent me) {
        view3dController.onMousePressed(me);
    }

    public void onMouseDragged(MouseEvent me) {
        view3dController.onMouseDragged(me);
    }
    
//    public void onMouseWheel(ZoomEvent ze) {
//    	view3dController.onMouseWheel(ze);
//    }
}
