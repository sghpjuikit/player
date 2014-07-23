
package GUI.LayoutAggregators;

import GUI.GUI;
import Layout.Layout;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.TranslateTransition;
import javafx.scene.Parent;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import utilities.Animation.Interpolators.CircularInterpolator;
import static utilities.Animation.Interpolators.EasingMode.EASE_OUT;
import utilities.FxTimer;

/**
 * Pane with switchable content.
 * <p>
 * Pane allowing unlimited amount of contents, displaying exactly one spanning
 * its entire space and providing mechanism for content switching. It can be
 * compared to virtual desktops.
 * <p>
 * The content switches by invoking drag event using the right (secondary) mouse
 * button.
 *
 * @author plutonium_
 */
public class SwitchPane implements LayoutAggregator {
    
    private final AnchorPane root = new AnchorPane();
    public final AnchorPane ui = new AnchorPane();
    
    public SwitchPane() {
        // set ui
        root.getChildren().add(ui);
        AnchorPane.setBottomAnchor(ui, 0d);
        AnchorPane.setTopAnchor(ui, 0d);
        AnchorPane.setLeftAnchor(ui, 0d);
        AnchorPane.setRightAnchor(ui, 0d);
        
        // initialize ui drag behavior
        root.addEventFilter(MOUSE_PRESSED, e -> {
            // doesnt work because the isStillSincePress is too insensitive
//            if(!e.isStillSincePress()) {
//                if(e.getButton()==SECONDARY) {
//                        startUiDrag(e);
//                        ui.setMouseTransparent(true);
//                }
//            }
        });
        
        root.addEventFilter(MOUSE_DRAGGED, e -> {
            if(e.getButton()==SECONDARY) {
                ui.setMouseTransparent(true);
                startUiDrag(e);
            }
            if(e.getButton()==SECONDARY)
                dragUi(e);
        });
        
        root.addEventFilter(MOUSE_CLICKED, e-> {
            if(e.getButton()==SECONDARY) {
                endUIDrag(e);
                ui.setMouseTransparent(false);
            }
        });
        
        uiDrag = new TranslateTransition(Duration.millis(400),ui);
        uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));
        
        // bind widths for automatic dynamic resizing (works perfectly)
        ui.widthProperty().addListener(l-> {
            tabs.forEach((i,p)->p.setLayoutX(i*(uiWidth() + 5)));
            ui.setTranslateX(-getTabX(currTab()));
        });
    };

/********************************    TABS   ***********************************/
    
    public final Map<Integer,AnchorPane> tabs = new HashMap();
    
    public void addTab(int i, Layout layout) {
        // remove first
        removeTab(i);
        // add layout, if we dont an empty one will be provided
        layouts.put(i, layout);
        // reload tab
        addTab(i);
    }
    
    /**
     * Adds tab at specified position and initialized new empty layout. If tab
     * already exists this method is a no-op.
     * @param i 
     */
    public void addTab(int i) {
        if (tabs.containsKey(i)) return;
        
        double width = uiWidth();
        double pos = getTabX(i);
        
        AnchorPane t = new AnchorPane();
        ui.getChildren().add(t);
        tabs.put(i,t);
            t.setMinSize(0,0);
            t.setPrefWidth(width);      // standard width
            t.setLayoutX(pos);
            t.prefWidthProperty().bind(ui.widthProperty());
            // id love this to wokr but something tries to set LayoutX behind
            // the curtains and we get bound value could not be set error
//            t.layoutXProperty().bind(ui.widthProperty().add(5).multiply(i));
            AnchorPane.setTopAnchor(t, 0.0);
            AnchorPane.setBottomAnchor(t, 0.0);
        
        layouts.putIfAbsent(i, new Layout());
        layouts.get(i).load(t);
    }
    
    /**
     * Removes the tab at specified position and frees the resources. If the tab
     * at the position does not exist the method is a no-op.
     * @param i 
     */
    public void removeTab(int i) {
        if(tabs.containsKey(i)) {
            // detach from scene graph
            ui.getChildren().remove(tabs.get(i));
            // remove layout
            layouts.get(i).close();
            layouts.remove(i);
            // remove from tabs
            tabs.remove(i);
        }
    }

    
/****************************  TAB  ANIMATIONS   ******************************/
    
    private static final double MASS_COEFICIENT = 1.5;
    private static final double SNAP_TRESHOLD_COEFICIENT = 1/4;
    private final TranslateTransition uiDrag;
    private double uiTransX;
    private double uiStartX;
    boolean uiDragActive = false;
    
    private double lastX = 0;
    private double nowX = 0;
    FxTimer measurePulser = FxTimer.createPeriodic(Duration.millis(100), () -> {
        lastX = nowX;
        nowX = ui.getTranslateX();
    });
    
    private void startUiDrag(MouseEvent e) {
        if(uiDragActive) return;System.out.println("start");
        uiDrag.stop();
        uiStartX = e.getSceneX();
        uiTransX = ui.getTranslateX();
        uiDragActive = true;
        measurePulser.restart();
        e.consume();
    }
    private void endUIDrag(MouseEvent e) {
        if(!uiDragActive) return;System.out.println("end");
        // stop drag
        uiDragActive = false;
        measurePulser.stop();
        // handle drag end
        if(always_align_tabs)
            alignTabs(e);
        else {
            // ease out manual drag animation
            double x = ui.getTranslateX(); 
            double traveled = lastX==0 ? e.getSceneX()-uiStartX : nowX-lastX;
            // simulate mass - the more traveled the longer ease out
            uiDrag.setToX(x + traveled * MASS_COEFICIENT);
            uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));
//            uiDrag.setInterpolator(new CircularInterpolator(EASE_IN){
//                @Override protected double baseCurve(double x) {
//                    return Math.pow(2-2/(x+1), 0.4);
//                }
//            });
            // at the end of animation snap if close to edge 
            uiDrag.setOnFinished( a -> {
                int currT = currTab();
                double is = ui.getTranslateX();
                double should_be = -getTabX(currT);
                double dist = Math.abs(is-should_be);
                double treshold = uiWidth()/SNAP_TRESHOLD_COEFICIENT;
                if(dist < treshold) {             
                    uiDrag.setOnFinished( of -> addTab(currT));
                    uiDrag.setToX(should_be);
                    uiDrag.play();
                }
                // & make sure layout appears
                addTab(currT);
            });
            uiDrag.play();
        }
        // reset
        nowX = 0;
        lastX = 0;
        // prevent from propagating the event - disable app behavior while ui drag
        e.consume();
    }
    private void dragUi(MouseEvent e) {
        if(!uiDragActive) return;
        
        double byX = e.getSceneX()-uiStartX;
        ui.setTranslateX(uiTransX + byX);
         
//        if(GUI.snapping) {        // snap closest
//            double distance = ui.getTranslateX()%(uiWidth()+5);
//            if(Math.abs(distance) < GUI.snapDistance) {
//                ui.setTranslateX(-getTabX(currTab()));
//            }
//        }
        // prevent from propagating the event - disable app behavior while ui drag
        e.consume();
    }
    private void alignTabs(MouseEvent e) {
        double dist = lastX==0 ? e.getSceneX()-uiStartX : nowX-lastX;   // distance
        int byT = 0;                            // tabs to travel by

        double dAbs = Math.abs(dist);
        if (dAbs > ui.getWidth()*GUI.dragFraction || dAbs > GUI.dragDistance)
            byT = (int) -Math.signum(dist);

        int currentT = (int) Math.rint(-1*ui.getTranslateX()/(uiWidth()+5));
        int toT = currentT + byT;
        uiDrag.stop();
        uiDrag.setOnFinished( a -> addTab(toT));
        uiDrag.setToX(-getTabX(toT));
        uiDrag.play();
    }
    
    /** 
     * Scrolls to tab best suited to be shown. It is the tab that already occupies
     * the biggest portion of this pane.
     * It is pointless to use this method when autoalign is enabled.
     */
    public void alignTabs() {
        int toT = currTab();
        uiDrag.stop();
        uiDrag.setOnFinished( a -> addTab(toT));
        uiDrag.setToX(-getTabX(toT));
        uiDrag.play();
    }
    
    /** 
     * @return index of currently viewed tab. It is the tab consuming the most
     * of the view space on the layout screen.
     */
    public final int currTab() {
        return (int) Math.rint(-1*ui.getTranslateX()/(uiWidth()+5));
    }
    
    // get current ui width
    private double uiWidth() { // must never return 0 (divisio by zero)        
        return ui.getWidth()==0 ? 50 : ui.getWidth(); 
    }
    
    // get current X position of the tab with the specified index
    private double getTabX(int i) {
        if (i==0) 
            return 0;
        else if (tabs.containsKey(i))
            return tabs.get(i).getLayoutX();
        else
            return (uiWidth()+5)*i;
    }
    
/******************************************************************************/
    
    private boolean always_align_tabs = true;
    
    public void setAlwaysAlignTabs(boolean val) {
        always_align_tabs = val;
        if(val) alignTabs();
    }
    
/******************************************************************************/
    
    private final Map<Integer,Layout> layouts = new HashMap<>();
    
    /** {@inheritDoc} */
    @Override
    public Map<Integer,Layout> getLayouts() {
        return layouts;
    }

    /** {@inheritDoc} */
    @Override
    public Parent getRoot() {
        return root;
    }

    /** {@inheritDoc} */
    @Override
    public long getCapacity() {
        return Long.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @Override
    public Layout getActive() {
        return layouts.get(currTab());
    }
}