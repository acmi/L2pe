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
package acmi.l2.clientmod.l2pe.view.helpers;

import static acmi.l2.clientmod.io.BufferUtil.getCompactInt;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil.Type;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

public class Util {
    public static final FileFilter MAP_FILE_FILTER = pathname ->
            (pathname != null) && (pathname.isFile()) && (pathname.getName().endsWith(".unr"));
    public static final FileFilter STATICMESH_FILE_FILTER = pathname ->
            (pathname != null) && (pathname.isFile()) && (pathname.getName().endsWith(".usx"));

    public static Float getFloat(TextField textField, Float defaultValue) {
        try {
            return Float.valueOf(textField.getText());
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static Integer getInt(TextField textField, Integer defaultValue) {
        try {
            return Integer.valueOf(textField.getText());
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static double range(float[] loc, Double x, Double y, Double z) {
        double s = 0.0;
        if (x != null)
            s += Math.pow(loc[0] - x, 2.0);
        if (y != null)
            s += Math.pow(loc[1] - y, 2.0);
        if (z != null)
            s += Math.pow(loc[2] - z, 2.0);
        return Math.sqrt(s);
    }

    public static Double getDoubleOrClearTextField(TextField textField) {
        try {
            return Double.valueOf(textField.getText());
        } catch (NumberFormatException nfe) {
            if (!textField.getText().equals("-"))
                textField.setText("");
            return null;
        }
    }

    public static void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }

    public static boolean showConfirm(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        return ButtonType.YES == alert.showAndWait().orElse(ButtonType.NO);
    }

    public static void readStateFrame(ByteBuffer buffer) throws BufferUnderflowException {
        getCompactInt(buffer);
        getCompactInt(buffer);
        buffer.getLong();
        buffer.getInt();
        getCompactInt(buffer);
    }

    public static void iterateProperties(ByteBuffer buffer, UnrealPackage up, TriConsumer<String, Integer, ByteBuffer> func) throws BufferUnderflowException {
        String name;
        while (!"None".equals(name = up.getNameTable().get(getCompactInt(buffer)).getName())) {
            byte info = buffer.get();
            Type type = Type.values()[info & 15];
            int size = (info & 112) >> 4;
            boolean array = (info & 128) == 128;
            if (type == Type.STRUCT) {
                getCompactInt(buffer);
            }

            size = StaticMeshActorUtil.getSize(size, buffer);
            if (array && type != Type.BOOL) {
                buffer.get();
            }

            byte[] obj = new byte[size];
            int pos = buffer.position();
            buffer.get(obj);

            func.accept(name, pos, ByteBuffer.wrap(obj).order(ByteOrder.LITTLE_ENDIAN));
        }
    }

    public static int getXY(File mapDir, String mapName) throws IOException {
        int[] m = new int[2];

        try (UnrealPackage up = new UnrealPackage(new File(mapDir, mapName), true)) {
            List<UnrealPackage.ExportEntry> infos = up.getExportTable().stream()
                    .filter(e -> e.getObjectClass().getObjectFullName().equals("Engine.TerrainInfo"))
                    .collect(Collectors.toList());
            for (UnrealPackage.ExportEntry e : infos) {
                byte[] staticMeshActor = e.getObjectRawData();
                ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                readStateFrame(buffer);
                iterateProperties(buffer, up, (name, pos, obj) -> {
                    switch (name) {
                        case "MapX":
                            m[0] = obj.getInt();
                            break;
                        case "MapY":
                            m[1] = obj.getInt();
                            break;
                    }
                });
            }
        }

        return m[0] | (m[1] << 8);
    }

    public static CharSequence tab(int indent) {
        StringBuilder sb = new StringBuilder(indent);
        for (int i = 0; i < indent; i++)
            sb.append('\t');
        return sb;
    }

    public static CharSequence newLine(int indent) {
        StringBuilder sb = new StringBuilder("\r\n");
        sb.append(tab(indent));
        return sb;
    }

    public static CharSequence newLine() {
        return newLine(0);
    }

    public static Throwable getTop(Throwable t) {
        while (t.getCause() != null)
            t = t.getCause();
        return t;
    }

    public static Predicate<File> nameFilter(String name) {
        return f -> f.getName().equalsIgnoreCase(name);
    }

    @SafeVarargs
    public static File find(File folder, Predicate<File>... filters) {
        if (folder == null)
            return null;

        File[] children = folder.listFiles();
        if (children == null)
            return null;

        Stream<File> stream = Arrays.stream(children);
        for (Predicate<File> filter : filters)
            stream = stream.filter(filter);
        return stream
                .findAny()
                .orElse(null);
    }
}
