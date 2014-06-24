
package GUI;

import Configuration.Configuration;
import Layout.Layout;
import Layout.LayoutManager;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import utilities.Animation.Interpolators.CircularInterpolator;
import static utilities.Animation.Interpolators.EasingMode.EASE_OUT;

/**
 * FXML Controller class
 */
public class UIController {
    
    @FXML public AnchorPane root;
    @FXML public AnchorPane layout;
    @FXML public AnchorPane ui;
    @FXML public AnchorPane overlayPane;
    @FXML public AnchorPane contextPane;

    /** Initializes the controller class. */
    public void initialize() {        
        
        // currently ui drag causes some problems so filters are registered on
        // contextPane trather than uipane. On uiPane the events somehow still
        // propagated to widgets and caused unwanted behavior, hence thi temp fix
        layout.addEventFilter(MOUSE_PRESSED, e -> {
            // doesnt work because the isStillSincePress is too insensitive
//            if(!e.isStillSincePress()) {
//                if(e.getButton()==SECONDARY) {
//                        startUiDrag(e);
//                        ui.setMouseTransparent(true);
//                }
//            }
        });
        layout.addEventFilter(MOUSE_DRAGGED, e -> {
            if(e.getButton()==SECONDARY) {
                    startUiDrag(e);
                    ui.setMouseTransparent(true);
            }
            if(e.getButton()==SECONDARY)
                dragUi(e);
        });
        layout.addEventFilter(MOUSE_CLICKED, e-> {
            if(e.getButton()==SECONDARY) {
                endUIDrag(e);
                ui.setMouseTransparent(false);
            }
        });
        
        uiDrag = new TranslateTransition(Duration.millis(400),ui);
        uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));
        
        root.widthProperty().addListener(l-> {
//            double uiwidth = uiWidth() + 5; // 5 is the tab padding
            tabs.forEach((i,p)->p.setLayoutX(i*(uiWidth() + 5)));
        });
    };

/********************************    TABS   ***********************************/
    
    public final Map<Integer,AnchorPane> tabs = new HashMap<>();
    
    public void addTab(int i) {        
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
    
    private TranslateTransition uiDrag;
    private double uiCurrX;
    private double uiStartX;
    boolean uiDragActive = false;
    
    private void startUiDrag(MouseEvent e) {
        if(uiDragActive) return;System.out.println("start");
        uiDrag.stop();
        uiStartX = e.getSceneX();
        uiCurrX = ui.getTranslateX();
        uiDragActive = true;
//        ui.setMouseTransparent(true);
        e.consume();
    }
    private void endUIDrag(MouseEvent e) {
        if(!uiDragActive) return;System.out.println("end");
        uiDragActive = false;
//        ui.setMouseTransparent(false);
        
        if(GUI.align_tabs)     // switch tabs if allowed
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
        // prevent from propagating the event - disable app behavior while ui drag
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
        // prevent from propagating the event - disable app behavior while ui drag
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
    
    private double uiWidth() { // must never return 0 (divisio by zero)
        // gui might not be initialized, use window size
        return (ui.getWidth()==0) ? Configuration.windowWidth-10 : ui.getWidth(); 
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

}