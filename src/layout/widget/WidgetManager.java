
package layout.widget;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import javafx.scene.layout.Pane;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gui.objects.window.stage.UiContext;
import gui.objects.window.stage.Window;
import gui.objects.window.stage.WindowManager;
import layout.Component;
import layout.container.Container;
import layout.container.layout.Layout;
import layout.widget.controller.Controller;
import layout.widget.feature.Feature;
import util.SwitchException;
import util.collections.mapset.MapSet;
import util.dev.Idempotent;
import util.file.FileMonitor;
import util.file.Util;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static layout.widget.WidgetManager.WidgetSource.*;
import static main.App.APP;
import static util.Util.capitalize;
import static util.async.Async.runFX;
import static util.async.Async.runNew;
import static util.file.Util.*;
import static util.functional.Util.*;

/**
 * Handles operations with Widgets.
 */
public final class WidgetManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowManager.class);

    /**
     * Collection of valid widget factories by their name..
     * <p/>
     * Factories can be removed, added or swapped for new one in runtime. This happens when
     * widgets files are discovered/deleted/modified.
     */
    public final MapSet<String,WidgetFactory<?>> factories = new MapSet<>(WidgetFactory::name);
    private final MapSet<String,WidgetDir> monitors = new MapSet<>(wd -> wd.widgetname);
    private boolean initialized = false;
    private final WindowManager windowManager; // use App instead, but that requires different App initialization

    public WidgetManager(WindowManager windowManager) {
        this.windowManager = windowManager;
    }

    public void initialize() {
        if (initialized) throw new IllegalStateException("Already initialized");

        // internal factories
        // Factories for classes known at compile time and packaged along the application requesting
        // factory generation.
        ClassIndex.getAnnotated(GenerateWidgetFactory.class).forEach(c -> constructFactory(c, null));
        factories.add(new WidgetFactory<>(EmptyWidget.class));

        // external factories
        File dirW = APP.DIR_WIDGETS;
        if (!Util.isValidatedDirectory(dirW)) {
            LOGGER.error("External widgets registration failed.");
        } else {
	        listFiles(dirW).filter(File::isDirectory).forEach(widgetDir -> {
		        String name = capitalize(getName(widgetDir));
		        monitors.computeIfAbsent(name, n -> new WidgetDir(name, widgetDir)).registerExternalFactory();
	        });

	        FileMonitor.monitorDirsFiles(dirW, File::isDirectory, (type, widget_dir) -> {
		        String name = capitalize(getName(widget_dir));
		        if (type == ENTRY_CREATE) {
			        LOGGER.info("Discovered widget type: {}", name);
			        monitors.computeIfAbsent(name, n -> new WidgetDir(name, widget_dir)).registerExternalFactory();
		        }
		        if (type == ENTRY_DELETE) {
			        LOGGER.info("Disposing widget type: {}", name);
			        WidgetDir wd = monitors.get(name);
			        if (wd != null) wd.dispose();
		        }
	        });
        }

	    // external factories for .fxwl widgets - serialized widgets
	    File dirL = APP.DIR_LAYOUTS;
	    if (!Util.isValidatedDirectory(dirL)) {
		    LOGGER.error("External .fxwl widgets registration failed.");
	    } else {
		    listFiles(dirL).filter(f -> f.getPath().endsWith(".fxwl"))
			               .filter(f ->  readFileLines(f).limit(1).filter(line -> line.startsWith("<Widget")).count() > 0)
			               .forEach(fxwl -> {
			    String name = capitalize(getName(fxwl));
                factories.computeIfAbsent(name, key -> new WidgetFactory<>(fxwl));
		    });

		    FileMonitor.monitorDirsFiles(dirL, f -> f.getPath().endsWith(".fxwl"), (type, fxwl) -> {
			    String name = capitalize(getName(fxwl));
			    if (type == ENTRY_CREATE && !factories.contains(name)) {
				    LOGGER.info("Discovered widget type: {}", name);
				    factories.computeIfAbsent(name, key -> new WidgetFactory<>(fxwl));
			    }
			    if (type == ENTRY_DELETE && factories.get(name)!=null && factories.get(name).isDelegated) {
				    LOGGER.info("Disposing widget type: {}", name);
				    factories.remove(name);
			    }
		    });
	    }

        initialized = true;
    }

    private void constructFactory(Class<?> controller_class, File dir) {
        if (controller_class==null) {
            LOGGER.warn("Widget class {} is null", dir);
            return;
        }
        if (!Controller.class.isAssignableFrom(controller_class)) {
            LOGGER.warn("Widget class {} {} does not implement Controller", controller_class,dir);
            return;
        }

        WidgetFactory<?> wf = new WidgetFactory<>((Class<Controller<?>>) controller_class, dir);
        boolean was_replaced = factories.containsKey(wf.name());
        factories.removeKey(wf.name());
        factories.add(wf);
        LOGGER.info("Registering widget factory: {}", wf.name());

        if (was_replaced) {
            LOGGER.info("Reloading all open widgets of {}", wf.name());
            findAll(OPEN)
            .filter(w -> w.getInfo().name().equals(wf.name())) // can not rely on type since we just reloaded the class!
            .collect(toList()) // guarantees no concurrency problems due to forEach side effects
            .forEach(w -> {
                Widget<?> nw = wf.create();
                nw.setStateFrom((Widget)w);
                Integer i = w.indexInParent();
                Container c = w.getParent();
                c.removeChild(i);
                c.addChild(i, nw);
            });
        }
    }

    private class WidgetDir {
        final String widgetname;
        final File widgetdir;
        final File classfile;
        final File srcfile;
        final File skinfile;
        FileMonitor classMonitor;
        FileMonitor srcMonitor;
        FileMonitor skinsMonitor;
        boolean monitorOn = false;

        WidgetDir(String name, File dir) {
            this.widgetname = name;
            this.widgetdir = dir;
            classfile = new File(widgetdir,widgetname + ".class");
            srcfile = new File(widgetdir,widgetname + ".java");
            skinfile = new File(widgetdir,"skin.css");
        }

        @Idempotent
        void monitorStart() {
            if (monitorOn) return;
            monitorOn = true;

            // monitor source files (any .java file) & recompile on change
	        // Because the widget may be skinned (.css), load from fxml (.fxml) or use any kind of resource
	        // (.jar, .txt, etc.), we will rather monitor all of the files except for .class. This may lead to
	        // unnecessary reloads when these resources are being edited, but its just for developer convenience and
	        // its still more inconvenient to force developer to refresh manually
            classMonitor = FileMonitor.monitorDirsFiles(widgetdir, file -> !file.getPath().endsWith(".class"), (type,file) -> {
                if (type==ENTRY_CREATE || type==ENTRY_MODIFY) {
                    LOGGER.info("Widget {} source file changed {}", file,type);
                    runNew(() -> compile(getSrcFiles(),getLibFiles()));
                }
            });
            // monitor class file (only the main class' one) & recreate factory on change
            srcMonitor = FileMonitor.monitorFile(classfile, type -> {
                if (type==ENTRY_CREATE || type==ENTRY_MODIFY) {
                    LOGGER.info("Widget {} class file changed {}", widgetname,type);
                    // run on fx thread & give the compilation some time to finish
                    runFX(500, () ->
                        registerExternalFactory()
                    );
                }
            });
            // monitor skin file & reload factory on change
            srcMonitor = FileMonitor.monitorFile(skinfile, type -> {
                if (type==ENTRY_CREATE || type==ENTRY_MODIFY) {
                    LOGGER.info("Widget {} SKIN file changed {}", widgetname,type);
                    // reload skin on all open widgets
                    // run on fx thread
                    runFX(() ->
                        findAll(OPEN).filter(w -> w.getName().equals(widgetname))
                            .forEach(w -> {
                                try {
                                    if (w.root instanceof Pane) {
                                        ((Pane)w.root).getStylesheets().remove(skinfile.toURI().toURL().toExternalForm());
                                        ((Pane)w.root).getStylesheets().add(skinfile.toURI().toURL().toExternalForm());
                                    }
                                } catch (MalformedURLException ex) {
                                    java.util.logging.Logger.getLogger(WidgetManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            })
                    );
                }
            });
        }

        @Idempotent
        void monitorStop() {
            monitorOn = false;
            if (classMonitor!=null) classMonitor.stop();
            if (classMonitor!=null) srcMonitor.stop();
            if (classMonitor!=null) skinsMonitor.stop();
        }

	    @Idempotent
        void dispose() {
            monitorStop();
            factories.removeKey(widgetname);
            monitors.removeKey(widgetname);
        }

        void registerExternalFactory() {
            File classFile = new File(widgetdir, widgetname + ".class");
            File srcFile = new File(widgetdir, widgetname + ".java");

            // Source file is available if exists
            // Class file is available if exists and source file does not. But if both do, classfile must
            // not be outdated, which we check by modification time. This avoids nasty class version
            // errors as consequently we recompile the source file.
            boolean srcFile_available = srcFile.exists();
            boolean classFile_available = classFile.exists() && (!srcFile_available || classFile.lastModified()>getSrcFiles().mapToLong(File::lastModified).max().orElseGet(srcFile::lastModified));

            // If class file is available, we just create factory for it.
            if (classFile_available) {
                Class<?> controller_class = loadClass(getName(widgetdir), classFile, getLibFiles());
                constructFactory(controller_class, widgetdir);
            }

            // If only source file is available, compile
            else if (srcFile_available) {
                // If we are initialized, app is running and some widget has been added (its factory
                // does not exist yet), so we compile in the bgr as we should.
                // Else, we are initializing now, and we must be able to
                // provide all factories before widgets start loading so we compile on this (ui/fx)
                // thread. This blocks and delays startup, but it is fine - it is necessary
                if (initialized) runNew(() -> compile(getSrcFiles(),getLibFiles()));
                else {
                    boolean isSuccess = compile(getSrcFiles(),getLibFiles());
                    // File monitoring is not and must not be running yet (as it creates factory
                    // asynchronously). We do it manually.
                    if (isSuccess) registerExternalFactory();
                }
            }

            monitorStart();
        }

        Stream<File> getSrcFiles() {
            return listFiles(widgetdir).filter(f -> f.getPath().endsWith(".java"));
        }

	    Stream<File> getClassFiles() {
		    return listFiles(widgetdir).filter(f -> f.getPath().endsWith(".class"));
	    }

        Stream<File> getLibFiles() {
            return stream(listFiles(widgetdir),listFiles(widgetdir.getParentFile()))
	                   .filter(f -> f.getPath().endsWith(".jar"));
        }

    }

    /**
     * Compiles the .java file into .class file. All project dependencies (including the project
     * itself - its jar) are available because they are on the classpath.
     *
     * @param srcFiles .java files to compile
     */
    private static boolean compile(Stream<File> srcFiles, Stream<File> libFiles) {
    	String classpath = System.getProperty("java.class.path");
        Stream<String> options = stream(                            // Command-line options
	        // Compiler defaults to system encoding, we need consistent system independent encoding, such as UTF-8
			"-encoding",APP.encoding.name(),
			// Includes information about each class loaded and each source file compiled.
			// "-verbose"
			"-cp", classpath + ";" + libFiles.map(File::getAbsolutePath).collect(joining(";"))
        );
        Stream<String> sourceFiles = srcFiles.map(File::getPath);   // One or more source files to be compiled
        Stream<String> classes = stream();   // One or more classes to be processed for annotations
        Stream<String> argFiles = stream();   // One or more files that lists options and source files. The -J options are not allowed in these files
        String[] arguments = stream(options,sourceFiles,classes,argFiles).flatMap(s -> s).toArray(String[]::new);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        LOGGER.info("Compiling with arguments: {} ", toS(", ", arguments));
        int success = compiler.run(null, null, null, arguments);
	    boolean isSuccess = success==0;
        if (isSuccess) LOGGER.info("Compilation succeeded");
        else LOGGER.error("Compilation failed");

	    return isSuccess;
    }

    private static Class<?> loadClass(String widgetName, File classFile, Stream<File> libFiles) {
        try {
            File dir = classFile.getParentFile();
            String classname = widgetName + "." + Util.getName(classFile);

            ClassLoader controllerClassloader = createControllerClassLoader(dir, libFiles);

            // debug - checks if the classloader can load the same class multiple times
            // boolean isDifferentClassInstance =
            //         createControllerClassLoader(dir, name_widget).loadClass(name_class) ==
            //         createControllerClassLoader(dir, name_widget).loadClass(name_class);
            // System.out.println(isDifferentClassInstance);

            return controllerClassloader.loadClass(classname);
        } catch (ClassNotFoundException e) {
            LOGGER.info("FXML widget factory controller class loading failed for: " + classFile, e);
            return null;
        }
    }

    /**
     * Creates class loader which first loads classes in the given directory.
     * <p/>
     * In order to support runtime class reloading, classes are not cached. Multiple instances of this class loader
     * load different class even when loading the same class file!
     */
    private static ClassLoader createControllerClassLoader(File widget_dir, Stream<File> libFiles) {
	    File dir = widget_dir.getParentFile();
	    URL[] classpath = stream(dir).append(libFiles)
		                 .map(f -> {
			                 try {
				                 return f.toURI().toURL();
			                 } catch (MalformedURLException e) {
				                 return null;
			                 }
		                 })
		                 .filter(ISNTØ).toArray(URL[]::new);
	    return new URLClassLoader(classpath);
    }

    /**
     * @return read only list of registered widget factories
     */
    public Stream<WidgetFactory<?>> getFactories() {
        return factories.streamV();
    }

    public Stream<Feature> getFeatures() {
        return getFactories().flatMap(f -> f.getFeatures().stream()).distinct();
    }

/******************************************************************************/

    /**
     * remembers standalone widgets not part of any layout, mostly in popups
     * as for normal widgets - they can be obtained from layouts
     * Do not use.
     */
    public final List<Widget<?>> standaloneWidgets = new ArrayList<>();

    public Stream<Widget<?>> findAll(WidgetSource source) {
        switch(source) {
            case LAYOUT:
                return getLayouts().flatMap(l -> l.getAllWidgets());
            case STANDALONE:
            case NO_LAYOUT:
                return standaloneWidgets.stream();
            case NEW:
                return Stream.empty();
            case OPEN:
            case ANY:
                return Stream.concat(findAll(STANDALONE),findAll(LAYOUT));
            default: throw new SwitchException(source);
        }
    }

    public Optional<Widget<?>> find(String name, WidgetSource source, boolean ignore) {
        return find(w -> w.name().equals(name) || w.nameGui().equals(name), source, ignore);
    }

    /**
     * Equivalent to {@code find(filter, source, false);}
     * @see #find(java.util.function.Predicate, layout.widget.WidgetManager.WidgetSource, boolean)
     */
    public Optional<Widget<?>> find(Predicate<WidgetInfo> filter, WidgetSource source) {
        return find(filter, source, false);
    }

    /**
     * Returns widget fulfilling condition.
     * <pre>
     * - any widget can be returned (if it fulfills the cond.), but:
     * - if there is preferred widget it will always be returned first
     * - if no widget available it will be attempted to be created
     * - preferred factory will be used first.
     * - if all methods fail, null is returned
     * <pre>
     * Created widgets are displayed in a popup
     *
     * @param filter filter
     * @param source strategy for widget lookup
     * @param ignore whether the widget will partake in application layout and be
     * available for future widget search query. Use true if you want to simply
     * use the widget as a custom graphics, use false to let it be part of layout.
     * Can not be changed later.
     * <p/>
     * Note that using true will cause the returned widget (if any) be visible in
     * one way or another depending on strategy. So for example newly created
     * widget will be put into layout or will show in a popup. If this behavior is
     * not desired, use false.
     * @return optional of widget fulfilling condition or empty if not available
     */
    public Optional<Widget<?>> find(Predicate<WidgetInfo> filter, WidgetSource source, boolean ignore) {
        Widget<?> out = null;

        // get preferred type
        String preferred = getFactories()
                .filter(filter::test)
                .filter(w -> !w.isIgnored())
                .filter(f -> f.isPreferred())
                .findAny().map(f -> f.nameGui()).orElse("");

        // get viable widgets - widgets of the feature & of preferred type if any
        List<Widget<?>> widgets = findAll(source)
                .filter(w -> filter.test(w.getInfo()))
                .filter(w -> !w.forbid_use.getValue())
                .filter(preferred.isEmpty() ? w -> true : w -> w.getInfo().nameGui().equals(preferred))
                .collect(toList());

        // get preferred widget or any if none preferred
        for (Widget<?> w : widgets) {
            if (out==null) out = w;
            if (w.preferred.getValue()) {
                out = w;
                break;
            }
        }

        // if no active or layout widget available & new widgets allowed
        if (out == null && source.newWidgetsAllowed()) {
            // get factory
            WidgetFactory<?> f = getFactories()
                   .filter(filter::test)
                   .filter(w -> !w.isIgnored())
                   .filter(preferred.isEmpty() ? w->true : w->w.nameGui().equals(preferred))
                   .findAny().orElse(null);

            // open widget as standalone if found
            if (f!=null) {
                out = f.create();
                standaloneWidgets.add(out);
                if (!ignore) {
                    UiContext.showFloating(out);
                }
            }
        }

        return Optional.ofNullable(out);
    }

    /**
     * Equivalent to: {@code
     * getWidget(w->w.hasFeature(feature), source).map(w->(F)w.getController())}
     */
    public <F> Optional<F> find(Class<F> feature, WidgetSource source, boolean ignore) {
        return find(w -> w.hasFeature(feature), source, ignore).map(w -> (F)w.getController());
    }

    public <F> Optional<F> find(Class<F> feature, WidgetSource source) {
        return find(w -> w.hasFeature(feature), source).map(w -> (F)w.getController());
    }

    /** Equivalent to: {@code getWidget(type, source).ifPresent(action)} */
    public <F> void use(Class<F> type, WidgetSource source, Consumer<F> action) {
        find(type, source).ifPresent(action);
    }

    /** Equivalent to: {@code getWidget(cond, source).ifPresent(action)} */
    public void use(Predicate<WidgetInfo> cond, WidgetSource source, Consumer<Widget> action) {
        find(cond, source).ifPresent(action);
    }

    public void use(String name, WidgetSource source, Consumer<Widget> action) {
        find(name, source, false).ifPresent(action);
    }
    public void use(String name, WidgetSource source, Consumer<Widget> action, boolean ignore) {
        find(name, source, ignore).ifPresent(action);
    }

    /**
     * Denotes source for widgets. Used when looking up a widget. Sometimes it
     * is desirable to limit the source.
     */
    public enum WidgetSource {

        /**
         * The source is be all currently active widgets contained within all
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         *//**
         * The source is be all currently active widgets contained within all
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         *//**
         * The source is be all currently active widgets contained within all
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         *//**
         * The source is be all currently active widgets contained within all
         * of the layouts in all of the windows. Does not contain standalone
         * widgets. Standalone widget is one that is not part of the layout. for
         * example located in the popup. Most limited source.
         */
        LAYOUT,

        /**
         * Source is all currently active standalone widgets - widgets not part
         * of the layout, such as in popups.
         */
        STANDALONE,

        /**
         * Union of {@link #LAYOUT} and {@link #STANDALONE}.
         * <p/>
         * This is the recommended source when creating widget is not intended.
         */
        OPEN,

        /**
         * The source is all available widget factories. In other words new
         * widget will always be created if possible.
         */
        NEW,

        /**
         * Union of {@link #NEW}, {@link #STANDALONE}.
         * <p/>
         * This is the recommended source when it is expected to call the widget
         * multiple times and layout is not to be included,
         * because it creates new widget, but reuses standalone ones.
         */
        NO_LAYOUT,

        /**
         * Union of {@link #LAYOUT}, {@link #STANDALONE} and {@link #NO_LAYOUT}
         * Most complete source.
         */
        ANY;

        public boolean newWidgetsAllowed() {
            return this==NO_LAYOUT || this==ANY || this==NEW;
        }
    }

    public enum Widget_Target {
        LAYOUT,
        TAB,
        POPUP,
        WINDOW
    }








    private final List<String> layoutsAvailable = new ArrayList<>();

    public Layout getActive() {
        // If no window is focused no layout should be active as application
	    // is either not focused or in an illegal state itself.
        return windowManager.getFocused().map(Window::getLayout).orElse(null);
    }

    /**
     * @return all Layouts in the application.
     */
    public Stream<Layout> getLayouts() {
        return Stream.concat(
        	windowManager.windows.stream().map(Window::getLayout).filter(ISNTØ),
	        standaloneWidgets.stream().map(Component::getRootParent).filter(ISNTØ).map(c -> (Layout)c)
        );
    }

    /**
     * Return all names of all layouts available to the application, including
     * serialized layouts in files.
     * @return
     */
    public Stream<String> getAllLayoutsNames() {
        findLayouts();
        // get all windows and fetch their layouts
        return Stream.concat(getLayouts().map(Layout::getName), layoutsAvailable.stream()).distinct();
    }

    /**
     * Searches for .l files in layout folder and registers them as available
     * layouts. Use on app start or to discover newly added layouts.
     */
    public void findLayouts() {
        // get + verify path
        File dir = APP.DIR_LAYOUTS;
        if (!Util.isValidatedDirectory(dir)) {
            LOGGER.error("Layout directory not accessible: ", dir);
            return;
        }
        // find layout files
        File[] files = listFiles(dir).filter(f -> f.getName().endsWith(".l")).toArray(File[]::new);
        // load layouts
	    layoutsAvailable.clear();
        if (files.length == 0) return;
        for (File f : files) {
	        layoutsAvailable.add(Util.getName(f));
        }
    }

}