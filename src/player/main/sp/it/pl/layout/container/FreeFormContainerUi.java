package sp.it.pl.layout.container;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import org.jetbrains.annotations.NotNull;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.window.Resize;
import sp.it.pl.gui.objects.window.pane.PaneWindowControls;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.Layouter;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.WidgetUi;
import sp.it.pl.layout.widget.controller.io.IOLayer;
import sp.it.pl.main.Df;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLOSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.VIEW_DASHBOARD;
import static javafx.application.Platform.runLater;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.util.Duration.millis;
import static sp.it.pl.layout.widget.WidgetUi.PSEUDOCLASS_DRAGGED;
import static sp.it.pl.main.AppDragKt.contains;
import static sp.it.pl.main.AppDragKt.get;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.access.PropertiesKt.toggle;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.functional.Util.findFirstEmptyKey;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.reactive.UtilKt.syncTo;
import static sp.it.util.ui.Util.setAnchor;
import static sp.it.util.ui.Util.setAnchors;

@SuppressWarnings("WeakerAccess")
public class FreeFormContainerUi extends ContainerUi<FreeFormContainer> {

    public static final String autoLayoutTooltipText = "Auto-layout\n\nResize components to maximize used space.";
    private static final String layoutButtonTooltipText = "Maximize & align\n\n"
        + "Sets best size and position for the widget. Maximizes widget size "
        + "and tries to align it with other widgets so it does not cover other "
        + "widgets.";

    private final FreeFormContainer container;
    private final AnchorPane rt = new AnchorPane();
    private final Map<Integer,PaneWindowControls> windows = new HashMap<>();
    private boolean resizing = false;
    private boolean any_window_resizing = false;

    public FreeFormContainerUi(FreeFormContainer c) {
        super(c);
        container = getContainer();

        setAnchor(getRoot(), rt, 0d);

        // add new widget on left click
        BooleanProperty isEmptySpace = new SimpleBooleanProperty(false);
        rt.setOnMousePressed(e -> isEmptySpace.set(isEmptySpace(e)));
        rt.setOnMouseClicked(e -> {
            if (!isContainerMode() && (APP.ui.isLayoutMode() || !container.lockedUnder.get())) {
                isEmptySpace.set(isEmptySpace.get() && isEmptySpace(e));
                if (e.getButton()==PRIMARY && isEmptySpace.get() && !any_window_resizing) {
                    addEmptyWindowAt(e.getX(),e.getY());
                    e.consume();
                }
            }
        });

        // drag
        rt.setOnDragDone(e -> rt.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false));
        installDrag(getRoot(), EXCHANGE, () -> "Move component here",
            e -> contains(e.getDragboard(), Df.COMPONENT),
            e -> get(e.getDragboard(), Df.COMPONENT) == container,
            consumer(e -> get(e.getDragboard(), Df.COMPONENT).swapWith(container, addEmptyWindowAt(e.getX(),e.getY()))),
            e -> bestRecBounds(e.getX(),e.getY(),null) // alternatively: e -> bestRecBounds(e.getX(),e.getY(),DragUtilKt.get(e, Df.COMPONENT).getWindow()))
        );

        rt.widthProperty().addListener((o,ov,nv) -> {
            resizing = true;
            windows.forEach((i,w) -> {
                boolean s = w.snappable.get();
                w.snappable.unbind();
                w.snappable.set(false);
                double wx = container.properties.getD(i+"x");
                double ww = container.properties.getD(i+"w");
                if (container.properties.containsKey(i+"x")) w.x.set(wx*nv.doubleValue());
                if (container.properties.containsKey(i+"w")) w.w.set((ww-wx)*nv.doubleValue());
                syncTo(APP.ui.getSnapping(), w.snappable);    // this is bad!
            });
            resizing = false;
        });
        rt.heightProperty().addListener((o,ov,nv) -> {
            resizing = true;
            windows.forEach((i,w) -> {
                boolean s = w.snappable.get();
                w.snappable.unbind();
                w.snappable.set(false);
                double wy = container.properties.getD(i+"y");
                double wh = container.properties.getD(i+"h");
                if (container.properties.containsKey(i+"y")) w.y.set(wy*nv.doubleValue());
                if (container.properties.containsKey(i+"h")) w.h.set((wh-wy)*nv.doubleValue());
                syncTo(APP.ui.getSnapping(), w.snappable);    // this is bad!
            });
            resizing = false;
        });
    }

    @NotNull
    @Override
    protected ContainerUiControls buildControls() {
        var c = super.buildControls();

        c.addExtraIcon(
            new Icon(VIEW_DASHBOARD, -1, autoLayoutTooltipText, this::autoLayoutAll).styleclass("header-icon")
        );

        c.addExtraIcon(
            new Icon(FontAwesomeIcon.HEADER, -1, "Show window headers.", () -> toggle(container.getShowHeaders())).styleclass("header-icon")
        );

        return c;
    }

    private boolean isEmptySpace(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        return windows.values().stream().noneMatch(w-> w.x.get()<x && w.y.get()<y && w.x.get()+w.w.get()>x && w.y.get()+w.h.get()>y);
    }

    public void load() {
        container.getChildren().forEach(this::loadWindow);
    }

    public void loadWindow(int i, Component cm) {
        PaneWindowControls w = getWindow(i, cm);

        Node n;
        Layouter l = null;
        if (cm instanceof Container) {
            Container c  = (Container) cm;
            n = c.load(w.content);
            if (c.ui instanceof ContainerUi) {
                var ui = (ContainerUi<?>) c.ui;
                if (ui.getControls().isSet())
                    ui.getControls().get().updateIcons();
            }
        } else if (cm instanceof Widget) {
            WidgetUi wa = new WidgetUi(container,i,(Widget)cm);
            Icon lb = new Icon(VIEW_DASHBOARD, 12, layoutButtonTooltipText, () -> autoLayout(w));
            wa.getControls().header_buttons.getChildren().add(1,lb);
            n = wa.getRoot();
        } else {
            l = new Layouter(container, i);
            l.setOnCancel(runnable(() -> container.removeChild(i)));
            n = l.getRoot();
        }

        w.moveOnDragOf(w.root);
        w.content.getChildren().setAll(n);
        setAnchors(n, 0d);
        if (l!=null) l.show();
    }

    public void closeWindow(int i) {
        PaneWindowControls w = windows.get(i);
        if (w!=null) {
            w.close();
            windows.remove(i);
            container.properties.remove(i+"x");
            container.properties.remove(i+"y");
            container.properties.remove(i+"w");
            container.properties.remove(i+"h");
        }
    }

    private PaneWindowControls getWindow(int i, Component cm) {
        PaneWindowControls w = windows.get(i);
        if (w==null) {
            w = buildWindow(i, cm);
            windows.put(i, w);
        }
        return w;
    }

    private PaneWindowControls buildWindow(int i, Component cm) {
        PaneWindowControls w = new PaneWindowControls(rt);
        w.root.getStyleClass().add("freeflowcontainer-window");
        w.offscreenFixOn.set(false);

        // initial size/pos
        w.open();
        w.resizeHalf();
        w.alignCenter();
        w.snappable.set(false);

        // values from previous session (used when deserializing)
        if (container.properties.containsKey(i+"x")) w.x.set(container.properties.getD(i+"x")*rt.getWidth());
        if (container.properties.containsKey(i+"y")) w.y.set(container.properties.getD(i+"y")*rt.getHeight());
        if (container.properties.containsKey(i+"w")) w.w.set(container.properties.getD(i+"w")*rt.getWidth()-container.properties.getD(i+"x")*rt.getWidth());
        if (container.properties.containsKey(i+"h")) w.h.set(container.properties.getD(i+"h")*rt.getHeight()-container.properties.getD(i+"y")*rt.getHeight());

        // store for restoration (runLater avoids initialization problems)
        runLater(() -> {
            syncC(w.x, v -> { if (!resizing) container.properties.put(i+"x", v.doubleValue()/rt.getWidth());});
            syncC(w.y, v -> { if (!resizing) container.properties.put(i+"y", v.doubleValue()/rt.getHeight());});
            syncC(w.w, v -> { if (!resizing) container.properties.put(i+"w", (w.x.get()+v.doubleValue())/rt.getWidth());});
            syncC(w.h, v -> { if (!resizing) container.properties.put(i+"h", (w.y.get()+v.doubleValue())/rt.getHeight());});
            syncC(APP.ui.getSnapDistance(), it -> w.snapDistance.setValue(it));
            syncTo(APP.ui.getSnapping(), w.snappable);
        });
        syncC(container.lockedUnder, it -> w.resizable.setValue(!it));
        syncC(container.lockedUnder, it -> w.movable.setValue(!it));
        syncC(container.getShowHeaders(), it -> w.setHeaderVisible(it));
        syncC(container.getShowHeaders(), it -> {
            if (it) {
                w.controls.getChildren().addAll(
                    new Icon(VIEW_DASHBOARD, -1, autoLayoutTooltipText, () -> autoLayout(w)).styleclass("header-icon"),
                    new Icon(CLOSE, -1, "Close this component", () -> { container.removeChild(i); closeWindow(i); }).styleclass("header-icon")
                );
            } else {
                w.controls.getChildren().clear();
            }
        });

        if (cm instanceof Widget) syncC(((Widget) cm).custom_name, it -> w.setTitle(it));
        else w.setTitle("");


        w.resizing.addListener((o,ov,nv) -> {
            if (nv!=Resize.NONE) any_window_resizing = true;
            else runFX(millis(100.0), () -> any_window_resizing = false);
        });

        // report component graphics changes
        syncC(w.root.parentProperty(), v -> IOLayer.allLayers.forEach(it -> it.requestLayout()));
        syncC(w.root.boundsInParentProperty(), v -> IOLayer.allLayers.forEach(it -> it.requestLayout()));

        return w;
    }

    /** Optimal size/position strategy returning greatest empty square. */
    TupleM4 bestRec(double x, double y, PaneWindowControls new_w) {
        TupleM4 b = new TupleM4(0d, rt.getWidth(), 0d, rt.getHeight());

        for (PaneWindowControls w : windows.values()) {
            if (w==new_w) continue;   // ignore self
            double wl = w.x.get()+w.w.get();
            if (wl<x && wl>b.a) b.a = wl;
            double wr = w.x.get();
            if (wr>x && wr<b.b) b.b = wr;
            double ht = w.y.get()+w.h.get();
            if (ht<y && ht>b.c) b.c = ht;
            double hb = w.y.get();
            if (hb>y && hb<b.d) b.d = hb;
        }

        b.a = 0d;
        b.b = rt.getWidth();
        for (PaneWindowControls w : windows.values()) {
            if (w==new_w) continue;   // ignore self
            double wl = w.x.get()+w.w.get();
            double wr = w.x.get();
            double ht = w.y.get()+w.h.get();
            double hb = w.y.get();
            boolean intheway = !((ht<y && ht<=b.c) || (hb>y && hb>=b.d));
            if (intheway) {
                if (wl<x && wl>b.a) b.a = wl;
                if (wr>x && wr<b.b) b.b = wr;
            }
        }

        return new TupleM4(b.a/rt.getWidth(),b.c/rt.getHeight(), (b.b-b.a)/rt.getWidth(),(b.d-b.c)/rt.getHeight());
    }

    Bounds bestRecBounds(double x, double y, PaneWindowControls newW) {
        TupleM4 b = new TupleM4(0d, rt.getWidth(), 0d, rt.getHeight());

        for (PaneWindowControls w : windows.values()) {
            if (w==newW) continue;   // ignore self
            double wl = w.x.get()+w.w.get();
            if (wl<x && wl>b.a) b.a = wl;
            double wr = w.x.get();
            if (wr>x && wr<b.b) b.b = wr;
            double ht = w.y.get()+w.h.get();
            if (ht<y && ht>b.c) b.c = ht;
            double hb = w.y.get();
            if (hb>y && hb<b.d) b.d = hb;
        }

        b.a = 0d;
        b.b = rt.getWidth();
        for (PaneWindowControls w : windows.values()) {
            if (w==newW) continue;   // ignore self
            double wl = w.x.get()+w.w.get();
            double wr = w.x.get();
            double ht = w.y.get()+w.h.get();
            double hb = w.y.get();
            boolean intheway = !((ht<y && ht<=b.c) || (hb>y && hb>=b.d));
            if (intheway) {
                if (wl<x && wl>b.a) b.a = wl;
                if (wr>x && wr<b.b) b.b = wr;
            }
        }

        return new BoundingBox(b.a,b.c,b.b-b.a,b.d-b.c);
    }

    /** Optimal size/position strategy returning centeraligned 3rd of window size
      dimensions. */
    TupleM4 bestRecSimple(double x, double y) {
        return new TupleM4(x/rt.getWidth()-1/6d, y/rt.getHeight()-1/6d, 1/3d, 1/3d);
    }

    /** Initializes position & size for i-th window, ignoring self w if passed as param. */
    private void storeBestRec(int i, double x, double y, PaneWindowControls w) {
        TupleM4 bestPos = bestRec(x, y, w);
        // add empty window at index
        // the method call eventually invokes load() method below, with
        // component/child == null (3rd case)
        container.properties.put(i + "x", bestPos.a);
        container.properties.put(i + "y", bestPos.b);
        container.properties.put(i + "w", bestPos.c+bestPos.a);
        container.properties.put(i + "h", bestPos.d+bestPos.b);
    }

    private int addEmptyWindowAt(double x, double y) {
        // get index
        int i = findFirstEmptyKey(container.getChildren(), 1);
        // preset viable area
        storeBestRec(i, x,y, null);
        // add empty window at index (into viable area)
        // the method call eventually invokes load() method below, with
        // component/child == null (3rd case)
        container.addChild(i, null);

        return i;
    }

    public void autoLayout(Component c) {
        var i = c.indexInParent();
        var w = i==null ? null : getWindow(i, c);
        if (w!=null) autoLayout(w);
    }

    public void autoLayout(PaneWindowControls w) {
        TupleM4 p = bestRec(w.x.get()+w.w.get()/2, w.y.get()+w.h.get()/2, w);
        w.x.set(p.a*rt.getWidth());
        w.y.set(p.b*rt.getHeight());
        w.w.set(p.c*rt.getWidth());
        w.h.set(p.d*rt.getHeight());
    }

    public void autoLayoutAll() {
        windows.forEach((i,w) -> autoLayout(w));
    }

    private static class TupleM4 {
        public double a;
        public double b;
        public double c;
        public double d;

        TupleM4(double a, double b, double c, double d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }
}