/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.hierarchy;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import util.collections.Tuple2;

/**
 <p>
 @author Plutonium_
 */
public class HierarchyView<V> {
    private V v;
    public Predicate<V> hasUp;
    public Predicate<V> hasDown;
    public Function<V,V> up;
    public Function<V,Stream<V>> childrenSupplier;
    public Supplier<Tuple2<Pane,Pane>> layoutFactory;
    public Function<V,Node> graphicFactory;
    StackPane root = new StackPane();
    
    public HierarchyView(){
        root.addEventFilter(MOUSE_CLICKED, e -> {
            if(e.getButton()==SECONDARY && hasUp.test(v))
                setItem(up.apply(v));
        });
    }
    
    public StackPane getPane() {
        return root;
    }
    
    public void setItem(V v) {
        this.v = v;
        
        Tuple2<Pane,Pane> l = layoutFactory.get();        
        
        Pane content = l._2;
        childrenSupplier.apply(v).map(c -> {
            Node n = graphicFactory.apply(c);
                n.setOnMouseClicked(e -> {
                    if(e.getClickCount()==2 && hasDown.test(c))
                        setItem(c);
                    e.consume();
                });
            return n;
        }).forEach(content.getChildren()::add);
        
        Pane lroot = l._1;
        root.getChildren().setAll(lroot);
    }
    
}
