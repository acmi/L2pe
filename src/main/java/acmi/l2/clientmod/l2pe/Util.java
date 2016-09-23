package acmi.l2.clientmod.l2pe;

import acmi.l2.clientmod.io.*;
import acmi.l2.clientmod.unreal.UnrealRuntimeContext;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.properties.L2Property;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Stream;

import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.HasStack;
import static acmi.l2.clientmod.io.UnrealPackage.ObjectFlag.Standalone;
import static acmi.l2.clientmod.unreal.UnrealSerializerFactory.IS_STRUCT;

public class Util {
    public static void createClass(UnrealSerializerFactory serializer, UnrealPackage up, String objName, String objSuperClass, int flags, List<L2Property> properties) {
        flags |= Standalone.getMask();

        Stream.of(up.getPackageName(), "System")
                .filter(s -> up.nameReference(s) < 0)
                .forEach(up::addNameEntries);
        if (up.objectReferenceByName("Core.Object", IS_STRUCT) == 0)
            up.addImportEntries(Collections.singletonMap("Core.Object", "Core.Class"));
        up.addExportEntry(
                objName,
                null,
                objSuperClass,
                new byte[0],
                flags);
        UnrealPackage.ExportEntry entry = up.getExportTable().get(up.getExportTable().size() - 1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput<UnrealRuntimeContext> dataOutput = new ObjectOutputStream<>(baos, up.getFile().getCharset(), serializer, new UnrealRuntimeContext(entry, serializer));
        dataOutput.writeCompactInt(up.objectReferenceByName(objSuperClass, IS_STRUCT));
        dataOutput.writeCompactInt(0);
        dataOutput.writeCompactInt(0);
        dataOutput.writeCompactInt(0);
        dataOutput.writeCompactInt(up.nameReference(entry.getObjectName().getName()));
        dataOutput.writeCompactInt(0);
        dataOutput.writeInt(-1);
        dataOutput.writeInt(-1);
        dataOutput.writeInt(0);
        dataOutput.writeLong(0x0080000000000040L);
        dataOutput.writeLong(-1L);
        dataOutput.writeShort(-1);
        dataOutput.writeInt(0);
        dataOutput.writeInt(0x00000212);
        dataOutput.writeBytes(new byte[16]);
        dataOutput.writeCompactInt(2);
        dataOutput.writeCompactInt(entry.getObjectReference());
        dataOutput.writeInt(1);
        dataOutput.writeInt(0);
        dataOutput.writeCompactInt(entry.getObjectSuperClass().getObjectReference());
        dataOutput.writeInt(1);
        dataOutput.writeInt(0);
        Set<String> packages = new HashSet<>(Arrays.asList("Core", "Engine", up.getPackageName()));
        dataOutput.writeCompactInt(packages.size());
        for (String packageName : packages)
            dataOutput.writeCompactInt(up.nameReference(packageName));
        dataOutput.writeCompactInt(up.objectReferenceByName("Core.Object", IS_STRUCT));
        dataOutput.writeCompactInt(up.nameReference("System"));
        dataOutput.writeCompactInt(0);
        PropertiesUtil.writeProperties(dataOutput, properties);

        entry.setObjectRawData(baos.toByteArray());
    }

    public static void createObject(UnrealSerializerFactory serializer, UnrealPackage up, String objName, String objClass, int flags, boolean hasStack, List<L2Property> properties) {
        if (hasStack)
            flags |= HasStack.getMask();

        up.addExportEntry(
                objName,
                objClass,
                null,
                new byte[0],
                flags);
        UnrealPackage.ExportEntry entry = up.getExportTable().get(up.getExportTable().size() - 1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutput<UnrealRuntimeContext> dataOutput = new ObjectOutputStream<>(baos, up.getFile().getCharset(), serializer, new UnrealRuntimeContext(entry, serializer));
        if (hasStack) {
            int classRef = up.objectReferenceByName(objClass, IS_STRUCT);
            if (classRef == 0) {
                up.addImportEntries(Collections.singletonMap(objClass, "Core.Class"));
                classRef = up.objectReferenceByName(objClass, IS_STRUCT);
            }
            dataOutput.writeCompactInt(classRef);
            dataOutput.writeCompactInt(classRef);
            dataOutput.writeLong(-1);
            dataOutput.writeInt(0);
            dataOutput.writeCompactInt(-1);
        }
        PropertiesUtil.writeProperties(dataOutput, properties);

        entry.setObjectRawData(baos.toByteArray());
    }
}
