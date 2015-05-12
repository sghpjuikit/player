
package GUI.LayoutAggregators;

import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.objects.Window.stage.Window;
import Layout.Component;
import Layout.Layout;
import static java.lang.Double.*;
import static java.lang.Math.signum;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javafx.animation.*;
import static javafx.animation.Animation.INDEFINITE;
import javafx.beans.property.DoubleProperty;
import javafx.event.Event;
import javafx.scene.Parent;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.input.ScrollEvent.SCROLL;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import main.App;
import util.Animation.Interpolators.CircularInterpolator;
import static util.Animation.Interpolators.EasingMode.EASE_IN;
import static util.Animation.Interpolators.EasingMode.EASE_OUT;
import static util.Util.clip;
import static util.Util.setAnchors;
import util.async.Async;
import util.async.executor.FxTimer;

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
    public static boolean align_tabs = false;
    @IsConfig(name = "Switch drag distance (D)", info = "Required length of drag at"
            + " which tab switch animation gets activated. Tab switch activates if"
            + " at least one condition is fulfilled min distance or min fraction.")
    private static double min_tab_switch_dist = 150;
    @IsConfig(name = "Switch drag distance coeficient (D)", info = "Defines distance from edge in "
            + "percent of tab's width in which the tab switches.", min = 0, max = 1)
    private static double min_tab_switch_coeficient = 0.15;
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
    @IsConfig(editable = false, min=0.2, max=1)
    private static double zoomScaleFactor = 0.7;
    
    @AppliesConfig( "align_tabs")
    private static void applyAlignTabs() {
        Window.windows.stream()
                .map(Window::getLayoutAggregator)
                .filter(SwitchPane.class::isInstance)
                .map(SwitchPane.class::cast)
                .forEach(sp -> sp.setAlwaysAlignTabs(align_tabs));
    }
    
    @AppliesConfig( "snap_tabs")
    private static void applySnapTabs() {
        Window.windows.stream()
                .map(Window::getLayoutAggregator)
                .filter(SwitchPane.class::isInstance)
                .map(SwitchPane.class::cast)
                .forEach(sp -> sp.snapTabs());
    }
    
/******************************************************************************/
    
    private final AnchorPane root = new AnchorPane();
    private final AnchorPane zoom = new AnchorPane();
    private final AnchorPane ui = new AnchorPane();
    
    public SwitchPane() {
        // set ui
        root.getChildren().add(zoom);
        setAnchors(zoom, 0);
        zoom.getChildren().add(ui);
        setAnchors(ui, 0);
        
        // always zoom x:y == 1:1, to zoom change x, y will simply follow
        zoom.scaleYProperty().bind(zoom.scaleXProperty());
        
        // prevent problematic events
        // technically we only need to consume MOUSE_PRESSED and ContextMenuEvent.ANY
        root.addEventFilter(Event.ANY, e -> {
            if(uiDragActive) e.consume();
        });
        
//        PerspectiveTransform pt = new PerspectiveTransform();
//            pt.setUlx(0);
//            pt.setUly(0);
//            pt.setLlx(0);
//            pt.setLly(1440/2);
//            pt.setUrx(2560/2);
//            pt.setUry(0);
//            pt.setLrx(2560/2);
//            pt.setLry(1440/2);
//        root.setEffect(pt);
//        root.addEventFilter(MOUSE_MOVED, e -> {
//            if(isZoomed()) {
//                double x = abs((e.getX()-root.getWidth()/2)/root.getWidth());
//                double y = -1*(e.getY()-root.getHeight()/2)/root.getHeight();
//                pt.setUlx(50*x);
//                pt.setUly(50*y);
//                pt.setLlx(50*x);
//                pt.setLly(50*y);
//                pt.setUrx(root.getWidth()-50*x);
//                pt.setUry(root.getHeight()-50*y);
//                pt.setLrx(root.getWidth()-50*x);
//                pt.setLry(root.getHeight()-50*y);
//            pt.setUlx(0+50*x);
//            pt.setUly(0+50*y);
//            pt.setLlx(0+50*x);
//            pt.setLly(1440/2+50*y);
//            pt.setUrx(2560/2-50*x);
//            pt.setUry(0-50*y);
//            pt.setLrx(2560/2-50*x);
//            pt.setLry(1440/2-50*y);
//            }
//        });
        
        root.addEventFilter(MOUSE_DRAGGED, e -> {
            if(e.getButton()==SECONDARY) {
                ui.setMouseTransparent(true);
                dragUiStart(e);
                dragUi(e);
            }
        });
        
        root.addEventFilter(MOUSE_RELEASED, e-> {
            if(e.getButton()==SECONDARY) {
                dragUiEnd(e);
                ui.setMouseTransparent(false);
            }
        });
        
        // if mouse exits the root (and quite possibly window) we can not
        // capture mouse release/click events so lets end the drag right there
        root.addEventFilter(MOUSE_EXITED, e-> {
            dragUiEnd(e);
        });
        
        root.addEventHandler(SCROLL, e-> {
            if(GUI.GUI.isLayoutMode()) {
                double i = zoom.getScaleX() + Math.signum(e.getDeltaY())/10d;
                       i = clip(0.2d,i,1d);
                byx = signum(-1*e.getDeltaY())*(e.getX()-uiWidth()/2);
                double fromcentre = e.getX()-uiWidth()/2;
                       fromcentre = fromcentre/zoom.getScaleX();
                tox = signum(-1*e.getDeltaY())*(fromcentre);
                zoom(i);
                e.consume();
            }
        });
        
        uiDrag = new TranslateTransition(Duration.millis(400),ui);
        uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));
        
        // bind widths for automatic dynamic resizing (works perfectly)
        ui.widthProperty().addListener(l -> {
            tabs.forEach((i,p)->p.setLayoutX(i*(uiWidth() + 5)));
            ui.setTranslateX(-getTabX(currTab()));
        });
    };
    
    double byx = 0;
    double tox = 0;

/********************************    TABS   ***********************************/
    
    public final Map<Integer,AnchorPane> tabs = new HashMap();
    
    /** 
     * Adds specified layout as new tab on the first empty tab from 0 to right 
     * 
     * @return tab index
     */
    public int addTabToRight(Layout layout) {
        int i = 0;
        while(!layouts.containsKey(i)) i+=1;
        addTab(i, layout);
        return i;
    }
    
    public void addTab(int i, Layout layout) {
        // remove first
        removeTab(i);
        // add layout, if we dont an empty one will be provided
        layouts.put(i, layout);
        // reload tab
        addTab(i);
        // initialize layouts to left & right
        addTab(i+1);
        addTab(i-1);
    }
    
    /**
     * Adds mew tab at specified position and initializes new empty layout. If tab
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
    
    private double lastX = 0;
    private double nowX = 0;
    FxTimer measurePulser = new FxTimer(100, INDEFINITE, () -> {
        lastX = nowX;
        nowX = ui.getTranslateX();
    });
    
    private void dragUiStart(MouseEvent e) {
        if(uiDragActive) return;//System.out.println("start");
        uiDrag.stop();
        uiStartX = e.getSceneX();
        uiTransX = ui.getTranslateX();
        uiDragActive = true;
        measurePulser.restart();
        e.consume();
    }
    private void dragUiEnd(MouseEvent e) {
        if(!uiDragActive) return;//System.out.println("end");
        // stop drag
//        uiDragActive = false;
        Async.run(100, () -> uiDragActive=false);
        measurePulser.stop();
        // handle drag end
        if(always_align) {
            uiDrag.setInterpolator(new CircularInterpolator(EASE_IN){
                @Override protected double baseCurve(double x) {
                    return Math.pow(2-2/(x+1), 0.4);
                }
            });
            alignNextTab(e);
        } else {
            // ease out manual drag animation
            double x = ui.getTranslateX();
            double traveled = lastX==0 ? e.getSceneX()-uiStartX : nowX-lastX;
            // simulate mass - the more traveled the longer ease out
            uiDrag.setToX(x + traveled * DRAG_INERTIA);
            uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));
            // snap at the end of animation 
            uiDrag.setOnFinished( a -> {
                int i = snapTabs();
                // initialize layouts to left & right
                // otherwise the layouds are added only if we activate the snapping
                // which for non-discrete mode is a problem
                addTab(i-1);
                addTab(i+1);
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
        // drag
        double byX = e.getSceneX()-uiStartX;
        ui.setTranslateX(uiTransX + byX);
        // prevent from propagating the event - disable app behavior while ui drag
        e.consume();
    }

    
    /** 
     * Scrolls to the current tab.
     * It is pointless to use this method when autoalign is enabled.
     * <p>
     * Use to force-align tabs.
     * <p>
     * Equivalent to {@code alignTab(currTab());}
     *
     * @return index of current tab after aligning
     */
    public int alignTabs() {
        return alignTab(currTab());
    }
    
    /** 
     * Scrolls to tab right from the current tab.
     * Use to force-align tabs.
     * <p>
     * Equivalent to {@code alignTab(currTab()+1);}
     *
     * @return index of current tab after aligning
     */
    public int alignRightTab() {
        return alignTab(currTab()+1);
    }
    
    /** 
     * Scrolls to tab left from the current tab.
     * Use to force-align tabs.
     * <p>
     * Equivalent to {@code alignTab(currTab()-1);}
     *
     * @return index of current tab after aligning
     */
    public int alignLeftTab() {
        return alignTab(currTab()-1);
    }
    
    /** 
     * Scrolls to tab on provided position. Positions are (-infinity;infinity)
     * with 0 being the main.
     *
     * @param i position
     * @return index of current tab after aligning
     */
    public int alignTab(int i) {
        uiDrag.stop();
        uiDrag.setOnFinished( a -> addTab(i));
        uiDrag.setToX(-getTabX(i));
        uiDrag.play();
        return i;
    }
    
    /** 
     * Scrolls to tab containing given layout.
     *
     * @param l layout
     * @return index of current tab after aligning
     */
    public int alignTab(Layout l) {
        int i = -1;
        for(Entry<Integer,Layout> e : layouts.entrySet()) {
            if(e.getValue().equals(l)) {
                i = e.getKey();
                alignTab(i);
                break;
            }
        }
        return i;
    }
    
    /** 
     * Scrolls to tab containing given component in its layout.
     *
     * @param c component
     * @return index of current tab after aligning
     */
    public int alignTab(Component c) {
        int i = -1;
        for(Entry<Integer,Layout> e : layouts.entrySet()) {
            boolean has = e.getValue().getAllChildren().anyMatch(ch -> ch==c);
            if(has) {
                i = e.getKey();
                alignTab(i);
                break;
            }
        }
        return i;
    }
    
    /**
     * Executes {@link #alignTabs()} if the position of the tabs fullfills
     * snap requirements.
     * <p>
     * Use to align tabs while adhering to user settings.
     *
     * @return index of current tab after aligning
     */
    public int snapTabs() {
        int i = currTab();
        if(!snap_tabs) return i;
        
        double is = ui.getTranslateX();
        double should_be = -getTabX(currTab());
        double dist = Math.abs(is-should_be);
        double treshold1 = ui.getWidth()*SNAP_TRESHOLD_COEFICIENT;
        double treshold2 = SNAP_TRESHOLD_DIST;
        if(dist < Math.max(treshold1, treshold2))  
            return alignTabs();
        else
            return i;
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
    
    public DoubleProperty translateProperty() {
        return ui.translateXProperty();
    }
    
    
/********************************** ALIGNING **********************************/
    
    private boolean always_align = true;
    
    public void setAlwaysAlignTabs(boolean val) {
        always_align = val;
        if(val) alignTabs();
    }
    
/*********************************** ZOOMING **********************************/
    
    private final ScaleTransition z = new ScaleTransition(Duration.ZERO,zoom);
    private final TranslateTransition zt = new TranslateTransition(Duration.ZERO,ui);
    
    /** Animates zoom on when true, or off when false. */
    public void zoom(boolean v) {
        z.setInterpolator(new CircularInterpolator(EASE_OUT));
        zt.setInterpolator(new CircularInterpolator(EASE_OUT));
        zoomNoAcc(v ? zoomScaleFactor : 1);
    }
    
    /** @return true if zoomed to value <0,1), false when zoomed to 1*/
    public boolean isZoomed() {
        return zoom.getScaleX()!=1;
    }
    
    /** Animates zoom on/off. Equivalent to: {@code zoom(!isZoomed());} */
    public void toggleZoom() {
        zoom(!isZoomed());
    }
    
    /** Use to animate or manipulate zooming 
    @return zoom scale prperty taking on values from (0,1> */
    public DoubleProperty zoomProperty() {
        return zoom.scaleXProperty();
    }
    
    private void zoom(double d) {
        if(d<0 || d>1) throw new IllegalStateException("zooming interpolation out of 0-1 range");
        // remember amount
        if (d!=1) zoomScaleFactor = d;
        // play
        z.stop();
        z.setDuration(Duration.millis(GUI.GUI.duration_LM));
        z.setToX(d);
        z.play();
        zt.stop();
        zt.setDuration(z.getDuration());
        zt.setByX(tox/5);
        zt.play();
        App.actionStream.push("Zoom mode");
    }
    private void zoomNoAcc(double d) {
        if(d<0 || d>1) throw new IllegalStateException("zooming interpolation out of 0-1 range");   
        // calculate amount
        double missed = Double.compare(NaN, z.getToX())==0 ? 0 : z.getToX() - zoom.getScaleX();
               missed = signum(missed)==signum(d) ? missed : 0;
        d += missed;
        d = max(0.2,min(d,1));
        // zoom normally
        zoom(d);
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