package sp.it.pl.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.layout.controller.io.IOLayer;
import sp.it.util.access.V;
import sp.it.util.animation.Anim;
import sp.it.util.animation.interpolator.CircularInterpolator;
import sp.it.util.async.executor.EventReducer;
import sp.it.util.async.executor.FxTimer;
import sp.it.util.reactive.Subscribed;
import static java.lang.Double.NaN;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static java.util.function.Predicate.not;
import static javafx.animation.Animation.INDEFINITE;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_DRAGGED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.scene.input.ScrollEvent.SCROLL;
import static javafx.util.Duration.millis;
import static kotlin.sequences.SequencesKt.any;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.Util.clip;
import static sp.it.util.animation.interpolator.EasingMode.EASE_IN;
import static sp.it.util.animation.interpolator.EasingMode.EASE_OUT;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.async.executor.FxTimer.fxTimer;
import static sp.it.util.collections.UtilKt.setTo;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.functional.Util.firstNotNull;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.attach;
import static sp.it.util.reactive.UtilKt.sync1IfInScene;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.Util.setAnchors;
import static sp.it.util.ui.UtilKt.initClip;
import static sp.it.util.ui.UtilKt.removeFromParent;

/**
 * Pane with switchable content.
 * <p/>
 * Pane allowing unlimited amount of contents, displaying exactly one spanning
 * its entire space and providing mechanism for content switching. It can be
 * compared to virtual desktops.
 * <p/>
 * The content switches by invoking drag event using the right (secondary) mouse button.
 */
@SuppressWarnings("WeakerAccess")
public class ContainerSwitchUi implements ComponentUi {

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

    public final V<Boolean> align = new V<>(true).initAttachC(v -> { if (v) alignTabs(); });
    public final V<Boolean> snap = new V<>(true).initAttachC(v -> { if (v) snapTabs(); });
    public final V<Double> switchDistAbs = new V<>(150.0);
    public final V<Double> switchDistRel = new V<>(0.15); // 0 - 1
    public final V<Double> dragInertia = new V<>(1.5);
    public final V<Double> snapThresholdRel = new V<>(0.05); // 0 - 0.5
    public final V<Double> snapThresholdAbs = new V<>(25.0);
    public final V<Double> zoomScaleFactor = new V<>(0.7); // 0.2 - 1

    @SuppressWarnings("unused")
    private double byx = 0;
    private double tox = 0;

    public ContainerSwitchUi(ContainerSwitch container) {
        this.container = container;

        root.setId("switch-pane-root");
        zoom.setId("switch-pane-zoom");
        ui.setId("switch-pane-ui");
        widget_io.setId("switch-pane-io");

        // set ui
        root.getChildren().add(zoom);
        setAnchors(zoom, 0d);
        zoom.getChildren().add(ui);
        setAnchors(ui, 0d);
        root.getChildren().add(widget_io);
        setAnchors(widget_io, 0d);

        initClip(root);

        // always zoom x:y == 1:1, to zoom change x, y will simply follow
        zoom.scaleYProperty().bind(zoom.scaleXProperty());

        // prevent problematic events
        root.addEventFilter(Event.ANY, e -> {
            if (uiDragActive) {
                if (e.getEventType()==MOUSE_PRESSED || e.getEventType()==MOUSE_RELEASED || e.getEventType()==MOUSE_CLICKED)
                    e.consume();
            } else {
                // technically we only need to consume MOUSE_PRESSED and ContextMenuEvent.ANY
                if (e.getEventType()==MOUSE_PRESSED && ((MouseEvent) e).getButton()==SECONDARY)
                    e.consume();
            }
        });

        root.addEventFilter(MOUSE_DRAGGED, e -> {
            if (e.getButton()==SECONDARY) {
                dragUiStart(e);
                dragUi(e);
            }
        });

        root.addEventFilter(MOUSE_RELEASED, e -> {
            if (e.getButton()==SECONDARY) {
                dragUiEnd(e);
            }
        });

        // if mouse exits the root (and quite possibly window) we can not
        // capture mouse release/click events so lets end the drag right there
        root.addEventFilter(MOUSE_EXITED, this::dragUiEnd);

        root.addEventHandler(SCROLL, e -> {
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

        sync1IfInScene(root, runnable(() -> {
            // restore last position
            ui.setTranslateX(container.getTranslate().getValue());
            // store latest position for deserialization
            syncC(ui.translateXProperty(), v -> container.getTranslate().setValue(v.doubleValue()));

            // maintain position during resize
            // 1 `{}` // does not maintain position:
            // 2 `ui.widthProperty().addListener(o -> ui.setTranslateX(-getTabX(currTab())));`  // this forces integer position during resize:
            // 3 The below works by referencing position before resize to compute proper value
            var uiI = new V<>(-currTabAsDouble());
            var uiIObserver = EventReducer.toLast(200, it -> {
                uiI.setValue(-currTabAsDouble());
                if (align.getValue()) alignTabs();
                else snapTabs();
            });
            var uiTObserver = new Subscribed(it -> attach(ui.translateXProperty(), consumer(v -> uiI.setValue(-currTabAsDouble()))));
            uiTObserver.subscribe(true);
            ui.widthProperty().addListener(o -> {
                uiIObserver.push(null);
                uiDrag.stop();
                uiTObserver.subscribe(false);
                ui.setTranslateX(uiI.getValue()*uiWidth());
                uiTObserver.subscribe(true);
            });

            // initialize
            updateEmptyTabs();
        }));
    }



    @Override
    public void dispose() {
        widget_io.dispose();
    }

    @Override
    public void focusTraverse(@NotNull Component child, @NotNull Widget source) {
        var componentI = container.indexOf(child);
        var componentTab = tabs.get(componentI);
        var sourceUi = source.getUi().getRoot();
        var sourceBounds = componentTab.sceneToLocal(sourceUi.localToScene(sourceUi.getBoundsInLocal()));

        var tabWidth = uiWidth();
        var is = -ui.getTranslateX();
        var shouldBe = -getTabX(componentI);
        var isFullyVisible = shouldBe+sourceBounds.getMinX()>=is && shouldBe+sourceBounds.getMaxX()<=is+tabWidth;
        if (!isFullyVisible) alignTab(componentI);
    }

    /********************************    TABS   ***********************************/

    private final Map<Integer,TabPane> tabs = new HashMap<>();

    @SuppressWarnings("StatementWithEmptyBody")
    public void addTab(int i, Component c) {
        if (c==layouts.get(i)) {

        } else if (c==null) {
            removeTab(i);
            updateEmptyTabs();
        } else {
            removeTab(i);
            layouts.put(i, c);
            loadTab(i);
            updateEmptyTabs();
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
        AltState as;
        Component c = layouts.get(i);
        TabPane tab = tabs.computeIfAbsent(i, index -> {
            TabPane t = new TabPane(index);
            ui.getChildren().add(t);
            return t;
        });
        if (c instanceof Container) {
            if (tab.ui!=null) tab.ui.dispose();

            n = ((Container<?>) c).load(tab);
            as = (Container<?>) c;
            tab.ui = ((Container<?>) c).ui;
        } else if (c instanceof Widget) {
            tab.ui = firstNotNull(
                () -> tab.ui instanceof WidgetUi && ((WidgetUi) tab.ui).getWidget()==c ? tab.ui : null,
                () -> {
                    if (tab.ui!=null) tab.ui.dispose();
                    return new WidgetUi(container, i, (Widget) c);
                }
            );
            n = tab.ui.getRoot();
            as = tab.ui;
        } else {
            tab.ui = firstNotNull(
                () -> tab.ui instanceof Layouter ? tab.ui : null,
                () -> {
                    if (tab.ui!=null) tab.ui.dispose();
                    return new Layouter(container, i);
                }
            );
            n = tab.ui.getRoot();
            as = tab.ui;
        }
        failIf(tabs.get(i).ui==null);
        tab.setContent(n);
        if (APP.ui.isLayoutMode()) as.show();
    }

    public void removeTab(int i) {
        var tab = tabs.remove(i);
        if (tab!=null) {
            removeFromParent(tab);
            if (tab.ui!=null) tab.ui.dispose();
            layouts.remove(i);
        }
    }

    public void removeAllTabs() {
        new ArrayList<>(tabs.keySet()).forEach(this::removeTab);
    }

    private void updateEmptyTabs() {
        var i = currTab();
        var isClose = (Predicate<Integer>) it -> container.getChildren().get(i)==null && abs(currTabAsDouble()-it)<0.8;

        var toAdd = Stream.of(i-1, i, i+1);
        var toRem = new ArrayList<>(tabs.keySet()).stream();

        toAdd.filter(isClose).forEach(it -> addTab(it));
        toRem.filter(not(isClose)).forEach(it -> {
            var t = tabs.get(it);
            if (t.ui == null) removeTab(it);
            if (t.ui instanceof Layouter) ((Layouter) t.ui).hideAnd(runnable(() -> {
                if (!isClose.test(it))
                    removeTab(it);
            }));
        });
    }

/****************************  DRAG ANIMATIONS   ******************************/

    private final XTransition uiDrag;
    private double uiTransX;
    private double uiStartX;
    private boolean uiDragActive = false;

    private double lastX = 0;
    private double nowX = 0;
    private final FxTimer measurePulse = fxTimer(millis(100), INDEFINITE, runnable(() -> {
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
            uiDrag.setOnFinished(a -> snapTabs());
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
        uiDrag.setOnFinished(a -> updateEmptyTabs());
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
            boolean has = cm==c || (cm instanceof Container && any(((Container<?>)cm).getAllChildren(), ch -> ch==c));
            if (has) {
                i = e.getKey();
                alignTab(i);
                break;
            } else {
                updateEmptyTabs();
            }
        }
        return i;
    }

    /**
     * Executes {@link #alignTabs()} if the position of the tabs fulfills snap requirements.
     * <p/>
     * Use to align tabs while adhering to user settings.
     */
    public void snapTabs() {
        if (!snap.get()) {
            updateEmptyTabs();
            return;
        }

        var is = ui.getTranslateX();
        var should_be = -getTabX(currTab());
        var dist = Math.abs(is-should_be);
        var threshold1 = ui.getWidth()* snapThresholdRel.get();
        var threshold2 = snapThresholdAbs.get();
        var needsAlign = dist < Math.max(threshold1, threshold2);

        if (needsAlign) alignTabs();
        else updateEmptyTabs();
    }

    /**
     * @return index of currently viewed tab. It is the tab consuming the most
     * of the view space on the layout screen.
     */
    public final int currTab() {
        return (int) Math.rint(currTabAsDouble());
    }

    public final double currTabAsDouble() {
        return -1*ui.getTranslateX()/tabWidth();
    }

    // get current ui width
    private double uiWidth() {
	    double someNonZeroNumber = 50; // must never return 0 (division by zero)
        return ui.getWidth()==0 ? someNonZeroNumber : ui.getWidth();
    }

    private double tabWidth() {
        return uiWidth();
    }

    // get current X position of the tab with the specified index
    private double getTabX(int i) {
        return i==0 ? 0 : tabWidth()*i;
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
        uiDrag.setOnFinished(a -> updateEmptyTabs());
        uiDrag.setToX(-getTabX(toT));
        uiDrag.play();
    }

    public DoubleProperty translateProperty() {
        return ui.translateXProperty();
    }

/*********************************** ZOOMING **********************************/

    private final ScaleTransition z1 = new ScaleTransition(Duration.ZERO, zoom);
    private final TranslateTransition z2 = new TranslateTransition(Duration.ZERO, ui);
    private final FadeTransition z3 = new FadeTransition(Duration.ZERO, zoom);

    /** Animates zoom on when true, or off when false. */
    public void zoom(boolean v) {
        z1.setInterpolator(new CircularInterpolator(EASE_OUT));
        z2.setInterpolator(new CircularInterpolator(EASE_OUT));
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
        z1.stop();
        z1.setDuration(APP.ui.getLayoutModeDuration());
        z1.setToX(d);
        z1.play();
        z2.stop();
        z2.setDuration(z1.getDuration());
        z2.setByX(tox/5);
        z2.play();
        z3.stop();
        z3.setDuration(z1.getDuration());
        z3.setToValue(0.2+0.8*d);
        z3.play();
        byx = 0;
        tox = 0;
        APP.getActionStream().invoke("Zoom mode");
    }
    private void zoomNoAcc(double d) {
        if (d<0 || d>1) throw new IllegalStateException("zooming interpolation out of 0-1 range");
        // calculate amount
        double missed = Double.compare(NaN, z1.getToX())==0 ? 0 : z1.getToX() - zoom.getScaleX();
               missed = signum(missed)==signum(d) ? missed : 0;
        d += missed;
        d = max(0.2,min(d,1));
        // zoom normally
        zoom(d);
    }

/******************************************************************************/

    public final ContainerSwitch container;
    private final Map<Integer,Component> layouts = new HashMap<>();

    public Map<Integer,Component> getComponents() {
        return layouts;
    }

    public long getCapacity() {
        return Long.MAX_VALUE;
    }

    public Component getActive() {
        return layouts.get(currTab());
    }

    @NotNull
    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void show() {
        layouts.values().forEach(c -> { if (c instanceof Container) ((Container<?>) c).show(); });
        tabs.forEach((i,t) -> { if (t.ui!=null) t.ui.show(); });
    }

    @Override
    public void hide() {
        layouts.values().forEach(c -> { if (c instanceof Container) ((Container<?>) c).hide(); });
        tabs.forEach((i,t) -> { if (t.ui!=null) t.ui.hide(); });
    }

    private static class TabPane extends AnchorPane {
        final int index;
        final StackPane bgr = new StackPane();
        ComponentUi ui;

        TabPane(int index) {
            this.index = index;
            this.getStyleClass().add("switch-pane-tab");

            getChildren().add(bgr);
            bgr.getChildren().add(new StackPane());
            bgr.getChildren().get(0).getStyleClass().add("switch-pane-tab-bgr");
        }

        void setContent(Node node) {
            setTo(getChildren(), node==null ? List.of(bgr) : List.of(bgr, node));
        }

        @Override
        protected void layoutChildren() {
            var p = getPadding();
            for (Node n : getChildren()) {
                n.resizeRelocate(p.getLeft(),p.getTop(),getWidth()-p.getLeft()-p.getRight(),getHeight()-p.getTop()-p.getBottom());
            }
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
		    super(d -> {});
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