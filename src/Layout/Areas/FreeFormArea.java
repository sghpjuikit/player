/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import Layout.*;
import Layout.container.Container;
import Layout.container.freeformcontainer.FreeFormContainer;
import Layout.widget.Widget;
import gui.GUI;
import gui.objects.Window.Pane.PaneWindowControls;
import gui.objects.Window.Resize;
import gui.objects.icon.Icon;
import util.collections.TupleM4;
import util.graphics.drag.DragUtil;

import static Layout.Areas.Area.DRAGGED_PSEUDOCLASS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.VIEW_DASHBOARD;
import static gui.GUI.closeAndDo;
import static javafx.application.Platform.runLater;
import static javafx.scene.input.MouseButton.PRIMARY;
import static util.async.Async.runFX;
import static util.functional.Util.findFirstEmpty;
import static util.functional.Util.isInR;
import static util.graphics.Util.layAnchor;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

/**
 * <p>
 * @author Plutonium_
 */
public class FreeFormArea extends ContainerNodeBase<FreeFormContainer> {

    private static final String laybTEXT = "Maximize & align\n\n"
        + "Sets best size and position for the widget. Maximizes widget size "
        + "and tries to align it with other widgets so it does not cover other "
        + "widgets.";
    private static final String autolbTEXT = "Autolayout\n\nLayout algorithm will resize widgets "
        + "to maximalize used space.";

    private final AnchorPane rt = new AnchorPane();
    private final Map<Integer,PaneWindowControls> windows = new HashMap();
    boolean resizing = false;

    public FreeFormArea(FreeFormContainer con) {
        super(con);
        layAnchor(root, rt,0d);

        Icon layB = new Icon(VIEW_DASHBOARD, 12, autolbTEXT, this::bestLayout);
        icons.getChildren().add(1,layB);

        BooleanProperty any_window_clicked = new SimpleBooleanProperty(false);
        rt.setOnMousePressed(e -> any_window_clicked.set(isHere(e)));
        rt.setOnMouseClicked(e -> {
            if(!isAltCon && (GUI.isLayoutMode() || !container.lockedUnder.get())) {
                any_window_clicked.set(any_window_clicked.get() && isHere(e));
                // add new widget on left click
                if(e.getButton()==PRIMARY && any_window_clicked.get() && !any_window_resizing) {
                    addEmptyWindowAt(e.getX(), e.getY());
                    e.consume();
                }
            }
        });

        // drag
        rt.setOnDragDone(e -> rt.pseudoClassStateChanged(DRAGGED_PSEUDOCLASS, false));
        DragUtil.installDrag(
            root, EXCHANGE, () -> "Move component here",
            DragUtil::hasComponent,
            e -> isInR(container, DragUtil.getComponent(e).child,DragUtil.getComponent(e).container),
            e -> {
                int i = addEmptyWindowAt(e.getX(), e.getY());
                container.swapChildren(i,DragUtil.getComponent(e));
            },
            e -> bestRecBounds(e.getX(),e.getY(),null)
        );

        rt.widthProperty().addListener((o,ov,nv) -> {
            resizing = true;
            windows.forEach((i,w) -> {
                boolean s = w.snappable.get();
                w.snappable.unbind();
                w.snappable.set(false);
                double wx = container.properties.getD(i+"x");
                double ww = container.properties.getD(i+"w");
                if(container.properties.containsKey(i+"x")) w.x.set(wx*nv.doubleValue());
                if(container.properties.containsKey(i+"w")) w.w.set((ww-wx)*nv.doubleValue());
                maintain(GUI.snapping, w.snappable);    // this is bad!
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
                if(container.properties.containsKey(i+"y")) w.y.set(wy*nv.doubleValue());
                if(container.properties.containsKey(i+"h")) w.h.set((wh-wy)*nv.doubleValue());
                maintain(GUI.snapping, w.snappable);    // this is bad!
            });
            resizing = false;
        });
    }

    private boolean isHere(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        return !windows.values().stream().anyMatch(w-> w.x.get()<x && w.y.get()<y && w.x.get()+w.w.get()>x && w.y.get()+w.h.get()>y);
    }

    public void load() {
        container.getChildren().forEach(this::loadWindow);
    }

    public void loadWindow(int i, Component cm) {
        PaneWindowControls w = getWindow(i);

        Node n;
        Layouter l = null;
        Icon lb = cm==null ? null : new Icon(VIEW_DASHBOARD, 12, laybTEXT, () -> {
                TupleM4<Double,Double,Double,Double> p = bestRec(w.x.get()+w.w.get()/2, w.y.get()+w.h.get()/2, w);
                w.x.set(p.a*rt.getWidth());
                w.y.set(p.b*rt.getHeight());
                w.w.set(p.c*rt.getWidth());
                w.h.set(p.d*rt.getHeight());
            });
        if(cm instanceof Container) {
            Container c  = (Container) cm;
            n = c.load(w.content);
            if(c.ui instanceof ContainerNodeBase)
                ((ContainerNodeBase)c.ui).icons.getChildren().add(1,lb);
        } else
        if(cm instanceof Widget) {
            WidgetArea wa = new WidgetArea(container,i);
            // add maximize button
            wa.controls.header_buttons.getChildren().add(1,lb);
            wa.loadWidget((Widget)cm);
            w.moveOnDragOf(wa.content_root);
            n = wa.root;
        } else {
            l = new Layouter(container, i);
            l.cp.consumeCancelClick = true;
            l.cp.onCancel = () -> closeAndDo(w.root, () -> container.removeChild(i));
            n = l.root;
        }

        w.content.getChildren().setAll(n);
        setAnchors(n, 0d);
        if(l!=null) l.show();
    }
    public void closeWindow(int i) {
        PaneWindowControls w = windows.get(i);
        if(w!=null) { // null can happen only in illegal call, but cant prevent that for now (layouter calls close 2 times)
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
        if(w==null) {
            w = buidWindow(i);
            windows.put(i,w);
        }
        return w;
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
        if(container.properties.containsKey(i+"x")) w.x.set(container.properties.getD(i+"x")*rt.getWidth());
        if(container.properties.containsKey(i+"y")) w.y.set(container.properties.getD(i+"y")*rt.getHeight());
        if(container.properties.containsKey(i+"w")) w.w.set(container.properties.getD(i+"w")*rt.getWidth()-container.properties.getD(i+"x")*rt.getWidth());
        if(container.properties.containsKey(i+"h")) w.h.set(container.properties.getD(i+"h")*rt.getHeight()-container.properties.getD(i+"y")*rt.getHeight());
        // store for restoration (runLater avoids initialization problems)
        runLater(()->{
            maintain(w.x, v -> { if(!resizing) container.properties.put(i+"x", v.doubleValue()/rt.getWidth());});
            maintain(w.y, v -> { if(!resizing) container.properties.put(i+"y", v.doubleValue()/rt.getHeight());});
            maintain(w.w, v -> { if(!resizing) container.properties.put(i+"w", (w.x.get()+v.doubleValue())/rt.getWidth());});
            maintain(w.h, v -> { if(!resizing) container.properties.put(i+"h", (w.y.get()+v.doubleValue())/rt.getHeight());});
            maintain(GUI.snapDistance, d->d, w.snapDistance);
            maintain(GUI.snapping, w.snappable);
        });
        maintain(container.lockedUnder, l -> !l, w.resizable);
        maintain(container.lockedUnder, l -> !l, w.movable);
        w.resizing.addListener((o,ov,nv) -> {
            if(nv!=Resize.NONE) any_window_resizing = true;
            else runFX(100, () -> any_window_resizing = false);
        });

        return w;
    }

    boolean any_window_resizing = false;

    /** Optimal size/position strategy returning greatest empty square. */
    TupleM4<Double,Double,Double,Double> bestRec(double x, double y, PaneWindowControls new_w) {
        TupleM4<Double,Double,Double,Double> b = new TupleM4(0d, rt.getWidth(), 0d, rt.getHeight());

        for(PaneWindowControls w : windows.values()) {
            if(w==new_w) continue;   // ignore self
            double wl = w.x.get()+w.w.get();
            if(wl<x && wl>b.a) b.a = wl;
            double wr = w.x.get();
            if(wr>x && wr<b.b) b.b = wr;
            double ht = w.y.get()+w.h.get();
            if(ht<y && ht>b.c) b.c = ht;
            double hb = w.y.get();
            if(hb>y && hb<b.d) b.d = hb;
        }

        b.a = 0d;
        b.b = rt.getWidth();
        for(PaneWindowControls w : windows.values()) {
            if(w==new_w) continue;   // ignore self
            double wl = w.x.get()+w.w.get();
            double wr = w.x.get();
            double ht = w.y.get()+w.h.get();
            double hb = w.y.get();
            boolean intheway = !((ht<y && ht<=b.c) || (hb>y && hb>=b.d));
            if(intheway) {
                if(wl<x && wl>b.a) b.a = wl;
                if(wr>x && wr<b.b) b.b = wr;
            }
        }

        return new TupleM4<>(b.a/rt.getWidth(),b.c/rt.getHeight(),
                            (b.b-b.a)/rt.getWidth(),(b.d-b.c)/rt.getHeight());
    }
    Bounds bestRecBounds(double x, double y, PaneWindowControls new_w) {
        TupleM4<Double,Double,Double,Double> b = new TupleM4(0d, rt.getWidth(), 0d, rt.getHeight());

        for(PaneWindowControls w : windows.values()) {
            if(w==new_w) continue;   // ignore self
            double wl = w.x.get()+w.w.get();
            if(wl<x && wl>b.a) b.a = wl;
            double wr = w.x.get();
            if(wr>x && wr<b.b) b.b = wr;
            double ht = w.y.get()+w.h.get();
            if(ht<y && ht>b.c) b.c = ht;
            double hb = w.y.get();
            if(hb>y && hb<b.d) b.d = hb;
        }

        b.a = 0d;
        b.b = rt.getWidth();
        for(PaneWindowControls w : windows.values()) {
            if(w==new_w) continue;   // ignore self
            double wl = w.x.get()+w.w.get();
            double wr = w.x.get();
            double ht = w.y.get()+w.h.get();
            double hb = w.y.get();
            boolean intheway = !((ht<y && ht<=b.c) || (hb>y && hb>=b.d));
            if(intheway) {
                if(wl<x && wl>b.a) b.a = wl;
                if(wr>x && wr<b.b) b.b = wr;
            }
        }

        return new BoundingBox(b.a,b.c,b.b-b.a,b.d-b.c);
    }

    /** Optimal size/position strategy returning centeraligned 3rd of window size
      dimensions. */
    TupleM4<Double,Double,Double,Double> bestRecSimple(double x, double y) {
        return new TupleM4<>(x/rt.getWidth()-1/6d,
                             y/rt.getHeight()-1/6d,
                             1/3d, 1/3d);
    }

    /** Initializes position & size for i-th window, ignoring self w if passed as param. */
    private void storeBestRec(int i, double x, double y, PaneWindowControls w) {
        TupleM4<Double,Double,Double,Double> bestPos = bestRec(x, y, w);
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
        int i = findFirstEmpty(container.getChildren(), 1);
        // preset viable area
        storeBestRec(i, x, y, null);
        // add empty window at index (into viable area)
        // the method call eventually invokes load() method below, with
        // component/child == null (3rd case)
        container.addChild(i, null);

        return i;
    }

    public void bestLayout() {
        windows.forEach((i,w) -> {
            TupleM4<Double,Double,Double,Double> p = bestRec(w.x.get()+w.w.get()/2, w.y.get()+w.h.get()/2, w);
            w.x.set(p.a*rt.getWidth());
            w.y.set(p.b*rt.getHeight());
            w.w.set(p.c*rt.getWidth());
            w.h.set(p.d*rt.getHeight());
        });
    }
}