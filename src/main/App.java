
package main;

import Action.Action;
import AudioPlayer.Player;
import AudioPlayer.playback.PlaycountIncrementer;
import AudioPlayer.playlist.Item;
import AudioPlayer.plugin.IsPlugin;
import AudioPlayer.plugin.IsPluginType;
import AudioPlayer.services.Database.DB;
import AudioPlayer.services.Notifier.Notifier;
import AudioPlayer.services.Service;
import AudioPlayer.services.ServiceManager;
import AudioPlayer.services.Tray.TrayService;
import AudioPlayer.tagging.MetadataReader;
import Configuration.*;
import Layout.Widgets.WidgetManager;
import gui.objects.TableCell.RatingCellFactory;
import gui.objects.TableCell.TextStarRatingCellFactory;
import gui.objects.Window.stage.Window;
import gui.objects.Window.stage.WindowManager;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import org.atteo.classindex.ClassIndex;
import org.reactfx.EventSource;
import util.File.FileUtil;
import util.access.AccessorEnum;
import static util.async.Async.eFX;
import static util.async.Async.run;
import util.plugin.PluginMap;


/**
 * Application. Launches and terminates program.
 */
@IsConfigurable("General")
public class App extends Application {


    /**
     * Starts program.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
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
    private Window windowOwner;
    public static Guide guide;
    private boolean initialized = false;
    
    private static final ServiceManager services = new ServiceManager();
    public static PluginMap plugins = new PluginMap();
    
    private static App instance;
    public App() {
        instance = this;
    }    
    
/*********************************** CONFIGS **********************************/
    
    @IsConfig(name = "Show guide on app start", info = "Automatically show "
     + "guide hints next time application starts up. Automatically set to false afterwards.")
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
    @IsConfig(name = "Debug", info = "For debug purposes.")
    public static final DoubleProperty debug = new SimpleDoubleProperty(0);
    
    @IsConfig(info = "Preffered text when no tag value for field. This value is overridable.")
    public static String TAG_NO_VALUE = "-- no assigned value --";
    @IsConfig(info = "Preffered text when multiple tag values per field. This value is overridable.")
    public static String TAG_MULTIPLE_VALUE = "-- multiple values --";
    public static boolean ALBUM_ARTIST_WHEN_NO_ARTIST = true;
    
    
/******************************************************************************/
    
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
    public void init() {}
    
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
            Action.startGlobalListening();
            
            // create hidden main window
            windowOwner = Window.createWindowOwner();
            windowOwner.show();
            
            // discover plugins
            ClassIndex.getAnnotated(IsPluginType.class).forEach(plugins::registerPluginType);
            ClassIndex.getAnnotated(IsPlugin.class).forEach(plugins::registerPlugin);
            WidgetManager.initialize();     // must initialize before below
            
            Configuration.load();
            
            // initializing, the order is important
            Player.initialize();
            
            // initialize windows from previous session
            WindowManager.deserialize();
            
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
            // copy exception trace to clipboard
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(ex);
            clipboard.setContent(content);
            
            e.printStackTrace();
        }
         //initialize non critical parts
        Player.loadLast();                      // should load in the end

        Action.getActions().forEach(Action::register);
        
        // apply all (and gui) settings
        Configuration.getFields().forEach(Config::applyValue);
//        
        services.addService(new TrayService());
        services.addService(new Notifier());
        services.addService(new PlaycountIncrementer());
        services.getAllServices()
                .filter(s->!s.isDependency() && s.isSupported())
                .forEach(Service::start);
        
        // handle guide
        guide = new Guide();
        if(showGuide) {
            showGuide = false;
            run(2222, () -> guide.start());
        }
        
//        List<String> s = new ArrayList<>();
//        System.out.println("raw");
//        App.getInstance().getParameters().getRaw().forEach(s::add);
//        System.out.println("unnamed");
//        App.getInstance().getParameters().getUnnamed().forEach(s::add);
//        System.out.println("named");
//        App.getInstance().getParameters().getNamed().forEach( (t,tt) -> s.add(t+" "+tt) );
//        
//        String t = s.stream().collect(Collectors.joining("/n"));
//        Notifier.showTextNotification(t , "");
//        System.out.println("GGGGG " + t);
    }
 
    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     * NOTE: This method is called on the JavaFX Application Thread. 
     */
    @Override
    public void stop() {
        
        if(initialized) {
            services.getAllServices()
                    .filter(Service::isRunning)
                    .forEach(Service::stop);
            Player.state.serialize();            
            Configuration.save();
        }
        DB.stop();
        Action.stopGlobalListening();
        // remove temporary files
        FileUtil.removeDirContent(TMP_FOLDER());
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
        // close window
        instance.windowOwner.close();
        // close app
        Platform.exit();
    }
    
/******************************************************************************/
    
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
        return new Image(new File("icon.jpg").toURI().toString());
    }
    
    /** @return github link for project of this application. */
    public static URI getGithubLink() {
        return URI.create("https://www.github.com/sghpjuikit/player/");
    }

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
     * Use for temporary files & junk. This folder is emptied on app close.
     * 
     * @return absolute file of directory for temporary files.
     */
    public static File TMP_FOLDER() {
        return new File(DATA_FOLDER(),"Temp");
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
        ProgressIndicator p = App.getWindow().taskAdd();
        CompletableFuture.runAsync(()->p.setProgress(-1),eFX)
           .thenApplyAsync(nothing -> MetadataReader.readMetadata(items))
           .thenAcceptAsync(Player::refreshItemsWithUpdated,eFX)
           .thenRunAsync(() -> p.setProgress(1),eFX)
           .thenRun(()->{})
           .complete(null);
    }
    
}