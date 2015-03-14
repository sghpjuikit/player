/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;

import GUI.DragUtil;
import GUI.GUI;
import static GUI.GUI.closeAndDo;
import GUI.objects.Window.Pane.PaneWindowControls;
import Layout.*;
import static Layout.Areas.Area.draggedPSEUDOCLASS;
import Layout.Widgets.Widget;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import static javafx.application.Platform.runLater;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import static util.Util.setAnchors;
import util.collections.TupleM4;
import static util.functional.Util.findFirstEmpty;
import static util.reactive.Util.maintain;

/**
 <p>
 @author Plutonium_
 */
public class FreeFormArea implements ContainerNode {
    
    private final FreeFormContainer container;
    private final AnchorPane root = new AnchorPane();
    private final Map<Integer,PaneWindowControls> windows = new HashMap();
    public final Map<Integer,WidgetArea> widgets = new HashMap();
    
    public FreeFormArea(FreeFormContainer con) {
        container = con;
        BooleanProperty isHere = new SimpleBooleanProperty(false);
        root.setOnMousePressed(e -> isHere.set(isHere(e)));
        root.setOnMouseClicked(e -> {
            if(GUI.isLayoutMode() || !container.isUnderLock()) {
                isHere.set(isHere.get() && isHere(e));
                if(e.getButton()==PRIMARY && isHere.get()) {
                    int index = findFirstEmpty(container.getChildren(), 1);
                    TupleM4<Double,Double,Double,Double> bestPos = bestRec(e.getX(), e.getY());
                    // add empty window at index
                    // the method call eventually invokes load() method below, with
                    // component/child == null (3rd case)
                    // first we initialize position & size
                    container.properties.put(index + "x", bestPos.a);
                    container.properties.put(index + "y", bestPos.b);
                    container.properties.put(index + "w", bestPos.c);
                    container.properties.put(index + "h", bestPos.d);
                    container.addChild(index, null);
                }
                if(e.getButton()==SECONDARY && container.getChildren().isEmpty()) {
                    container.close();
                }
                e.consume();
            }
        });
        
        // do not support drag from (widget areas already do that for us)
        root.setOnDragDetected(null);
        // return graphics to normal
        root.setOnDragDone( e -> root.pseudoClassStateChanged(draggedPSEUDOCLASS, false));
        // accept drag onto
        root.setOnDragOver(DragUtil.componentDragAcceptHandler);
        // handle drag onto
        root.setOnDragDropped( e -> {
            if (DragUtil.hasComponent()) {
                int index = findFirstEmpty(container.getChildren(), 1);
                container.addChild(index, null);
                container.swapChildren(index,DragUtil.getComponent());
                e.setDropCompleted(true);
                e.consume();
            }
        });
        root.widthProperty().addListener((o,ov,nv) -> {
            windows.forEach((i,w) -> {
                boolean s = w.snappable.get();
                w.snappable.unbind();
                w.snappable.set(false);
                if(container.properties.containsKey(i+"x")) w.x.set(container.properties.getD(i+"x")*nv.doubleValue());
                if(container.properties.containsKey(i+"w")) w.w.set(container.properties.getD(i+"w")*nv.doubleValue());
                maintain(GUI.snapping, w.snappable);
            });
        });
        root.heightProperty().addListener((o,ov,nv) -> {
            windows.forEach((i,w) -> {
                boolean s = w.snappable.get();
                w.snappable.unbind();
                w.snappable.set(false);
                if(container.properties.containsKey(i+"y")) w.y.set(container.properties.getD(i+"y")*nv.doubleValue());
                if(container.properties.containsKey(i+"h")) w.h.set(container.properties.getD(i+"h")*nv.doubleValue());
                maintain(GUI.snapping, w.snappable);
            });
        });
    }
    
    private boolean isHere(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        return !windows.values().stream().anyMatch(w-> w.x.get()<x && w.y.get()<y && w.x.get()+w.w.get()>x && w.y.get()+w.h.get()>y);
    }
    
    public void load() {
        widgets.clear();
        container.getChildren().forEach(this::loadWindow);
    }
    
    public void loadWindow(int i, Component cm) {
        PaneWindowControls w = getWindow(i);

        Node n;
        Layouter l=null;
        if(cm instanceof Container) {
            Container c  = (Container) cm;
            n = c.load(w.content);
        } else
        if(cm instanceof Widget) {
            WidgetArea wa = new WidgetArea(container, i);
                       wa.loadWidget((Widget)cm);
                       widgets.put(i,wa);
                       w.moveOnDragOf(w.content);
            n = wa.root;
        } else {
            BooleanProperty tmp = new SimpleBooleanProperty(true);
            l = new Layouter(container, i);
            final Consumer<String> onS = l.cp.onSelect;
            l.cp.onSelect = v -> { tmp.set(false); onS.accept(v); };
            l.cp.onCancel = () -> { if(tmp.get())closeAndDo(w.root, () -> container.removeChild(i));};
            n = l.root;
        }

        w.content.getChildren().setAll(n);
        setAnchors(n, 0);
        if(l!=null) l.show();
    }
    public void closeWindow(int i) {
        PaneWindowControls w = windows.get(i);
        if(w!=null) { // null can happen only in illegal call, but cant prevent that for now (layouter calls close 2 times)
            w.close();
            windows.remove(i);
            widgets.remove(i);
            container.properties.remove(i+"x");
            container.properties.remove(i+"y");
            container.properties.remove(i+"w");
            container.properties.remove(i+"h");
        }
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void show() { }

    @Override
    public void hide() {
        windows.forEach((i,w) -> {
            if(container.getChildren().get(i)==null) closeAndDo(w.root, () -> container.removeChild(i));
        });
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
        PaneWindowControls w = new PaneWindowControls(root);
        w.root.getStyleClass().add("freeflowcontainer-window");
        w.setHeaderVisible(false);
        w.offscreenFixOn.set(false);
        // initial size/pos
        w.open();
        w.resizeHalf();
        w.alignCenter();
        w.snappable.set(false);
        // values from previous session (used when deserializing)
        if(container.properties.containsKey(i+"x")) w.x.set(container.properties.getD(i+"x")*root.getWidth());
        if(container.properties.containsKey(i+"y")) w.y.set(container.properties.getD(i+"y")*root.getHeight());
        if(container.properties.containsKey(i+"w")) w.w.set(container.properties.getD(i+"w")*root.getWidth());
        if(container.properties.containsKey(i+"h")) w.h.set(container.properties.getD(i+"h")*root.getHeight());
        // store for restoration (runLater avoids initialization problems)
        runLater(()->{
            maintain(w.x, v -> { container.properties.put(i+"x", v.doubleValue()/root.getWidth());}); 
            maintain(w.y, v -> { container.properties.put(i+"y", v.doubleValue()/root.getHeight());});
            maintain(w.w, v -> { container.properties.put(i+"w", v.doubleValue()/root.getWidth());});
            maintain(w.h, v -> { container.properties.put(i+"h", v.doubleValue()/root.getHeight());});
        maintain(GUI.snapDistance, d->d, w.snapDistance);
        maintain(GUI.snapping, w.snappable);
//            maintain(w.x, v -> { if(w.resizing.get()==NONE) container.properties.put(i+"x", v.doubleValue()/root.getWidth());}); 
//            maintain(w.y, v -> { if(w.resizing.get()==NONE) container.properties.put(i+"y", v.doubleValue()/root.getHeight());});
//            maintain(w.w, v -> { if(w.resizing.get()!=NONE) container.properties.put(i+"w", v.doubleValue()/root.getWidth());});
//            maintain(w.h, v -> { if(w.resizing.get()!=NONE) container.properties.put(i+"h", v.doubleValue()/root.getHeight());});
        });
        return w;
    }
    
    TupleM4<Double,Double,Double,Double> bestRec(double x, double y) {
        TupleM4<Double,Double,Double,Double> b = new TupleM4(0d, root.getWidth(), 0d, root.getHeight());
        
        for(PaneWindowControls w : windows.values()) {
           double wl = w.x.get()+w.w.get();
           if(wl<x && wl>b.a) b.a = wl;
           double wr = w.x.get();
           if(wr>x && wr<b.b) b.b = wr;
           double ht = w.y.get()+w.h.get();
           if(ht<y && ht>b.c) b.c = ht;
           double hb = w.y.get();
           if(hb>y && hb<b.d) b.d = hb;
        }
        
        return new TupleM4<>(b.a/root.getWidth(),b.c/root.getHeight(),
                            (b.b-b.a)/root.getWidth(),(b.d-b.c)/root.getHeight());
    }
    
    TupleM4<Double,Double,Double,Double> bestRecSimple(double x, double y) {
        return new TupleM4<>(x/root.getWidth()-1/6d,
                             y/root.getHeight()-1/6d,
                             1/3d, 1/3d);
    }
    
}