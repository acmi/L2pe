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

import acmi.l2.clientmod.io.UnrealPackage;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.core.*;
import acmi.l2.clientmod.unreal.core.Enum;
import acmi.l2.clientmod.unreal.core.Object;
import acmi.l2.clientmod.unreal.properties.L2Property;
import acmi.l2.clientmod.unreal.properties.PropertiesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static acmi.l2.clientmod.properties.control.PropertiesEditor.removeDefaults;

@SuppressWarnings("unchecked")
public class Decompiler {
    static Object instantiate(UnrealPackage.ExportEntry entry, UnrealSerializerFactory objectFactory) {
        return objectFactory.getOrCreateObject(entry);
    }

    public static CharSequence decompileProperties(Object object, UnrealSerializerFactory objectFactory, int indent) {
        UnrealPackage.ExportEntry e = object.entry;
        UnrealPackage up = e.getUnrealPackage();
        String structName = e.getObjectClass() == null ? e.getObjectSuperClass().getObjectFullName() : e.getFullClassName();
        List<L2Property> props = new ArrayList<>(object.properties);

        return decompileProperties(props, structName, up, objectFactory, indent);
    }

    public static CharSequence decompileProperties(List<L2Property> props, String structName, UnrealPackage up, UnrealSerializerFactory objectFactory, int indent) {
        Stream.Builder<CharSequence> properties = Stream.builder();

        removeDefaults(props, structName, objectFactory, up);

        props.forEach(property -> {
            StringBuilder sb = new StringBuilder();

            Property template = property.getTemplate();

            for (int i = 0; i < template.arrayDimension; i++) {
                java.lang.Object obj = property.getAt(i);

                if (template instanceof ByteProperty ||
                        template instanceof ObjectProperty ||
                        template instanceof NameProperty ||
                        template instanceof ArrayProperty) {
                    assert obj != null;
                }

                if (obj == null) {
                    if (template instanceof StructProperty)
                        continue;
                }

                if (i > 0)
                    sb.append(newLine(indent));

                if (template instanceof ByteProperty) {
                    sb.append(property.getName());
                    if (template.arrayDimension > 1) {
                        sb.append("(").append(i).append(")");
                    }
                    sb.append("=");
                    if (((ByteProperty) template).enumType != null) {
                        Enum en = ((ByteProperty) template).enumType;
                        sb.append(en.values[(Integer) obj]);
                    } else {
                        sb.append(obj);
                    }
                } else if (template instanceof IntProperty ||
                        template instanceof BoolProperty) {
                    sb.append(property.getName());
                    if (template.arrayDimension > 1) {
                        sb.append("(").append(i).append(")");
                    }
                    sb.append("=");
                    sb.append(obj);
                } else if (template instanceof FloatProperty) {
                    sb.append(property.getName());
                    if (template.arrayDimension > 1) {
                        sb.append("(").append(i).append(")");
                    }
                    sb.append("=");
                    sb.append(String.format(Locale.US, "%f", (Float) obj));
                } else if (template instanceof ObjectProperty) {
                    UnrealPackage.Entry entry = up.objectReference((Integer) obj);
                    if (needExport(template, objectFactory)) {
                        properties.add(toT3d(instantiate((UnrealPackage.ExportEntry) entry, objectFactory), objectFactory, indent));
                    }
                    sb.append(property.getName());
                    if (template.arrayDimension > 1) {
                        sb.append("(").append(i).append(")");
                    }
                    sb.append("=");
                    if (entry == null) {
                        sb.append("None");
                    } else if (entry instanceof UnrealPackage.ImportEntry) {
                        sb.append(((UnrealPackage.ImportEntry) entry).getClassName().getName())
                                .append("'")
                                .append(entry.getObjectFullName())
                                .append("'");
                    } else if (entry instanceof UnrealPackage.ExportEntry) {
                        String clazz = "Class";
                        if (((UnrealPackage.ExportEntry) entry).getObjectClass() != null)
                            clazz = ((UnrealPackage.ExportEntry) entry).getObjectClass().getObjectName().getName();
                        sb.append(clazz)
                                .append("'")
                                .append(entry.getObjectInnerFullName())
                                .append("'");
                    } else {
                        throw new IllegalStateException("wtf");
                    }
                } else if (template instanceof NameProperty) {
                    sb.append(property.getName());
                    if (template.arrayDimension > 1) {
                        sb.append("(").append(i).append(")");
                    }
                    sb.append("=");
                    sb.append("'").append(up.nameReference((Integer) obj)).append("'");
                } else if (template instanceof ArrayProperty) {
                    ArrayProperty arrayProperty = (ArrayProperty) property.getTemplate();
                    Property innerProperty = arrayProperty.inner;
                    L2Property fakeProperty = new L2Property(innerProperty);
                    List<java.lang.Object> list = (List<java.lang.Object>) obj;

                    for (int j = 0; j < list.size(); j++) {
                        java.lang.Object innerObj = list.get(j);

                        if (needExport(innerProperty, objectFactory)) {
                            if (innerProperty instanceof ObjectProperty) {
                                UnrealPackage.Entry entry = up.objectReference((Integer) innerObj);
                                properties.add(toT3d(instantiate((UnrealPackage.ExportEntry) entry, objectFactory), objectFactory, indent));
                            } else if (innerProperty instanceof StructProperty) {
                                properties.add(toT3d((List<L2Property>) innerObj, "", ((StructProperty)innerProperty).struct.getFullName(), up, objectFactory, indent));
                                throw new RuntimeException("FIXME");
                            }
                        }

                        fakeProperty.putAt(0, innerObj);
                        if (j > 0)
                            sb.append(newLine(indent));
                        sb.append(property.getName()).append("(").append(j).append(")")
                                .append("=")
                                .append(inlineProperty(fakeProperty, up, objectFactory, true));
                    }
                } else if (template instanceof StructProperty) {
                    sb.append(property.getName());
                    if (template.arrayDimension > 1) {
                        sb.append("(").append(i).append(")");
                    }
                    sb.append("=");
                    sb.append(inlineStruct((List<L2Property>) obj, up, objectFactory));
                } else if (template instanceof StrProperty) {
                    sb.append(property.getName());
                    if (template.arrayDimension > 1) {
                        sb.append("(").append(i).append(")");
                    }
                    sb.append("=");
                    sb.append("\"").append(Objects.toString(obj)).append("\"");
                }
            }

            properties.add(sb);
        });

        return properties.build().collect(Collectors.joining(newLine(indent)));
    }

    public static CharSequence inlineProperty(L2Property property, UnrealPackage up, UnrealSerializerFactory objectFactory, boolean valueOnly) {
        StringBuilder sb = new StringBuilder();

        Property template = property.getTemplate();

        for (int i = 0; i < template.arrayDimension; i++) {
            if (!valueOnly) {
                sb.append(property.getName());

                if (template.arrayDimension > 1) {
                    sb.append("(").append(i).append(")");
                }

                sb.append("=");
            }

            java.lang.Object object = property.getAt(i);

            if (template instanceof ByteProperty) {
                if (((ByteProperty) template).enumType != null) {
                    Enum en = ((ByteProperty) template).enumType;
                    sb.append(en.values[(Integer) object]);
                } else {
                    sb.append(object);
                }
            } else if (template instanceof IntProperty ||
                    template instanceof BoolProperty) {
                sb.append(object);
            } else if (template instanceof FloatProperty) {
                sb.append(String.format(Locale.US, "%f", (Float) object));
            } else if (template instanceof ObjectProperty) {
                UnrealPackage.Entry entry = up.objectReference((Integer) object);
                if (entry == null) {
                    sb.append("None");
                } else if (entry instanceof UnrealPackage.ImportEntry) {
                    sb.append(((UnrealPackage.ImportEntry) entry).getClassName().getName())
                            .append("'")
                            .append(entry.getObjectFullName())
                            .append("'");
                } else if (entry instanceof UnrealPackage.ExportEntry) {
                    if (Property.CPF.getFlags(template.propertyFlags).contains(Property.CPF.ExportObject)) {
                        sb.append("\"").append(entry.getObjectName().getName()).append("\"");
                    } else {
                        String clazz = "Class";
                        if (((UnrealPackage.ExportEntry) entry).getObjectClass() != null)
                            clazz = ((UnrealPackage.ExportEntry) entry).getObjectClass().getObjectName().getName();
                        sb.append(clazz)
                                .append("'")
                                .append(entry.getObjectName().getName())
                                .append("'");
                    }
                } else {
                    throw new IllegalStateException("wtf");
                }
            } else if (template instanceof NameProperty) {
                sb.append("'").append(Objects.toString(object)).append("'");
            } else if (template instanceof ArrayProperty) {
                ArrayProperty arrayProperty = (ArrayProperty) property.getTemplate();
                Property innerProperty = arrayProperty.inner;
                L2Property fakeProperty = new L2Property(innerProperty);
                List<java.lang.Object> list = (List<java.lang.Object>) object;

                sb.append(list.stream()
                        .map(o -> {
                            fakeProperty.putAt(0, o);
                            return inlineProperty(fakeProperty, up, objectFactory, true);
                        }).collect(Collectors.joining(",", "(", ")")));
            } else if (template instanceof StructProperty) {
                if (object == null) {
                    sb.append("None");
                } else {
                    sb.append(inlineStruct((List<L2Property>) object, up, objectFactory));
                }
            } else if (template instanceof StrProperty) {
                sb.append("\"").append(Objects.toString(object)).append("\"");
            }

            if (i != template.arrayDimension - 1)
                sb.append(",");
        }

        return sb;
    }

    public static CharSequence inlineStruct(List<L2Property> struct, UnrealPackage up, UnrealSerializerFactory objectFactory) {
        return struct.stream().map(p -> inlineProperty(p, up, objectFactory, false)).collect(Collectors.joining(",", "(", ")"));
    }

    public static boolean needExport(Property template, UnrealSerializerFactory objectFactory) {
        return flags(template, objectFactory);
    }

    private static boolean flags(Property template, UnrealSerializerFactory objectFactory) {
        return Property.CPF.getFlags(template.propertyFlags).contains(Property.CPF.ExportObject) ||
                Property.CPF.getFlags(template.propertyFlags).contains(Property.CPF.EditInlineNotify) ||
                (template instanceof ArrayProperty && flags(((ArrayProperty) template).inner, objectFactory)) ||
                (template instanceof StructProperty && PropertiesUtil.getPropertyFields(objectFactory, ((StructProperty) template).struct.getFullName()).filter(p -> Decompiler.flags(p, objectFactory)).findAny().isPresent());
    }

    public static CharSequence toT3d(Object object, UnrealSerializerFactory objectFactory, int indent) {
        return toT3d(object.properties, object.entry.getObjectFullName(), object.entry.getFullClassName(), object.entry.getUnrealPackage(), objectFactory, indent);
    }

    public static CharSequence toT3d(List<L2Property> props, String name, String clazz, UnrealPackage up, UnrealSerializerFactory objectFactory, int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append("Begin Object");
        sb.append(" Class=").append(clazz.substring(clazz.lastIndexOf(".") + 1));
        sb.append(" Name=").append(name.substring(name.lastIndexOf(".") + 1));
        sb.append(newLine(indent + 1)).append(decompileProperties(props, clazz, up, objectFactory, indent + 1));
        sb.append(newLine(indent)).append("End Object");

        return sb;
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
}