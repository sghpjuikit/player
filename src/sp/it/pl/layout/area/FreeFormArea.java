package sp.it.pl.layout.area;

import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.window.Resize;
import sp.it.pl.gui.objects.window.pane.PaneWindowControls;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.freeformcontainer.FreeFormContainer;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.main.Df;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.VIEW_DASHBOARD;
import static javafx.application.Platform.runLater;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.util.Duration.millis;
import static sp.it.pl.layout.area.Area.PSEUDOCLASS_DRAGGED;
import static sp.it.pl.main.AppDragKt.contains;
import static sp.it.pl.main.AppDragKt.get;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.functional.Util.findFirstEmptyKey;
import static sp.it.pl.util.functional.UtilKt.consumer;
import static sp.it.pl.util.functional.UtilKt.runnable;
import static sp.it.pl.util.reactive.UtilKt.maintain;
import static sp.it.pl.util.reactive.UtilKt.syncTo;
import static sp.it.pl.util.ui.Util.setAnchor;
import static sp.it.pl.util.ui.Util.setAnchors;
import static sp.it.pl.util.ui.drag.DragUtilKt.installDrag;

public class FreeFormArea extends ContainerNodeBase<FreeFormContainer> {

    private static final String laybTEXT = "Maximize & align\n\n"
        + "Sets best size and position for the widget. Maximizes widget size "
        + "and tries to align it with other widgets so it does not cover other "
        + "widgets.";
    private static final String autolbTEXT = "Autolayout\n\nLayout algorithm will resize widgets "
        + "to maximalize used space.";

    private final AnchorPane rt = new AnchorPane();
    private final Map<Integer,PaneWindowControls> windows = new HashMap<>();
    private boolean resizing = false;
    private boolean any_window_resizing = false;

    public FreeFormArea(FreeFormContainer con) {
        super(con);
        setAnchor(root, rt,0d);

        Icon layB = new Icon(VIEW_DASHBOARD, 12, autolbTEXT, this::bestLayout);
        icons.getChildren().add(1,layB);

        // add new widget on left click
        BooleanProperty isEmptySpace = new SimpleBooleanProperty(false);
        rt.setOnMousePressed(e -> isEmptySpace.set(isEmptySpace(e)));
        rt.setOnMouseClicked(e -> {
            if (!isAltCon && (APP.ui.isLayoutMode() || !container.lockedUnder.get())) {
                isEmptySpace.set(isEmptySpace.get() && isEmptySpace(e));
                if (e.getButton()==PRIMARY && isEmptySpace.get() && !any_window_resizing) {
                    addEmptyWindowAt(e.getX(),e.getY());
                    e.consume();
                }
            }
        });

        // drag
        rt.setOnDragDone(e -> rt.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false));
        installDrag(
            root, EXCHANGE, () -> "Move component here",
            e -> contains(e.getDragboard(), Df.COMPONENT),
            e -> get(e.getDragboard(), Df.COMPONENT) == container,
            consumer(e -> get(e.getDragboard(), Df.COMPONENT).swapWith(container,addEmptyWindowAt(e.getX(),e.getY()))),
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

    private boolean isEmptySpace(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        return windows.values().stream().noneMatch(w-> w.x.get()<x && w.y.get()<y && w.x.get()+w.w.get()>x && w.y.get()+w.h.get()>y);
    }

    public void load() {
        container.getChildren().forEach(this::loadWindow);
    }

    public void loadWindow(int i, Component cm) {
        PaneWindowControls w = getWindow(i);

        Node n;
        Layouter l = null;
        Icon lb = cm==null ? null : new Icon(VIEW_DASHBOARD, 12, laybTEXT, () -> {
                TupleM4 p = bestRec(w.x.get()+w.w.get()/2, w.y.get()+w.h.get()/2, w);
                w.x.set(p.a*rt.getWidth());
                w.y.set(p.b*rt.getHeight());
                w.w.set(p.c*rt.getWidth());
                w.h.set(p.d*rt.getHeight());
            });
        if (cm instanceof Container) {
            Container c  = (Container) cm;
            n = c.load(w.content);
            if (c.ui instanceof ContainerNodeBase)
                ((ContainerNodeBase)c.ui).icons.getChildren().add(1,lb);
        } else
        if (cm instanceof Widget) {
            WidgetArea wa = new WidgetArea(container,i,(Widget)cm);
            // add maximize button
            wa.controls.header_buttons.getChildren().add(1,lb);
            w.moveOnDragOf(wa.contentRoot);
            n = wa.getRoot();
        } else {
            l = new Layouter(container, i);
            l.setOnCancel(runnable(() -> container.removeChild(i)));
            n = l.getRoot();
        }

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

    private PaneWindowControls getWindow(int i) {
        PaneWindowControls w = windows.get(i);
        if (w==null) {
            w = buidWindow(i);
            windows.put(i,w);
        }
        return w;
    }

    private PaneWindowControls getWindow(Component c) {
        Integer i = container.indexOf(c);
        return i==null ? null : windows.get(i);
    }

    private PaneWindowControls buidWindow(int i) {
        PaneWindowControls w = new PaneWindowControls(rt);
        w.root.getStyleClass().add("freeflowcontainer-window");
        w.setHeaderVisible(false);
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
        runLater(()->{
            maintain(w.x, v -> { if (!resizing) container.properties.put(i+"x", v.doubleValue()/rt.getWidth());});
            maintain(w.y, v -> { if (!resizing) container.properties.put(i+"y", v.doubleValue()/rt.getHeight());});
            maintain(w.w, v -> { if (!resizing) container.properties.put(i+"w", (w.x.get()+v.doubleValue())/rt.getWidth());});
            maintain(w.h, v -> { if (!resizing) container.properties.put(i+"h", (w.y.get()+v.doubleValue())/rt.getHeight());});
            maintain(APP.ui.getSnapDistance(), d->d, w.snapDistance);
            syncTo(APP.ui.getSnapping(), w.snappable);
        });
        maintain(container.lockedUnder, l -> !l, w.resizable);
        maintain(container.lockedUnder, l -> !l, w.movable);
        w.resizing.addListener((o,ov,nv) -> {
            if (nv!=Resize.NONE) any_window_resizing = true;
            else runFX(millis(100.0), () -> any_window_resizing = false);
        });

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
    Bounds bestRecBounds(double x, double y, PaneWindowControls new_w) {
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

    public void bestLayout() {
        windows.forEach((i,w) -> {
            TupleM4 p = bestRec(w.x.get()+w.w.get()/2, w.y.get()+w.h.get()/2, w);
            w.x.set(p.a*rt.getWidth());
            w.y.set(p.b*rt.getHeight());
            w.w.set(p.c*rt.getWidth());
            w.h.set(p.d*rt.getHeight());
        });
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