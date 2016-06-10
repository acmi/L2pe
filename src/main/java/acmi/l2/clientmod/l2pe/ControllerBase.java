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
import acmi.l2.clientmod.unreal.Environment;
import acmi.l2.clientmod.unreal.UnrealSerializerFactory;
import acmi.l2.clientmod.unreal.core.Object;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ControllerBase {
    private ObjectProperty<Environment> environment = new SimpleObjectProperty<>(this, "environment");
    private ObjectProperty<UnrealSerializerFactory> serializerFactory = new SimpleObjectProperty<>(this, "serializerFactory");
    private MapProperty<File, List<File>> packages = new SimpleMapProperty<>(this, "packages");
    private ObjectProperty<UnrealPackage> unrealPackage = new SimpleObjectProperty<>(this, "unrealPackage");
    private ObjectProperty<UnrealPackage.ExportEntry> entry = new SimpleObjectProperty<>(this, "entry");
    private ObjectProperty<Object> object = new SimpleObjectProperty<>(this, "object");

    private BooleanBinding environmentSelected = Bindings.createBooleanBinding(() -> Objects.nonNull(getEnvironment()), environmentProperty());
    private BooleanBinding packageSelected = Bindings.createBooleanBinding(() -> Objects.nonNull(getUnrealPackage()), unrealPackageProperty());
    private BooleanBinding entrySelected = Bindings.createBooleanBinding(() -> Objects.nonNull(getEntry()), entryProperty());

    private ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "L2pe Executor") {{
        setDaemon(true);
    }});

    public Environment getEnvironment() {
        return environment.get();
    }

    public ObjectProperty<Environment> environmentProperty() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment.set(environment);
    }

    public UnrealSerializerFactory getSerializerFactory() {
        return serializerFactory.get();
    }

    public ReadOnlyObjectProperty<UnrealSerializerFactory> serializerFactoryProperty() {
        return serializerFactory;
    }

    public ObservableMap<File, List<File>> getPackages() {
        return packages.get();
    }

    public ReadOnlyMapProperty<File, List<File>> packagesProperty() {
        return packages;
    }

    protected void setPackages(ObservableMap<File, List<File>> packages) {
        this.packages.set(packages);
    }

    public UnrealPackage getUnrealPackage() {
        return unrealPackage.get();
    }

    public ObjectProperty<UnrealPackage> unrealPackageProperty() {
        return unrealPackage;
    }

    public void setUnrealPackage(UnrealPackage unrealPackage) {
        this.unrealPackage.set(unrealPackage);
    }

    public UnrealPackage.ExportEntry getEntry() {
        return entry.get();
    }

    public ObjectProperty<UnrealPackage.ExportEntry> entryProperty() {
        return entry;
    }

    public void setEntry(UnrealPackage.ExportEntry entry) {
        this.entry.set(entry);
    }

    public Object getObject() {
        return object.get();
    }

    public ReadOnlyObjectProperty<Object> objectProperty() {
        return object;
    }

    protected void setObject(Object object) {
        this.object.set(object);
    }

    public BooleanBinding environmentSelected() {
        return environmentSelected;
    }

    public boolean isEnvironmentSelected() {
        return environmentSelected.get();
    }

    public BooleanBinding packageSelected() {
        return packageSelected;
    }

    public boolean isPackageSelected() {
        return packageSelected.get();
    }

    public BooleanBinding entrySelected() {
        return entrySelected;
    }

    public boolean isEntrySelected() {
        return entrySelected.get();
    }

    public ControllerBase() {
        environmentProperty().addListener(observable -> {
            setPackages(FXCollections.observableMap(getEnvironment().listFiles()
                    .collect(Collectors.groupingBy(File::getParentFile))));
        });

        serializerFactory.bind(Bindings.createObjectBinding(() -> getEnvironment() != null ? new UnrealSerializerFactory(getEnvironment()) : null, environmentProperty()));
    }

    protected void execute(Task task, Consumer<Exception> exceptionHandler) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        });
    }

    protected interface Task {
        void run() throws Exception;
    }
}
