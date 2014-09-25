
package main;

import Action.Action;
import AudioPlayer.Player;
import AudioPlayer.playback.PlaycountIncrementer;
import AudioPlayer.services.Database.DB;
import AudioPlayer.services.Notifier.NotifierManager;
import AudioPlayer.services.Service;
import AudioPlayer.services.ServiceManager;
import AudioPlayer.services.Tray.TrayService;
import AudioPlayer.tagging.MoodManager;
import Configuration.Config;
import Configuration.Configuration;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.GUI;
import GUI.Window;
import GUI.WindowManager;
import Layout.Widgets.WidgetManager;
import Library.BookmarkManager;
import java.io.File;
import java.net.URI;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import util.FxTimer;
import util.Parser.File.FileUtil;


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
    
    // NOTE: for some reason cant make fields final in this class +
    // initializing fields right up here (or constructor) will have no effect
    public static Window window;
    private Window windowOwner;
    public static Guide guide;
    private boolean initialized = false;
    
    private static final ServiceManager services = new ServiceManager();
    
    private static App instance;
    public App() {
        instance = this;
    }
    
//    /** Returns instance of this app singleton. */
//    public static App getInstance() {
//        return instance;
//    }
    
    
/*********************************** CONFIGS **********************************/
    
    @IsConfig(name = "Show guide when on app start", info = "Automatically show guide hints next time application starts up. Automatically set to false afterwards.")
    public static boolean showGuide = true;
    
    @IsConfig(info = "Preffered editability of rating controls. This value is overridable.")
    public static boolean allowRatingChange = true;
    @IsConfig(info = "Preffered number of elements in rating control. This value is overridable.")
    public static int maxRating = 5;
    @IsConfig(info = "Preffered value for partial values in rating controls. This value is overridable.")
    public static boolean partialRating = true;
    @IsConfig(info = "Preffered hoverability of rating controls. This value is overridable.")
    public static boolean hoverRating = true;
    
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
    public void init() {
        
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
            Action.startGlobalListening();
            Configuration.load();           // must initialize first
        
            // initializing, the order is important
            Player.initialize();
            WidgetManager.initialize();             // must initialize before below
            GUI.initialize();                       // must initialize before below

            // create hidden main window
            windowOwner = Window.createWindowOwner();
            windowOwner.show();
            
            // this might me deprecated
            // we need to initialize skin before windows do
            Configuration.getField("skin").applyValue();
            
            DB.start();
            
            // initialize windows from previous session
            WindowManager.deserialize();
            
            initialized = true;
            
        } catch(Exception e) {
            String ex = Stream.of(e.getStackTrace()).map(s->s.toString()).collect(Collectors.joining("\n"));
           
            // copy exception trace to clipboard
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(ex);
            clipboard.setContent(content);
            
            e.printStackTrace();
        }
        
        // initialize non critical parts
        Player.loadLast();                      // should load in the end

        PlaycountIncrementer.initialize();
        MoodManager.initialize();
        Action.getActions().forEach(Action::register);
        
        // apply all (and gui) settings
        Configuration.getFields().forEach(Config::applyValue);
        
        services.addService(new TrayService());
        services.addService(new NotifierManager());
        
        services.getAllServices()
                .filter(s->!s.isDependency()).filter(Service::isSupported)
                .forEach(Service::start);
        
        // handle guide
        guide = new Guide();
        if(showGuide) {
            showGuide = false;
            FxTimer.run(2222, () -> guide.start());
        }
        
        // playing with parameters/ works, but how do i pass param when executing this from windows?
        
//        List<String> s = new ArrayList<>();
//        System.out.println("raw");
//        App.getInstance().getParameters().getRaw().forEach(s::add);
//        System.out.println("unnamed");
//        App.getInstance().getParameters().getUnnamed().forEach(s::add);
//        System.out.println("named");
//        App.getInstance().getParameters().getNamed().forEach( (t,tt) -> s.add(t+" "+tt) );
//        
//        String t = s.stream().collect(Collectors.joining("/n"));
//        NotifierManager.showTextNotification(t , "");
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
                    .filter(s->!s.isDependency()).filter(Service::isRunning)
                    .forEach(Service::stop);
            Player.state.serialize();            
            Configuration.save();
            BookmarkManager.saveBookmarks();
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
        services.getService(type).ifPresent(action);
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
        return new Image(new File("icon.png").toURI().toString());
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
}