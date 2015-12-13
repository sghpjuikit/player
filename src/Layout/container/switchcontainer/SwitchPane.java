
package Layout.container.switchcontainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Layout.Areas.Area;
import Layout.Areas.ContainerNode;
import Layout.Areas.IOLayer;
import Layout.Areas.Layouter;
import Layout.Areas.WidgetArea;
import Layout.Component;
import Layout.container.Container;
import Layout.widget.Widget;
import gui.GUI;
import gui.objects.Window.stage.Window;
import util.animation.interpolator.CircularInterpolator;
import util.async.Async;
import util.async.executor.FxTimer;

import static java.lang.Double.*;
import static java.lang.Math.signum;
import static javafx.animation.Animation.INDEFINITE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.input.ScrollEvent.SCROLL;
import static main.App.APP;
import static util.Util.clip;
import static util.animation.interpolator.EasingMode.EASE_IN;
import static util.animation.interpolator.EasingMode.EASE_OUT;
import static util.functional.Util.ISNTØ;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

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
public class SwitchPane implements ContainerNode {

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
        Window.WINDOWS.stream()
              .map(Window::getSwitchPane).filter(ISNTØ)
              .forEach(sp -> sp.setAlwaysAlignTabs(align_tabs));
    }

    @AppliesConfig( "snap_tabs")
    private static void applySnapTabs() {
        Window.WINDOWS.stream()
              .map(Window::getSwitchPane).filter(ISNTØ)
              .forEach(sp -> sp.snapTabs());
    }

    public static void applyGlobalSettings(SwitchPane p) {
        p.setAlwaysAlignTabs(align_tabs);
        p.snapTabs();
    }

/******************************************************************************/

    private final AnchorPane root = new AnchorPane();
    private final AnchorPane zoom = new AnchorPane();
    private final AnchorPane ui = new AnchorPane() {
        @Override
        protected void layoutChildren() {
            double H = getHeight();
            double W = getWidth();
            double tW = tabWidth();
            tabs.forEach((index,tab) -> tab.resizeRelocate(tab.index*tW,0,W,H));
        }
    };
    public final IOLayer widget_io = new IOLayer(this);

    // use when outside of container
    public SwitchPane() {
        this(null);
    }

    public SwitchPane(SwitchContainer container) {
        this.container = container;

        // set ui
        root.getChildren().add(zoom);
        setAnchors(zoom, 0d);
        zoom.getChildren().add(ui);
        setAnchors(ui, 0d);
        root.getChildren().add(widget_io);
        setAnchors(widget_io, 0d);

        // Clip mask.
        // Hides content 'outside' of this pane
        Rectangle mask = new Rectangle();
        root.setClip(mask);
        mask.widthProperty().bind(root.widthProperty());
        mask.heightProperty().bind(root.heightProperty());

        // always zoom x:y == 1:1, to zoom change x, y will simply follow
        zoom.scaleYProperty().bind(zoom.scaleXProperty());

        // prevent problematic events
        // technically we only need to consume MOUSE_PRESSED and ContextMenuEvent.ANY
        root.addEventFilter(Event.ANY, e -> {
            if(uiDragActive) e.consume();
            else if(e.getEventType().equals(MOUSE_PRESSED) && ((MouseEvent)e).getButton()==SECONDARY) e.consume();
        });

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
            if(gui.GUI.isLayoutMode()) {
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
        ui.widthProperty().addListener(o -> ui.setTranslateX(-getTabX(currTab())));


        // Maintain container properties
        double translate = (double)container.properties.computeIfAbsent("translate", key -> ui.getTranslateX());
        ui.setTranslateX(translate);
        // remember latest position for deserialisation (we must not rewrite init value above)
        maintain(ui.translateXProperty(), v -> container.properties.put("translate",v));
    };

    double byx = 0;
    double tox = 0;

/********************************    TABS   ***********************************/

    public final Map<Integer,TabPane> tabs = new HashMap<>();
    boolean changed = false;

    public void addTab(int i, Component c) {
        if(c==layouts.get(i)) {
            return;
        } else if(c==null) {
            removeTab(i);
        } else {
            removeTab(i);
            layouts.put(i, c);
            changed = true;
            loadTab(i);
            // left & right
            addTab(i+1);
            addTab(i-1);
        }
    }

    public void addTab(int i) {
        if(!container.getChildren().containsKey(i)) loadTab(i);
    }

    /**
     * Adds mew tab at specified position and initializes new empty layout. If tab
     * already exists this method is a no-op.
     * @param i
     */
    public void loadTab(int i) {
        Node n = null;
        Component c = layouts.get(i);
        TabPane tab = tabs.computeIfAbsent(i, index -> {
            TabPane t = new TabPane(i);
            ui.getChildren().add(t);
            return t;
        });

        if(c==null) {
            Layouter l = layouters.computeIfAbsent(i, index -> new Layouter(container,index));
            if(GUI.isLayoutMode()) l.show();
            n = l.getRoot();
        } else if (c instanceof Container) {
            layouters.remove(i);
            n = ((Container)c).load(tab);
        } else if (c instanceof Widget) {
            layouters.remove(i);
            WidgetArea wa = new WidgetArea(container, i, (Widget)c);
            n = wa.root;
        }
        tab.getChildren().setAll(n);
    }

    void removeTab(int i) {
        if(tabs.containsKey(i)) {
            // detach from scene graph
            ui.getChildren().remove(tabs.get(i));
            // remove layout
            layouts.remove(i);
            // remove from tabs
            tabs.remove(i);
        }

        if(currTab()==i) addTab(i);
    }

    void removeAllTabs() {
        layouts.keySet().forEach(this::removeTab);
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
        measurePulser.start();
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
                // setParentRec layouts to left & right
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
        uiDrag.setOnFinished(a -> addTab(i));
        uiDrag.setToX(-getTabX(i));
        uiDrag.play();
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
        for(Entry<Integer,Component> e : layouts.entrySet()) {
            Component cm = e.getValue();
            boolean has = cm==c || (cm instanceof Container && ((Container)cm).getAllChildren().anyMatch(ch -> ch==c));
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
        return (int) Math.rint(-1*ui.getTranslateX()/tabWidth());
    }

    // get current ui width
    private double uiWidth() { // must never return 0 (divisio by zero)
        return ui.getWidth()==0 ? 50 : ui.getWidth();
    }

    private double tabWidth() {
        return uiWidth(); // + tab_spacing; // we can set gap between tabs easily here
    }

    // get current X position of the tab with the specified index
    private double getTabX(int i) {
        if (i==0)
            return 0;
        else if (tabs.containsKey(i))
            return tabs.get(i).getLayoutX();
        else
            return tabWidth()*i;
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

        int currentT = (int) Math.rint(-1*ui.getTranslateX()/tabWidth());
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
        z.setDuration(gui.GUI.duration_LM);
        z.setToX(d);
        z.play();
        zt.stop();
        zt.setDuration(z.getDuration());
        zt.setByX(tox/5);
        zt.play();
        APP.actionStream.push("Zoom mode");
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

    public final SwitchContainer container;
    private final Map<Integer,Component> layouts = new HashMap<>();
    private final Map<Integer,Layouter> layouters = new HashMap<>();

    public Map<Integer,Component> getComponents() {
        return layouts;
    }

    public long getCapacity() {
        return Long.MAX_VALUE;
    }

    public Component getActive() {
        return layouts.get(currTab());
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void show() {
        layouts.values().forEach(c -> {
            if(c instanceof Container) ((Container)c).show();
            if(c instanceof Widget) {
                Area ct = ((Widget)c).areaTemp;
                if(ct!=null) ct.show();
            }
        });
        layouters.forEach((i,l) -> l.show());
    }

    @Override
    public void hide() {
        layouts.values().forEach(c -> {
            if(c instanceof Container) ((Container)c).hide();
            if(c instanceof Widget) {
                Area ct = ((Widget)c).areaTemp;
                if(ct!=null) ct.hide();
            }
        });
        layouters.forEach((i,l) -> l.hide());
    }

    private class TabPane extends AnchorPane {
        final int index;

        TabPane(int index) {
            this.index = index;
        }

        @Override
        protected void layoutChildren() {
            for(Node n : getChildren())
                n.resizeRelocate(0,0,getWidth(),getHeight());
        }

    }
}