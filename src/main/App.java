
package main;

import AudioPlayer.Player;
import AudioPlayer.tagging.MoodManager;
import Configuration.ConfigManager;
import Configuration.Configuration;
import Configuration.SkinEnum;
import GUI.GUI;
import GUI.NotifierManager;
import GUI.UIController;
import GUI.WindowBase;
import Layout.LayoutManager;
import Layout.WidgetManager;
import Library.BookmarkManager;
import com.melloware.jintellitype.JIntellitype;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import utilities.FileUtil;
import utilities.Log;


/**
 * Main Application. This class handles navigation and user session.
 */
public class App extends Application {
    // NOTE: for some reason cant make fields final in this class +
    // initializing fields right up here (or constructor) will have no effect
    private WindowBase window;
    private static App instance;
    
    public App() {
        instance = this;
    }
    
    /**
     * Starts program.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    /**
     * Returns instance of this app singleton.
     * @return 
     */
    public static App getInstance() {
        return instance;
    }
    
/******************************************************************************/
    
    /**
     * The application initialization method. This method is called immediately
     * after the Application class is loaded and constructed. Can perform 
     * initialization prior to the actual starting of the application.
     * 
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
     * 
     * NOTE: This method is called on the JavaFX Application Thread. 
     * @param primaryStage the primary stage for this application, onto which
     * the application scene can be set. The primary stage will be embedded in
     * the browser if the application was launched as an applet. Applications
     * may create other stages, if needed, but they will not be primary stages
     * and will not be embedded in the browser.
     */
    @Override
    public void start(Stage primaryStage) {
        window = new WindowBase(true);   
        
        // initializing, the order is important
        Configuration c = ConfigManager.loadConfiguration();      // must initialize first   
        
        Player.initialize();
        WidgetManager.initialize();             // must initialize before below
        GUI.initialize();                       // must initialize before below
        LayoutManager.findLayouts();            // must initialize before below
        initializeGui();                   
        ConfigManager.apply(c);                 // apply gui settings
        NotifierManager.initialize();
        
        // loading states, etc
        LayoutManager.loadLast();
        window.show();                          // should load when GUI is ready
        Player.loadLast();                      // should load in the end
        
        // post
        MoodManager.initialize();
        
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
        // the order doesnt matter
        JIntellitype.getInstance().cleanUp();
        Player.state.serialize();
        LayoutManager.serialize();
        ConfigManager.saveConfiguration();
        BookmarkManager.saveBookmarks();
    }

    /**
     * Returns applications' window. Never null, but be aware that the window
     * might not be completely initialized. To find out whether it is, run
     * isGuiInitialized() beforehand.
     * @return window. 
     */
    public WindowBase getWindow() {
        return window;
    }
    /**
     * Returns applications' stage. Never null, but be aware that the stage
     * might not be completely initialized (initializes alongside window). To
     * find out whether it is, run isGuiInitialized() beforehand.
     * @return stage or null if application not GUI initialized yet. 
     */
    public Stage getStage() {
        return window.getStage();
    }
    /**
     * Returns applications' scene (this application will always have only one).
     * Always run isGuiInitialized() check before running this method or
     * NullPointerException will follow.
     * @return stage or null if application not GUI initialized yet. 
     */
    public Scene getScene() {
        if (isGuiInitialized())
            return getWindow().getStage().getScene();
        else {
            Log.err("GUI is not initialized. No scene exists.");
            return null;
        }
    }
    
    /**
     * Before this method is invoked for the first time, the gui will not be
     * ready to execute operations. To check whether it is, use isGuiInitialized()
     * method.
     * Run this method to completely rebuild the GUI. It involves:
     * - rebuilding of the application window
     * - reregistering shortcuts
     * - setting skin
     * - setting fontsize
     */
    public void initializeGui() {
        replaceSceneContent("UI.fxml");
        GUI.refresh();
    }
    
    /**
     * Checks whether the GUI of the application has completed its initialization.
     * If true is returned, the GUI is returned, the Window, Stage and Scene of
     * this application are fully prepared for any operation. In other case, some
     * operations might be unupported and Scene will be null. Window and Stage
     * will never be null, but their state might not be optimal for carrying out
     * operations - this method guarantees that optimality.
     * It is recommended to run this check before executing operations operating
     * on Window, Stage and Scene objects of he application and handle the case,
     * when the initialization has not been completed differently.
     * This method helps avoid exceptions resulting from uninitialized GUI state.
     * @return 
     */
    public boolean isGuiInitialized() {
        return (!(getWindow().getStage().getScene() == null ||
                    getWindow().getStage().getScene().getRoot() == null));
    }

    /**
     * Changes application's skin and applies it.
     * This is a convenience method that constructs a file from the skinname
     * and calls the setSkin(File skin) method.
     * The skin file will be constructed based on the fact that it must pass
     * the isValidSkinFile() check. The path is constructed like this:
     * Application.SkinsPath/skinname/skinname.css
     * For any details regarding the mechanics behind the method see documentation
     * of that method.
     * @param skinname name of the skin to apply.
     * @return return value of the setSkin(File) method ran with the constructed
     * file of this method.
//     * @throws NullPointerException if parameter null
     */
    public boolean setSkin(String skinname) {
        if (skinname== null || skinname.isEmpty() || skinname.equalsIgnoreCase(App.STYLESHEET_MODENA))  //handle modena
            return setSkinModena();
        else if (skinname.equalsIgnoreCase(App.STYLESHEET_CASPIAN))  //handle caspian
            return setSkinCaspian();
                                                        // handle external skins
        String path = Configuration.SKIN_FOLDER + File.separator + skinname +
                      File.separator + skinname + ".css";
        File skin = new File(path);
        return setSkin(skin);
    }
    
    /**
     * Changes application's skin and applies it. 
     * @param skin - css file of the skin to load. It is expected that the skin
     * will have all its external resources ready and usable or it could fail
     * to load properly.
     * To avoid exceptions and errors, isValidSkinFile() check  is ran on this
     * parameter before running this method.
     * @return true if the skin has been applied.
     * False return value signifies that gui has not been initialized,
     * skin file could be loaded or was not valid skin file. 
     * True return value doesnt imply successful skin loading, but guarantees
     * that the new skin has been applied regardless of the success.
     */
    private boolean setSkin(File skin) {
        if (isGuiInitialized() && FileUtil.isValidSkinFile(skin)) {
            try {
                setUserAgentStylesheet(skin.toURI().toURL().toExternalForm());
                GUI.skin = new SkinEnum(FileUtil.getName(skin));
                return true;
            } catch (MalformedURLException ex) {
                Log.err(ex.getMessage());
                return false;
            }
        }
        return false;
    }
    
    public boolean setSkinModena() {
        if (isGuiInitialized()) {
            setUserAgentStylesheet(STYLESHEET_MODENA);
            GUI.skin = new SkinEnum("Modena");
            return true;
        }
        return false;
    }
    public boolean setSkinCaspian() {
        if (isGuiInitialized()) {
            setUserAgentStylesheet(STYLESHEET_CASPIAN);
            GUI.skin = new SkinEnum("Caspian");
            return true;
        }
        return false;
    }
    
    /** Closes the application. */
    public void close() {
        // close window to allow any window close-related operations take place
        window.close();
        // close app
        Platform.exit();
    }
        
    private void replaceSceneContent(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(UIController.class.getResource(fxml));
            // load GUI
            Parent page = (Parent) loader.load();
            // paste GUI onto window
            Scene scene = new Scene(page);
            window.getStage().setScene(scene); // dont run stage.sizeToScene() - it will prevent application to initialize correct size
        } catch (IOException ex) {
            Log.err("Error during GUI initialization");
        }
    }
}