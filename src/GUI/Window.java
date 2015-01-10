
package GUI;

import Action.Action;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.LastFM.LastFMManager;
import AudioPlayer.tagging.Metadata;
import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.LayoutAggregators.EmptyLayoutAggregator;
import GUI.LayoutAggregators.LayoutAggregator;
import GUI.LayoutAggregators.SwitchPane;
import GUI.objects.PopOver.PopOver;
import GUI.objects.Text;
import static GUI.objects.Window.Resize.E;
import static GUI.objects.Window.Resize.N;
import static GUI.objects.Window.Resize.NE;
import static GUI.objects.Window.Resize.NONE;
import static GUI.objects.Window.Resize.NW;
import static GUI.objects.Window.Resize.S;
import static GUI.objects.Window.Resize.SE;
import static GUI.objects.Window.Resize.SW;
import static GUI.objects.Window.Resize.W;
import Layout.Component;
import Layout.Layout;
import Layout.WidgetImpl.LayoutManagerComponent;
import Layout.Widgets.Features.ConfiguringFeature;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.WidgetSource;
import Serialization.SelfSerializator;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.jensd.fx.fontawesome.AwesomeDude;
import static de.jensd.fx.fontawesome.AwesomeIcon.COLUMNS;
import static de.jensd.fx.fontawesome.AwesomeIcon.CSS3;
import static de.jensd.fx.fontawesome.AwesomeIcon.FOLDER;
import static de.jensd.fx.fontawesome.AwesomeIcon.GEARS;
import static de.jensd.fx.fontawesome.AwesomeIcon.GITHUB;
import static de.jensd.fx.fontawesome.AwesomeIcon.GRADUATION_CAP;
import static de.jensd.fx.fontawesome.AwesomeIcon.IMAGE;
import static de.jensd.fx.fontawesome.AwesomeIcon.INFO;
import static de.jensd.fx.fontawesome.AwesomeIcon.LOCK;
import static de.jensd.fx.fontawesome.AwesomeIcon.UNLOCK;
import de.jensd.fx.fontawesome.test.IconsBrowser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import static javafx.scene.input.KeyCode.ESCAPE;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import static javafx.scene.paint.Color.BLACK;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import static javafx.stage.StageStyle.TRANSPARENT;
import static javafx.stage.StageStyle.UTILITY;
import main.App;
import org.reactfx.Subscription;
import util.Log;
import static util.Parser.File.Enviroment.browse;
import util.Util;
import static util.Util.createIcon;
import static util.Util.setAPAnchors;
import util.access.Accessor;
import static util.functional.FunctUtil.find;

/**
 * Window for application.
 * <p>
 * Window with basic functionality.
 * <p>
 * Below is a code example creating and configuring custom window instance:
 * <pre>
 *     Window w = Window.create();
 *            w.setIsPopup(true);
 *            w.getStage().initOwner(App.getWindow().getStage());
 *            w.setTitle(title);
 *            w.setContent(content);
 *            w.show();
 *            w.setLocationCenter();
 * </pre>
 * 
 * @author plutonium_, simdimdim
 */
@IsConfigurable
public class Window extends WindowBase implements SelfSerializator<Window> {
    
    private static final PseudoClass focusedPseudoClass = PseudoClass.getPseudoClass("focused");
    public static final ArrayList<Window> windows = new ArrayList();
    
    /**
     * Get focused window. There is zero or one focused window in the application
     * at any given time.
     * <p>
     * @see #getActive()
     * @return focused window or null if none focused.
     */
    public static Window getFocused() {
        return find(windows,Window::isFocused).orElse(null);
    }
    
    /**
     * Same as {@link #getFocused()} but when none focused returns main window
     * instead of null.
     * <p>
     * Both methods are equivalent except for when the application itself has no
     * focus - which is when no window has focus.
     * <p>
     * Use when null must absolutely be avoided and the main window substitute 
     * for focused window will not break expected behavior and when this method
     * can get called when app has no focus (such as through global shortcut).
     * 
     * @return focused window or main window if none. Never null.
     */
    public static Window getActive() {
        return find(windows,Window::isFocused).orElse(App.getWindow());
    }
    
/******************************** Configs *************************************/
    
    @IsConfig(name="Window header visiblility preference", info="Remembers header state for both fullscreen and not. When selected 'auto off' is true ")
    public static boolean headerVisiblePreference = true;
    
    @IsConfig(name = "Opacity", info = "Window opacity.", min=0, max=1)
    public static final Accessor<Double> windowOpacity = new Accessor<>(1d, v -> windows.forEach(w->w.getStage().setOpacity(v)));
    
    @IsConfig(name = "Overlay effect", info = "Use color overlay effect.")
    public static boolean gui_overlay = false;
    
    @IsConfig(name = "Overlay effect use song color", info = "Use song color if available as source color for gui overlay effect.")
    public static boolean gui_overlay_use_song = false;
    
    @IsConfig(name = "Overlay effect color", info = "Set color for color overlay effect.")
    public static final Accessor<Color> gui_overlay_color = new Accessor<>(BLACK, v -> {
        if(!gui_overlay_use_song) applyColorEffect(v);
    });
    
    @IsConfig(name = "Overlay effect normalize", info = "Forbid contrast and brightness change. Applies only hue portion of the color for overlay effect.")
    public static boolean gui_overlay_normalize = true;
    
    @IsConfig(name = "Overlay effect intensity", info = "Intensity of the color overlay effect.", min=0, max=1)
    public static double overlay_norm_factor = 0.5;
    
    @IsConfig(name = "Borderless", info = "Borderless window has hidden header and borders.")
    public static final Accessor<Boolean> window_borderless = new Accessor<>(false, v -> windows.forEach(w->w.setBorderless(v)));
    
    @AppliesConfig( "headerVisiblePreference")
    private static void applyHeaderVisiblePreference() {
        // weird that this still doesnt apply it correctly, whats wrong?
        windows.forEach(w->w.setHeaderVisible(w.headerVisible));
    }
    
    @AppliesConfig( "overlay_norm_factor")
    @AppliesConfig( "gui_overlay_use_song")
    @AppliesConfig( "gui_overlay_normalize")
    @AppliesConfig( "gui_overlay")
    public static void applyColorOverlay() {
        if(gui_overlay_use_song) applyOverlayUseSongColor();
        else applyColorEffect(gui_overlay_color.getValue());
    }
    private static void applyColorEffect(Color c) {
        if(!App.isInitialized()) return;
        if(gui_overlay) {            
            // normalize color
            if(gui_overlay_normalize)  c = Color.hsb(c.getHue(), 0.5, 0.5);
            
            Color cl = c;
            // apply effect
            // IMPLEMENT
            windows.forEach( w -> {                
                // apply overlay_norm_factor
                // overlay_norm_factor
//                String s = ".root{ skin-main-color: rgb(" + (int)(cl.getRed()*255) + ","+ (int)(cl.getGreen()*255)+ ","+ (int)(cl.getBlue()*255)+ "); }";
//                w.root.setStyle(s);System.out.println(s);
            });

        } else {
            // disable effect
            // IMPLEMENT
        }
    }
    
    //TODO: this is too much for sch simple task, simplify the colorizing
    private static Subscription playingItemMonitoring;
    private static ChangeListener<Metadata> colorListener;
    
    private static void applyOverlayUseSongColor() {
        if(gui_overlay_use_song) {
            // lazily create and add listener
            if(colorListener==null) {
                colorListener = (o,ov,nv) -> {
                    Color c = nv.getColor();
                    applyColorEffect( c==null ? gui_overlay_color.getValue() : c);
                };
                playingItemMonitoring = Player.playingtem.subscribeToUpdates(item -> colorListener.changed(null,null,item));
                // fire upon binding to create immediate response
                colorListener.changed(null, null, Player.playingtem.get());
            } else {
                colorListener.changed(null, null, Player.playingtem.get());
            }            
        }
        else {
            // remove and destroy listener
            if(colorListener!=null) {
                playingItemMonitoring.unsubscribe();
                colorListener=null;
            }
            applyColorOverlay();
        }
    }
    
    
    
/******************************************************************************/
    
    /** @return new window or null if error occurs during initialization. */
    public static Window create() {
        try {
            Window w = new Window();
                   w.getStage().initOwner(App.getWindowOwner().getStage());
            URL fxml = Window.class.getResource("Window.fxml");
            FXMLLoader l = new FXMLLoader(fxml);
                       l.setRoot(w.root);
                       l.setController(w);
                       l.load();
            w.initialize();
            w.minimizeB.setVisible(false);
                   
            return w;
        } catch (IOException ex) {
            Log.err("Couldnt create Window. " + ex.getMessage());
            return null;
        }   
    }
    
    public static Window createWindowOwner() {
        Window w = new Window();
               w.getStage().initStyle(WindowManager.show_taskbar_icon ? TRANSPARENT : UTILITY);
               w.s.setOpacity(0);
               w.s.setScene(new Scene(new Region()));
        return w;
    }
    
/******************************************************************************/
    
    private LayoutAggregator layout_aggregator = new EmptyLayoutAggregator();
    boolean main = false;
    
    @FXML AnchorPane root = new AnchorPane();
    public @FXML AnchorPane borders;
    @FXML public AnchorPane content;
    @FXML private HBox controls;

    @FXML Button pinB;
    @FXML Button miniB;
    @FXML Button minimizeB;
    @FXML AnchorPane bgrImgLayer;
    public @FXML AnchorPane overlayPane;
    
    private Window() {
        super();
    }
    
    /** Initializes the controller class. */
    private void initialize() {
        getStage().setScene(new Scene(root));
        getStage().setOpacity(windowOpacity.getValue());
        
        // clip the content to its bounds to prevent leaking out
        Rectangle contentMask = new Rectangle(1, 1, Color.BLACK);
        contentMask.widthProperty().bind(content.widthProperty());
        contentMask.heightProperty().bind(content.heightProperty());
        content.setClip(contentMask);
        
//        bgrImgLayer.prefWidthProperty().bind(root.widthProperty());
        
        // avoid some instances of not closing properly
        s.setOnCloseRequest(e -> close());
        
        // disable default exit fullscreen on ESC key
        // it doesnt exit fullscreen properly for this class, because it calls
        // stage.setFullscreen() directly
        s.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        // implement the functionality properly
        s.addEventHandler(KEY_PRESSED, e -> {
            if (e.getCode()==ESCAPE && isFullscreen()) setFullscreen(false);
        });
        
        // :focused pseudoclass
        // root is also assigned a .window styleclass to allow css skinning
        // maintain :focused pseudoclass for .window styleclass
        s.focusedProperty().addListener( (o,ov,nv) -> 
                root.pseudoClassStateChanged(focusedPseudoClass,nv));
        
        // add to list of active windows
        windows.add(this);
        
        // set local shortcuts
        Action.getActions().stream().filter(a->!a.isGlobal()).forEach(a -> a.registerInScene(s.getScene()));
        
        // update coordinates for context manager
        root.addEventFilter(MOUSE_PRESSED,  e -> {
            ContextManager.setX(e.getSceneX());
            ContextManager.setY(e.getSceneY());
        });
        
        // app dragging
        root.addEventHandler(DRAG_DETECTED, this::appDragStart);
        root.addEventHandler(MOUSE_DRAGGED, this::appDragDo);
        root.addEventHandler(MOUSE_RELEASED, this::appDragEnd);
        
        // header double click maximize, show header on/off
        root.setOnMouseClicked(e -> {
            if(e.getButton()==MouseButton.PRIMARY) {
                if(e.getClickCount()==2)
                    toggleMaximize();
            }
            if(e.getButton()==MouseButton.SECONDARY) {
                if(e.getClickCount()==2)
                    setHeaderVisible(!headerVisible);
            }
        });
        
        // change volume on scroll
        // if some component has its own onScroll behavior, it should consume
        // the event so this will not fire
        root.setOnScroll( e -> {
            if(e.getDeltaY()>0) PLAYBACK.incVolume();
            else if(e.getDeltaY()<0) PLAYBACK.decVolume();
        });
        
        root.addEventFilter(KEY_PRESSED, e -> {
            if (e.getCode().equals(Action.Shortcut_ALTERNATE)) {
                GUI.setLayoutMode(true);
    //            SwitchPane.class.cast(layout_aggregator).zoomOut();
                showHeader(true);  
            }
        });
        
        root.addEventFilter(KEY_RELEASED, e -> {
            if (e.getCode().equals(Action.Shortcut_ALTERNATE)) {
                GUI.setLayoutMode(false);
    //            SwitchPane.class.cast(layout_aggregator).zoomIn();

                if(headerVisiblePreference){
                    if(isFullscreen()) {
                        showHeader(false);
                        setBorderless(true);
                    }  else {
                        showHeader(headerVisible);
                        setBorderless(!headerVisible);
                    }
                } else {
                    showHeader(headerVisible);
                    setBorderless(!headerVisible);
                }
            }
        });
        

        
        
    };
    
    public void setAsMain() {
        if (App.getWindow()!=null)
            throw new RuntimeException("Only one window can be main");
        main = true;
        
        setIcon(App.getIcon());
        setTitle(App.getAppName());
        setTitlePosition(Pos.CENTER_LEFT);
        miniB.setVisible(true);
        minimizeB.setVisible(true);
        App.window = this;
    }
    
/******************************* CONTENT **************************************/
    
    public void setContent(Node n) {
        content.getChildren().clear();
        content.getChildren().add(n);
        Util.setAPAnchors(n, 0);
    }
    
    public void setContent(Component c) {
        Layout l = new Layout();
        SwitchPane la = new SwitchPane();
                   la.addTab(0, l);
                   la.setAlwaysAlignTabs(SwitchPane.align_tabs);
        l.setChild(c);
        setLayoutAggregator(la);
    }
    
    public void setLayoutAggregator(LayoutAggregator la) {
        Objects.requireNonNull(la);
        
        
        
        // clear previous content
        layout_aggregator.getLayouts().values().forEach(Layout::close);
        // set new content
        layout_aggregator = la;
        setContent(layout_aggregator.getRoot());
        // load new content
        layout_aggregator.getLayouts().values().forEach(Layout::load);
        
        if(la instanceof SwitchPane) {
            double scaleFactor = 1.25; // to prevent running out of bgr when moving gui
            bgrImgLayer.translateXProperty().unbind();
            bgrImgLayer.setTranslateX(0);
            bgrImgLayer.setScaleX(scaleFactor);
            bgrImgLayer.setScaleY(scaleFactor);
            // scroll bgr along with the tabs
            // using: (|x|/x)*AMPLITUDE*(1-1/(1+SCALE*|x|))  
            // -try at: http://www.mathe-fa.de
            ((SwitchPane)la).translateProperty().addListener((o,oldx,newV) -> {
                double x = newV.doubleValue();
                double space = bgrImgLayer.getWidth()*((scaleFactor-1)/2d);
                double dir = signum(x);
                       x = abs(x);
                bgrImgLayer.setTranslateX(dir * space * (1-(1/(1+0.0005*x))));
            });
            ((SwitchPane)la).zoomProperty().addListener((o,oldx,newV) -> {
                double x = newV.doubleValue();
                       x = 1-(1-x)/5;
                bgrImgLayer.setScaleX(scaleFactor * pow(x, 0.25));
                bgrImgLayer.setScaleY(scaleFactor * pow(x, 0.25));
            });
        }
    }
                
    /**
     * Returns layout aggregator of this window.
     * @return layout aggregator, never null.
     */
    public LayoutAggregator getLayoutAggregator() {
        return layout_aggregator;
    }
    
    /**
     * Blocks input to content, but not to root.
     * <p>
     * Use when any input to content is not desirable, for example during
     * window manipulation like animations.
     * <p>
     * Sometimes content could consume or interfere with the input directed
     * towards the window (root), in such situations this method will help.
     * 
     * @param val 
     */
    public void setContentMouseTransparent(boolean val) {
        content.setMouseTransparent(val);
    }
    
/******************************    HEADER & BORDER    **********************************/
    
    @FXML private BorderPane header;
    @FXML private Region lBorder;
    @FXML private Region rBorder;
    @FXML private ImageView iconI;
    @FXML private Label titleL;
    @FXML private HBox leftHeaderBox;
    private boolean headerVisible = true;
    private boolean headerAllowed = true;
    
    /**
     * Sets visibility of the window header, including its buttons for control
     * of the window (close, etc).
     */
    public void setHeaderVisible(boolean val) {
        // prevent pointless operation
        if(!headerAllowed) return;
        headerVisible = val;
        showHeader(val);
    }
    
    public boolean isHeaderVisible() {
        return headerVisible;
    }
    
    private void showHeader(boolean val) {
        controls.setVisible(val);
        leftHeaderBox.setVisible(val);
        if(val) {
            header.setPrefHeight(25);
            AnchorPane.setTopAnchor(content, 25d);
            AnchorPane.setTopAnchor(lBorder, 25d);
            AnchorPane.setTopAnchor(rBorder, 25d);
            setBorderless(!val);
        } else {
            header.setPrefHeight(5);
            AnchorPane.setTopAnchor(content, 5d);
            AnchorPane.setTopAnchor(lBorder, 5d);
            AnchorPane.setTopAnchor(rBorder, 5d);
        }
    }
    
    /** Set false to permanently hide header. */
    public void setHeaderAllowed(boolean val) {
        setHeaderVisible(val);
        headerAllowed = val;
    }
    
    /** Set title for this window shown in the header.*/
    public void setTitle(String text) {
        titleL.setText(text);
    }
    
    /** Set title alignment. */
    public void setTitlePosition(Pos align) {
        BorderPane.setAlignment(titleL, align);
    }
    
    /** Set icon. Null clears. */
    public void setIcon(Image img) {
        iconI.setImage(img); 
        leftHeaderBox.getChildren().remove(iconI);
//       if(img!=null)leftHeaderBox.getChildren().add(0, iconI);
       
        // github button - show all available FontAwesome icons in a popup
        Label gitB = createIcon(GITHUB,13,"Open github project page for this application",
                e -> browse(App.getGithubLink()));
        // github button - show all available FontAwesome icons in a popup
        Label dirB = createIcon(CSS3,13,"Open css guide",
                e -> browse(URI.create("http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html")));
        // css button - show all available FontAwesome icons in a popup
        Label cssB = createIcon(FOLDER,13,"Open application location (development tool)",
                e -> browse(App.getLocation().toURI()));
        // icon button - show all available FontAwesome icons in a popup
        Label iconsB = createIcon(IMAGE,13,"Icon browser (development tool)",null);
              iconsB.setOnMouseClicked( e -> new PopOver(new IconsBrowser()).show(iconsB));
        // settings button - show application settings in a popup
        Label propB = createIcon(GEARS,13,"Application settings",
                e ->  WidgetManager.find(ConfiguringFeature.class,WidgetSource.NOLAYOUT));
        // manage layout button - sho layout manager in a popp
        Label layB = createIcon(COLUMNS,13,"Manage layouts", 
                e -> ContextManager.showFloating(new LayoutManagerComponent().getPane(), "Layout Manager"));
        // lasFm button - show basic lastFm settings and toggle scrobbling on/off 
              // create graphics once
        Image lastFMon = Util.loadImage(new File("lastFMon.png"), 30);
        Image lastFMoff = Util.loadImage(new File("lastFMoff.png"), 30);
        ImageView lastFMview = new ImageView();
                  lastFMview.setFitHeight(15);
                  lastFMview.setFitWidth(15);         
                  lastFMview.setPreserveRatio(true);
            // maintain proper icon
        ChangeListener<Boolean> fmlistener = (o,ov,nv) ->
              lastFMview.setImage(nv ? lastFMon : lastFMoff);
        LastFMManager.scrobblingEnabledProperty().addListener(fmlistener);
            // initialize proper icon
        fmlistener.changed(null, false, LastFMManager.getScrobblingEnabled());
        
        Label lastFMB = new Label("",lastFMview);
              lastFMB.setTooltip(new Tooltip("LastFM"));
              lastFMB.setOnMouseClicked( e -> {
                  if(e.getButton() == MouseButton.PRIMARY){
                      if(LastFMManager.getScrobblingEnabled()){
                          LastFMManager.toggleScrobbling();
                      } else {
                          if(LastFMManager.isLoginSuccess()){
                              LastFMManager.toggleScrobbling();
                          } else {                        
                              new PopOver("LastFM login", LastFMManager.getLastFMconfig()).show(lastFMB);                        
                          }
                      }
                  }else if(e.getButton() == MouseButton.SECONDARY){
                      new PopOver("LastFM login", LastFMManager.getLastFMconfig()).show(lastFMB);  
                  }
              });
        // lock layout button
        Label lockB = createIcon(GUI.isLayoutLocked() ? LOCK : UNLOCK,13, 
                GUI.isLayoutLocked() ? "Unlock widget layout" : "Lock widget layout", null);
              lockB.setOnMouseClicked( e -> {
                  GUI.toggleLayoutLocked();
                  boolean lck = GUI.isLayoutLocked();
//                  AwesomeDude.setIcon(lockB,lck ? UNLOCK : LOCK,"13");
                  lockB.getTooltip().setText(lck ? "Unlock widget layout" : "Lock widget layout");
              });
            // initialize proper icon
        GUI.layoutLockedProperty().addListener( (o,ov,nv) -> AwesomeDude.setIcon(lockB, nv ? LOCK : UNLOCK, "13"));
        // help button - show hel information
        Label helpB = createIcon(INFO,13,"Help",null);
              helpB.setOnMouseClicked( e -> {
                  PopOver<Text> helpP = PopOver.createHelpPopOver(
                      "Available actions:\n" +
                      "    Header icons : Providing custom functionalities. See tooltips.\n" +
                      "    Header buttons : Providing window contorl. See tooltips.\n" +
                      "    Mouse drag : Move window. Windows snap to screen or to other windows.\n" +
                      "    Mouse drag to screen edge : Activates one of 7 maximized modes.\n" +
                      "    Mouse drag edge : Resizes window.\n" +
                      "    Double left click : Toggle meximized mode on/off.\n" +
                      "    Double right click : Toggle hide header on/off.\n" +
                      "    Press ALT : Show hidden header temporarily.\n" +
                      "    Press ALT : Activate layout mode.\n" +
                      "    Content right drag : drag tabs."
                  );
                  helpP.show(helpB);
                  helpP.getContentNode().setWrappingWidth(400);
                  Action.actionStream.push("Layout info popup");
                });
        // guide button - sho layout manager in a popp
        Label guideB = createIcon(GRADUATION_CAP,13,"Resume or start the guide", e -> {
                   App.guide.resume();
                   Action.actionStream.push("Guide resumed");
              });
              
        leftHeaderBox.getChildren().addAll(gitB,cssB,dirB,iconsB,layB,propB,lastFMB,lockB,helpB,guideB);
    }
    
/**************************** WINDOW MECHANICS ********************************/
    
    @Override
    public void close() {
        // serialize windows if this is main app window
        if(main) WindowManager.serialize();
        // after serialisation, close content it could prevent app closing
        // normally if it runs bgr threads + we want to sae it
        layout_aggregator.getLayouts().values().forEach(Layout::close);
        // remove from window list as life time of this ends
        windows.remove(this); 
        if(main) {
            // close all pop overs first (or we risk an exception and not closing app
            // properly - PopOver bug
            // also have to create new list or we risk ConcurrentModificationError
            new ArrayList<>(PopOver.active_popups).forEach(PopOver::hideImmediatelly);
            // act as main window and close whole app
            App.getWindowOwner().close();
        }
        // in the end close itself
        super.close();
        
        if(main)  App.close();
        else App.getWindow().focus();
    }
    
   @Override  
    public void setFullscreen(boolean v) {  
        super.setFullscreen(v);
        // handle header visibility
        if(headerVisiblePreference){
            if(v)showHeader(false);
            else showHeader(headerVisible);
        } else {
            setHeaderVisible(!headerVisible);
        }
        setBorderless(v);
    }
    
    public boolean isBorderless() {
        return AnchorPane.getBottomAnchor(content)==0;
    }
    
    public void setBorderless(boolean v) {
        if(v) setAPAnchors(content, 0);
        else setAPAnchors(content, 25,5,5,5);
        borders.setVisible(!v);
    }
    
    @FXML public void toggleMini() {
        WindowManager.toggleMini();
    }

    
/*********************************    DRAGGING   ******************************/
    
    private double appX;
    private double appY;
    private boolean app_drag = false;
    
    private void appDragStart(MouseEvent e) {
        // disable when being resized, resize starts at mouse pressed so
        // it can not consume drag detected event and prevent dragging
        // should be fixed
        if (e.getButton()!=MouseButton.PRIMARY || isResizing()) return;
        
        app_drag = true;
        appX = e.getSceneX();
        appY = e.getSceneY();
    }
    private void appDragDo(MouseEvent e) {
        if(!app_drag || e.getButton()!=MouseButton.PRIMARY) return;
        
        
        double X = e.getScreenX();
        double Y = e.getScreenY();
        
        // get screen
        Screen screen = getScreen(X,Y);
        
        
        double SWm = screen.getBounds().getMinX(); //screen_wbegin
        double SHm = screen.getBounds().getMinY(); //screen_wbegin
        double SW = screen.getBounds().getMaxX(); //screen_width
        double SH = screen.getBounds().getMaxY(); //screen_height
        double SW5 = screen.getBounds().getWidth()/5;
        double SH5 = screen.getBounds().getHeight()/5;
                
        if (isMaximized() == Maximized.NONE)
            setXY(X - appX, Y - appY);
        
        // (imitate Aero Snap)
        Maximized to;
        
        //left screen edge
        if (X <= SWm +10) {
            if (Y<SHm + SH5) to = Maximized.LEFT_TOP;
            else if (Y<SH-SH5) to = Maximized.LEFT;
            else to = Maximized.LEFT_BOTTOM;
        // left screen part
        } else if (X<SWm+SW5) {
            if(Y<=SHm) to = Maximized.LEFT_TOP;
            else if (Y<SH-1) to = Maximized.NONE;
            else to = Maximized.LEFT_BOTTOM;
        // middle screen
        } else if (X<SW-SW5) {
            if(Y<=SHm) to = Maximized.ALL;
            else if (Y<SH-1) to = Maximized.NONE;
            else to = Maximized.NONE;
        // right screen part
        } else if (X<SW-10) {
            if(Y<=SHm) to = Maximized.RIGHT_TOP;
            else if (Y<SH-1) to = Maximized.NONE;
            else to = Maximized.RIGHT_BOTTOM;
        // right screen edge
        } else {
            if (Y<SHm + SH5) to = Maximized.RIGHT_TOP;
            else if (Y<SH-SH5) to = Maximized.RIGHT;
            else to = Maximized.RIGHT_BOTTOM;
        }
        
        setScreen(screen);
        setMaximized(to);
    }
    private void appDragEnd(MouseEvent e) {
        app_drag = false;
    }

    
/*******************************    RESIZING    *******************************/
        
    @FXML
    private void border_onDragStart(MouseEvent e) {
        double X = e.getSceneX();
        double Y = e.getSceneY();
        double WW = getWidth();
        double WH = getHeight();
        double L = 18; // corner treshold

        if ((X > WW - L) && (Y > WH - L)) {
            is_being_resized = SE;
        } else if ((X < L) && (Y > WH - L)) {
            is_being_resized = SW;
        } else if ((X < L) && (Y < L)) {
            is_being_resized = NW;
        } else if ((X > WW - L) && (Y < L)) {
            is_being_resized = NE;
        } else if ((X > WW - L)) {
            is_being_resized = E;
        } else if ((Y > WH - L)) {
            is_being_resized = S;
        } else if ((X < L)) {
            is_being_resized = W;
        } else if ((Y < L)) {
            is_being_resized = N;
        }
        e.consume();
    }

    @FXML
    private void border_onDragEnd(MouseEvent e) {
        is_being_resized = NONE;
        e.consume();
    }

    @FXML
    private void border_onDragged(MouseEvent e) {
        if (is_being_resized == SE) {
            setSize(e.getScreenX() - getX(), e.getScreenY() - getY());
        } else if (is_being_resized == S) {
            setSize(getWidth(), e.getScreenY() - getY());
        } else if (is_being_resized == E) {
            setSize(e.getScreenX() - getX(), getHeight());
        } else if (is_being_resized == SW) {
            setSize(getX()+getWidth()-e.getScreenX(), e.getScreenY() - getY());
            setXY(e.getScreenX(), getY());
        } else if (is_being_resized == W) {
            setSize(getX()+getWidth()-e.getScreenX(), getHeight());
            setXY(e.getScreenX(), getY());
        } else if (is_being_resized == NW) {
            setSize(getX()+getWidth()-e.getScreenX(), getY()+getHeight()-e.getScreenY());
            setXY(e.getScreenX(), e.getScreenY());
        } else if (is_being_resized == N) {
            setSize(getWidth(), getY()+getHeight()-e.getScreenY());
            setXY(getX(), e.getScreenY());
        } else if (is_being_resized == NE) {
            setSize(e.getScreenX() - getX(), getY()+getHeight()-e.getScreenY());
            setXY(getX(), e.getScreenY());
        }
        e.consume();
    }
    
/******************************************************************************/
    
    @FXML
    private void consumeMouseEvent(MouseEvent e) {
        e.consume();
    }
    
    @FXML  
    private void entireArea_OnKeyPressed(KeyEvent e) {  
//        if (e.getCode().equals(Action.Shortcut_ALTERNATE)) {
//            GUI.setLayoutMode(true);
////            SwitchPane.class.cast(layout_aggregator).zoomOut();
//            showHeader(true);  
//        }
    }  

    @FXML  
    private void entireArea_OnKeyReleased(KeyEvent e) {
//        if (e.getCode().equals(Action.Shortcut_ALTERNATE)) {
//            GUI.setLayoutMode(false);
////            SwitchPane.class.cast(layout_aggregator).zoomIn();
//            
//            if(headerVisiblePreference){
//                if(isFullscreen()) {
//                    showHeader(false);
//                    setBorderless(true);
//                }  else {
//                    showHeader(headerVisible);
//                    setBorderless(!headerVisible);
//                }
//            } else {
//                showHeader(headerVisible);
//                setBorderless(!headerVisible);
//            }
//        }
    }
    
/**************************** SERIALIZATION ***********************************/
    
    @Override
    public void serialize(Window w, File f) throws IOException {
        XStream xstream = new XStream(new DomDriver());
        xstream.autodetectAnnotations(true);
        xstream.registerConverter(new Window.WindowConverter());
        xstream.toXML(w, new BufferedWriter(new FileWriter(f)));
    }

    public static Window deserialize(File f) throws IOException {
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.registerConverter(new Window.WindowConverter());
            return (Window) xstream.fromXML(f);
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load window from the file: " + f.getPath() +
                    ". The file not found or content corrupted. ");
            throw new IOException();
        }
    }
    public static Window deserializeSuppressed(File f) {
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.registerConverter(new Window.WindowConverter());
            return (Window) xstream.fromXML(f);
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load window from the file: " + f.getPath() +
                    ". The file not found or content corrupted. ");
            return null;
        }
    }
    
    public static final class WindowConverter implements Converter {
        
        @Override
        public boolean canConvert(Class type) {
            return Window.class.equals(type);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
            Window w = (Window) value;
            writer.startNode("W");
            writer.setValue(w.WProp.getValue().toString());
            writer.endNode();
            writer.startNode("H");
            writer.setValue(w.HProp.getValue().toString());
            writer.endNode();
            writer.startNode("X");
            writer.setValue(w.XProp.getValue().toString());
            writer.endNode();
            writer.startNode("Y");
            writer.setValue(w.YProp.getValue().toString());
            writer.endNode();
            writer.startNode("minimized");
            writer.setValue(w.s.iconifiedProperty().getValue().toString());
            writer.endNode();
            writer.startNode("maximized");
            writer.setValue(w.MaxProp.getValue().toString());
            writer.endNode();
            writer.startNode("fullscreen");
            writer.setValue(w.FullProp.getValue().toString());
            writer.endNode();
            writer.startNode("resizable");
            writer.setValue(w.s.resizableProperty().getValue().toString());
            writer.endNode();
            writer.startNode("alwaysOnTop");
            writer.setValue(w.s.alwaysOnTopProperty().getValue().toString());
            writer.endNode();
            writer.startNode("layout-aggregator-type");
            writer.setValue(w.layout_aggregator.getClass().getName());
            writer.endNode();
            writer.startNode("header-visible");
            writer.setValue(String.valueOf(w.isHeaderVisible()));
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Window w = Window.create();
            if(w==null) return null;
            
            reader.moveDown();
            w.WProp.set(Double.parseDouble(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.HProp.set(Double.parseDouble(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.XProp.set(Double.parseDouble(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.YProp.set(Double.parseDouble(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.s.setIconified(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.MaxProp.set(Maximized.valueOf(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.FullProp.set(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.s.setResizable(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.setAlwaysOnTop(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            reader.getValue(); // ignore layout aggregator
            reader.moveUp();
            reader.moveDown();
            w.setHeaderVisible(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            return w;
        }
    }

}