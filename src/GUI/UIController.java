
package GUI;

import Configuration.Configuration;
import GUI.Components.LayoutManagerComponent;
import GUI.objects.ClickEffect;
import Layout.Layout;
import Layout.LayoutManager;
import PseudoObjects.Maximized;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.util.Duration;
import main.App;
import utilities.Animation.Interpolators.CircularInterpolator;
import utilities.Animation.Interpolators.EasingMode;

/**
 * FXML Controller class
 */
public class UIController {
    @FXML AnchorPane border;
    @FXML public AnchorPane entireArea;
    @FXML public AnchorPane layout;
    @FXML public AnchorPane ui;
    @FXML public AnchorPane overlayPane;
    @FXML public AnchorPane contextPane;
    @FXML public Pane colorEffectPane;
    
    //app drag

    private Resize resizing = Resize.NONE;
    private final WindowBase window = App.getInstance().getWindow();

    /** Initializes the controller class. */
    public void initialize() {
        // initialize Contextmanager
        new ContextManager(this);

        entireArea.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            // update coordinates for context manager
            ContextManager.setX(e.getX());
            ContextManager.setY(e.getY());
            if (ClickEffect.trail_effect) ClickEffect.run(e.getX(), e.getY());
        });
        entireArea.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!ClickEffect.trail_effect) ClickEffect.run(e.getX(), e.getY());
            ContextManager.closeMenus();
            ContextManager.closeFloatingWindows(null);
            startAppDrag(e);
        });
        entireArea.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.getButton()==MouseButton.PRIMARY)
                dragApp(e);
        });
        
        layout.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            disable_menus=false;
            if(e.getButton()==MouseButton.SECONDARY)
                startUiDrag(e);
//            else
//                startAppDrag(e);
        });
        layout.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if(e.getButton()==MouseButton.SECONDARY)
                dragUi(e);
//            else
//                dragApp(e)
        });
        
        layout.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if(!e.isStillSincePress()) disable_menus=true;
            if(e.getButton()==MouseButton.SECONDARY)
                endUIDrag(e);
        });
        
        uiDrag = new TranslateTransition(Duration.millis(400),ui);
        uiDrag.setInterpolator(new CircularInterpolator(EasingMode.EASE_OUT));
        
        entireArea.widthProperty().addListener(l->
            tabs.forEach((i,p)->p.setLayoutX(i*(uiWidth()+5)))
        );
    };

/********************************    TABS   ***********************************/
    
    public final Map<Integer,AnchorPane> tabs = new HashMap<>();
    
    public void addTab(final int i) {        
        if (tabs.containsKey(i)) return;
        
        double width = uiWidth();
        double pos = getTabX(i);
        
        AnchorPane t = new AnchorPane();
        ui.getChildren().add(t);
        tabs.put(i,t);
            t.setPrefWidth(width);      // standard width
            t.setLayoutX(pos);
            t.prefWidthProperty().bind(ui.widthProperty());
        
            AnchorPane.setTopAnchor(t, 0.0);
            AnchorPane.setBottomAnchor(t, 0.0);
        
        LayoutManager.active.putIfAbsent(i, new Layout("layout"+i));
        LayoutManager.active.get(i).load(t);
    }
    public void removeTab(int i) {
        if(tabs.containsKey(i))
            ui.getChildren().remove(tabs.get(i));
            tabs.remove(i);
            LayoutManager.active.remove(i);
    }

    
/****************************  TAB  ANIMATIONS   ******************************/
    
    /** menus cant open if this is true. prevention from opening menus in 
     *  undesired situations, like dragging
     */
    boolean disable_menus = false;
    private TranslateTransition uiDrag;
    private double uiCurrX;
    private double uiStartX;
    boolean uiDragActive = false;
    
    private void startUiDrag(MouseEvent e) {
        uiDrag.stop();
        uiStartX = e.getSceneX();
        uiCurrX = ui.getTranslateX();
        uiDragActive = true;
        e.consume();
    }
    private void endUIDrag(MouseEvent e) {
        if(!uiDragActive) return;
        uiDragActive = false;
        
        if(GUI.align_tabs)      // switch tabs if allowed
            alignTabs(e);
        else {                  
            int currT = currTab();
            double is = ui.getTranslateX();
            double should_be = -getTabX(currT);
            double dist = Math.abs(is-should_be);
            double treshold = uiWidth()/10;
            if(Math.abs(dist) < treshold) {             // else animation snap if close to edge 
                uiDrag.setOnFinished( a -> addTab(currT));
                uiDrag.setToX(should_be);
                uiDrag.play();
            } else {                                    // ease out manual drag animation
                double dir = Math.signum(e.getSceneX()-uiStartX);
                uiDrag.setOnFinished( a -> addTab(currT));
                uiDrag.setToX(is + treshold/2 * dir);
                uiDrag.play();
            }
        }
        e.consume();
    }
    private void dragUi(MouseEvent e) {
        if(!uiDragActive) return;
        
        double byX = e.getSceneX()-uiStartX;
        ui.setTranslateX(uiCurrX + byX);
         
        if(GUI.snapping) {        // snap closest
            double distance = ui.getTranslateX()%(uiWidth()+5);
            if(Math.abs(distance)<GUI.snapDistance) {
                ui.setTranslateX(getTabX(currTab()));
            }
        }
        e.consume();
    }
    private void alignTabs(MouseEvent e) {
        double dist = e.getSceneX()-uiStartX;   // distance
        int byT = 0;                            // tabs to travel by

        double dAbs = Math.abs(dist);
        if (dAbs > layout.getWidth()*GUI.dragFraction || dAbs > GUI.dragDistance)
            byT = (int) - Math.signum(dist);

        int currentT = (int) Math.rint(-1*uiCurrX/(uiWidth()+5));
        int toT = currentT + byT;

        uiDrag.stop();
        uiDrag.setOnFinished( a -> addTab(toT));
        uiDrag.setToX(-getTabX(toT));
        uiDrag.play();
        e.consume();
    }
    
    /** Scrolls current tab on the layout screen to center. */
    public void alignTabs() {
        int toT = currTab();
        uiDrag.stop();
        uiDrag.setOnFinished( a -> addTab(toT));
        uiDrag.setToX(-getTabX(toT));
        uiDrag.play();
    }
    
    /** @return index of currently viewed tab. It is the tab consuming the most
     * of the view space on the layout screen.*/
    public int currTab() {
        return (int) Math.rint(-1*ui.getTranslateX()/(uiWidth()+5));
    }
    
    private double uiWidth() { // must never return null (divisio by zero)
        if (ui.getWidth()==0)           // gui might not be initialized, use window size
            return Configuration.windowWidth-10;
        else                            // ui.getWidth(); <- less precise for some reason, cuses problems dont use
            return entireArea.getWidth()-10; 
    }
    private double getTabX(int i) {
        if (i==0) 
            return 0;
        else
        if (tabs.containsKey(i))
            return tabs.get(i).getLayoutX();
        else
            return (uiWidth()+5)*i;
    }
    
/*****************************    APP DRAGGING   ******************************/
    
    private double appX;
    private double appY;
    
    private void startAppDrag(MouseEvent e) {
        appX = e.getSceneX();
        appY = e.getSceneY();
    }
    private void dragApp(MouseEvent e) {
        if (window.isMaximised() == Maximized.NONE) {
            window.setLocation(e.getScreenX() - appX, e.getScreenY() - appY);

            double SW = Screen.getPrimary().getVisualBounds().getWidth(); //screen_width
            // (imitate Aero Snap)
            if (e.getScreenY() <= 0)
                    window.setMaximized(Maximized.ALL);
            else if (e.getScreenX() <= 0)
                    window.setMaximized(Maximized.LEFT);
            else if (e.getScreenX() >= SW - 1)  //for some reason it wont work without -1
                    window.setMaximized(Maximized.RIGHT);
        }
        else {
            double SW = Screen.getPrimary().getVisualBounds().getWidth(); //screen_width
            if (e.getScreenY()>0 && e.getScreenX()>0 && e.getScreenX()<SW-1)
            window.setMaximized(Maximized.NONE);
        }
    }
    
/***************************    APP OPERATIONS   ******************************/
    
    @FXML public void closeApp() {
        App.getInstance().close();
    }

    @FXML public void toggleMaximize() {
        window.toggleMaximize();
    }

    @FXML public void minimizeApp() {
        window.minimize();
    }

    @FXML public void toggleFulscreen() {
        window.toggleFullscreen();
    }
    
    @FXML public void toggleMini() {
        WindowManager.toggleMini();
    }
    
    @FXML public void manageLayouts() {
        ContextManager.openFloatingWindow((new LayoutManagerComponent()).getPane(), "Layout Manager");
    }

    
/*******************************    RESIZING    *******************************/
    
    @FXML
    private void border_onDragStart(MouseEvent event) {
        double X = event.getSceneX();
        double Y = event.getSceneY();
        double W = window.getWidth();
        double H = window.getHeight();
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
    }

    @FXML
    private void border_onDragEnd(MouseEvent event) {
        resizing = Resize.NONE;
    }

    @FXML
    private void border_onDragged(MouseEvent event) {
        if (resizing == Resize.SE) {
            window.setSize(event.getScreenX() - window.getX(), event.getScreenY() - window.getY());
        } else if (resizing == Resize.S) {
            window.setSize(window.getWidth(), event.getScreenY() - window.getY());
        } else if (resizing == Resize.E) {
            window.setSize(event.getScreenX() - window.getX(), window.getHeight());
        } else if (resizing == Resize.SW) {
            window.setSize(window.getX()+window.getWidth()-event.getScreenX(), event.getScreenY() - window.getY());
            window.setLocation(event.getScreenX(), window.getY());
        } else if (resizing == Resize.W) {
            window.setSize(window.getX()+window.getWidth()-event.getScreenX(), window.getHeight());
            window.setLocation(event.getScreenX(), window.getY());
        } else if (resizing == Resize.NW) {
            window.setSize(window.getX()+window.getWidth()-event.getScreenX(), window.getY()+window.getHeight()-event.getScreenY());
            window.setLocation(event.getScreenX(), event.getScreenY());
        } else if (resizing == Resize.N) {
            window.setSize(window.getWidth(), window.getY()+window.getHeight()-event.getScreenY());
            window.setLocation(window.getX(), event.getScreenY());
        } else if (resizing == Resize.NE) {
            window.setSize(event.getScreenX() - window.getX(), window.getY()+window.getHeight()-event.getScreenY());
            window.setLocation(window.getX(), event.getScreenY());
        }
    }
    
/******************************************************************************/
    
    @FXML
    private void consumeMouseEvent(MouseEvent event) {
        event.consume();
    }

    @FXML
    private void entireArea_OnKeyPressed(KeyEvent event) {
        if (event.getCode().equals(KeyCode.getKeyCode(Configuration.Shortcut_ALTERNATE)))
            GUI.setLayoutMode(true);
    }

    @FXML
    private void entireArea_OnKeyReleased(KeyEvent event) {
        if (event.getCode().equals(KeyCode.getKeyCode(Configuration.Shortcut_ALTERNATE)))
            GUI.setLayoutMode(false);
    }

}