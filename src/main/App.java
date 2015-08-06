
package main;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.ImageCursor;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.atteo.classindex.ClassIndex;
import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import AudioPlayer.Item;
import AudioPlayer.Player;
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
import Layout.Widgets.WidgetFactory;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.WidgetSource;
import Layout.Widgets.feature.ConfiguringFeature;
import Layout.Widgets.feature.ImageDisplayFeature;
import Layout.Widgets.feature.PlaylistFeature;
import action.Action;
import action.IsAction;
import action.IsActionable;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.GUI;
import gui.objects.PopOver.PopOver;
import gui.objects.TableCell.RatingCellFactory;
import gui.objects.TableCell.TextStarRatingCellFactory;
import gui.objects.Window.stage.ContextManager;
import gui.objects.Window.stage.Window;
import gui.objects.Window.stage.WindowManager;
import gui.objects.icon.IconInfo;
import gui.pane.CellPane;
import util.ClassName;
import util.File.AudioFileFormat;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.InstanceName;
import util.access.AccessorEnum;
import util.async.future.Fut;
import util.plugin.PluginMap;

import static Layout.Widgets.WidgetManager.WidgetSource.ANY;
import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
import static gui.objects.PopOver.PopOver.ScreenCentricPos.App_Center;
import static util.File.AudioFileFormat.Use.APP;
import static util.File.Environment.browse;
import static util.async.Async.*;
import static util.functional.Util.map;

/**
 * Application. Launches and terminates program.
 */
@IsActionable
@IsConfigurable("General")
public class App extends Application {


    /**
     * Starts program.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    
/******************************************************************************/
    
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
    public static final EventSource<String> actionStream = new EventSource();
    
/******************************************************************************/
    
    
    // NOTE: for some reason cant make fields final in this class +
    // initializing fields right up here (or constructor) will have no effect
    public static Window window;
    public static final ServiceManager services = new ServiceManager();
    public static PluginMap plugins = new PluginMap();
    private static App instance;
    public static Guide guide;
    private Window windowOwner;
    public final AppInstanceComm appCommunicator = new AppInstanceComm();
    public final AppParameterProcessor parameterProcessor = new AppParameterProcessor();
    private boolean initialized = false;
    private boolean normalLoad = true;
    
    public App() {
        instance = this;
    }    
    
/*********************************** CONFIGS **********************************/
    
    @IsConfig(name = "Show guide on app start", info = "Show guide when application "
            + "starts. Default true, but when guide is shown, it is set to false "
            + "so the guide will never appear again on its own.")
    public static boolean showGuide = true;
    
    @IsConfig(name = "Rating control.", info = "The style of the graphics of the rating control.")
    public static final AccessorEnum<RatingCellFactory> ratingCell = new AccessorEnum<>(new TextStarRatingCellFactory(),() -> plugins.getPlugins(RatingCellFactory.class));
    @IsConfig(name = "Rating icon amount", info = "Number of icons in rating control.", min = 1, max = 10)
    public static final IntegerProperty maxRating = new SimpleIntegerProperty(5);
    @IsConfig(name = "Rating allow partial", info = "Allow partial values for rating.")
    public static final BooleanProperty partialRating = new SimpleBooleanProperty(true);
    @IsConfig(name = "Rating editable", info = "Allow change of rating. Defaults to application settings")
    public static final BooleanProperty allowRatingChange = new SimpleBooleanProperty(true);
    @IsConfig(name = "Rating react on hover", info = "Move rating according to mouse when hovering.")
    public static final BooleanProperty hoverRating = new SimpleBooleanProperty(true);
    @IsConfig(name = "Debug value", info = "For application testing. Generic number value"
            + "to control some application value manually.")
    public static final DoubleProperty debug = new SimpleDoubleProperty(0);
    
    @IsConfig(info = "Preffered text when no tag value for field. This value is overridable.")
    public static String TAG_NO_VALUE = "-- no assigned value --";
    @IsConfig(info = "Preffered text when multiple tag values per field. This value is overridable.")
    public static String TAG_MULTIPLE_VALUE = "-- multiple values --";
    public static boolean ALBUM_ARTIST_WHEN_NO_ARTIST = true;
    
    
/******************************************************************************/
    
    public static InstanceName instanceName = new InstanceName();
    public static ClassName className = new ClassName();
    
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
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> 
            LOGGER.error(t.getName(), e)
        );
        
        
        // add optional object instance -> string converters
        className.add(Item.class, "Song");
        className.add(PlaylistItem.class, "Playlist Song");
        className.add(Metadata.class, "Library Song");
        className.add(MetadataGroup.class, "Song Group");

        // add optional object class -> string converters
        instanceName.add(Item.class, Item::getPath);
        instanceName.add(PlaylistItem.class, PlaylistItem::getTitle);
        instanceName.add(Metadata.class,Metadata::getTitle);
        instanceName.add(MetadataGroup.class, o -> Objects.toString(o.getValue()));
        instanceName.add(Component.class, o -> o.getName());
        instanceName.add(List.class, o -> String.valueOf(o.size()));
        instanceName.add(File.class, File::getPath);
        
        
        // initialize app parameter processor
        parameterProcessor.addFileProcessor(
            f -> AudioFileFormat.isSupported(f, APP),
            fs -> WidgetManager.use(PlaylistFeature.class, ANY, p -> p.getPlaylist().addFiles(fs))
        );
        parameterProcessor.addFileProcessor(
            FileUtil::isValidSkinFile,
            fs -> GUI.setSkin(FileUtil.getName(fs.get(0)))
        );
        parameterProcessor.addFileProcessor(
            ImageFileFormat::isSupported,
            fs -> WidgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w->w.showImages(fs))
        );
//        parameterProcessor.addFileProcessor(
//            FileUtil::isValidWidgetFile,
//            fs -> WidgetManager.find(w -> w.name().equals(FileUtil.getName(fs.get(0))), NO_LAYOUT)
//        );
        parameterProcessor.addFileProcessor(
            f -> f.getPath().endsWith(".fxwl"),
            fs -> {
                WidgetFactory wf = WidgetManager.getFactory(FileUtil.getName(fs.get(0)));
                if(wf!=null) ContextManager.showWindow(wf.create());
            }
        );
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
        try {
            // forbid multiple application instances, instead
            // notify the 1st instance of 2nd (this) trying to run and exit
            if(getInstances()>1) {
                appCommunicator.fireNewInstanceEvent(fetchParameters());
                App.close();
                return;
            }
            
            // listen to other application instance launches
            // process app parameters of newly started instance
            appCommunicator.start();
            appCommunicator.onNewInstanceHandlers.add(parameterProcessor::process);
            
            Action.startGlobalListening();
            
            // create hidden main window
            windowOwner = Window.createWindowOwner();
            windowOwner.show();
            
            // discover plugins
            ClassIndex.getAnnotated(IsPluginType.class).forEach(plugins::registerPluginType);
            ClassIndex.getAnnotated(IsPlugin.class).forEach(plugins::registerPlugin);
            WidgetManager.initialize();
            
            // services must be loaded before Configuration
            services.addService(new TrayService());
            services.addService(new Notifier());
            services.addService(new PlaycountIncrementer());
            services.addService(new ClickEffect());
            
            // gather configs
            Configuration.collectAppConfigs();
            // deserialize values (some configs need to apply it, will do when ready)
            Configuration.load();
            
            // initializing, the order is important
            Player.initialize();
            
            // collectAppConfigs windows from previous session
            List<String> ps = fetchParameters();
            normalLoad = !ps.stream().anyMatch(s->s.endsWith(".fxwl"));
            WindowManager.deserialize(normalLoad);
            
            DB.start();
//            GUI.setLayoutMode(true);
//            Transition t = par(
//                Window.windows.stream().map(w -> 
//                    seq(
//                        new Anim(at -> ((SwitchPane)w.getLayoutAggregator()).zoomProperty().set(1-0.6*at))
//                                .dur(500).intpl(new CircularInterpolator()),
//                        par(
//                            par(
//                                forEachIStream(w.left_icons.box.getChildren(),(i,icon)-> 
//                                    new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*200))
//                            ),
//                            par(
//                                forEachIRStream(w.right_icons.box.getChildren(),(i,icon)-> 
//                                    new Anim(at->setScaleXY(icon,at*at)).dur(500).intpl(new ElasticInterpolator()).delay(i*200))
//                            ),
//                            par(
//                                w.getLayoutAggregator().getLayouts().values().stream()
//                                 .flatMap(l -> l.getAllWidgets())
//                                 .map(wi -> (Area)wi.load().getUserData())
//                                 .map(a -> 
//                                    seq(
//                                        new Anim(a.content_root::setOpacity).dur(2000+random()*1000).intpl(0),
//                                        new Anim(a.content_root::setOpacity).dur(700).intpl(isAroundMin1(0.04, 0.1,0.2,0.3))
//                                    )
//                                 )
//                            )
//                        ),
//                        par(
//                            new Anim(at -> ((SwitchPane)w.getLayoutAggregator()).zoomProperty().set(0.4+0.7*at))
//                                    .dur(500).intpl(new CircularInterpolator())
//                        )
//                    )
//                )
//            );
//            t.setOnFinished(e -> {
//                GUI.setLayoutMode(false);
//            });
//            t.play();
            
            initialized = true;
            
        } catch(Exception e) {
            String ex = Stream.of(e.getStackTrace()).map(s->s.toString()).collect(Collectors.joining("\n"));
            System.out.println(ex);
            
            LOGGER.error("Application failed to start", e);
        }
        // initialization complete -> apply all settings
        Configuration.getFields().forEach(Config::applyValue);
        
        // app ready
        
            
         //initialize non critical parts
        if(normalLoad) Player.loadLast();                      // should load in the end
        
        // handle guide
        guide = new Guide();
        if(showGuide) {
            showGuide = false;
            run(2222, () -> guide.start());
        }
        
        System.out.println(new File("cursor.png").getAbsoluteFile().toURI().toString());
        Image image = new Image(new File("cursor.png").getAbsoluteFile().toURI().toString());  //pass in the image path
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
            if(normalLoad) Player.state.serialize();            
            if(normalLoad) WindowManager.serialize();
            Configuration.save();
            services.getAllServices()
                    .filter(Service::isRunning)
                    .forEach(Service::stop);
        }
        DB.stop();
        Action.stopGlobalListening();
        appCommunicator.stop();
    }
    
    public static boolean isInitialized() {
        return App.instance.initialized;
    }

    /**
     * Returns applications' main window. Never null, but be aware that the window
     * might not be completely initialized. To find out whether it is, run
     * isGuiInitialized() beforehand.
     * @return window
     */
    public static Window getWindow() {
        return instance.window;
    }
    public static Window getWindowOwner() {
        return instance.windowOwner;
    }
    
    public static<S extends Service> void use(Class<S> type, Consumer<S> action) {
        services.getService(type).filter(Service::isRunning).ifPresent(action);
    }
    
    /**
     * Closes the application. Normally application closes when main window 
     * closes. Therefore this method should not need to be used.
     */
    public static void close() {
        // close app
        Platform.exit();
    }
    
/******************************************************************************/
    
    public List<String> fetchParameters() {
        List<String> params = new ArrayList<>();
        getParameters().getRaw().forEach(params::add);
        // getParameters().getUnnamed().forEach(params::add);
        // getParameters().getNamed().forEach( (t,tt) -> s.add(t+" "+tt) );
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
        
        int i=0;
        String ud = System.getProperty("user.dir");

        for(VirtualMachineDescriptor vmd : VirtualMachine.list()) {
            try {
                VirtualMachine vm = VirtualMachine.attach(vmd);
                String udi = vm.getSystemProperties().getProperty("user.dir");
                if(ud.equals(udi)) i++;
                vm.detach();
            } catch (AttachNotSupportedException | IOException ex) {
                LOGGER.warn("Unable to inspect virtual machine {}", vmd);
            }
        }
        
        return i;
    }
    
    /**
     * The root location of the application. Equivalent to new File("").getAbsoluteFile().
     * @return absolute file of location of the root directory of this
     * application.
     */
    public static File getLocation() {
        return new File("").getAbsoluteFile();
    }

    /** @return Name of the application. */
    public static String getAppName() {
        return "PlayerFX";
    }
    
    /** @return image of the icon of the application. */
    public static Image getIcon() {
        return new Image(new File("icon512.png").toURI().toString());
    }
    
    /** Github url for project of this application. */
    public static URI GITHUB_URI = URI.create("https://www.github.com/sghpjuikit/player/");

    /** @return Player state file. */
    public static String PLAYER_STATE_FILE() {
        return "PlayerState.cfg";
    }

    /** @return absolute file of Location of widgets. */
    public static File WIDGET_FOLDER() {
        return new File("Widgets").getAbsoluteFile();
    }

    /** @return absolute file of Location of layouts. */
    public static File LAYOUT_FOLDER() {
        return new File("Layouts").getAbsoluteFile();
    }

    /** @return absolute file of Location of skins. */
    public static File SKIN_FOLDER() {
        return new File("Skins").getAbsoluteFile();
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
    
    /** Temporary directory of the os. */
    public static File DIR_TEMP = new File(System.getProperty("java.io.tmpdir"));
    /** Directory for application logging. */
    public static File DIR_LOG = new File("log").getAbsoluteFile();
    /** File for application logging configuration. */
    public static File FILE_LOG_CONFIG = new File(DIR_LOG,"log_configuration.xml");
    
    
    // jobs
    
    public static void refreshItemsFromFileJob(List<? extends Item> items) {
        Fut.fut()
           .then(() -> Player.refreshItemsWithUpdated(MetadataReader.readMetadata(items)),Player.IO_THREAD)
           .showProgress(App.getWindow().taskAdd())
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
    
    
    
/************************************ actions *********************************/
    
    @IsAction(name = "Open github page", descr = "Open github project "
            + "website of this application in default browser. For developers.")
    public static void openAppGithubPage() {
        browse(GITHUB_URI);
    }
    
    @IsAction(name = "Open app dir", descr = "Open application location.")
    public static void openAppLocation() {
        browse(getLocation());
    }
    
    @IsAction(name = "Open css guide", descr = "Open official oracle css "
            + "reference guide. Helps with skinning. For developers.")
    public static void openCssGuide() {
        browse("http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html");
    }
    
    @IsAction(name = "Open icon viewer", descr = "Open viewer to browse "
            + "application supported icons. For developers")
    public static void openIconViewer() {
        Fut.fut()
           .then(() -> {
                CellPane c = new CellPane(70,80,5);
                c.getChildren().addAll(map(FontAwesomeIcon.values(),i -> new IconInfo(i,55)));
                ScrollPane p = c.scrollable();
                p.setPrefSize(500, 720);
                PopOver o = new PopOver(p);
                runLater(() -> o.show(App_Center));
           })
           .showProgress(Window.getActive().taskAdd())
           .run();
    }
    
    @IsAction(name = "Open settings", descr = "Open preferred "
            + "settings widget to show applciation settings. Widget is open in "
            + "a popup or layout, or already open widget is reused, depending "
            + "on the settings")
    public static void openSettings() {
        WidgetManager.find(ConfiguringFeature.class, WidgetSource.NO_LAYOUT);
    }
}