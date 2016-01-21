
package main;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableListBase;
import javafx.scene.ImageCursor;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import org.atteo.classindex.ClassIndex;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.Mapper;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.SimpleItem;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.plugin.IsPlugin;
import AudioPlayer.plugin.IsPluginType;
import AudioPlayer.services.ClickEffect;
import AudioPlayer.services.Database.DB;
import AudioPlayer.services.Service;
import AudioPlayer.services.ServiceManager;
import AudioPlayer.services.notif.Notifier;
import AudioPlayer.services.playcount.PlaycountIncrementer;
import AudioPlayer.services.tray.TrayService;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataGroup;
import AudioPlayer.tagging.MetadataReader;
import Configuration.*;
import Layout.Component;
import Layout.widget.Widget;
import Layout.widget.WidgetManager;
import Layout.widget.WidgetManager.WidgetSource;
import Layout.widget.feature.ConfiguringFeature;
import Layout.widget.feature.ImageDisplayFeature;
import Layout.widget.feature.ImagesDisplayFeature;
import Layout.widget.feature.PlaylistFeature;
import Layout.widget.feature.SongWriter;
import action.Action;
import action.IsAction;
import action.IsActionable;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.octicons.OctIcon;
import de.jensd.fx.glyphs.weathericons.WeatherIcon;
import gui.GUI;
import gui.objects.PopOver.PopOver;
import gui.objects.TableCell.RatingCellFactory;
import gui.objects.TableCell.TextStarRatingCellFactory;
import gui.objects.Window.stage.UiContext;
import gui.objects.Window.stage.Window;
import gui.objects.Window.stage.WindowManager;
import gui.objects.icon.IconInfo;
import gui.pane.ActionPane;
import gui.pane.ActionPane.FastAction;
import gui.pane.ActionPane.FastColAction;
import gui.pane.ActionPane.SlowColAction;
import gui.pane.CellPane;
import gui.pane.InfoPane;
import gui.pane.ShortcutPane;
import util.ClassName;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Environment;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.InstanceInfo;
import util.InstanceName;
import util.access.V;
import util.access.VarEnum;
import util.animation.Anim;
import util.async.future.Fut;
import util.dev.TODO;
import util.plugin.PluginMap;
import util.reactive.RunnableSet;
import util.serialize.xstream.BooleanPropertyConverter;
import util.serialize.xstream.DoublePropertyConverter;
import util.serialize.xstream.IntegerPropertyConverter;
import util.serialize.xstream.LongPropertyConverter;
import util.serialize.xstream.ObjectPropertyConverter;
import util.serialize.xstream.PlaybackStateConverter;
import util.serialize.xstream.PlaylistItemConverter;
import util.serialize.xstream.StringPropertyConverter;
import util.units.FileSize;

import static Layout.widget.WidgetManager.WidgetSource.ANY;
import static Layout.widget.WidgetManager.WidgetSource.NEW;
import static Layout.widget.WidgetManager.WidgetSource.NO_LAYOUT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CSS3;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FOLDER;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GITHUB;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.IMAGE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.BRUSH;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.EXPORT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.IMPORT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.INFORMATION_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.KEYBOARD_VARIANT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static gui.objects.PopOver.PopOver.ScreenPos.App_Center;
import static gui.objects.Window.stage.Window.WINDOWS;
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.toList;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.TOP_CENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static org.atteo.evo.inflector.English.plural;
import static util.File.Environment.browse;
import static util.Util.getEnumConstants;
import static util.Util.getGenericPropertyType;
import static util.Util.getImageDim;
import static util.UtilExp.setupCustomTooltipBehavior;
import static util.async.Async.*;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.functional.Util.forEachAfter;
import static util.functional.Util.map;
import static util.functional.Util.stream;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layVertically;

/**
 * Application. Represents the program.
 * <p>
 * Single instance:<br>
 * Application can only instantiate single instance, any subsequent call to constructor throws
 * an exception.
 */
@IsActionable
@IsConfigurable("General")
public class App extends Application implements Configurable {

    /** Single instance of the application representing this running application. */
    public static App APP;
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    /**
     * Starts program.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
        // auncherImpl.launchApplication(App.class, preloaderclass, args); launch with preloader
    }


    /** Name of this application. Works only in dev mode.*/
    public final String name = "PlayerFX";
    /** Url for github website for project of this application. */
    public final URI GITHUB_URI = URI.create("https://www.github.com/sghpjuikit/player/");

    public final Charset encoding = StandardCharsets.UTF_8;

    /** Absolute file of directory of this app. Equivalent to new File("").getAbsoluteFile(). */
    public final File DIR_APP = new File("").getAbsoluteFile();
    /** Temporary directory of the os. */
    public final File DIR_TEMP = new File(System.getProperty("java.io.tmpdir"));
    /** Home directory of the os. */
    public final File DIR_HOME = new File(System.getProperty("user.home"));
    /** File for application configuration. */
    public final File FILE_SETTINGS = new File(DIR_APP,"settings.cfg");
    /** Directory for application logging. */
    public final File DIR_LOG = new File(DIR_APP,"log");
    /** File for application logging configuration. */
    public final File FILE_LOG_CONFIG = new File(DIR_LOG,"log_configuration.xml");
    /** Directory containing widgets - source files, class files and widget's resources. */
    public final File DIR_WIDGETS = new File(DIR_APP,"widgets");
    /** Directory containing skins. */
    public final File DIR_SKINS = new File(DIR_APP,"skins");
    public final File DIR_LAYOUTS = new File(DIR_APP,"layouts");;

    /**
     * Event source and stream for executed actions, providing their name. Use
     * for notifications of running the action or executing additional behavior.
     * <p>
     * A use case could be an application wizard asking user to do something.
     * The code in question simply notifies this stream of the name of action
     * upon execution. The wizard would then monitor this stream
     * and get notified if the expected action was executed.
     * <p>
     * Running an {@link Action} always fires an event.
     * Supports custom actions. Simply push a String value into the stream.
     */
    public final EventSource<String> actionStream = new EventSource<>();
    public final AppInstanceComm appCommunicator = new AppInstanceComm();
    public final AppParameterProcessor parameterProcessor = new AppParameterProcessor();
    public final AppSerializator serializators = new AppSerializator(encoding);
    public final Configuration configuration = new Configuration();

    public Window window;
    public Window windowOwner;
    public final TaskBar taskbarIcon = new TaskBar();
    public final ActionPane actionPane = new ActionPane();
    public final ShortcutPane shortcutPane = new ShortcutPane();
    public final InfoPane infoPane = new InfoPane();
    public final Guide guide = new Guide();

    /**
     * Actions ran just before application stopping.
     * <p>
     * At the time of execution all parts of application are fully operational, i.e., the stopping
     * has not started yet. However, this assumption is valid only for operations on fx thread.
     * Simply put, do not run any more background tasks, as the application will begin closing in
     * the meantime and be in inconsistent state.
     */
    public final RunnableSet onStop = new RunnableSet();
    public boolean normalLoad = true;
    public boolean initialized = false;
    public final WindowManager windowManager = new WindowManager();
    public final WidgetManager widgetManager = new WidgetManager();
    public final ServiceManager services = new ServiceManager();
    public final PluginMap plugins = new PluginMap();

    public final ClassName className = new ClassName();
    public final InstanceName instanceName = new InstanceName();
    public final InstanceInfo instanceInfo = new InstanceInfo();


    @IsConfig(name = "Rating control.", info = "The style of the graphics of the rating control.")
    public final VarEnum<RatingCellFactory> ratingCell = new VarEnum<>(new TextStarRatingCellFactory(),
            () -> plugins.getPlugins(RatingCellFactory.class));

    @IsConfig(name = "Rating icon amount", info = "Number of icons in rating control.", min = 1, max = 10)
    public final V<Integer> maxRating = new V<>(5);

    @IsConfig(name = "Rating allow partial", info = "Allow partial values for rating.")
    public final V<Boolean> partialRating = new V<>(true);

    @IsConfig(name = "Rating editable", info = "Allow change of rating. Defaults to application settings")
    public final V<Boolean> allowRatingChange = new V<>(true);

    @IsConfig(name = "Rating react on hover", info = "Move rating according to mouse when hovering.")
    public final V<Boolean> hoverRating = new V<>(true);

    @IsConfig(name = "Debug value (double)", info = "For application testing. Generic number value"
            + "to control some application value manually.")
    public final V<Double> debug = new V<>(0d, () -> {});

    @IsConfig(name = "Debug value (boolean)", info = "For application testing. Generic yes/false value"
            + "to control some application value manually.")
    public final V<Boolean> debug2 = new V<>(false,() -> {});

    @IsConfig(name = "Enabled", group = "Taskbar", info = "Show taskbar icon. Disabling taskbar will"
            + "also disable ALT+TAB functionality.")
    public final V<Boolean> taskbarEnabled = new V<>(true,taskbarIcon::setVisible);

    @IsConfig(info = "Preffered text when no tag value for field. This value is overridable.")
    public String TAG_NO_VALUE = "<none>";

    @IsConfig(info = "Preffered text when multiple tag values per field. This value is overridable.")
    public String TAG_MULTIPLE_VALUE = "<multi>";


    private boolean close_prematurely = false;

    public App() {
        if(APP==null) APP = this;
        else throw new RuntimeException("Multiple application instances disallowed");
    }

    /**
     * The application initialization method. This method is called immediately
     * after the Application class is loaded and constructed. An application may
     * override this method to perform initialization prior to the actual starting
     * of the application.
     * <p>
     * NOTE: This method is not called on the JavaFX Application Thread. An
     * application must not construct a Scene or a Stage in this method. An
     * application may construct other JavaFX objects in this method.
     */
    @Override
    public void init() {
        // configure logging
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator jc = new JoranConfigurator();
            jc.setContext(lc);
            lc.reset();
            lc.putProperty("LOG_DIR", DIR_LOG.getPath());
            // override default configuration
            jc.doConfigure(FILE_LOG_CONFIG);
        } catch (JoranException ex) {
            LOGGER.error(ex.getMessage());
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

        // log uncaught thread termination exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread,ex) -> LOGGER.error(thread.getName(), ex));

        // mark app instance so other instances can recognize it. !work so far
//        try {
//            String id = String.valueOf(ProcessHandle.current().getPid());
//            VirtualMachine vm = VirtualMachine.attach(id);
//            vm.getAgentProperties().put("apptype", "somevalue");
//            vm.getAgentProperties().forEach((key,val) -> System.out.println(" vm key: " + key + ": " + val));
//            System.out.println("isntnull " + vm.getAgentProperties().get("apptype"));
//            vm.detach();
//        } catch (AttachNotSupportedException|IOException ex) {
//            LOGGER.error("Failed to mark application instance");
//        }

        // Forbid multiple application instances, instead notify the 1st instance of 2nd (this one)
        // trying to run and this instance's run parameters and close prematurely
        if(getInstances()>1) {
            appCommunicator.fireNewInstanceEvent(fetchParameters());
            close_prematurely = true;
            LOGGER.info("Multiple app instances detected. App wil close prematurely.");
        }
        if(close_prematurely) return;

        // configure serialization
        XStream x = serializators.x;
        Mapper xm = x.getMapper();
        x.autodetectAnnotations(true);
        x.registerConverter(new StringPropertyConverter(xm));
        x.registerConverter(new BooleanPropertyConverter(xm));
        x.registerConverter(new ObjectPropertyConverter(xm));
        x.registerConverter(new DoublePropertyConverter(xm));
        x.registerConverter(new LongPropertyConverter(xm));
        x.registerConverter(new IntegerPropertyConverter(xm));
        x.registerConverter(new Window.WindowConverter());
        x.registerConverter(new PlaybackStateConverter());
        x.registerConverter(new PlaylistItemConverter());
        // x.registerConverter(new ObservableListConverter(xm)); // interferes with Playlist.class
        x.omitField(ObservableListBase.class, "listenerHelper");
        x.omitField(ObservableListBase.class, "changeBuilder");
        x.omitField(ObservableListWrapper.class, "elementObserver");
        x.alias("Component", Component.class);
        x.alias("Playlist", Playlist.class);
        x.alias("item", PlaylistItem.class);
        ClassIndex.getSubclasses(Component.class).forEach(c -> x.alias(c.getSimpleName(),c));
        x.useAttributeFor(Component.class, "id");
        x.useAttributeFor(Widget.class, "name");
        x.useAttributeFor(Playlist.class, "id");

        // add optional object class -> string converters
        className.add(Item.class, "Song");
        className.add(PlaylistItem.class, "Playlist Song");
        className.add(Metadata.class, "Library Song");
        className.add(MetadataGroup.class, "Song Group");
        className.add(List.class, "List");

        // add optional object instance -> string converters
        instanceName.add(Void.class, o -> "none");
        instanceName.add(Item.class, Item::getPath);
        instanceName.add(PlaylistItem.class, PlaylistItem::getTitle);
        instanceName.add(Metadata.class,Metadata::getTitle);
        instanceName.add(MetadataGroup.class, o -> Objects.toString(o.getValue()));
        instanceName.add(Component.class, o -> o.getName());
//        instanceName.add(List.class, o -> o.size() + " " + plural("item",o.size()));
        instanceName.add(List.class, o -> {
            Class<?> ec = getGenericPropertyType(o.getClass());
            return o.size() + " " + plural(className.get(ec == null ? Object.class : ec),o.size());
                });
        instanceName.add(File.class, File::getPath);

        // add optional object instance -> info string converters
        instanceInfo.add(File.class, o -> {
            HashMap<String,String> m = new HashMap<>();

            FileSize fs = new FileSize(o);
            m.put("Size", fs.toString() + " (" + String.format("%,d ", fs.inBytes()).replace(',', ' ') + "bytes)");
            m.put("Format", FileUtil.getSuffix(o));

            ImageFileFormat iff = ImageFileFormat.of(o.toURI());
            if(iff.isSupported()) {
                Dimension id = getImageDim(o);
                m.put("Resolution", id==null ? "n/a" : id.width + " x " + id.height);
            }
            return m;
        });
        instanceInfo.add(Metadata.class, m -> {
            HashMap<String,String> map = new HashMap<>();
            stream(Metadata.Field.values())
                    .filter(f -> f.isTypeStringRepresentable() && !f.isFieldEmpty(m))
                    .forEach(f ->
                map.put(f.name(), m.getFieldS(f))
            );
            return map;
        });
        instanceInfo.add(PlaylistItem.class, o -> {
            HashMap<String,String> m = new HashMap<>();
            stream(PlaylistItem.Field.values()).filter(f -> f.isTypeString()).forEach(f ->
                m.put(f.name(), (String)o.getField(f))
            );
            return m;
        });

        // register actions
        actionPane.register(Widget.class,
            new FastAction<>("Create launcher (def)","Creates a launcher "
                + "for this widget with default (no predefined) settings. \n"
                + "Opening the launcher with this application will open this "
                + "widget as if it were a standalone application.",
                EXPORT, w -> {
                    DirectoryChooser dc = new DirectoryChooser();
                                     dc.setInitialDirectory(DIR_APP);
                                     dc.setTitle("Export to...");
                    File dir = dc.showDialog(APP.actionPane.getScene().getWindow());
                    if(dir!=null) w.exportFxwlDefault(dir);
            })
        );
        actionPane.register(Component.class,
            new FastAction<>("Export","Creates a launcher "
                + "for this component with current settings. \n"
                + "Opening the launcher with this application will open this "
                + "component as if it were a standalone application.",
                EXPORT, w -> {
                    DirectoryChooser dc = new DirectoryChooser();
                                     dc.setInitialDirectory(DIR_LAYOUTS);
                                     dc.setTitle("Export to...");
                    File dir = dc.showDialog(APP.actionPane.getScene().getWindow());
                    if(dir!=null) w.exportFxwl(dir);
            })
        );
        actionPane.register(Item.class,
            new FastColAction<>("Add to new playlist",
                "Add items to new playlist widget.",
                PLAYLIST_PLUS,
                items -> widgetManager.use(PlaylistFeature.class, NEW, p -> p.getPlaylist().addItems(items))
            ),
            new FastColAction<>("Add to existing playlist",
                "Add items to exsisting playlist widget if possible or to a new one if not.",
                PLAYLIST_PLUS,
                items -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addItems(items))
            ),
            new FastColAction<>("Update from file",
                "Updates library metadata of the specified items from their files. The difference betwee"
                + "database and real metadata information is a result of a bug or file edited externally. "
                + "After this library will contain uptodate metadata for specified items.",
                FontAwesomeIcon.REFRESH,
                Player::refreshItems // this needs to be asynchronous
            ),
            new FastColAction<>("Remove from library",
                "Removes all specified items from library. After this library will contain none of these items.",
                MaterialDesignIcon.DATABASE_MINUS,
                DB::removeItems // id like this to be async. too
            )
        );
        actionPane.register(File.class,
            new FastAction<>("Open (OS)", "Opens file in a native program associated with this file type.",
                MaterialIcon.OPEN_IN_NEW,
                Environment::open),
            new FastAction<>("Edit (OS)", "Edit file in a native editor program associated with this file type.",
                FontAwesomeIcon.EDIT,
                Environment::edit),
            new FastAction<>("Browse (OS)", "Browse file in a native file system browser.",
                FontAwesomeIcon.FOLDER_OPEN_ALT,
                Environment::browse),
            new FastColAction<>("Add to new playlist",
                "Add items to exsisting playlist widget if possible or to a new one if not.",
                PLAYLIST_PLUS,
                f -> AudioFileFormat.isSupported(f, Use.APP),
                f -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addFiles(f))
            ),
            new SlowColAction<>("Add to library",
                "Add items to library if not yet contained.",
                MaterialDesignIcon.DATABASE_PLUS,
                f -> AudioFileFormat.isSupported(f, Use.APP),
                items -> MetadataReader.readAaddMetadata(map(items,SimpleItem::new), (ok,added) -> {}, false)
                                       .run()
            ),
            new SlowColAction<>("Edit & Add to library",
                "Add items to library if not yet contained and edit added items in tag editor. If "
                + "item already was in the database it will not be added or edited.",
                MaterialDesignIcon.DATABASE_PLUS,
                f -> AudioFileFormat.isSupported(f, Use.APP),
                items -> MetadataReader.readAaddMetadata(map(items,SimpleItem::new), (ok,added) -> {
                            if(ok && !added.isEmpty())
                                 APP.widgetManager.use(SongWriter.class, NO_LAYOUT, w -> w.read(added));
                        }, false).run()
            ),
            new FastColAction<>("Add to existing playlist",
                "Add items to new playlist widget.",
                PLAYLIST_PLUS,
                f -> AudioFileFormat.isSupported(f, Use.APP),
                f -> widgetManager.use(PlaylistFeature.class, NEW, p -> p.getPlaylist().addFiles(f))
            ),
            new FastAction<>("Apply skin", "Apply skin on the application.",
                BRUSH,
                FileUtil::isValidSkinFile,
                skin_file -> GUI.setSkin(FileUtil.getName(skin_file))),
            new FastAction<>("View image", "Opens image in an image viewer widget.",
                IMAGE,
                ImageFileFormat::isSupported,
                img_file -> widgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w -> w.showImage(img_file))
            ),
            new FastColAction<>("View image", "Opens image in an image browser widget.",
                IMAGE,
                ImageFileFormat::isSupported,
                img_files -> widgetManager.use(ImagesDisplayFeature.class, NO_LAYOUT, w -> w.showImages(img_files))
            ),
            new FastAction<>("Open widget", "Opens exported widget.",
                IMPORT,
                f -> f.getPath().endsWith(".fxwl"),
                UiContext::launchComponent
            )
        );

        // add app parameter handlers
        parameterProcessor.addFileProcessor(
            f -> AudioFileFormat.isSupported(f, Use.APP),
            fs -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addFiles(fs))
        );
        parameterProcessor.addFileProcessor(
            FileUtil::isValidSkinFile,
            fs -> GUI.setSkin(FileUtil.getName(fs.get(0)))
        );
        parameterProcessor.addFileProcessor(
            ImageFileFormat::isSupported,
            fs -> widgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w->w.showImages(fs))
        );
        parameterProcessor.addFileProcessor(f -> f.getPath().endsWith(".fxwl"),
            fs -> fs.forEach(UiContext::launchComponent)
        );
        parameterProcessor.addStringProcessor(
            s -> widgetManager.getFactories().anyMatch(f -> f.nameGui().equals(s) || f.name().equals(s)),
            ws -> ws.forEach(UiContext::launchComponent)
        );

        // listen to other application instance launches
        try {
            appCommunicator.start();
            // process app parameters of newly started instance
            appCommunicator.onNewInstanceHandlers.add(parameterProcessor::process);
        } catch (RemoteException e) {
            LOGGER.warn("App instance communicator failed to start.", e);
        }

        // start global shortcuts
        Action.startGlobalListening();

        // custom tooltip behavior
        setupCustomTooltipBehavior(1000, 10000, 200);
    }

    /**
     * The main entry point for applications. The start method is
     * called after the init method has returned, and after the system is ready
     * for the application to begin running.
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set. The primary stage will be embedded in
     * the browser if the application was launched as an applet. Applications
     * may create other stages, if needed, but they will not be primary stages
     * and will not be embedded in the browser.
     */
    @Override
    public void start(Stage primaryStage) {
        if(close_prematurely) {
            LOGGER.info("Application closing prematurely.");
            close();
            return;
        }

        try {
            taskbarIcon.setTitle(name);
            taskbarIcon.setIcon(getIcon());
            taskbarIcon.setOnClose(this::close);
            taskbarIcon.setOnMinimize(v -> WINDOWS.forEach(w -> w.setMinimized(v)));
            taskbarIcon.setOnAltTab(() -> {
                boolean apphasfocus = Window.getFocused()!=null;
                if(!apphasfocus) {
                    boolean allminimized = WINDOWS.stream().allMatch(Window::isMinimized);
                    if(allminimized)
                        WINDOWS.stream().forEach(w -> w.setMinimized(false));
                    else
                        WINDOWS.stream().filter(w -> w.isShowing()).forEach(Window::focus);
                }
            });

            // create window owner - all 'top' windows are owned by it
            windowOwner = Window.createWindowOwner();
            windowOwner.show();

            // discover plugins
            ClassIndex.getAnnotated(IsPluginType.class).forEach(plugins::registerPluginType);
            ClassIndex.getAnnotated(IsPlugin.class).forEach(plugins::registerPlugin);

            widgetManager.initialize();

            // services must be created before Configuration
            services.addService(new TrayService());
            services.addService(new Notifier());
            services.addService(new PlaycountIncrementer());
            services.addService(new ClickEffect());

            // gather configs
            configuration.collectStatic();
            services.forEach(configuration::collect);
            configuration.collect(this, windowManager, guide, actionPane);
            configuration.collectComplete();
            // deserialize values (some configs need to apply it, will do when ready)
            configuration.load(FILE_SETTINGS);

            // initializing, the order is important
            Player.initialize();

            List<String> ps = fetchParameters();
            normalLoad = ps.stream().noneMatch(s -> s.endsWith(".fxwl") || widgetManager.factories.get(s)!=null);

            // use for faster widget testing
            // Set project to run with application parameters and use widget name
            // This will only load the widget and start app faster
            // normalLoad = false;

            // load windows, layouts, widgets
            // we must apply skin before we load graphics, solely because if skin defines custom
            // Control Skins, it will only have effect when set before control is created
            // and yes, this means reapplying diferent skin will have no effect in this regard...
            configuration.getFields(f -> f.getGroup().equals("GUI") && f.getGuiName().equals("Skin")).get(0).applyValue();
            windowManager.deserialize(normalLoad);

            DB.start();

            initialized = true;

        } catch(Exception e) {
            LOGGER.info("Application failed to start", e);
            LOGGER.error("Application failed to start", e);
        }

        // initialization is complete -> apply all settings
        configuration.getFields().forEach(Config::applyValue);

        // initialize non critical parts
        if(normalLoad) Player.loadLast();

        // show guide
        if(guide.first_time) run(3000, guide::start);

        // get rid of this, load from skins
        Image image = new Image(new File("cursor.png").getAbsoluteFile().toURI().toString());  // pass in the image path
        ImageCursor c = new ImageCursor(image,3,3);
        window.getStage().getScene().setCursor(c);

        // process app parameters passed when app started
        parameterProcessor.process(fetchParameters());
    }

    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     * NOTE: This method is called on the JavaFX Application Thread.
     */
    @Override
    public void stop() {
        if(initialized) {
            onStop.run();
            if(normalLoad) Player.state.serialize();
            if(normalLoad) windowManager.serialize();
            configuration.save(name,FILE_SETTINGS);
            services.getAllServices()
                    .filter(Service::isRunning)
                    .forEach(Service::stop);
        }
        DB.stop();
        Action.stopGlobalListening();
        appCommunicator.stop();
    }

    /** Forces application to stop. Invokes {@link #stop()} as a result. */
    public void close() {
        // close app
        Platform.exit();
    }

    public <S extends Service> void use(Class<S> type, Consumer<S> action) {
        services.getService(type).filter(Service::isRunning).ifPresent(action);
    }

/******************************************************************************/

    public List<String> fetchParameters() {
        List<String> params = new ArrayList<>();
        getParameters().getRaw().forEach(params::add);
        getParameters().getUnnamed().forEach(params::add);
        getParameters().getNamed().forEach((t,value) -> params.add(value) );
        return params;
    }

    /**
     * Calculates number of instances of this application running on this
     * system at this moment. In this context, application instance is considered
     * a application running on java virtual machine from user directory equal
     * to that of this application. This application is included in the count.
     *
     * @return number of running instances
     */
    public static int getInstances() {
        // Old impl. Obviously not working as it is. Left here for documentation sake as a resource.
        // try {
        //     MonitoredHost mh = MonitoredHost.getMonitoredHost("//localhost" );
        //     for (int id : mh.activeVms() ) {
        //         VmIdentifier vmid = new VmIdentifier("" + id);
        //         MonitoredVm vm = mh.getMonitoredVm(vmid, 0 );
        //         System.out.printf( "%d %s %s %s%n\n", id,MonitoredVmUtil.mainClass( vm, true ),
        //                            MonitoredVmUtil.jvmArgs( vm ),MonitoredVmUtil.mainArgs( vm ) );
        //     }
        // } catch (MonitorException ex) {
        //     java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        // } catch (URISyntaxException ex) {
        //     java.util.logging.Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        // }
        // two occurrences of the result of MonitoredVmUtil.mainClass(), the program is started twice

        // We list all virtual machines (including this one) (each java app runs in its own vm) and
        // check some property to determine what kind of app this is to count instances of this app.
        //
        // Note: I tried to inject some custom property into vm using
        // vm.getAgentProeprties().setProperty(...)  but it appears to be read only. So we handle
        // the recognition differently.
        int instances = 0;
//        String ud = System.getProperty("user.dir");
        for(VirtualMachineDescriptor vmd : VirtualMachine.list()) {
            try {
                VirtualMachine vm = VirtualMachine.attach(vmd);

                // attempt 1:
                // User directory. Unfortunately nothing forbids user (or more likely
                // developer) to copypaste the app and run it from elsewhere). We want to
                // defend against this as well.
                // String udir = vm.getSystemProperties().getProperty("user.dir");
                //if(udir!=null && ud.equals(udir)) i++;

                // attempt 2:
                // Injected custom property, !work. Read-only? Id like this to work though.
                // if(vm.getAgentProperties().getProperty("apptype")!=null) i++;

                // attempt3:
                // We use command parameter which ends with the name of the executed jar. So far
                // this works. Its far from perfect, since user/dev could rename the jar.
                String command = vm.getAgentProperties().getProperty("sun.java.command");
                if(command!=null && command.endsWith("Player.jar")) instances++;

                vm.detach();
            } catch (AttachNotSupportedException | IOException ex) {
                LOGGER.warn("Unable to inspect virtual machine {}", vmd);
            }
        }
        return instances;
    }

    @Deprecated
    @TODO(purpose = FUNCTIONALITY, note="broken, might work only in dev mode & break if path has spaces")
    private static File obtainAppSourceJar() {
        // see: http://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
        try {
            return new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            LOGGER.error("Failed to obtain application source directory", e);
            return null;
        }
    }

    /** @return image of the icon of the application. */
    public Image getIcon() {
        return new Image(new File("icon512.png").toURI().toString());
    }

    /** @return absolute file of Location of data. */
    public static File DATA_FOLDER() {
        return new File("UserData").getAbsoluteFile();
    }

    /**
     * @return absolute file of Location of database. */
    public static File LIBRARY_FOLDER() {
        return new File(DATA_FOLDER(), "Library");
    }

    /** @return absolute file of Location of saved playlists. */
    public static File PLAYLIST_FOLDER() {
        return new File(DATA_FOLDER(),"Playlists");
    }


    // jobs

    public static void refreshItemsFromFileJob(List<? extends Item> items) {
        Fut.fut()
//           .then(() -> Player.refreshItemsWith(MetadataReader.readMetadata(items)),Player.IO_THREAD)
           .then(() -> {
               List<Metadata> l = MetadataReader.readMetadata(items);
               l.forEach(m -> System.out.println(m.getCustom5()));
               Player.refreshItemsWith(l);
           },Player.IO_THREAD)
           .showProgress(APP.window.taskAdd())
           .run();
    }

    public static void itemToMeta(Item i, Consumer<Metadata> action) {
       if (i.same(Player.playingtem.get())) {
           action.accept(Player.playingtem.get());
           return;
       }

       Metadata m = DB.items_byId.get(i.getId());
       if(m!=null) {
           action.accept(m);
       } else {
            Fut.fut(i).map(MetadataReader::create,Player.IO_THREAD)
                      .use(action, FX).run();
       }
    }

//    public static Metadata itemToMeta(Item i) {
//       if (i.same(Player.playingtem.get()))
//           return Player.playingtem.get();
//
//       Metadata m = DB.items_byId.get(i.getId());
//       return m==null ? MetadataReader.create(i) : m;
//    }

/************************************ actions *********************************/

    @IsAction(name = "Open on github", desc = "Opens github page for this application. For developers.")
    public static void openAppGithubPage() {
        browse(APP.GITHUB_URI);
    }

    @IsAction(name = "Open app directory", desc = "Opens directory from which this application is "
            + "running from.")
    public static void openAppLocation() {
        browse(APP.DIR_APP);
    }

    @IsAction(name = "Open css guide", desc = "Opens css reference guide. For developers.")
    public static void openCssGuide() {
        browse(URI.create("http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html"));
    }

    @IsAction(name = "Open icon viewer", desc = "Opens application icon browser. For developers.")
    public static void openIconViewer() {
        StackPane root = new StackPane();
                  root.setPrefSize(600, 720);
        List<Button> typeicons = stream(FontAwesomeIcon.class,WeatherIcon.class,OctIcon.class,
                                      MaterialDesignIcon.class,MaterialIcon.class)
                .map((Class<?> c) -> {
                    Button b = new Button(c.getSimpleName());
                    b.setOnMouseClicked(e -> {
                        if(e.getButton()==PRIMARY) {
                            if(b.getUserData()!=null) {
                                root.getChildren().setAll((ScrollPane)b.getUserData());
                            } else {
                                CellPane cp = new CellPane(70,80,5);
                                ScrollPane sp = cp.scrollable();
                                b.setUserData(sp);
                                runFX(() -> root.getChildren().setAll(sp));
                                Fut.fut()
                                   .then(() ->
                                       forEachAfter(2, map(getEnumConstants(c),i -> new IconInfo((GlyphIcons)i,55)), i -> {
                                           runFX(() -> {
                                               cp.getChildren().add(i);
                                               new Anim(i::setOpacity).dur(500).intpl(x -> sqrt(x)).play(); // animate
                                           });
                                       })
                                   )
                                   .showProgress(Window.getActive().taskAdd())
                                   .run();
                            }
                            e.consume();
                        }
                    });
                    return b;
                }).collect(toList());
        PopOver o = new PopOver(layVertically(20,TOP_CENTER,layHorizontally(8,CENTER,typeicons), root));
                o.show(App_Center);
    }

    @IsAction(name = "Open settings", desc = "Opens application settings.")
    public static void openSettings() {
        APP.widgetManager.use(ConfiguringFeature.class, WidgetSource.NO_LAYOUT, c -> c.configure(APP.configuration.getFields()));
    }

    @IsAction(name = "Open layout manager", desc = "Opens layout management widget.")
    public static void openLayoutManager() {
        APP.widgetManager.find("Layouts", WidgetSource.NO_LAYOUT, false);
    }

    @IsAction(name = "Open guide", desc = "Resume or start the guide.")
    public static void openGuide() {
        APP.guide.open();
    }

    @IsAction(name = "Open app actions", desc = "Actions specific to whole application.")
    public static void openActions() {
        APP.actionPane.show(Void.class, null, false,
            new FastAction<>(
                "Export widgets",
                "Creates launcher file in the destination directory for every widget.\n"
                + "Launcher file is a file that when opened by this application opens the widget. "
                + "If application was not running before, it will not load normally, "
                + "but will only open the widget.\n"
                + "but will only open the widget.\n"
                + "Essentially, this exports the widgets as 'standalone' applications",
                EXPORT,
                ignored -> {
                    DirectoryChooser dc = new DirectoryChooser();
                                     dc.setInitialDirectory(APP.DIR_LAYOUTS);
                                     dc.setTitle("Export to...");
                    File dir = dc.showDialog(APP.actionPane.getScene().getWindow());
                    if(dir!=null) {
                        APP.widgetManager.getFactories().forEach(w -> w.create().exportFxwlDefault(dir));
                    }
                }
            ),
            new FastAction<>(KEYBOARD_VARIANT,Action.get("Show shortcuts")),
            new FastAction<>(INFORMATION_OUTLINE,Action.get("Show system info")),
            new FastAction<>(GITHUB,Action.get("Open on github")),
            new FastAction<>(CSS3,Action.get("Open css guide")),
            new FastAction<>(IMAGE,Action.get("Open icon viewer")),
            new FastAction<>(FOLDER,Action.get("Open app directory"))
        );
    }

    @IsAction(name = "Open...", desc = "Opens all possible open actions.", keys = "CTRL + SHIFT + O", global = true)
    public static void openOpen() {
        APP.actionPane.show(Void.class, null, false,
            new FastAction<>(
                "Open widget",
                "Open file chooser to open an exported widget",
                MaterialIcon.WIDGETS,
                none -> {
                    FileChooser fc = new FileChooser();
                                fc.setInitialDirectory(APP.DIR_LAYOUTS);
                                fc.getExtensionFilters().add(new ExtensionFilter("skin file","*.fxwl"));
                                fc.setTitle("Open widget...");
                    File f = fc.showOpenDialog(APP.actionPane.getScene().getWindow());
                    if(f!=null) UiContext.launchComponent(f);
                }
            ),
            new FastAction<>(
                "Open skin",
                "Open file chooser to find a skin",
                MaterialIcon.BRUSH,
                none -> {
                    FileChooser fc = new FileChooser();
                                fc.setInitialDirectory(APP.DIR_SKINS);
                                fc.getExtensionFilters().add(new ExtensionFilter("skin file","*.css"));
                                fc.setTitle("Open skin...");
                    File f = fc.showOpenDialog(APP.actionPane.getScene().getWindow());
                    if(f!=null) GUI.setSkinExternal(f);
                }
            ),
            new FastAction<>(
                "Open audio files",
                "Open file chooser to find a audio files",
                MaterialDesignIcon.MUSIC_NOTE,
                none -> {
                    FileChooser fc = new FileChooser();
                                fc.setInitialDirectory(APP.DIR_SKINS);
                                fc.getExtensionFilters().addAll(map(AudioFileFormat.supportedValues(Use.APP),f -> f.toExtFilter()));
                                fc.setTitle("Open audio...");
                    List<File> fs = fc.showOpenMultipleDialog(APP.actionPane.getScene().getWindow());
                    // Action pane may autoclose when this action finishes, so we make sure to call
                    // show() after that happens by delaying using runLater
                    if(fs!=null) runLater(() -> APP.actionPane.show(fs));
                }
            )
        );
    }

    @IsAction(name = "Show shortcuts", desc = "Display all available shortcuts.", keys = "?")
    public static void showShortcuts() {
        APP.shortcutPane.show();
    }

    @IsAction(name = "Show system info", desc = "Display system information.")
    public static void showSysInfo() {
        APP.actionPane.hide();
        APP.infoPane.show();
    }

}