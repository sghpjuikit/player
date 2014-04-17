
package GUI;

import AudioPlayer.playback.PLAYBACK;
import Configuration.Configuration;
import GUI.objects.ClickEffect;
import Layout.Component;
import Layout.Layout;
import PseudoObjects.Maximized;
import Serialization.Serializes;
import Serialization.SerializesFile;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import utilities.Log;

/**
 * FXML Controller class
 */
public class Window extends WindowBase implements SerializesFile, Serializes {
    
    Layout layout;
    @FXML AnchorPane root = new AnchorPane();
    @FXML public AnchorPane content;
    @FXML private HBox controls;
    @FXML private BorderPane header;
    @FXML private Region lBorder;
    @FXML private Region rBorder;
    @FXML Label titleL;
    @FXML Button pinB;
    
    private Window() {}
    
    /**
     * @return new Window or null if error occurs during loading or initialization.
     */
    public static Window create() {
        try {
            Window controller = new Window();
            URL fxml = Window.class.getResource("Window.fxml");
            FXMLLoader l = new FXMLLoader(fxml);
                       l.setRoot(controller.root);
                       l.setController(controller);
                       l.load();
                   controller.initialize();
            return controller;
        } catch (IOException ex) {
            Log.err("Couldnt create Window. " + ex.getMessage());
            return null;
        }        
    }
    
    /** Initializes the controller class. */
    private void initialize() {
        Scene scene = new Scene(root);
        getStage().setScene(scene);
        
        root.setOnMouseMoved( e -> {
            // update coordinates for context manager
            ContextManager.setX(e.getX());
            ContextManager.setY(e.getY());
            if (ClickEffect.trail_effect) ClickEffect.run(e.getX(), e.getY());
        });
        root.setOnMousePressed( e -> {
            if (!ClickEffect.trail_effect) ClickEffect.run(e.getX(), e.getY());
            ContextManager.closeMenus();
            ContextManager.closeFloatingWindows(this);
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
        header.setOnScroll( e -> {
            if(e.getDeltaY()>0) PLAYBACK.incVolume();
            else if(e.getDeltaY()<0) PLAYBACK.decVolume();
        });
    };
    
/******************************    BASICS    **********************************/
    
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
    
    private boolean showHeader = true;
    /**
     * Sets visibility of the window header, including its buttons for control
     * of the window (close, etc).
     */
    public void setShowHeader(boolean val) {
        showHeader= val;
        controls.setVisible(val);
        titleL.setVisible(val);
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
        ContextManager.windows.remove(this);
    }
    
    /**
     * Closes window if (isPopup && autoclose) evaluates to true. This method is
     * designed specifically for autoclosing functionality.
     */
    public void closeWeak() {
        if(isPopup && autoclose) close(); 
    }

    
/*********************************    DRAGGING   ******************************/
    
    private double appX;
    private double appY;
    
    private void startAppDrag(MouseEvent e) {
        appX = e.getSceneX();
        appY = e.getSceneY();
        e.consume();
    }
    private void dragApp(MouseEvent e) {
        if (isMaximised() == Maximized.NONE) {
            setLocation(e.getScreenX() - appX, e.getScreenY() - appY);

            double SW = Screen.getPrimary().getVisualBounds().getWidth(); //screen_width
            // (imitate Aero Snap)
            if (e.getScreenY() <= 0)
                    setMaximized(Maximized.ALL);
            else if (e.getScreenX() <= 0)
                    setMaximized(Maximized.LEFT);
            else if (e.getScreenX() >= SW - 1)  //for some reason it wont work without -1
                    setMaximized(Maximized.RIGHT);
        }
        else {
            double SW = Screen.getPrimary().getVisualBounds().getWidth(); //screen_width
            if (e.getScreenY()>0 && e.getScreenX()>0 && e.getScreenX()<SW-1)
            setMaximized(Maximized.NONE);
        }
        e.consume();
    }

    
/*******************************    RESIZING    *******************************/
    
    private Resize resizing = Resize.NONE;
    
    @FXML
    private void border_onDragStart(MouseEvent e) {
        double X = e.getSceneX();
        double Y = e.getSceneY();
        double W = getWidth();
        double H = getHeight();
        double L = Configuration.borderCorner;

        if ((X > W - L) && (Y > H - L)) {
            resizing = Resize.SE;
        } else if ((X < L) && (Y > H - L)) {
            resizing = Resize.SW;
        } else if ((X < L) && (Y < L)) {
            resizing = Resize.NW;
        } else if ((X > W - L) && (Y < L)) {
            resizing = Resize.NE;
        } else if ((X > W - L)) {
            resizing = Resize.E;
        } else if ((Y > H - L)) {
            resizing = Resize.S;
        } else if ((X < L)) {
            resizing = Resize.W;
        } else if ((Y < L)) {
            resizing = Resize.N;
        }
        e.consume();
    }

    @FXML
    private void border_onDragEnd(MouseEvent e) {
        resizing = Resize.NONE;
        e.consume();
    }

    @FXML
    private void border_onDragged(MouseEvent e) {
        if (resizing == Resize.SE) {
            setSize(e.getScreenX() - getX(), e.getScreenY() - getY());
        } else if (resizing == Resize.S) {
            setSize(getWidth(), e.getScreenY() - getY());
        } else if (resizing == Resize.E) {
            setSize(e.getScreenX() - getX(), getHeight());
        } else if (resizing == Resize.SW) {
            setSize(getX()+getWidth()-e.getScreenX(), e.getScreenY() - getY());
            setLocation(e.getScreenX(), getY());
        } else if (resizing == Resize.W) {
            setSize(getX()+getWidth()-e.getScreenX(), getHeight());
            setLocation(e.getScreenX(), getY());
        } else if (resizing == Resize.NW) {
            setSize(getX()+getWidth()-e.getScreenX(), getY()+getHeight()-e.getScreenY());
            setLocation(e.getScreenX(), e.getScreenY());
        } else if (resizing == Resize.N) {
            setSize(getWidth(), getY()+getHeight()-e.getScreenY());
            setLocation(getX(), e.getScreenY());
        } else if (resizing == Resize.NE) {
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
        if (e.getCode().equals(KeyCode.getKeyCode(Configuration.Shortcut_ALTERNATE)))
            GUI.setLayoutMode(true);
    }

    @FXML
    private void entireArea_OnKeyReleased(KeyEvent e) {
        if (e.getCode().equals(KeyCode.getKeyCode(Configuration.Shortcut_ALTERNATE)))
            GUI.setLayoutMode(false);
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

            return w;
        }
    }

}