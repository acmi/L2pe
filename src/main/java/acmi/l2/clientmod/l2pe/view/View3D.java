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

import static acmi.l2.clientmod.io.BufferUtil.getCompactInt;
import static acmi.l2.clientmod.l2pe.view.helpers.Util.find;
import static acmi.l2.clientmod.l2pe.view.helpers.Util.iterateProperties;
import static acmi.l2.clientmod.l2pe.view.helpers.Util.nameFilter;
import static javafx.scene.shape.VertexFormat.POINT_NORMAL_TEXCOORD;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.l2pe.Controllers;
import acmi.l2.clientmod.l2pe.view.helpers.ImageUtil;
import acmi.l2.clientmod.l2pe.view.model.StaticMesh;
import acmi.l2.clientmod.unreal.properties.L2Property;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Pair;

public class View3D implements Initializable {
    private static final double CAMERA_INITIAL_X_ANGLE = 135;
    private static final double CAMERA_INITIAL_Z_ANGLE = -45;
    private static final double ROTATION_SPEED = 0.2;
    private static final double ZOOM_SPEED = 0.0025;

    @FXML
    private Xform staticmeshGroup;
    @FXML
    private PerspectiveCamera camera;
    @FXML
    private Xform cameraXform;
    @FXML
    private Xform cameraXform2;
    @FXML
    private Xform cameraXform3;

    private IntegerProperty points = new SimpleIntegerProperty(this, "points");
    private IntegerProperty triangles = new SimpleIntegerProperty(this, "triangles");
    private ListProperty<String> materials = new SimpleListProperty<>(this, "materials");

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;

    private double zoomSpeed = 1.0;

    public PerspectiveCamera getCamera() {
        return camera;
    }

    public int getPoints() {
        return points.get();
    }

    public ReadOnlyIntegerProperty pointsProperty() {
        return points;
    }

    public int getTriangles() {
        return triangles.get();
    }

    public ReadOnlyIntegerProperty trianglesProperty() {
        return triangles;
    }

    public ObservableList<String> getMaterials() {
        return materials.get();
    }

    public ListProperty<String> materialsProperty() {
        return materials;
    }
    
    public void setColorModifier(File packageFile, String obj) {
    	try(UnrealPackage up = new UnrealPackage(packageFile, true)) {
			up.getExportTable().parallelStream()
				.filter(e -> e.getObjectInnerFullName().equalsIgnoreCase(obj) || e.getObjectFullName().equalsIgnoreCase(obj))
				.filter(e -> e.getFullClassName().equalsIgnoreCase("Engine.ColorModifier"))
				.findAny()
				.ifPresent(export -> {
					staticmeshGroup.getChildren().clear();
					
					acmi.l2.clientmod.unreal.core.Object colorModifier = Controllers.getMainWndController().getSerializerFactory().getOrCreateObject(export);
					for(L2Property prop : colorModifier.properties) {
						if(!prop.getName().equals("Color")) {
							continue;
						}
							
						@SuppressWarnings("unchecked")
						final List<L2Property> colorStruct = (List<L2Property>) prop.getAt(0); //color struct
						Function<String, Integer> func = name -> (Integer) colorStruct.stream()
							.filter(e -> true)
							.findAny()
							.get()
							.getAt(0);
						int a = func.apply("A");
						int r = func.apply("R");
						int g = func.apply("G");
						int b = func.apply("B");
						Color fxColor = Color.rgb(r, g, b, a / 255.);
						
						final Box box = new Box(128, 128, 1);
						final PhongMaterial material = new PhongMaterial();
						material.setDiffuseColor(fxColor);
						box.setMaterial(material);
							
						points.set(8);
						triangles.set(2*6);
						materials.set(FXCollections.observableArrayList("Color"));
						camera.setTranslateZ(-128 * 2);
						zoomSpeed = 128 * ZOOM_SPEED;
							
						staticmeshGroup.getChildren().add(box);
					}
				}
			);
		}
    }
    
    public void setTexture(File packageFile, String obj) {
    	try (UnrealPackage up = new UnrealPackage(packageFile, true)) {
    		up.getExportTable().parallelStream()
				.filter(e -> e.getObjectInnerFullName().equalsIgnoreCase(obj) || e.getObjectFullName().equalsIgnoreCase(obj))
				.filter(e -> e.getFullClassName().equalsIgnoreCase("Engine.Texture"))
				.findAny()
				.ifPresent(entry -> {
					staticmeshGroup.getChildren().clear();
					
					BufferedImage image = ImageUtil.getImage(entry.getObjectRawData(), up);
					if(image == null) {
						return;
					}
					
					final Box box = new Box(image.getWidth(), image.getHeight(), 1);
					final PhongMaterial material = new PhongMaterial();
					image = ImageUtil.removeAlpha(image);
					material.setDiffuseMap(SwingFXUtils.toFXImage(image, null));
					box.setMaterial(material);
					
					points.set(8);
					triangles.set(2*6);
					materials.set(FXCollections.observableArrayList(entry.getObjectFullName()));
					camera.setTranslateZ(-Math.max(image.getWidth(), image.getHeight()) * 2);
					zoomSpeed = Math.max(image.getWidth(), image.getHeight()) * ZOOM_SPEED;
					
					staticmeshGroup.getChildren().add(box);
				}
			);
    	}
    }

    public void setStaticmesh(File packageFile, String obj) {
		try (UnrealPackage up = new UnrealPackage(packageFile, true)) {
			up.getExportTable().parallelStream()
				.filter(e -> e.getObjectInnerFullName().equalsIgnoreCase(obj) || e.getObjectFullName().equalsIgnoreCase(obj))
				.filter(e -> e.getFullClassName().equalsIgnoreCase("Engine.StaticMesh"))
				.findAny()
				.ifPresent(entry -> {
					staticmeshGroup.getChildren().clear();
					
					StaticMesh staticMesh = StaticMesh.readStaticMesh(entry);
	
					points.set(staticMesh.vertexStream.vert.length);
					triangles.set(staticMesh.indexStream1.indices.length / 3);
					materials.set(staticMesh.materials.stream().map(pair -> pair == null ? 
							null : 
							String.format("%s'%s'", pair.getValue(), pair.getKey())).collect(Collectors.toCollection(FXCollections::observableArrayList)));
	
					camera.setTranslateZ(-staticMesh.boundingSphere.r * 2);
					zoomSpeed = staticMesh.boundingSphere.r * ZOOM_SPEED;
	
					ObservableFloatArray points = FXCollections.observableFloatArray();
					ObservableFloatArray normals = FXCollections.observableFloatArray();
					ObservableFloatArray texCoords = FXCollections.observableFloatArray();
					for (int vertexIndex = 0; vertexIndex < staticMesh.vertexStream.vert.length; vertexIndex++) {
						StaticMesh.StaticMeshVertex vertex = staticMesh.vertexStream.vert[vertexIndex];
						StaticMesh.MeshUVFloat uv = staticMesh.UVStream[0].data[vertexIndex];
	
						points.addAll(vertex.pos.x, vertex.pos.y, vertex.pos.z);
						normals.addAll(vertex.normal.x, vertex.normal.y, vertex.normal.z);
						texCoords.addAll(uv.u, uv.v);
					}
	
					List<Node> sections = new ArrayList<>(staticMesh.sections.length);
					for (int j = 0; j < staticMesh.sections.length; j++) {
						StaticMesh.StaticMeshSection staticMeshSection = staticMesh.sections[j];
						MeshView meshView = new MeshView();
	
						TriangleMesh mesh = new TriangleMesh(POINT_NORMAL_TEXCOORD);
						mesh.getPoints().setAll(points);
						mesh.getNormals().setAll(normals);
						mesh.getTexCoords().setAll(texCoords);
	
						mesh.getFaces().addAll(
								IntStream.range(staticMeshSection.firstIndex, staticMeshSection.firstIndex + staticMeshSection.numFaces * 3)
									.map(i -> staticMesh.indexStream1.indices[i])
									.flatMap(i -> IntStream.of(i, i, i))
									.toArray()
						);
	
						meshView.setCullFace(CullFace.FRONT);
						meshView.setMesh(mesh);
	
						PhongMaterial material = new PhongMaterial();
						Pair<String, String> materialRef = staticMesh.materials.get(j);
						BufferedImage image = resolveMaterial(packageFile.getParentFile().getParentFile(), materialRef); //client root folder
						if (image != null) {
							image = ImageUtil.removeAlpha(image);
							material.setDiffuseMap(SwingFXUtils.toFXImage(image, null));
						}
						meshView.setMaterial(material);
	
						sections.add(meshView);
					}
	
					staticmeshGroup.getChildren().addAll(sections);
				}
			);
		}
    }

    private static final String[] TEXTURE_FOLDER_NAMES = new String[]{"textures", "systextures"};

    private static BufferedImage resolveMaterial(File gameFolder, Pair<String, String> mat) {
        if (mat == null)
            return null;

        for (String folderName : TEXTURE_FOLDER_NAMES) {
            File folder = find(gameFolder, File::isDirectory, nameFilter(folderName));
            if (folder == null)
                continue;

            File pack = find(folder, nameFilter(mat.getKey().substring(0, mat.getKey().indexOf('.')) + ".utx"), File::isFile);
            if (pack == null)
                continue;

            try (UnrealPackage up = new UnrealPackage(pack, true)) {
                UnrealPackage.ExportEntry entry = up.getExportTable()
                        .stream()
                        .filter(e -> e.getObjectFullName().equalsIgnoreCase(mat.getKey()))
                        .filter(e -> e.getFullClassName().equalsIgnoreCase(mat.getValue()))
                        .findAny()
                        .orElse(null);
                if (entry != null) {
                    if (entry.getFullClassName().equalsIgnoreCase("Engine.Texture")) {
                        return ImageUtil.getImage(entry.getObjectRawData(), up);
                    } else if (entry.getFullClassName().equalsIgnoreCase("Engine.Combiner")) {
                        return resolveMaterial(gameFolder, prop(entry, "Material1"));
                    } else if (entry.getFullClassName().equalsIgnoreCase("Engine.Shader")) {
                        return resolveMaterial(gameFolder, prop(entry, "Diffuse"));
                    } else {
                        return resolveMaterial(gameFolder, prop(entry, "Material"));
                    }
                }
            }
        }

        return null;
    }

    private static Pair<String, String> prop(UnrealPackage.ExportEntry entry, String propName) {
        UnrealPackage up = entry.getUnrealPackage();
        ByteBuffer buffer = ByteBuffer.wrap(entry.getObjectRawData());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        AtomicReference<Pair<String, String>> reference = new AtomicReference<>();
        iterateProperties(buffer, up, (name, offset, data) -> {
            if (name.equalsIgnoreCase(propName)) {
                UnrealPackage.Entry material = up.objectReference(getCompactInt(data));
                if (material != null)
                    reference.set(new Pair<>(material.getObjectFullName(), material.getFullClassName()));
            }
        });
        return reference.get();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cameraXform3.setRotateZ(180.0);

        cameraXform.rz.setAngle(CAMERA_INITIAL_Z_ANGLE);
        cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
    }

	public void onMousePressed(MouseEvent me) {
		mousePosX = me.getSceneX();
		mousePosY = me.getSceneY();
		mouseOldX = me.getSceneX();
		mouseOldY = me.getSceneY();
	}

	public void onMouseDragged(MouseEvent me) {
		mouseOldX = mousePosX;
		mouseOldY = mousePosY;
		mousePosX = me.getSceneX();
		mousePosY = me.getSceneY();
		double mouseDeltaX = (mousePosX - mouseOldX);
		double mouseDeltaY = (mousePosY - mouseOldY);

		if (me.isPrimaryButtonDown()) {
			cameraXform.rz.setAngle(cameraXform.rz.getAngle() - mouseDeltaX * ROTATION_SPEED);
			cameraXform.rx.setAngle(cameraXform.rx.getAngle() + mouseDeltaY * ROTATION_SPEED);
		} else if (me.isSecondaryButtonDown()) {
			double z = camera.getTranslateZ();
			double newZ = z - mouseDeltaY * zoomSpeed;
			camera.setTranslateZ(newZ);
		} else if (me.isMiddleButtonDown()) {
			cameraXform2.t.setX(cameraXform2.t.getX() + mouseDeltaX * zoomSpeed);
			cameraXform2.t.setY(cameraXform2.t.getY() + mouseDeltaY * zoomSpeed);
		}
	}

//    public void onMouseWheel(ZoomEvent ze) {
//    	final double zoomFactor = ze.getZoomFactor();
//    	final double z = camera.getTranslateZ();
//    	
//		double newZ = z - zoomFactor * zoomSpeed;
//		camera.setTranslateZ(newZ);
//    }
}
