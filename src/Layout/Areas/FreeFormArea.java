/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;

import GUI.DragUtil;
import static GUI.GUI.closeAndDo;
import GUI.objects.Window.Pane.PaneWindowControls;
import Layout.*;
import static Layout.Areas.Area.draggedPSEUDOCLASS;
import Layout.Widgets.Widget;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import static util.Util.setAnchors;
import static util.functional.Util.findFirstEmpty;

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
            isHere.set(isHere.get() && isHere(e));
            if(e.getButton()==PRIMARY && isHere.get()) {
                int index = findFirstEmpty(container.getChildren(), 1);
                // add empty window at index
                // the method call eventually invokes load() method below, with
                // component/child == null (3rd case)
                // first we initialize position & size
                container.properties.put(index + "x", e.getX()-root.getWidth()/6);
                container.properties.put(index + "y", e.getY()-root.getHeight()/6);
                container.properties.put(index + "w", root.getWidth()/3);
                container.properties.put(index + "h", root.getHeight()/3);
                container.addChild(index, null);
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
        w.close();
        windows.remove(i);
        widgets.remove(i);
        container.properties.remove(i+"x");
        container.properties.remove(i+"y");
        container.properties.remove(i+"w");
        container.properties.remove(i+"h");
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void show() { }

    @Override
    public void hide() { }
    
    
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
        w.setHeaderVisible(false);
        w.offscreenFixOn.set(false);
        // initial size/pos
        w.open();
        w.resizeHalf();
        w.alignCenter();
        // values from previous session
//       Async.runOnFX(()->{
        if(container.properties.containsKey(i+"x")) w.x.set(container.properties.getD(i+"x"));
        if(container.properties.containsKey(i+"y")) w.y.set(container.properties.getD(i+"y"));
        if(container.properties.containsKey(i+"w")) w.w.set(container.properties.getD(i+"w"));
        if(container.properties.containsKey(i+"h")) w.h.set(container.properties.getD(i+"h"));
//        });
        w.x.addListener((o,ov,nv) -> container.properties.put(i+"x", nv));
        w.y.addListener((o,ov,nv) -> container.properties.put(i+"y", nv));
        w.w.addListener((o,ov,nv) -> container.properties.put(i+"w", nv));
        w.h.addListener((o,ov,nv) -> container.properties.put(i+"h", nv));
        return w;
    }
    
}
