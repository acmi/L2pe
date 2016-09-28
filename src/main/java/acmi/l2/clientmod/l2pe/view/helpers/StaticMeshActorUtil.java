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
import static acmi.l2.clientmod.io.BufferUtil.putCompactInt;
import static acmi.l2.clientmod.io.ByteUtil.compactIntToByteArray;
import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.HasStack;
import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.LoadForEdit;
import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.LoadForServer;
import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.l2pe.view.model.Offsets;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil.Type;

public class StaticMeshActorUtil {
    public static Offsets getOffsets(byte[] staticMeshActor, UnrealPackage unrealPackage) throws BufferUnderflowException {
        Offsets offsets = new Offsets();
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Util.readStateFrame(buffer);
        Util.iterateProperties(buffer, unrealPackage, (name, offset, obj) -> {
            switch (name) {
                case "StaticMesh":
                    offsets.mesh = offset;
                    offsets.meshSize = obj.limit();
                    break;
                case "Location":
                    offsets.location = offset;
                    break;
                case "Rotation":
                    offsets.rotation = offset;
                    break;
                case "SwayRotationOrig":
                    offsets.swayRotationOrig = offset;
                    break;
                case "ColLocation":
                    offsets.colLocation = offset;
                    break;
                case "BasePos":
                    offsets.basePos = offset;
                    break;
                case "BaseRot":
                    offsets.baseRot = offset;
                    break;
                case "DrawScale":
                    offsets.drawScale = offset;
                    break;
                case "DrawScale3D":
                    offsets.drawScale3D = offset;
                    break;
                case "RotationRate":
                    offsets.rotationRate = offset;
                    break;
                case "ZoneRenderState":
                    offsets.zoneRenderState = offset;
                    offsets.zoneRenderStateCount = getCompactInt(obj);
                    break;
            }
        });
        return offsets;
    }

    public static int getSize(int size, ByteBuffer buffer) throws BufferUnderflowException {
        switch (size) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 12;
            case 4:
                return 16;
            case 5:
                return buffer.get() & 0xFF;
            case 6:
                return buffer.getShort() & 0xFFFF;
            case 7:
                return buffer.getInt();
        }
        throw new RuntimeException("invalid size " + size);
    }

    public static int getByteCount(int v) {
        return Math.abs(v) > 63 ? 2 : 1;
    }

    public static int getStaticMesh(byte[] staticMeshActor, Offsets offsets) {
        if (offsets.mesh == 0) {
            return 0;
        }
        return getCompactInt((ByteBuffer) ByteBuffer.wrap(staticMeshActor).order(ByteOrder.LITTLE_ENDIAN).position(offsets.mesh));
    }

    public static byte[] setStaticMesh(byte[] staticMeshActor, Offsets offsets, int staticMeshIndex) {
        if (offsets.meshSize != getByteCount(staticMeshIndex)) {
            byte[] bytes = new byte[staticMeshActor.length + getByteCount(staticMeshIndex) - offsets.meshSize];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.put(staticMeshActor, 0, offsets.mesh - 1);
            buffer.put((byte) (getByteCount(staticMeshIndex) == 2 ? 21 : 5));
            putCompactInt(buffer, staticMeshIndex);
            buffer.put(staticMeshActor, offsets.mesh + offsets.meshSize, staticMeshActor.length - (offsets.mesh + offsets.meshSize));
            if (offsets.location != 0) {
                offsets.location += getByteCount(staticMeshIndex) - offsets.meshSize;
            }
            if (offsets.rotation != 0) {
                offsets.rotation += getByteCount(staticMeshIndex) - offsets.meshSize;
            }
            if (offsets.swayRotationOrig != 0) {
                offsets.swayRotationOrig += getByteCount(staticMeshIndex) - offsets.meshSize;
            }
            if (offsets.colLocation != 0) {
                offsets.colLocation += getByteCount(staticMeshIndex) - offsets.meshSize;
            }
            offsets.meshSize = getByteCount(staticMeshIndex);
            return bytes;
        }
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
        buffer.position(offsets.mesh);
        putCompactInt(buffer, staticMeshIndex);
        return staticMeshActor;
    }

    public static float[] getLocation(byte[] staticMeshActor, Offsets offsets) {
        if ((offsets.location == 0) && (offsets.colLocation == 0)) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(offsets.location != 0 ? offsets.location : offsets.colLocation);
        return new float[]{buffer.getFloat(), buffer.getFloat(), buffer.getFloat()};
    }

    public static void setLocation(byte[] staticMeshActor, Offsets offsets, float x, float y, float z) {
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (offsets.location != 0) {
            buffer.position(offsets.location);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(z);
        }
        if (offsets.colLocation != 0) {
            buffer.position(offsets.colLocation);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(z);
        }
        if (offsets.basePos != 0) {
            buffer.position(offsets.basePos);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.putFloat(z);
        }
    }

    public static int[] getRotation(byte[] staticMeshActor, Offsets offsets) {
        if ((offsets.rotation == 0) && (offsets.swayRotationOrig == 0)) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(offsets.rotation != 0 ? offsets.rotation : offsets.swayRotationOrig);
        return new int[]{buffer.getInt(), buffer.getInt(), buffer.getInt()};
    }

    public static void setRotation(byte[] staticMeshActor, Offsets offsets, int p, int y, int r) {
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (offsets.rotation != 0) {
            buffer.position(offsets.rotation);
            buffer.putInt(p);
            buffer.putInt(y);
            buffer.putInt(r);
        }
        if (offsets.swayRotationOrig != 0) {
            buffer.position(offsets.swayRotationOrig);
            buffer.putInt(p);
            buffer.putInt(y);
            buffer.putInt(r);
        }
        if (offsets.baseRot != 0) {
            buffer.position(offsets.baseRot);
            buffer.putInt(p);
            buffer.putInt(y);
            buffer.putInt(r);
        }
    }

    public static int[] getRotationRate(byte[] staticMeshActor, Offsets offsets) {
        if (offsets.rotationRate == 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(offsets.rotationRate);
        return new int[]{buffer.getInt(), buffer.getInt(), buffer.getInt()};
    }

    public static void setRotationRate(byte[] staticMeshActor, Offsets offsets, int p, int y, int r) {
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        if (offsets.rotationRate != 0) {
            buffer.position(offsets.rotationRate);
            buffer.putInt(p);
            buffer.putInt(y);
            buffer.putInt(r);
        }
    }

    public static Float getDrawScale(byte[] staticMeshActor, Offsets offsets) {
        if (offsets.drawScale == 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getFloat(offsets.drawScale);
    }

    public static void setDrawScale(byte[] staticMeshActor, Offsets offsets, float scale) {
        if (offsets.drawScale == 0) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(offsets.drawScale, scale);
    }

    public static float[] getDrawScale3D(byte[] staticMeshActor, Offsets offsets) {
        if (offsets.drawScale3D == 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(offsets.drawScale3D);
        return new float[]{buffer.getFloat(), buffer.getFloat(), buffer.getFloat()};
    }

    public static void setDrawScale3D(byte[] staticMeshActor, Offsets offsets, float scaleX, float scaleY, float scaleZ) {
        if (offsets.drawScale3D == 0) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(offsets.drawScale3D);
        buffer.putFloat(scaleX);
        buffer.putFloat(scaleY);
        buffer.putFloat(scaleZ);
    }

    public static int[] getZoneRenderState(byte[] staticMeshActor, Offsets offsets) {
        if (offsets.zoneRenderState == 0)
            return null;

        ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(offsets.zoneRenderState);
        int[] zrs = new int[getCompactInt(buffer)];
        for (int i = 0; i < zrs.length; i++)
            zrs[i] = buffer.getInt();
        return zrs;
    }

    public static byte[] setZoneRenderState(byte[] staticMeshActor, Offsets offsets, int... states) {
        if (offsets.zoneRenderState == 0)
            return staticMeshActor;

        byte[] zrs = new byte[getIntArraySizeByCount(states.length)];
        ByteBuffer tmp = ByteBuffer.wrap(zrs).order(ByteOrder.LITTLE_ENDIAN);
        putCompactInt(tmp, states.length);
        for (int zs : states)
            tmp.putInt(zs);

        if (states.length != offsets.zoneRenderStateCount) {
            byte[] newBytes = new byte[staticMeshActor.length - getIntArraySizeByCount(offsets.zoneRenderStateCount) + zrs.length];
            System.arraycopy(staticMeshActor, 0, newBytes, 0, offsets.zoneRenderState - 1);
            newBytes[offsets.zoneRenderState - 1] = (byte) zrs.length;
            System.arraycopy(zrs, 0, newBytes, offsets.zoneRenderState, zrs.length);
            System.arraycopy(staticMeshActor, offsets.zoneRenderState + getIntArraySizeByCount(offsets.zoneRenderStateCount), newBytes, offsets.zoneRenderState + zrs.length, newBytes.length - (offsets.zoneRenderState + zrs.length));
            return newBytes;
        } else {
            ByteBuffer buffer = ByteBuffer.wrap(staticMeshActor);
            buffer.position(offsets.zoneRenderState);
            buffer.put(zrs);
            return staticMeshActor;
        }
    }

    private static int getIntArraySizeByCount(int count) {
        return count * 4 + (count > 63 ? 2 : 1);
    }

    public static byte[] createActor(UnrealPackage up, String clazz, int staticMeshRef,
                                     boolean rotating, boolean zoneRenderState) {
        ByteBuffer buffer = ByteBuffer.allocate(0x100).order(ByteOrder.LITTLE_ENDIAN);

        byte[] tmp = compactIntToByteArray(up.objectReferenceByName(clazz, c -> true));
        buffer.put(tmp);
        buffer.put(tmp);
        buffer.putLong(-1);
        buffer.putInt(0);
        buffer.put(compactIntToByteArray(-1));

        //Properties

        if (zoneRenderState) {
            buffer.put(compactIntToByteArray(up.nameReference("ZoneRenderState")));
            buffer.put((byte) 0x59);
            buffer.put((byte) 0x05);
            buffer.put((byte) 0x01);
            buffer.putInt(1);

            buffer.put(compactIntToByteArray(up.nameReference("bDynamicActorFilterState")));
            buffer.put((byte) 0xD3);
            buffer.put((byte) 0x00);
        }

        if (rotating) {
            buffer.put(compactIntToByteArray(up.nameReference("Physics")));
            buffer.put((byte) 0x01);
            buffer.put((byte) 0x05);
        }

        tmp = compactIntToByteArray(staticMeshRef);
        buffer.put(compactIntToByteArray(up.nameReference("StaticMesh")));
        buffer.put((byte) (Type.OBJECT.ordinal() | ((tmp.length - 1) << 4)));
        buffer.put(tmp);

        if (rotating) {
            buffer.put(compactIntToByteArray(up.nameReference("bStatic")));
            buffer.put((byte) 0x53);
            buffer.put((byte) 0x0);
        }

        tmp = compactIntToByteArray(up.objectReferenceByName("LevelInfo0", c -> c.equalsIgnoreCase("Engine.LevelInfo")));
        buffer.put(compactIntToByteArray(up.nameReference("Level")));
        buffer.put((byte) (Type.OBJECT.ordinal() | ((tmp.length - 1) << 4)));
        buffer.put(tmp);

        //Region

        buffer.put(compactIntToByteArray(up.nameReference("bSunAffect")));
        buffer.put((byte) 0xd3);
        buffer.put((byte) 0);

        tmp = compactIntToByteArray(up.nameReference("StaticMeshActor"));
        buffer.put(compactIntToByteArray(up.nameReference("Tag")));
        buffer.put((byte) (Type.NAME.ordinal() | ((tmp.length - 1) << 4)));
        buffer.put(tmp);

        //PhysicsVolume

        buffer.put(compactIntToByteArray(up.nameReference("Location")));
        buffer.put((byte) 0x3a);
        buffer.put(compactIntToByteArray(up.nameReference("Vector")));
        buffer.putFloat(0);
        buffer.putFloat(0);
        buffer.putFloat(0);
        buffer.put(compactIntToByteArray(up.nameReference("ColLocation")));
        buffer.put((byte) 0x3a);
        buffer.put(compactIntToByteArray(up.nameReference("Vector")));
        buffer.putFloat(0);
        buffer.putFloat(0);
        buffer.putFloat(0);

        buffer.put(compactIntToByteArray(up.nameReference("Rotation")));
        buffer.put((byte) 0x3a);
        buffer.put(compactIntToByteArray(up.nameReference("Rotator")));
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.put(compactIntToByteArray(up.nameReference("SwayRotationOrig")));
        buffer.put((byte) 0x3a);
        buffer.put(compactIntToByteArray(up.nameReference("Rotator")));
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.putInt(0);

        if (rotating) {
            buffer.put(compactIntToByteArray(up.nameReference("bFixedRotationDir")));
            buffer.put((byte) 0xd3);
            buffer.put((byte) 0x0);
            buffer.put(compactIntToByteArray(up.nameReference("RotationRate")));
            buffer.put((byte) 0x3a);
            buffer.put(compactIntToByteArray(up.nameReference("Rotator")));
            buffer.putInt(0);
            buffer.putInt(0);
            buffer.putInt(0);
        }

        buffer.put(compactIntToByteArray(up.nameReference("DrawScale")));
        buffer.put((byte) 0x24);
        buffer.putFloat(1);

        buffer.put(compactIntToByteArray(up.nameReference("DrawScale3D")));
        buffer.put((byte) 0x3a);
        buffer.put(compactIntToByteArray(up.nameReference("Vector")));
        buffer.putFloat(1);
        buffer.putFloat(1);
        buffer.putFloat(1);

        //TexModifyInfo

        buffer.put(compactIntToByteArray(up.nameReference("None")));

        buffer.flip();
        byte[] actor = new byte[buffer.limit()];
        buffer.get(actor);
        return actor;
    }

    public static final int STATIC_MESH_ACTOR_FLAGS = UnrealPackage.ObjectFlag.getFlags(
            Transactional, LoadForServer, LoadForEdit, HasStack);

    /**
     * @param staticMesh object ref
     * @return object ref
     * @throws IOException
     */
    public static int addStaticMeshActor(UnrealPackage up, int staticMesh, String clazz, boolean rotating, boolean zoneState) throws UncheckedIOException {
        Map<String, Integer> names = new HashMap<>();
//                "StaticMesh",
//                //"Region", "Zone", "iLeaf", "ZoneNumber",
//                "bSunAffect", "Tag", "StaticMeshActor",
//                //"PhysicsVolume",
//                "Location", "ColLocation", "Vector",
//                "Rotation", "SwayRotationOrig", "Rotator",
//                "DrawScale", "DrawScale3D"
//                //"TexModifyInfo", "bUseModify", "bTwoSide", "bAlphaBlend",
//                //"bDummy", "Color", "AlphaOp", "ColorOp"
        names.put("StaticMesh", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("bSunAffect", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("Tag", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("StaticMeshActor", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("Location", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("ColLocation", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("Vector", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("Rotation", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("SwayRotationOrig", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("Rotator", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("DrawScale", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        names.put("DrawScale3D", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        if (rotating) {
            names.put("RotationRate", UnrealPackage.DEFAULT_OBJECT_FLAGS);
            names.put("Physics", UnrealPackage.DEFAULT_OBJECT_FLAGS);
            names.put("bStatic", UnrealPackage.DEFAULT_OBJECT_FLAGS);
            names.put("bFixedRotationDir", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        }
        if (zoneState) {
            names.put("ZoneRenderState", UnrealPackage.DEFAULT_OBJECT_FLAGS);
            names.put("bDynamicActorFilterState", UnrealPackage.DEFAULT_OBJECT_FLAGS);
        }

        up.addNameEntries(names);

        if (up.objectReferenceByName(clazz, c -> c.equalsIgnoreCase("Core.Class")) == 0)
            up.addImportEntries(Collections.singletonMap(clazz, "Core.Class"));

        up.getNameTable();
        up.getImportTable();
        up.getExportTable();

        String cName = clazz.substring(clazz.lastIndexOf(".") + 1);
        String name = cName + sm(cName, up);
        up.addExportEntry(name, clazz, null, StaticMeshActorUtil.createActor(
                up, clazz, staticMesh, rotating, zoneState),
                STATIC_MESH_ACTOR_FLAGS
        );

        up.getNameTable();
        up.getImportTable();
        up.getExportTable();

        int newActorInd = up.objectReferenceByName(name, c -> c.equalsIgnoreCase(clazz));

        //System.out.println("0x"+Integer.toHexString(newActorInd-1)+" "+name+" added");

        byte[] compact = compactIntToByteArray(newActorInd);

        UnrealPackage.ExportEntry level = (UnrealPackage.ExportEntry) up.objectReference(up.objectReferenceByName("myLevel", c -> c.equalsIgnoreCase("Engine.Level")));
        ByteBuffer levelBuffer = ByteBuffer.wrap(level.getObjectRawData()).order(ByteOrder.LITTLE_ENDIAN);

        byte[] newBytes = new byte[levelBuffer.capacity() + compact.length];

        getCompactInt(levelBuffer);
        levelBuffer.getInt();
        int count = levelBuffer.getInt();
        for (int i = 0; i < count; i++)
            getCompactInt(levelBuffer);

        int countPos = levelBuffer.position();

        levelBuffer.getInt();
        count = levelBuffer.getInt();
        for (int i = 0; i < count; i++)
            getCompactInt(levelBuffer);

        levelBuffer.putInt(countPos, count + 1);
        levelBuffer.putInt(countPos + 4, count + 1);

        System.arraycopy(levelBuffer.array(), 0, newBytes, 0, levelBuffer.position());
        System.arraycopy(compact, 0, newBytes, levelBuffer.position(), compact.length);
        System.arraycopy(levelBuffer.array(), levelBuffer.position(), newBytes, levelBuffer.position() + compact.length, levelBuffer.capacity() - levelBuffer.position());

        level.setObjectRawData(newBytes);

        return newActorInd;
    }

    public static int copyStaticMeshActor(UnrealPackage up, int ind) throws IOException {
        UnrealPackage.ExportEntry entry = up.getExportTable().get(ind);

        String name = entry.getObjectClass().getObjectName().getName() + sm(entry.getObjectClass().getObjectName().getName(), up);
        up.addExportEntry(name, entry.getObjectClass().getObjectFullName(), null, entry.getObjectRawData(), entry.getObjectFlags());

        up.getNameTable();
        up.getImportTable();
        up.getExportTable();

        int newActorInd = up.objectReferenceByName(name, c -> c.equalsIgnoreCase(entry.getObjectClass().getObjectFullName()));

        //System.out.println("0x"+Integer.toHexString(newActorInd-1)+" "+name+" added");

        byte[] compact = compactIntToByteArray(newActorInd);

        UnrealPackage.ExportEntry level = (UnrealPackage.ExportEntry) up.objectReference(up.objectReferenceByName("myLevel", c -> c.equalsIgnoreCase("Engine.Level")));
        ByteBuffer levelBuffer = ByteBuffer.wrap(level.getObjectRawData()).order(ByteOrder.LITTLE_ENDIAN);

        byte[] newBytes = new byte[levelBuffer.capacity() + compact.length];

        getCompactInt(levelBuffer);
        levelBuffer.getInt();
        int count = levelBuffer.getInt();
        for (int i = 0; i < count; i++)
            getCompactInt(levelBuffer);

        int countPos = levelBuffer.position();

        levelBuffer.getInt();
        count = levelBuffer.getInt();
        for (int i = 0; i < count; i++)
            getCompactInt(levelBuffer);

        levelBuffer.putInt(countPos, count + 1);
        levelBuffer.putInt(countPos + 4, count + 1);

        System.arraycopy(levelBuffer.array(), 0, newBytes, 0, levelBuffer.position());
        System.arraycopy(compact, 0, newBytes, levelBuffer.position(), compact.length);
        System.arraycopy(levelBuffer.array(), levelBuffer.position(), newBytes, levelBuffer.position() + compact.length, levelBuffer.capacity() - levelBuffer.position());

        level.setObjectRawData(newBytes);

        return newActorInd;
    }

    private static int sm(String clazz, UnrealPackage up) {
        Pattern pattern = Pattern.compile(clazz + "\\d+");
        return 1 + up.getExportTable()
                .stream()
                .map(e -> e.getObjectName().getName())
                .filter(name -> pattern.matcher(name).matches())
                .mapToInt(name -> Integer.parseInt(name.substring(clazz.length())))
                .max()
                .orElse(-1);
    }
}
