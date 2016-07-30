package main;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;

import org.atteo.classindex.ClassIndex;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.Mapper;

import audio.Item;
import audio.Player;
import audio.SimpleItem;
import audio.playlist.Playlist;
import audio.playlist.PlaylistItem;
import audio.tagging.Metadata;
import audio.tagging.MetadataGroup;
import audio.tagging.MetadataReader;
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
import gui.Gui;
import gui.objects.grid.GridCell;
import gui.objects.grid.GridView;
import gui.objects.grid.GridView.SelectionOn;
import gui.objects.icon.Icon;
import gui.objects.icon.IconInfo;
import gui.objects.popover.PopOver;
import gui.objects.spinner.Spinner;
import gui.objects.tablecell.RatingCellFactory;
import gui.objects.tablecell.TextStarRatingCellFactory;
import gui.objects.textfield.DecoratedTextField;
import gui.objects.textfield.autocomplete.ConfigSearch;
import gui.objects.window.stage.UiContext;
import gui.objects.window.stage.Window;
import gui.objects.window.stage.WindowBase;
import gui.objects.window.stage.WindowManager;
import gui.pane.ActionPane;
import gui.pane.ActionPane.FastAction;
import gui.pane.ActionPane.FastColAction;
import gui.pane.ActionPane.SlowColAction;
import gui.pane.InfoPane;
import gui.pane.OverlayPane;
import gui.pane.ShortcutPane;
import layout.Component;
import layout.area.ContainerNode;
import layout.widget.Widget;
import layout.widget.WidgetManager;
import layout.widget.WidgetManager.WidgetSource;
import layout.widget.feature.*;
import services.ClickEffect;
import services.Service;
import services.ServiceManager;
import services.database.Db;
import services.notif.Notifier;
import services.playcount.PlaycountIncrementer;
import services.tray.TrayService;
import unused.SimpleConfigurator;
import util.access.TypedValue;
import util.access.V;
import util.access.VarEnum;
import util.access.fieldvalue.FileField;
import util.access.fieldvalue.ObjectField;
import util.action.Action;
import util.action.IsAction;
import util.action.IsActionable;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.future.Fut;
import util.conf.*;
import util.dev.TODO;
import util.file.AudioFileFormat;
import util.file.AudioFileFormat.Use;
import util.file.Environment;
import util.file.ImageFileFormat;
import util.file.Util;
import util.file.mimetype.MimeTypes;
import util.functional.Functors;
import util.graphics.MouseCapture;
import util.plugin.IsPlugin;
import util.plugin.IsPluginType;
import util.plugin.PluginMap;
import util.reactive.SetƑ;
import util.serialize.xstream.*;
import util.system.SystemOutListener;
import util.type.ClassName;
import util.type.InstanceInfo;
import util.type.InstanceName;
import util.type.ObjectFieldMap;
import util.units.FileSize;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FOLDER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.*;
import static gui.objects.popover.PopOver.ScreenPos.App_Center;
import static gui.pane.OverlayPane.Display.SCREEN_OF_MOUSE;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.TOP_CENTER;
import static javafx.scene.control.PopupControl.USE_COMPUTED_SIZE;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.paint.Color.BLACK;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static layout.widget.WidgetManager.WidgetSource.*;
import static layout.widget.WidgetManager.WidgetSource.NEW;
import static org.atteo.evo.inflector.English.plural;
import static util.Util.getImageDim;
import static util.async.Async.*;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.file.Environment.browse;
import static util.functional.Util.*;
import static util.graphics.Util.*;
import static util.type.Util.getEnumConstants;

/**
 * Application. Represents the program.
 * <p/>
 * Single instance:<br>
 * Application can only instantiate single instance, any subsequent call to constructor throws
 * an exception.
 *
 * @author Martin Polakovic
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
        // LauncherImpl.launchApplication(App.class, preloaderClass, args); launch with preloader
    }


    /** Name of this application. */
    public final String name = "PlayerFX";

    /** Application code encoding. Useful for compilation during runtime. */
    public final Charset encoding = StandardCharsets.UTF_8;
    /** Uri for github website for project of this application. */
    public final URI GITHUB_URI = URI.create("https://www.github.com/sghpjuikit/player/");

    /** Absolute file of directory of this app. Equivalent to new File("").getAbsoluteFile(). */
    public final File DIR_APP = new File("").getAbsoluteFile();
    /** Temporary directory of the os. */
    public final File DIR_TEMP = new File(System.getProperty("java.io.tmpdir"));
    /** Home directory of the os. */
    public final File DIR_HOME = new File(System.getProperty("user.home"));
    /** Directory for application logging. */
    public final File DIR_LOG = new File(DIR_APP, "log");
    /** File for application logging configuration. */
    public final File FILE_LOG_CONFIG = new File(DIR_LOG, "log_configuration.xml");
    /** Directory containing widgets - source files, class files and widget's resources. */
    public final File DIR_WIDGETS = new File(DIR_APP, "widgets");
    /** Directory containing skins. */
    public final File DIR_SKINS = new File(DIR_APP, "skins");
    /** Directory containing user data created by application usage, such as customizations, song library, etc. */
    public final File DIR_USERDATA =  new File(DIR_APP, "user");
    /** Directory containing library database. */
    public final File DIR_LIBRARY = new File(DIR_USERDATA, "library");
    public final File DIR_LAYOUTS = new File(DIR_USERDATA, "layouts");
    /** Directory containing playlists. */
    public final File DIR_PLAYLISTS = new File(DIR_USERDATA, "playlists");
    /** Directory containing application resources. */
    public final File DIR_RESOURCES = new File(DIR_APP, "resources");
    /** File for application configuration. */
    public final File FILE_SETTINGS = new File(DIR_USERDATA, "application.properties");

    /**
     * File mime type map.
     * Initialized with the built-in mime types definitions.
     */
    public final MimeTypes mimeTypes = MimeTypes.standard();

    /**
     * Event source and stream for executed actions, providing their name. Use
     * for notifications of running the action or executing additional behavior.
     * <p/>
     * A use case could be an application wizard asking user to do something.
     * The code in question simply notifies this stream of the name of action
     * upon execution. The wizard would then monitor this stream
     * and get notified if the expected action was executed.
     * <p/>
     * Running an {@link Action} always fires an event.
     * Supports custom actions. Simply push a String value into the stream.
     */
    public final EventSource<String> actionStream = new EventSource<>();
    public final AppInstanceComm appCommunicator = new AppInstanceComm();
    public final AppParameterProcessor parameterProcessor = new AppParameterProcessor();
    public final AppSerializer serializators = new AppSerializer(encoding);
    public final Configuration configuration = new Configuration();
    public final MouseCapture mouseCapture = new MouseCapture();
    /** {@link System#out} provider. Allows multiple parties to observe it.. */
    public final SystemOutListener systemout = new SystemOutListener();

    public Window window;
    public Window windowOwner;
    public final TaskBar taskbarIcon = new TaskBar();
    public final ActionPane actionPane = new ActionPane();
    public final ShortcutPane shortcutPane = new ShortcutPane();
    public final InfoPane infoPane = new InfoPane();
    public final Guide guide = new Guide();
    private final ConfigSearch.History configSearchHistory = new ConfigSearch.History();

    /**
     * Actions ran just before application stopping.
     * <p/>
     * At the time of execution all parts of application are fully operational, i.e., the stopping
     * has not started yet. However, this assumption is valid only for operations on fx thread.
     * Simply put, do not run any more background tasks, as the application will begin closing in
     * the meantime and be in inconsistent state.
     */
    public final SetƑ onStop = new SetƑ();
    public boolean normalLoad = true;
    public boolean initialized = false;
    private boolean close_prematurely = false;

    public final WindowManager windowManager = new WindowManager();
    public final WidgetManager widgetManager = new WidgetManager(windowManager);
    public final ServiceManager services = new ServiceManager();
    public final PluginMap plugins = new PluginMap();

    public final ClassName className = new ClassName();
    public final InstanceName instanceName = new InstanceName();
    public final InstanceInfo instanceInfo = new InstanceInfo();
    public final ObjectFieldMap classFields = new ObjectFieldMap();

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

    @IsConfig(info = "Preferred text when no tag value for field. This value can be overridden.")
    public String TAG_NO_VALUE = "<none>";

    @IsConfig(info = "Preferred text when multiple tag values per field. This value can be overridden.")
    public String TAG_MULTIPLE_VALUE = "<multi>";


    public App() {
        if(APP==null) APP = this;
        else throw new RuntimeException("Multiple application instances disallowed");
    }

    /**
     * The application initialization method. This method is called immediately
     * after the Application class is loaded and constructed. An application may
     * override this method to perform initialization prior to the actual starting
     * of the application.
     * <p/>
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
        Thread.setDefaultUncaughtExceptionHandler((thread,ex) -> LOGGER.error("Uncaught exception", ex));

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
        x.registerConverter(new StringPropertyConverter(xm));   // javafx properties
        x.registerConverter(new BooleanPropertyConverter(xm));  // -||-
        x.registerConverter(new DoublePropertyConverter(xm));   // -||-
        x.registerConverter(new LongPropertyConverter(xm));     // -||-
        x.registerConverter(new IntegerPropertyConverter(xm));  // -||-
        x.registerConverter(new ObjectPropertyConverter(xm));   // -||-
	    x.registerConverter(new ObservableListConverter(xm));   // -||-
	    x.registerConverter(new PlaylistConverter(xm));
        x.registerConverter(new VConverter(xm));
        x.registerConverter(windowManager.new WindowConverter());
        x.registerConverter(new PlaybackStateConverter());
        x.registerConverter(new PlaylistItemConverter());
        x.alias("Component", Component.class);
        x.alias("Playlist", Playlist.class);
        x.alias("item", PlaylistItem.class);
        ClassIndex.getSubclasses(Component.class).forEach(c -> x.alias(c.getSimpleName(),c));
        x.useAttributeFor(Component.class, "id");
        x.useAttributeFor(Widget.class, "name");

        // add optional object fields
        classFields.add(PlaylistItem.class, set(getEnumConstants(PlaylistItem.Field.class)));
        classFields.add(Metadata.class, set(getEnumConstants(Metadata.Field.class)));
        classFields.add(MetadataGroup.class, set(getEnumConstants(MetadataGroup.Field.class)));
        classFields.add(File.class, set(getEnumConstants(FileField.class)));

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
        instanceName.add(Metadata.class, Metadata::getTitle);
        instanceName.add(MetadataGroup.class, o -> Objects.toString(o.getValue()));
        instanceName.add(Component.class, Component::getName);
        instanceName.add(File.class, File::getPath);
        instanceName.add(Collection.class, o -> {
            Class<?> eType = util.type.Util.getGenericPropertyType(o.getClass());
            String eName = eType == null || eType==Object.class ? "Item" : className.get(eType);
            return o.size() + " " + plural(eName,o.size());
        });

        // add optional object instance -> info string converters
        instanceInfo.add(File.class, (f,map) -> {
            FileSize fs = new FileSize(f);
            map.put("Size", fs.toString() + " (" + String.format("%,d ", fs.inBytes()).replace(',', ' ') + "bytes)");
            map.put("Format", Util.getSuffix(f));

            ImageFileFormat iff = ImageFileFormat.of(f.toURI());
            if(iff.isSupported()) {
                Dimension id = getImageDim(f);
                map.put("Resolution", id==null ? "n/a" : id.width + " x " + id.height);
            }
        });
        instanceInfo.add(Metadata.class, (m,map) ->
            stream(Metadata.Field.values())
                    .filter(f -> f.isTypeStringRepresentable() && !f.isFieldEmpty(m))
                    .forEach(f -> map.put(f.name(), m.getFieldS(f)))
        );
        instanceInfo.add(PlaylistItem.class, p ->
            stream(PlaylistItem.Field.values())
                    .filter(TypedValue::isTypeString)
                    .toMap(ObjectField::name, f -> (String)f.getOf(p))
        );

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
                "Add items to existing playlist widget if possible or to a new one if not.",
                PLAYLIST_PLUS,
                items -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addItems(items))
            ),
            new FastColAction<>("Update from file",
                "Updates library data for the specified items from their file metadata. The difference between the data "
                + "in the database and real metadata cab be a result of a bug or file edited externally. "
                + "After this, the library will be synchronized with the file data.",
                FontAwesomeIcon.REFRESH,
                Player::refreshItems // this needs to be asynchronous
            ),
            new FastColAction<>("Remove from library",
                "Removes all specified items from library. After this library will contain none of these items.",
                MaterialDesignIcon.DATABASE_MINUS,
                Db::removeItems // this needs to be asynchronous
            )
        );
        actionPane.register(File.class,
            new FastAction<>("Read metadata", "Prints all image metadata to console.",
                MaterialIcon.IMAGE_ASPECT_RATIO,
	            ImageFileFormat::isSupported,
                App.Actions::printAllImageFileMetadata),
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
                "Add items to new playlist widget.",
                PLAYLIST_PLUS,
                f -> AudioFileFormat.isSupported(f, Use.APP),
                f -> widgetManager.use(PlaylistFeature.class, NEW, p -> p.getPlaylist().addFiles(f))
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
                "Add items to existing playlist widget if possible or to a new one if not.",
                PLAYLIST_PLUS,
                f -> AudioFileFormat.isSupported(f, Use.APP),
                f -> widgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addFiles(f))
            ),
            new FastAction<>("Apply skin", "Apply skin on the application.",
                BRUSH,
                Util::isValidSkinFile,
                skin_file -> Gui.setSkin(Util.getName(skin_file))),
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
            Util::isValidSkinFile,
            fs -> Gui.setSkin(Util.getName(fs.get(0)))
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
        Action.startActionListening();
	    Action.loadCommandActions();
    }

    /**
     * The main entry point for applications. The start method is
     * called after the init method has returned, and after the system is ready
     * for the application to begin running.
     * <p/>
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
            taskbarIcon.setOnMinimize(v -> windowManager.windows.forEach(w -> w.setMinimized(v)));
            taskbarIcon.setOnAltTab(() -> {
                boolean apphasfocus = windowManager.getFocused()!=null;
                if(!apphasfocus) {
                    boolean allminimized = windowManager.windows.stream().allMatch(Window::isMinimized);
                    if(allminimized)
                        windowManager.windows.forEach(w -> w.setMinimized(false));
                    else
                        windowManager.windows.stream().filter(WindowBase::isShowing).forEach(Window::focus);
                }
            });

            // create window owner - all 'top' windows are owned by it
            windowOwner = windowManager.createWindowOwner();
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
            configuration.rawAdd(FILE_SETTINGS);
            configuration.collectStatic();
            configuration.collect(Action.getActions());
            services.forEach(configuration::collect);
            configuration.collect(this, windowManager, guide, actionPane);

            // deserialize values (some configs need to apply it, will do when ready)
            configuration.rawSet();

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
            // and yes, this means reapplying different skin will have no effect in this regard...
            configuration.getFields(f -> f.getGroup().equals("Gui") && f.getGuiName().equals("Skin")).get(0).applyValue();
            windowManager.deserialize(normalLoad);

            Db.start();

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
        if(guide.first_time.get()) run(3000, guide::start);

        // get rid of this, load from skins
        runNew(() -> {
            Image image = new Image(new File("cursor.png").getAbsoluteFile().toURI().toString());  // pass in the image path
            ImageCursor c = new ImageCursor(image,3,3);
            runLater(() -> window.getStage().getScene().setCursor(c));
        });

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
        Db.stop();
        Action.stopActionListening();
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
        String ud = System.getProperty("user.dir");
        for(VirtualMachineDescriptor vmd : VirtualMachine.list()) {
            try {
                int i = 0;
                VirtualMachine vm = VirtualMachine.attach(vmd);

                // attempt 1:
                // User directory. Unfortunately nothing forbids user (or more likely
                // developer) to copy-paste the app and run it from elsewhere). We want to
                // defend against this as well.
                String udir = vm.getSystemProperties().getProperty("user.dir");
                if(udir!=null && ud.equals(udir)) i=1;

                // attempt 2:
                // Injected custom property, !work. Read-only? Id like this to work though.
                // if(vm.getAgentProperties().getProperty("apptype")!=null) i++;

                // attempt3:
                // We use command parameter which ends with the name of the executed jar. So far
                // this works. Its far from perfect, since user/dev could rename the jar.
                String command = vm.getAgentProperties().getProperty("sun.java.command");
                if(command!=null && command.endsWith("Player.jar")) i=1;

                instances += i;
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

       Metadata m = Db.items_byId.get(i.getId());
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

    public static void openImageFullscreen(File image, Screen screen) {
        // find appropriate widget
        Widget<?> c = APP.widgetManager.find(w -> w.hasFeature(ImageDisplayFeature.class),NEW,true).orElse(null);
        if(c==null) return; // one can never know
        Node cn = c.load();
        setMinPrefMaxSize(cn, USE_COMPUTED_SIZE); // make sure no settings prevents full size
        StackPane root = new StackPane(cn);
        root.setBackground(bgr(BLACK));
        Stage s = createFMNTStage(screen);
        s.setScene(new Scene(root));
        s.show();

        cn.requestFocus(); // for key events to work - just focus some root child
        root.addEventFilter(KEY_PRESSED, ke -> {
            if(ke.getCode()==ESCAPE)
                s.hide();
        });

        // use widget for image viewing
        // note: although we know the image size (== screen size) we can not use it
        //       as widget will use its own size, which can take time to initialize,
        //       so we need to delay execution
        Functors.Ƒ a = () -> ((ImageDisplayFeature)c.getController()).showImage(image);
        Functors.Ƒ r = () -> runFX(100,a); // give layout some time to initialize (could display wrong size)
        if(s.isShowing()) r.apply(); /// execute when/after window is shown
        else add1timeEventHandler(s, WindowEvent.WINDOW_SHOWN, t -> r.apply());
    }

    public interface Build {

        static ProgressIndicator appProgressIndicator() {
            return appProgressIndicator(null, null);
        }

        static ProgressIndicator appProgressIndicator(Consumer<ProgressIndicator> onStart, Consumer<ProgressIndicator> onFinish) {
            Spinner p = new Spinner();
            Anim a = new Anim(at -> setScaleXY(p,at*at)).dur(500).intpl(new ElasticInterpolator());
                 a.applier.accept(0d);
            p.progressProperty().addListener((o,ov,nv) -> {
                if(nv.doubleValue()==-1) {
                    if(onStart!=null) onStart.accept(p);
                    a.then(null)
                     .play();
                }
                if(nv.doubleValue()==1) {
                    a.then(() -> { if(onFinish!=null) onFinish.accept(p); })
                     .playClose();
                }
            });
            return p;
        }

	    static Tooltip appTooltip() {
	        return appTooltip("");
	    }

	    static Tooltip appTooltip(String text) {
			Tooltip t = new Tooltip(text);
		    t.setHideOnEscape(true);
		    t.setConsumeAutoHidingEvents(true);
		    // TODO: make configurable
		    t.setShowDelay(seconds(1));
		    t.setShowDuration(seconds(10));
		    t.setHideDelay(millis(200));
		    return t;
	    }

    }

    @IsActionable
    public interface Actions {

	    @IsAction(name = "Open on Github", desc = "Opens Github page for this application. For developers.")
	    static void openAppGithubPage() {
		    browse(APP.GITHUB_URI);
	    }

	    @IsAction(name = "Open app directory", desc = "Opens directory from which this application is running from.")
	    static void openAppLocation() {
		    Environment.open(APP.DIR_APP);
	    }

	    @IsAction(name = "Open css guide", desc = "Opens css reference guide. For developers.")
	    static void openCssGuide() {
		    browse(URI.create("http://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html"));
	    }

	    @IsAction(name = "Open icon viewer", desc = "Opens application icon browser. For developers.")
	    static void openIconViewer() {
		    double iconSize = 45;
		    GridView<GlyphIcons,GlyphIcons> grid = new GridView<>(GlyphIcons.class, x -> x, iconSize+25,iconSize+35,5,5);
		    grid.selectOn.addAll(set(SelectionOn.MOUSE_HOVER, SelectionOn.MOUSE_CLICK, SelectionOn.KEY_PRESSED));
		    grid.setCellFactory(view -> new GridCell<>() {
			    Anim a;

			    {
				    getStyleClass().add("icon-grid-cell");
			    }

			    @Override
			    protected void updateItem(GlyphIcons icon, boolean empty) {
				    super.updateItem(icon, empty);
				    IconInfo graphics;
				    if(getGraphic() instanceof IconInfo)
					    graphics = (IconInfo) getGraphic();
				    else {
					    graphics = new IconInfo(null,iconSize);
					    setGraphic(graphics);
					    a = new Anim(graphics::setOpacity).dur(100).intpl(x -> x*x*x*x);
				    }
				    graphics.setGlyph(empty ? null : icon);

				    // really cool when scrolling with scrollbar
				    // but when using mouse wheel it is very ugly & distracting
				    // a.play();
			    }

			    @Override
			    public void updateSelected(boolean selected) {
				    super.updateSelected(selected);
				    IconInfo graphics = (IconInfo) getGraphic();
				    if(graphics!=null) graphics.select(selected);
			    }
		    });
		    StackPane root = new StackPane(grid);
		    root.setPrefSize(600, 720); // determines popup size
		    List<Button> groups = stream(FontAwesomeIcon.class,WeatherIcon.class,OctIcon.class,
			                             MaterialDesignIcon.class,MaterialIcon.class)
	              .map(c -> {
	                  Button b = new Button(c.getSimpleName());
	                  b.setOnMouseClicked(e -> {
	                      if(e.getButton()==PRIMARY) {
	                          grid.getItemsRaw().setAll(getEnumConstants(c));
	                          e.consume();
	                      }
	                  });
	                  return b;
	              })
                  .toList();
		    PopOver o = new PopOver<>(layVertically(20,TOP_CENTER,layHorizontally(8,CENTER,groups), root));
		    o.show(App_Center);
	    }

	    @IsAction(name = "Open launcher", desc = "Opens program launcher widget.", keys = "CTRL+P")
	    static void openLauncher() {
		    File f = new File(APP.DIR_LAYOUTS,"AppMainLauncher.fxwl");
		    Component c = UiContext.instantiateComponent(f);
		    if(c!=null) {
			    OverlayPane op = new OverlayPane() {
				    @Override
				    public void show() {
					    OverlayPane root = this;
					    getChildren().add(c.load());
					    // TODO: remove
					    run(millis(500), () ->
						    stream(((Pane)c.load()).getChildren()).findAny(GridView.class::isInstance).ifPresent(n -> ((GridView)n).implGetSkin().getFlow().requestFocus())
					    );
					    if(c instanceof Widget) {
						    ((Widget<?>)c).getController().getFieldOrThrow("closeOnLaunch").setValue(true);
						    ((Widget<?>)c).getController().getFieldOrThrow("closeOnRightClick").setValue(true);
						    ((Widget<?>)c).areaTemp = new ContainerNode() {
							    @Override public Pane getRoot() { return root; }
							    @Override public void show() {}
							    @Override public void hide() {}
							    @Override public void close() { root.hide(); }
						    };
					    }
					    super.show();
				    }
			    };
			    op.display.set(SCREEN_OF_MOUSE);
			    op.show();
			    c.load().prefWidth(900);
			    c.load().prefHeight(700);
		    }
	    }

	    @IsAction(name = "Open settings", desc = "Opens application settings.")
	    static void openSettings() {
		    APP.widgetManager.use(ConfiguringFeature.class, WidgetSource.NO_LAYOUT, c -> c.configure(APP.configuration.getFields()));
	    }

	    @IsAction(name = "Open layout manager", desc = "Opens layout management widget.")
	    static void openLayoutManager() {
		    APP.widgetManager.find("Layouts", WidgetSource.NO_LAYOUT, false);
	    }

	    @IsAction(name = "Open guide", desc = "Resume or start the guide.")
	    static void openGuide() {
		    APP.guide.open();
	    }

	    @IsAction(name = "Open app actions", desc = "Actions specific to whole application.")
	    static void openActions() {
		    APP.actionPane.show(Void.class, null, false,
			    new FastAction<>(
                    "Export widgets",
                    "Creates launcher file in the destination directory for every widget.\n"
	                    + "Launcher file is a file that when opened by this application opens the widget. "
	                    + "If application was not running before, it will not load normally, but will only "
	                    + "open the widget.\n"
	                    + "Essentially, this exports the widgets as 'standalone' applications.",
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
			    new FastAction<>(GITHUB,Action.get("Open on Github")),
			    new FastAction<>(CSS3,Action.get("Open css guide")),
			    new FastAction<>(IMAGE,Action.get("Open icon viewer")),
			    new FastAction<>(FOLDER,Action.get("Open app directory"))
		    );
	    }

	    @IsAction(name = "Open", desc = "Opens all possible open actions.", keys = "CTRL+SHIFT+O", global = true)
	    static void openOpen() {
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
	                    if(f!=null) Gui.setSkinExternal(f);
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
	                    // Action pane may auto-close when this action finishes, so we make sure to call
	                    // show() after that happens by delaying using runLater
	                    if(fs!=null) runLater(() -> APP.actionPane.show(fs));
                    }
			    )
		    );
	    }

	    @IsAction(name = "Show shortcuts", desc = "Display all available shortcuts.", keys = "?")
	    static void showShortcuts() {
		    APP.shortcutPane.show();
	    }

	    @IsAction(name = "Show system info", desc = "Display system information.")
	    static void showSysInfo() {
		    APP.actionPane.hide();
		    APP.infoPane.show();
	    }

	    @IsAction(name = "Run garbage collector", desc = "Runs java's garbage collector using 'System.gc()'.")
	    static void runGarbageCollector() {
		    System.gc();
	    }

	    @IsAction(name = "Run system command", desc = "Runs command just like in a system's shell's command line.", global = true)
	    static void runCommand() {
		    SimpleConfigurator sc = new SimpleConfigurator<>(
		    	new ValueConfig<>(String.class, "Command", ""),
                (String command) -> Environment.runCommand(command));
		    PopOver p = new PopOver<>(sc);
				    p.title.set("Run system command ");
				    p.show(PopOver.ScreenPos.App_Center);
	    }

	    @IsAction(name = "Run app command", desc = "Runs app command. Equivalent of launching this application with " +
		                                           "the command as a parameter.")
	    static void runAppCommand() {
		    SimpleConfigurator sc = new SimpleConfigurator<>(
		    	new ValueConfig<>(String.class, "Command", ""),
                (String command) -> APP.parameterProcessor.process(list(command)));
		    PopOver p = new PopOver<>(sc);
				    p.title.set("Run app command");
				    p.show(PopOver.ScreenPos.App_Center);
	    }

	    @IsAction(name = "Search (app)", desc = "Display application search.", keys = "CTRL+I")
	    @IsAction(name = "Search (os)", desc = "Display application search.", keys = "CTRL+SHIFT+I", global = true)
	    static void showSearch() {
	    	Window w = APP.windowManager.getFocused();
		    boolean isFocused = w != null;

		    DecoratedTextField tf = new DecoratedTextField();
		    Region clearButton = new Region();
		    clearButton.getStyleClass().addAll("graphic");
		    StackPane clearB = new StackPane(clearButton);
		    clearB.getStyleClass().addAll("clear-button");
		    clearB.setOpacity(0.0);
		    clearB.setCursor(Cursor.DEFAULT);
		    clearB.setOnMouseReleased(e -> tf.clear());
		    clearB.managedProperty().bind(tf.editableProperty());
		    clearB.visibleProperty().bind(tf.editableProperty());
		    tf.right.set(clearB);
		    FadeTransition fade = new FadeTransition(millis(250), clearB);
		    tf.textProperty().addListener(new InvalidationListener() {
			    @Override public void invalidated(Observable arg0) {
				    String text = tf.getText();
				    boolean isTextEmpty = text == null || text.isEmpty();
				    boolean isButtonVisible = fade.getNode().getOpacity() > 0;

				    if (isTextEmpty && isButtonVisible) {
					    setButtonVisible(false);
				    } else if (!isTextEmpty && !isButtonVisible) {
					    setButtonVisible(true);
				    }
			    }

			    private void setButtonVisible( boolean visible ) {
				    fade.setFromValue(visible? 0.0: 1.0);
				    fade.setToValue(visible? 1.0: 0.0);
				    fade.play();
			    }
		    });


		    tf.left.set(new Icon(FontAwesomeIcon.SEARCH));
		    tf.left.get().setMouseTransparent(true);

		    new ConfigSearch(tf, APP.configSearchHistory,
			    () -> APP.configuration.getFields().stream().map(ConfigSearch.Entry::of),
                () -> APP.widgetManager.factories.streamV().map(ConfigSearch.Entry::of)
		    );
		    PopOver<TextField> p = new PopOver<>(tf);
		    p.title.set("Search for an action or option");
		    p.setAutoHide(true);
		    p.show(isFocused ? PopOver.ScreenPos.App_Center : PopOver.ScreenPos.Screen_Center);
		    if(!isFocused) {
			    run(200, () -> {
			        APP.windowOwner.getStage().requestFocus();
				    p.requestFocus();
				    tf.requestFocus();
			    });
		    }
	    }

	    static void printAllImageFileMetadata(File file) {
		    try {
			    StringBuilder sb = new StringBuilder("Metadata of ").append(file.getPath());
			    com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(file);
			    metadata.getDirectories().forEach(d -> {
				    sb.append("\nName: " + d.getName());
				    d.getTags().forEach(tag -> sb.append("\n\t" + tag.toString() + "\n"));
			    });
			    APP.widgetManager.find(w -> w.name().equals("Logger"), WidgetSource.ANY); // open console automatically
			    System.out.println(sb.toString());
		    } catch (IOException | ImageProcessingException e) {
			    e.printStackTrace();
		    }
	    }

    }

}