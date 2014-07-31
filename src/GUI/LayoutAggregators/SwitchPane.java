
package GUI.LayoutAggregators;

import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.ContextManager;
import GUI.Window;
import Layout.Layout;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Parent;
import static javafx.scene.input.MouseButton.MIDDLE;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
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
@IsConfigurable("Tabs")
public class SwitchPane implements LayoutAggregator {
    
    @IsConfig(name = "Discrete mode (D)", info = "Use discrete (D) and forbid seamless (S) tab switching."
            + " Tabs are always aligned. Seamless mode alows any tab position.")
    public static boolean align_tabs = true;
    @IsConfig(name = "Switch drag distance (D)", info = "Required length of drag at"
            + " which tab switch animation gets activated. Tab switch activates if"
            + " at least one condition is fulfilled min distance or min fraction.")
    private static double min_tab_switch_dist = 250;
    @IsConfig(name = "Switch drag distance coeficient (D)", info = "Defines distance from edge in "
            + "percent of tab's width in which the tab switches.", min = 0, max = 1)
    private static double min_tab_switch_coeficient = 0.2;
    @IsConfig(name = "Drag inertia (S)", info = "Inertia of the tab switch animation. "
            + "Defines distance the dragging will travel after input has been stopped. Only when ", min = 0, max = 10)
    private static double DRAG_INERTIA = 1.5;
    @IsConfig(name = "Snap tabs (S)", info = "Align tabs when close to edge.")
    public static boolean snap_tabs = true;
    @IsConfig(name = "Snap distance coeficient (S)", info = "Defines distance from edge in "
            + "percent of tab's width in which the tab autoalignes. Setting to maximum "
            + "(0.5) has effect of always snapping the tabs, while setting to minimum"
            + " (0) has effect of disabling tab snapping.", min = 0, max = 0.5)
    private static double SNAP_TRESHOLD_COEFICIENT = 0.05;
    @IsConfig(name = "Snap distance (S)", info = "Required distance from edge at"
            + " which tabs align. Tab snap activates if"
            + " at least one condition is fulfilled min distance or min fraction.")
    public static double SNAP_TRESHOLD_DIST = 25;
    
    @AppliesConfig( "align_tabs")
    private static void applyAlignTabs() {
        ContextManager.windows.stream()
                .map(Window::getLayoutAggregator)
                .filter(la->la instanceof SwitchPane)
                .map(la->(SwitchPane)la)
                .forEach(sp -> sp.setAlwaysAlignTabs(align_tabs));
    }
    
    @AppliesConfig( "snap_tabs")
    private static void applySnapTabs() {
        ContextManager.windows.stream()
                .map(Window::getLayoutAggregator)
                .filter(la->la instanceof SwitchPane)
                .map(la->(SwitchPane)la)
                .forEach(sp -> sp.snapTabs());
    }
    
/******************************************************************************/
    
    
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
            uiDragActiveLock = false;
            // doesnt work because the isStillSincePress is too insensitive
//            if(!e.isStillSincePress()) {
//                if(e.getButton()==SECONDARY) {
//                        startUiDrag(e);
//                        ui.setMouseTransparent(true);
//                }
//            }
            
            if(e.getButton()==MIDDLE) {
                //ui.setMouseTransparent(true);
                //startScroll(e);
            }
        });
        
        root.addEventFilter(MOUSE_DRAGGED, e -> {
            if(!uiDragActiveLock && e.getButton()==SECONDARY ) {
                ui.setMouseTransparent(true);
                startUiDrag(e);
                dragUi(e);
            }
            //if(e.isMiddleButtonDown())
                //doScrolling(e);
        });
        
        root.addEventFilter(MOUSE_CLICKED, e-> {
            if(e.getButton()==SECONDARY) {
                endUIDrag(e);
                ui.setMouseTransparent(false);
            }
        });
        
        // if mouse exits the root (and quite possibly window) we can not
        // capture mouse release/click events so lets end the drag right there
        root.addEventFilter(MOUSE_EXITED, e-> {
            if (uiDragActive) endUIDrag(e);
            uiDragActiveLock = true;
            
        });
        
        root.addEventFilter(MOUSE_RELEASED, e-> {
            if(e.getButton()==MIDDLE) {
                //endScroll(e);
                //ui.setMouseTransparent(false);
            }
        });
        
        uiDrag = new TranslateTransition(Duration.millis(400),ui);
        uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));
        
        scrolling = new TranslateTransition(Duration.millis(400), ui);
        scrolling.setByX(3000);
        scrolling.setInterpolator(Interpolator.LINEAR);
        scrolling.setOnFinished( e -> {
            scrolling.play();
        });
        
        // bind widths for automatic dynamic resizing (works perfectly)
        ui.widthProperty().addListener(l -> {
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

    
/****************************  DRAG ANIMATIONS   ******************************/
    
    private final TranslateTransition uiDrag;
    private double uiTransX;
    private double uiStartX;
    boolean uiDragActive = false;
    boolean uiDragActiveLock = false;
    
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
        if(always_align)
            alignNextTab(e);
        else {
            // ease out manual drag animation
            double x = ui.getTranslateX();
            double traveled = lastX==0 ? e.getSceneX()-uiStartX : nowX-lastX;
            // simulate mass - the more traveled the longer ease out
            uiDrag.setToX(x + traveled * DRAG_INERTIA);
            uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));
//            uiDrag.setInterpolator(new CircularInterpolator(EASE_IN){
//                @Override protected double baseCurve(double x) {
//                    return Math.pow(2-2/(x+1), 0.4);
//                }
//            });
            // snap at the end of animation 
            uiDrag.setOnFinished( a -> snapTabs());
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
        // drag
        double byX = e.getSceneX()-uiStartX;
        ui.setTranslateX(uiTransX + byX);
        // prevent from propagating the event - disable app behavior while ui drag
        e.consume();
    }

    
    /** 
     * Scrolls to tab best suited to be shown. It is the tab that already occupies
     * the biggest portion of this pane.
     * It is pointless to use this method when autoalign is enabled.
     * <p>
     * Use to force-align tabs.
     */
    public void alignTabs() {
        int toT = currTab();
        uiDrag.stop();
        uiDrag.setOnFinished( a -> addTab(toT));
        uiDrag.setToX(-getTabX(toT));
        uiDrag.play();
    }
    
    /**
     * Executes {@link #alignTabs()} if the position of the tabs fullfills
     * snap requirements.
     * <p>
     * Use to align tabs while adhering to user settings.
     */
    public void snapTabs() {
        if(!snap_tabs) return;
        double is = ui.getTranslateX();
        double should_be = -getTabX(currTab());
        double dist = Math.abs(is-should_be);
        double treshold1 = ui.getWidth()*SNAP_TRESHOLD_COEFICIENT;
        double treshold2 = SNAP_TRESHOLD_DIST;
        if(dist < Math.max(treshold1, treshold2))  
            alignTabs();
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
    
    // align to next tab
    private void alignNextTab(MouseEvent e) {
        double dist = lastX==0 ? e.getSceneX()-uiStartX : nowX-lastX;   // distance
        int byT = 0;                            // tabs to travel by
        double dAbs = Math.abs(dist);
        double treshold1 = ui.getWidth()*min_tab_switch_coeficient;
        double treshold2 = min_tab_switch_dist;
        if (dAbs > Math.min(treshold1, treshold2))
            byT = (int) -Math.signum(dist);

        int currentT = (int) Math.rint(-1*ui.getTranslateX()/(uiWidth()+5));
        int toT = currentT + byT;
        uiDrag.stop();
        uiDrag.setOnFinished( a -> addTab(toT));
        uiDrag.setToX(-getTabX(toT));
        uiDrag.play();
    }    
    
/******************************** SCROLLING ***********************************/
    
    private final TranslateTransition scrolling;
    private double scrollStartX = 0;
    private boolean scrollActive = false;
    
    private void startScroll(MouseEvent e) {
        if(scrollActive) return;System.out.println("scroll start");
        scrollStartX = e.getSceneX();
        scrollActive = true;
        scrolling.play();
    }
    private void doScrolling(MouseEvent e) {
        if(!scrollActive) return;
        System.out.println("scroll do");
        // calculate distance from start
        double distance = e.getSceneX()-scrollStartX;
        // set animation speed, 1 = max speed, 0 = min
        scrolling.setRate(distance/uiWidth());
        
        e.consume();
    }
    private void endScroll(MouseEvent e) {
        if(!scrollActive) return;
        System.out.println("scroll end");
        scrollActive = false;
        scrolling.stop();
        alignTabs();
        
        e.consume();
    }
    
    
/******************************** PROPERTIES **********************************/
    
    private boolean always_align = true;
    
    public void setAlwaysAlignTabs(boolean val) {
        always_align = val;
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