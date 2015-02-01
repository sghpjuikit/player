/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;

import GUI.objects.Window.Pane.PaneWindowControls;
import Layout.*;
import Layout.Widgets.Widget;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.stream.IntStream.iterate;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import static util.Util.setAnchors;
import util.async.Async;

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
        
        root.setOnMouseClicked(e -> {
            if(e.getClickCount()==2) {
                int index = iterate(1, i -> i+1)
                                   .filter(i->container.getChildren().get(i)==null)
                                   .findFirst().getAsInt();
                container.addChild(index, null);
            }
        });
    }
    
    public void load() {
        widgets.clear();
        for(Entry<Integer,Component> e : container.getChildren().entrySet()) {
            int i = e.getKey();
            Component cm = e.getValue();
            PaneWindowControls w = windows.get(i);
            if(w==null) {
                w = buidWindow(i);
                windows.put(i,w);
            }
            
            Node n;
            if(cm instanceof Container) {
                Container c  = (Container) cm;
                n = c.load(w.content);
            } else
            if(cm instanceof Widget) {
                WidgetArea wa = new WidgetArea(container, i);
                           wa.loadWidget((Widget)cm);
                           widgets.put(i,wa);
                n = wa.root;
            } else {
                n = new Layouter(container, i).getRoot();
            }
            
            w.content.getChildren().setAll(n);
            setAnchors(n, 0);
        }
    }
    
    public void removeWindow(int i) {
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
    
    
    private PaneWindowControls buidWindow(int i) {
        PaneWindowControls w = new PaneWindowControls(root);
        w.open();
        w.offscreenFixOn.set(false);
        // initial size/pos
        w.resizeHalf();
        w.alignCenter();
        // values from previous session
        Async.runOnFX(()->{
        if(container.properties.containsKey(i+"x")) w.x.set(container.properties.getD(i+"x"));
        if(container.properties.containsKey(i+"y")) w.y.set(container.properties.getD(i+"y"));
        if(container.properties.containsKey(i+"w")) w.w.set(container.properties.getD(i+"w"));
        if(container.properties.containsKey(i+"h")) w.h.set(container.properties.getD(i+"h"));
        });
        w.x.addListener((o,ov,nv) -> container.properties.put(i+"x", nv));
        w.y.addListener((o,ov,nv) -> container.properties.put(i+"y", nv));
        w.w.addListener((o,ov,nv) -> container.properties.put(i+"w", nv));
        w.h.addListener((o,ov,nv) -> container.properties.put(i+"h", nv));
        return w;
    }
    
}
