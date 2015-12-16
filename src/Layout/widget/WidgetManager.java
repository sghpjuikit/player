
package Layout.widget;

import java.io.File;
import java.io.FileInputStream;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Layout.container.layout.LayoutManager;
import Layout.widget.controller.Controller;
import Layout.widget.feature.Feature;
import gui.objects.Window.stage.UiContext;
import util.File.FileMonitor;
import util.File.FileUtil;
import util.SwitchException;
import util.collections.mapset.MapSet;

import static Layout.widget.WidgetManager.WidgetSource.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.File.FileUtil.getName;
import static util.async.Async.runFX;
import static util.async.Async.runNewAfter;
import static util.functional.Util.stream;

/**
 * Handles operations with Widgets.
 */
public final class WidgetManager {

    private final Logger LOGGER = LoggerFactory.getLogger("WindowManager.class");

    /**
     * Collection of valid widget factories by their name..
     * <p>
     * Factories can be removed, added or swapped for new one in runtime. This happens when
     * widgets files are discovered/deleted/modified.
     */
    public final MapSet<String,WidgetFactory<?>> factories = new MapSet<>(factory -> factory.name());
    static final Set<String> DEPENDENCIES = new HashSet<>();

    public void initialize() {

        DEPENDENCIES.clear();
        stream(new File(APP.DIR_APP_SRC,"lib").listFiles()).map(File::getPath).
                forEach(DEPENDENCIES::add);
        DEPENDENCIES.add(APP.FILE_SRC_JAR.getPath());

        // internal factories
        ClassIndex.getAnnotated(IsWidget.class).forEach(c -> constructFactory(c, null));
        factories.add(new WidgetFactory<>(EmptyWidget.class));

        // external factories
        File dir = APP.DIR_WIDGETS;
        if (!FileUtil.isValidatedDirectory(dir)) {
            LOGGER.error("External widgets registration failed.");
            return;
        }

        for(File widget_dir : dir.listFiles(f -> f.isDirectory())) {
            registerExternalFactory(widget_dir);
        }

        FileMonitor.monitorDirectory(dir, (type,file) -> {
            if(file.isDirectory()) {
                String widget_name = getName(file);
                if(type==ENTRY_CREATE || type==ENTRY_MODIFY) {
                    LOGGER.info("Discovered widget: {}", widget_name);
                    registerExternalFactory(dir);
                }
                if(type==ENTRY_DELETE) {
                    LOGGER.info("Deleted widget: {}", widget_name);
                    factories.removeKey(widget_name);
                }
            }
        });
    }

    private void registerExternalFactory(File widgetdir) {
        String widgetname = getName(widgetdir);
        File classfile = new File(widgetdir,widgetname + ".class");
        File srcfile = new File(widgetdir,widgetname + ".java");

        boolean classfile_available = classfile.exists();
        boolean srcfile_available = srcfile.exists();

        // monitor source file & recompile on change
        FileMonitor.monitorFile(srcfile, type -> {
            if(type==ENTRY_CREATE || type==ENTRY_MODIFY) {
                runFX(500, () ->
                    compile(srcfile)
                );
            }
        });
        // monitor class file & recreate factory on change
        FileMonitor.monitorFile(classfile, type -> {
            if(type==ENTRY_CREATE || type==ENTRY_MODIFY) {
                // run on fx thread & give the compilation some time to finish
                runFX(500, () ->
                    registerExternalFactory(widgetdir)
                );
            }
        });

        // If class file is available, we just create factory for it.
        if(classfile_available) {
            factories.removeKey(widgetname);
            Class<?> controller_class = loadClass(widgetname, classfile);
            constructFactory(controller_class, widgetdir);
        }

        // If only source file is available, compile
        else if(srcfile_available) {
            runNewAfter(millis(500), () -> compile(srcfile));
        }
    }

    private void constructFactory(Class controller_class, File dir) {
        if(controller_class==null) return;
        if(!Controller.class.isAssignableFrom(controller_class)) {
            LOGGER.info("Widget class {} does implement Controller.",controller_class);
            return;
        }

        WidgetFactory<?> wf = new WidgetFactory<>((Class<Controller<?>>) controller_class, dir);
        factories.add(wf);
        LOGGER.info("Registering widget factory: {}", wf.name());
    }

    private void compile(File srcfile) {
        LOGGER.info("Compiling {}", srcfile);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int success = compiler.run(null, null, null, srcfile.getPath());
        if(success == 0){
            LOGGER.info("Compilation succeeded");
        } else{
            LOGGER.info("Compilation failed");
        }
    }

    private Class<?> loadClass(String widgetname, File classFile) {
        try {
            File dir = classFile.getParentFile();
//            String widgetname = FileUtil.getName(classFile);
            String classname = FileUtil.getName(classFile);

            // We can use system class loader when we dont need runtime class reloading
            ClassLoader controllerClassloader =
                                                createControllerClassLoader(dir, widgetname);
//                                                ClassLoader.getSystemClassLoader();

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
     * Creates class loader which first tries to load the class in the provided directory and only
     * delegates to its parent class loader if it fails.
     * <p>
     * This is not normal class loader behavior, since class loader first consults parent to avoid
     * loading any class more than once. Thus, multiple instances of this class loader load
     * different class even when loading the same class file!
     * If the controller attempts to load the same class more than once it throws LinkageError
     * (attempted  duplicate class definition).
     * <p>
     * Normally, this be accomplished easily using different instances of URLClassLoader, but
     * in case the loaded class is on the classpath this will not work because the parent class
     * loader is consulted first and it will find the class (since it is on the classpath) and load
     * it (and prevent loading it ever again, unless we use class loader which will not consult it).
     * <p>
     * We care, because if we limit ourselves (with loading classes multiple times) to classes not
     * on the classpath, all external widget classes must not be on the classpath, meaning we have
     * to create separate project for every widget, since if they were part of this project,
     * Netbeans (which can only put compiled class files into one place) would automatically put
     * them on classpath. Now we dont need multiple projects as we dont mind widget class files on
     * the classpath since this class loader will load them from their widget location.
     * <p>
     * To explain - yes, external widgets have 2 copies of compiled class files now. One where its
     * source files (.java) are, where they are loaded from and the other in the application jar.
     * The class files must be copied manually to from classpath widget's locations when they change.
     * The widget classes belong to no package (have no package declaration).
     */
    private static ClassLoader createControllerClassLoader(File widget_dir, String widget_name) {
        return new ClassLoader(){

            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return name.startsWith(widget_name)
                        ? loadCl(name)
                        : super.loadClass(name);
            }

            Class<?> loadCl(final String name) throws ClassNotFoundException {
                AccessControlContext acc = AccessController.getContext();
                try {
                    PrivilegedExceptionAction action = new PrivilegedExceptionAction() {
                        @Override
                        public Object run() throws ClassNotFoundException {
                            try {
                                FileInputStream fi = new FileInputStream(new File(widget_dir,name+".class"));
                                byte[] classBytes = new byte[fi.available()];
                                fi.read(classBytes);
                                return defineClass(name, classBytes, 0, classBytes.length);
                            } catch(Exception e ) {
                                throw new ClassNotFoundException(name);
                            }
                        }
                    };
                    return (Class)AccessController.doPrivileged(action, acc);
                } catch (java.security.PrivilegedActionException pae) {
                    return super.findClass(name);
                }
            }
        };
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
    public final List<Widget> standaloneWidgets = new ArrayList();

    public Stream<Widget> findAll(WidgetSource source) {
        switch(source) {
            case LAYOUT:
                return LayoutManager.getLayouts().flatMap(l->l.getAllWidgets());
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

    /**
     * Equivalent to {@code find(filter, source, false);}
     * @see #find(java.util.function.Predicate, Layout.Widgets.WidgetManager.WidgetSource, boolean)
     */
    public Optional<Widget> find(Predicate<WidgetInfo> filter, WidgetSource source) {
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
     * <p>
     * Note that using true will cause the returned widget (if any) be visible in
     * one way or another depending on strategy. So for example newly created
     * widget will be put into layout or will show in a popup. If this behavior is
     * not desired, use false.
     * @return optional of widget fulfilling condition or empty if not available
     */
    public Optional<Widget> find(Predicate<WidgetInfo> filter, WidgetSource source, boolean ignore) {
        Widget out = null;

        // get preferred type
        String preferred = getFactories()
                .filter(filter::test)
                .filter(w -> !w.isIgnored())
                .filter(f -> f.isPreferred())
                .findAny().map(f->f.name).orElse("");

        // get viable widgets - widgets of the feature & of preferred type if any
        List<Widget> widgets = findAll(source)
                .filter(w -> filter.test(w.getInfo()))
                .filter(w -> !w.isIgnored())
                .filter(preferred.isEmpty() ? w->true : w->w.getInfo().name().equals(preferred))
                .collect(Collectors.toList());

        // get preferred widget or any if none preferred
        for(Widget w : widgets) {
            if(out==null) out = w;
            if (w.isPreffered()) {
                out = w;
                break;
            }
        }

        // if no active or layout widget available & new widgets allowed
        if (out == null && source.newWidgetsAllowed()) {
            // get factory
            WidgetFactory f = getFactories()
                   .filter(filter::test)
                   .filter(w -> !w.isIgnored())
                   .filter(preferred.isEmpty() ? w->true : w->w.name().equals(preferred))
                   .findAny().orElse(null);

            // open widget as standalone if found
            if (f!=null) {
                out = f.create();
                if(!ignore) {
                    standaloneWidgets.add(out);
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
        return find(w -> w.hasFeature(feature), source, ignore).map(w->(F)w.getController());
    }
    public <F> Optional<F> find(Class<F> feature, WidgetSource source) {
        return find(w -> w.hasFeature(feature), source).map(w->(F)w.getController());
    }
    public <F> Optional<F> findExact(Class<? extends Controller> type, WidgetSource source) {
        return find(w -> w.type()==type, source).map(w -> (F)w.getController());
    }

    /** Equivalent to: {@code getWidget(type, source).ifPresent(action)} */
    public <F> void use(Class<F> type, WidgetSource source, Consumer<F> action) {
        find(type, source).ifPresent(action);
    }

    /** Equivalent to: {@code getWidget(cond, source).ifPresent(action)} */
    public void use(Predicate<WidgetInfo> cond, WidgetSource source, Consumer<Widget> action) {
        find(cond, source).ifPresent(action);
    }

    /**
     * Denotes source for widgets. Used when looking up a widget. Sometimes it
     * is desirable to limit the source.
     */
    public static enum WidgetSource {

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
         * <p>
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
         * <p>
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

    public static enum Widget_Target {
        LAYOUT,
        TAB,
        POPUP,
        WINDOW;
    }

}