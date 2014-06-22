
package GUI;

import Action.Action;
import AudioPlayer.playback.PLAYBACK;
import Configuration.Configuration;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.objects.ClickEffect;
import GUI.objects.PopOver.PopOver;
import GUI.objects.Window.Resize;
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
import PseudoObjects.Maximized;
import static PseudoObjects.Maximized.LEFT_BOTTOM;
import static PseudoObjects.Maximized.LEFT_TOP;
import static PseudoObjects.Maximized.RIGHT_BOTTOM;
import static PseudoObjects.Maximized.RIGHT_TOP;
import Serialization.Serializes;
import Serialization.SerializesFile;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.IOException;
import static java.lang.Boolean.TRUE;
import java.net.URL;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.ALT;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.util.Duration;
import main.App;
import static utilities.Animation.Interpolators.EasingMode.EASE_OUT;
import utilities.Animation.Interpolators.ElasticInterpolator;
import utilities.Log;

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
public class Window extends WindowBase implements SerializesFile, Serializes {
        
    @IsConfig(name="Window header visiblility preference", info=
                                 "Remembers header state for both fullscreen" +
                                 " and not. When selected 'auto off' is true ")
    public static boolean headerOnPreference = true;
    
    /** @return new window or null if error occurs during initialization. */
    public static Window create() {
        return create(false);
    }
    /** @return new Window or null if error occurs during initialization. */
    public static Window create(boolean main) {
        try {
            Window w = new Window(main);
            if(App.getWindowOwner()!=null) w.getStage().initOwner(App.getWindowOwner().getStage());
            URL fxml = Window.class.getResource("Window.fxml");
            FXMLLoader l = new FXMLLoader(fxml);
                       l.setRoot(w.root);
                       l.setController(w);
                       l.load();
            w.initialize();
            w.minimizeB.setVisible(main);
            
            if(main) {
                // activare main window functionality
                w.layB.setVisible(TRUE);
                w.miniB.setVisible(TRUE);
                // load main window content
                FXMLLoader loader = new FXMLLoader(Window.class.getResource("UI.fxml"));
                Parent ui = (Parent) loader.load();
                w.setContent(ui);
                w.setIcon(App.getIcon());
                w.setTitle(App.getAppName());
                w.setTitlePosition(Pos.CENTER_LEFT);
                UIController uic = (UIController)loader.getController();
                new ContextManager(uic);
                ContextManager.gui = uic; System.out.println(ContextManager.gui + " ggg ");
            }
                   
            return w;
        } catch (IOException ex) {
            Log.err("Couldnt create Window. " + ex.getMessage());
            return null;
        }   
    }
    
/******************************************************************************/
    
    Layout layout;
    @FXML AnchorPane root = new AnchorPane();
    @FXML public AnchorPane content;
    @FXML private HBox controls;

    @FXML Button pinB;
    @FXML Button layB;
    @FXML Button miniB;
    @FXML Button minimizeB;
    
    private Window(boolean is_main) {
        super(is_main);
    }
    
    /** Initializes the controller class. */
    private void initialize() {
        getStage().setScene(new Scene(root));
        
        // add to list of active windows
        ContextManager.windows.add(this);
        // maintain active (focused) window
        getStage().focusedProperty().addListener((o,oldV,newV)-> ContextManager.activeWindow=this );
        // set shortcuts
        Action.getActions().values().stream().filter(a->!a.isGlobal()).forEach(Action::register);
        
        root.addEventFilter(MOUSE_MOVED, e -> {
            // update coordinates for context manager
            ContextManager.setX(e.getSceneX());
            ContextManager.setY(e.getSceneY());
            if (ClickEffect.trail_effect) ClickEffect.run(e.getSceneX(), e.getSceneY());
        });
        root.addEventFilter(MOUSE_PRESSED,  e -> {
            if (!ClickEffect.trail_effect) ClickEffect.run(e.getSceneX(), e.getSceneY());
            ContextManager.closeMenus();
            ContextManager.closeFloatingWindows(this);
            PopOver.autoCloseFire();
            startAppDrag(e);
        });
        root.setOnMouseDragged( e -> {
            if (e.getButton()==MouseButton.PRIMARY)
                dragApp(e);
        });
        
        // header double click maximize, show header on/off
        header.setOnMouseClicked( e -> {
            if(e.getButton()==MouseButton.PRIMARY) {
                if(e.getClickCount()==2)
                    toggleMaximize();
            }
            if(e.getButton()==MouseButton.SECONDARY) {
                if(e.getClickCount()==2)
                    setShowHeader(!showHeader);
            }
        });
        
        // change volume on scroll
        // if some component has its own onScroll behavior, it should consume
        // the event so this one will not fire
        root.setOnScroll( e -> {
            if(e.getDeltaY()>0) PLAYBACK.incVolume();
            else if(e.getDeltaY()<0) PLAYBACK.decVolume();
        });
        
        
        Rectangle r = new Rectangle(15, 15, Color.BLACK);
                  r.widthProperty().bind(controls.widthProperty().subtract(20));
                  r.heightProperty().bind(controls.heightProperty());
                  r.relocate(controls.getLayoutX()+controls.getWidth()-20,controls.getLayoutY());
//        controls.setClip(r);
        
        TranslateTransition t = new TranslateTransition(Duration.millis(450), r);
                            t.setInterpolator(new ElasticInterpolator(EASE_OUT));
        header.setOnMouseEntered(e -> {
//            t.stop();
            t.setByX(-100);
            t.play();
        });
        header.setOnMouseExited(e -> {
//            t.stop();
            t.setToX(100);
            t.play();
        });
    };
    
/******************************* CONTENT **************************************/
    
    public void setContent(Node n) {
        content.getChildren().setAll(n);
        AnchorPane.setBottomAnchor(n, 0.0);
        AnchorPane.setRightAnchor(n, 0.0);
        AnchorPane.setLeftAnchor(n, 0.0);
        AnchorPane.setTopAnchor(n, 0.0);
    }
    public void setContent(Component c) {
        content.getChildren().clear();
        layout = new Layout();
        layout.setParentPane(content);
        layout.load();
        layout.setChild(c);
    }
    public Layout getLayout() {
        return layout;
    }
    
/******************************    HEADER & BORDER    **********************************/
    
    @FXML private BorderPane header;
    @FXML private Region lBorder;
    @FXML private Region rBorder;
    @FXML ImageView iconI;
    @FXML Label titleL;
    @FXML HBox leftHeaderBox;
    private boolean showHeader = true;
    
    /**
     * Sets visibility of the window header, including its buttons for control
     * of the window (close, etc).
     */
    public void setShowHeader(boolean val) {
        showHeader = val;
        showHeader(val);
    }
    private void showHeader(boolean val) {
        controls.setVisible(val);
        leftHeaderBox.setVisible(val);
        if(val) {
            header.setPrefHeight(25);
            AnchorPane.setTopAnchor(content, 25d);
            AnchorPane.setTopAnchor(lBorder, 25d);
            AnchorPane.setTopAnchor(rBorder, 25d);
        } else {
            header.setPrefHeight(5);
            AnchorPane.setTopAnchor(content, 5d);
            AnchorPane.setTopAnchor(lBorder, 5d);
            AnchorPane.setTopAnchor(rBorder, 5d);
        }
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
        // correct title padding to avoid gap
        if(img==null) header.setPadding(new Insets(0,0,0,0));
        else header.setPadding(new Insets(0,0,0,25));
    }
    
/**************************** WINDOW MECHANICS ********************************/
    
    private boolean isPopup = false;
    private boolean autoclose = true;
    
    /** 
     * Set false to get normal behavior. Set true to enable popup-like autoclosing
     * that can be turned off. Setting this to false will cause autoclosing
     * value be ignored. Default false;
     */
    public void setIsPopup(boolean val) {
        isPopup = val;
        pinB.setVisible(val);
    }
    public boolean isPopup() {
        return isPopup;
    }
    /**
     * Set autoclose functinality. If isPopup==false this property is ignored.
     * False is standard window behavior. Setting
     * true will cause the window to close on mouse click occuring anywhere
     * within the application and outside of this window - like popup. Default
     * is false.
     */
    public void setAutoClose(boolean val) {
        autoclose = val;
    }
    public boolean isAutoClose() {
        return autoclose;
    }
    public void toggleAutoClose() {
        autoclose = !autoclose;
    }

    @Override
    public void close() {
        super.close();
        if(layout !=null) layout.close();
        // remove from window list as life time of this ends
        ContextManager.windows.remove(this);    
        // remove reference if exist
        if(this.equals(ContextManager.activeWindow))
            ContextManager.activeWindow=null;
    }
    
    /**
     * Closes window if (isPopup && autoclose) evaluates to true. This method is
     * designed specifically for auto-closing functionality.
     */
    public void closeWeak() {
        if(isPopup && autoclose) close(); 
    }

    
   @Override  
    public void setFullscreen(boolean val) {  
        super.setFullscreen(val);
        if(headerOnPreference){
            if(val)showHeader(false);
            else showHeader(showHeader);
        }
        else setShowHeader(!showHeader);  
    }
    
    @FXML public void toggleMini() {
        WindowManager.toggleMini();
    }
    @FXML public void manageLayouts() {
        ContextManager.openFloatingWindow((new LayoutManagerComponent()).getPane(), "Layout Manager");
    }

    
/*********************************    DRAGGING   ******************************/
    
    private double appX;
    private double appY;
    
    private void startAppDrag(MouseEvent e) {
        appX = e.getSceneX();
        appY = e.getSceneY();
//        e.consume();
    }
    private void dragApp(MouseEvent e) {
        double SW = Screen.getPrimary().getVisualBounds().getWidth(); //screen_width
        double SH = Screen.getPrimary().getVisualBounds().getHeight(); //screen_height
        double SX = e.getScreenX();
        double SY = e.getScreenY();
        double SH10 = SH/5;
        double SW10 = SW/5;
        
        if (isMaximised() == Maximized.NONE) {
            setLocation(SX - appX, SY - appY);
            // (imitate Aero Snap)
            // misbehaves without -1 in the conditions
            if ((SX <= SW10 && SY <= 0) || (SX <= 0 && SY <= SH10))
                setMaximized(LEFT_TOP);
            else if ((SX >= SW-SW10 && SY <= 0) || (SX >= SW-1 && SY <= SH10))
                setMaximized(RIGHT_TOP);
            else if ((SX <= SW10 && SY >= SH-1) || (SX <= 0 && SY >= SH-SH10))
                setMaximized(LEFT_BOTTOM);
            else if ((SX >= SW-SW10 && SY >= SH-1) || (SX >= SW-1 && SY >= SH-SH10))
                setMaximized(RIGHT_BOTTOM);
            else if (e.getScreenY() <= 0)
                setMaximized(Maximized.ALL);
            else if (e.getScreenX() <= 0)
                setMaximized(Maximized.LEFT);
            else if (e.getScreenX() >= SW-1)  
                setMaximized(Maximized.RIGHT);
        } else {
            // demaximize if not touching LEFT,TOP,RIGHT borders and if
            // not touching BOTTOM border on the left or right
            if ((SY>0 && SX>0 && SX<SW-1) && !(SY>SH-10 && (SW<=SW10 || SW>=SW-SW10)))
                setMaximized(Maximized.NONE);
        }
        e.consume();
    }

    
/*******************************    RESIZING    *******************************/
    
    private Resize resizing = NONE;
    
    @FXML
    private void border_onDragStart(MouseEvent e) {
        double X = e.getSceneX();
        double Y = e.getSceneY();
        double Wi = getWidth();
        double H = getHeight();
        double L = Configuration.borderCorner;

        if ((X > Wi - L) && (Y > H - L)) {
            resizing = SE;
        } else if ((X < L) && (Y > H - L)) {
            resizing = SW;
        } else if ((X < L) && (Y < L)) {
            resizing = NW;
        } else if ((X > Wi - L) && (Y < L)) {
            resizing = NE;
        } else if ((X > Wi - L)) {
            resizing = E;
        } else if ((Y > H - L)) {
            resizing = S;
        } else if ((X < L)) {
            resizing = W;
        } else if ((Y < L)) {
            resizing = N;
        }
        e.consume();
    }

    @FXML
    private void border_onDragEnd(MouseEvent e) {
        resizing = NONE;
        e.consume();
    }

    @FXML
    private void border_onDragged(MouseEvent e) {
        if (resizing == SE) {
            setSize(e.getScreenX() - getX(), e.getScreenY() - getY());
        } else if (resizing == S) {
            setSize(getWidth(), e.getScreenY() - getY());
        } else if (resizing == E) {
            setSize(e.getScreenX() - getX(), getHeight());
        } else if (resizing == SW) {
            setSize(getX()+getWidth()-e.getScreenX(), e.getScreenY() - getY());
            setLocation(e.getScreenX(), getY());
        } else if (resizing == W) {
            setSize(getX()+getWidth()-e.getScreenX(), getHeight());
            setLocation(e.getScreenX(), getY());
        } else if (resizing == NW) {
            setSize(getX()+getWidth()-e.getScreenX(), getY()+getHeight()-e.getScreenY());
            setLocation(e.getScreenX(), e.getScreenY());
        } else if (resizing == N) {
            setSize(getWidth(), getY()+getHeight()-e.getScreenY());
            setLocation(getX(), e.getScreenY());
        } else if (resizing == NE) {
            setSize(e.getScreenX() - getX(), getY()+getHeight()-e.getScreenY());
            setLocation(getX(), e.getScreenY());
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
        if (e.getCode().equals(KeyCode.getKeyCode(Action.Shortcut_ALTERNATE)))  
            GUI.setLayoutMode(true);  
        if (e.getCode()==ALT )  
            showHeader(true);  
    }  

    @FXML  
    private void entireArea_OnKeyReleased(KeyEvent e) {
        if (e.getCode().equals(KeyCode.getKeyCode(Action.Shortcut_ALTERNATE)))  
            GUI.setLayoutMode(false);  
        if (e.getCode()==ALT)  
            if(headerOnPreference){
                if(isFullscreen()) showHeader(false); 
                else showHeader(showHeader);
            }
            else showHeader(showHeader);
    } 
    
    public static final class WindowConverter implements Converter {
        @Override
        public boolean canConvert(Class type) {
            return type.equals(Window.class);
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
            writer.setValue(w.ResProp.getValue().toString());
            writer.endNode();
            writer.startNode("alwaysOnTop");
            writer.setValue(w.s.alwaysOnTopProperty().getValue().toString());
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
            Boolean.parseBoolean(reader.getValue());
            reader.moveUp();
            reader.moveDown();
            w.MaxProp.set(Maximized.valueOf(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.FullProp.set(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.ResProp.set(Boolean.parseBoolean(reader.getValue()));
            reader.moveUp();
            reader.moveDown();
            w.ResProp.set(Boolean.parseBoolean(reader.getValue()));
            Boolean.parseBoolean(reader.getValue());                            // this should be fixed!
            return w;
        }
    }

}