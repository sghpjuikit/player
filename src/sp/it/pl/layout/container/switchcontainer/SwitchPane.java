package sp.it.pl.layout.container.switchcontainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import sp.it.pl.layout.AltState;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.area.ContainerNode;
import sp.it.pl.layout.area.IOLayer;
import sp.it.pl.layout.area.Layouter;
import sp.it.pl.layout.area.WidgetArea;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.util.access.V;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.animation.interpolator.CircularInterpolator;
import sp.it.pl.util.async.executor.FxTimer;
import static java.lang.Double.NaN;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.signum;
import static javafx.animation.Animation.INDEFINITE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.scene.input.ScrollEvent.SCROLL;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.Util.clip;
import static sp.it.pl.util.animation.interpolator.EasingMode.EASE_IN;
import static sp.it.pl.util.animation.interpolator.EasingMode.EASE_OUT;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.executor.FxTimer.fxTimer;
import static sp.it.pl.util.functional.UtilKt.runnable;
import static sp.it.pl.util.graphics.Util.setAnchors;
import static sp.it.pl.util.reactive.Util.maintain;

/**
 * Pane with switchable content.
 * <p/>
 * Pane allowing unlimited amount of contents, displaying exactly one spanning
 * its entire space and providing mechanism for content switching. It can be
 * compared to virtual desktops.
 * <p/>
 * The content switches by invoking drag event using the right (secondary) mouse
 * button.
 *
 * @author plutonium_
 */
public class SwitchPane implements ContainerNode {

    private final AnchorPane root = new AnchorPane();
    private final AnchorPane zoom = new AnchorPane();
    private final AnchorPane ui = new AnchorPane() {
        @Override
        protected void layoutChildren() {
            double H = getHeight();
            double W = getWidth();
            double tW = tabWidth();
            tabs.forEach((index,tab) -> tab.resizeRelocate(tab.index*tW, 0, W, H));
        }
    };
    public final IOLayer widget_io = new IOLayer(this);

    public final V<Boolean> align = new V<>(true, v -> { if (v) alignTabs(); });
    public final V<Boolean> snap = new V<>(true, v -> { if (v) snapTabs(); });
    public final V<Double> switchDistAbs = new V<>(150.0);
    public final V<Double> switchDistRel = new V<>(0.15); // 0 - 1
    public final V<Double> dragInertia = new V<>(1.5);
    public final V<Double> snapThresholdRel = new V<>(0.05); // 0 - 0.5
    public final V<Double> snapThresholdAbs = new V<>(25.0);
    public final V<Double> zoomScaleFactor = new V<>(0.7); // 0.2 - 1

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
            if (uiDragActive) e.consume();
            else if (e.getEventType().equals(MOUSE_PRESSED) && ((MouseEvent)e).getButton()==SECONDARY) e.consume();
        });

        root.addEventFilter(MOUSE_DRAGGED, e -> {
            if (e.getButton()==SECONDARY) {
                ui.setMouseTransparent(true);
                dragUiStart(e);
                dragUi(e);
            }
        });

        root.addEventFilter(MOUSE_RELEASED, e-> {
            if (e.getButton()==SECONDARY) {
                dragUiEnd(e);
                ui.setMouseTransparent(false);
            }
        });

        // if mouse exits the root (and quite possibly window) we can not
        // capture mouse release/click events so lets end the drag right there
        root.addEventFilter(MOUSE_EXITED, this::dragUiEnd);

        root.addEventHandler(SCROLL, e-> {
            if (APP.ui.isLayoutMode()) {
                double i = zoom.getScaleX() + Math.signum(e.getDeltaY())/10d;
                       i = clip(0.2d,i,1d);
                byx = signum(-1*e.getDeltaY())*(e.getX()-uiWidth()/2);
                double fromCentre = e.getX()-uiWidth()/2;
                       fromCentre = fromCentre/zoom.getScaleX();
                tox = signum(-1*e.getDeltaY())*(fromCentre);
                zoom(i);
                e.consume();
            }
        });

        uiDrag = new XTransition(millis(400),ui);
        uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));

        // bind widths for automatic dynamic resizing (works perfectly)
        ui.widthProperty().addListener(o -> ui.setTranslateX(-getTabX(currTab())));


        // Maintain container properties
        double translate = (double)container.properties.computeIfAbsent("translate", key -> ui.getTranslateX());
        ui.setTranslateX(translate);
        // remember latest position for deserialization (we must not rewrite init value above)
        maintain(ui.translateXProperty(), v -> container.properties.put("translate",v));

        // initialize
        addTab(0);
    }

	double byx = 0;
    double tox = 0;

/********************************    TABS   ***********************************/

    public final Map<Integer,TabPane> tabs = new HashMap<>();
    boolean changed = false;

    public void addTab(int i, Component c) {
        if (c==layouts.get(i)) {
        	return;
        } else if (c==null) {
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
        if (!container.getChildren().containsKey(i)) loadTab(i);
    }

    /**
     * Adds mew tab at specified position and initializes new empty layout. If tab
     * already exists this method is a no-op.
     *
     * @param i tab index
     */
    public void loadTab(int i) {
        Node n;
        Component c = layouts.get(i);
        TabPane tab = tabs.computeIfAbsent(i, index -> {
            TabPane t = new TabPane(i);
            ui.getChildren().add(t);
            return t;
        });

        AltState as;
        if (c instanceof Container) {
            layouters.remove(i);
            n = ((Container)c).load(tab);
            as = (Container)c;
        } else if (c instanceof Widget) {
            layouters.remove(i);
            WidgetArea wa = new WidgetArea(container, i, (Widget)c);
            n = wa.getRoot();
            as = wa;
        } else { // ==null
            Layouter l = layouters.computeIfAbsent(i, index -> new Layouter(container,index));
            n = l.getRoot();
            as = l;
        }
        if (APP.ui.isLayoutMode()) as.show();
        tab.getChildren().setAll(n);
    }

    void removeTab(int i) {
        if (tabs.containsKey(i)) {
            // detach from scene graph
            ui.getChildren().remove(tabs.get(i));
            // remove layout
            layouts.remove(i);
            // remove from tabs
            tabs.remove(i);
        }

        if (currTab()==i) addTab(i);
    }

    void removeAllTabs() {
        layouts.keySet().forEach(this::removeTab);
    }

/****************************  DRAG ANIMATIONS   ******************************/

    private final XTransition uiDrag;
    private double uiTransX;
    private double uiStartX;
    private boolean uiDragActive = false;

    private double lastX = 0;
    private double nowX = 0;
    private FxTimer measurePulse = fxTimer(millis(100), INDEFINITE, runnable(() -> {
        lastX = nowX;
        nowX = ui.getTranslateX();
    }));

    private void dragUiStart(MouseEvent e) {
        if (uiDragActive) return;
        uiDrag.stop();
        uiStartX = e.getSceneX();
        uiTransX = ui.getTranslateX();
        uiDragActive = true;
        measurePulse.start();
        e.consume();
    }
    private void dragUiEnd(MouseEvent e) {
        if (!uiDragActive) return;
        // stop drag
//        uiDragActive = false;
        runFX(millis(100), () -> uiDragActive=false);
        measurePulse.stop();
        // handle drag end
        if (align.get()) {
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
            uiDrag.setToX(x + traveled * dragInertia.get());
            uiDrag.setInterpolator(new CircularInterpolator(EASE_OUT));
            // snap at the end of animation
            uiDrag.setOnFinished( a -> {
                int i = snapTabs();
                // setParentRec layouts to left & right
                // otherwise the layouts are added only if we activate the snapping
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
        if (!uiDragActive) return;
        // drag
        double byX = e.getSceneX()-uiStartX;
        ui.setTranslateX(uiTransX + byX);
        // prevent from propagating the event - disable app behavior while ui drag
        e.consume();
    }


    /**
     * Scrolls to the current tab.
     * It is pointless to use this method when auto-align is enabled.
     * <p/>
     * Use to force-align tabs.
     * <p/>
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
     * <p/>
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
     * <p/>
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
        for (Entry<Integer,Component> e : layouts.entrySet()) {
            Component cm = e.getValue();
            boolean has = cm==c || (cm instanceof Container && ((Container<?>)cm).getAllChildren().anyMatch(ch -> ch==c));
            if (has) {
                i = e.getKey();
                alignTab(i);
                break;
            }
        }
        return i;
    }

    /**
     * Executes {@link #alignTabs()} if the position of the tabs fulfills snap requirements.
     * <p/>
     * Use to align tabs while adhering to user settings.
     *
     * @return index of current tab after aligning
     */
    public int snapTabs() {
        int i = currTab();
        if (!snap.get()) return i;

        double is = ui.getTranslateX();
        double should_be = -getTabX(currTab());
        double dist = Math.abs(is-should_be);
        double threshold1 = ui.getWidth()* snapThresholdRel.get();
        double threshold2 = snapThresholdAbs.get();

        return dist < Math.max(threshold1, threshold2) ? alignTabs() : i;
    }

    /**
     * @return index of currently viewed tab. It is the tab consuming the most
     * of the view space on the layout screen.
     */
    public final int currTab() {
        return (int) Math.rint(-1*ui.getTranslateX()/tabWidth());
    }

    // get current ui width
    private double uiWidth() {
	    double someNonZeroNumber = 50; // must never return 0 (division by zero)
        return ui.getWidth()==0 ? someNonZeroNumber : ui.getWidth();
    }

    private double tabWidth() {
        return uiWidth(); // + tab_spacing; // we can set gap between tabs easily here
    }

    // get current X position of the tab with the specified index
    private double getTabX(int i) {
        if (i==0) return 0;
        if (tabs.containsKey(i)) return tabs.get(i).getLayoutX();
        return tabWidth()*i;
    }

    // align to next tab
    private void alignNextTab(MouseEvent e) {
        double dist = lastX==0 ? e.getSceneX()-uiStartX : nowX-lastX;   // distance
        int byT = 0;                            // tabs to travel by
        double dAbs = Math.abs(dist);
        double threshold1 = ui.getWidth()*switchDistRel.get();
        double threshold2 = switchDistAbs.get();
        if (dAbs > Math.min(threshold1, threshold2))
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

/*********************************** ZOOMING **********************************/

    private final ScaleTransition z = new ScaleTransition(Duration.ZERO,zoom);
    private final TranslateTransition zt = new TranslateTransition(Duration.ZERO,ui);

    /** Animates zoom on when true, or off when false. */
    public void zoom(boolean v) {
        z.setInterpolator(new CircularInterpolator(EASE_OUT));
        zt.setInterpolator(new CircularInterpolator(EASE_OUT));
        zoomNoAcc(v ? zoomScaleFactor.get() : 1);
    }

    /** @return true if zoomed to value <0,1), false when zoomed to 1*/
    public boolean isZoomed() {
        return zoom.getScaleX()!=1;
    }

    /** Animates zoom on/off. Equivalent to: {@code zoom(!isZoomed());} */
    public void toggleZoom() {
        zoom(!isZoomed());
    }

    /**
     * Use to animate or manipulate zooming
     *
     * @return zoom scale property taking on values from (0,1>
     */
    public DoubleProperty zoomProperty() {
        return zoom.scaleXProperty();
    }

    // this is called in animation with 0-1 as parameter
    private void zoom(double d) {
        if (d<0 || d>1) throw new IllegalStateException("zooming interpolation out of 0-1 range");
        // remember amount
        // Design flaw. If we want to remember the value, use another property. This overwrites
        // the 'proper' value by setting zoom to 1 - effectively disallowing zooming more than once
        // if (d!=1) zoomScaleFactor.set(d);
        // play
        z.stop();
        z.setDuration(APP.ui.getDurationLM());
        z.setToX(d);
        z.play();
        zt.stop();
        zt.setDuration(z.getDuration());
        zt.setByX(tox/5);
        zt.play();
        APP.actionStream.push("Zoom mode");
    }
    private void zoomNoAcc(double d) {
        if (d<0 || d>1) throw new IllegalStateException("zooming interpolation out of 0-1 range");
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
            if (c instanceof Container) ((Container)c).show();
            if (c instanceof Widget) {
                ContainerNode ct = ((Widget)c).areaTemp;
                if (ct!=null) ct.show();
            }
        });
        layouters.forEach((i,l) -> l.show());
    }

    @Override
    public void hide() {
        layouts.values().forEach(c -> {
            if (c instanceof Container) ((Container)c).hide();
            if (c instanceof Widget) {
                ContainerNode ct = ((Widget)c).areaTemp;
                if (ct!=null) ct.hide();
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
            for (Node n : getChildren())
                n.resizeRelocate(0,0,getWidth(),getHeight());
        }

    }

    /**
     * Effectively same as {@link javafx.animation.TranslateTransition} of only X property,
     * but always rounds the translation values to integer values to prevent visual artifacts that can result due to
     * double coordinate system.
     */
    private static class XTransition extends Anim {
    	private final Node node;
	    private double from, to, by;

    	public XTransition(Duration length, Node node) {
		    super(d -> {}); // TODO remove this by adding proper support for Anim subclassing
		    dur(length);
		    this.node = node;
	    }

	    @Override
	    protected void interpolate(double at) {
	    	node.setTranslateX(Math.rint(from + at * by));
	    }

	    public void setToX(double to) {
			this.to = Math.rint(to);
	    }

	    @Override
	    public void play() {
	    	from = node.getTranslateX();
	    	by = Math.rint(to - from);
		    super.play();
	    }
    }
}