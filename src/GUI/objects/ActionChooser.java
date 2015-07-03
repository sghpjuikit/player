/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects;

import Layout.Widgets.controller.Controller;
import Layout.Widgets.controller.io.Input;
import Layout.Widgets.controller.io.Output;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import gui.objects.icon.Icon;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import static javafx.scene.input.DragEvent.*;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import static javafx.util.Duration.millis;
import util.animation.Anim;
import util.ClassName;
import util.dev.TODO;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.functional.Util.*;
import util.graphics.drag.DragUtil;

/**
 <p>
 @author Plutonium_
 */
@TODO(purpose = FUNCTIONALITY, note = "decide best functionality, complete + clean up")
public class ActionChooser<T> extends StackPane {

    public final Text description = new Text();
    public final HBox actionBox;

    private int icon_size = 40;
    public T item;

    public ActionChooser(Controller controller) {
        
        setAlignment(Pos.CENTER);

        actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);

        description.setTextAlignment(TextAlignment.CENTER);
//        description.wrappingWidthProperty().bind(actPane.widthProperty());
//        description.setWrappingWidthNatural(true);

//        StackPane h2 = new StackPane(descL);
//                  h2.setAlignment(TOP_CENTER);
        description.setWrappingWidth(150);
//        descL.wrappingWidthProperty().bind(actPane.widthProperty());
        getChildren().addAll(actionBox, description);
        StackPane.setMargin(description, new Insets(20));
        StackPane.setAlignment(description, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(actionBox, Pos.CENTER);
        
        
        c = controller;
        out_nodes = map(c.getOutputs().getOutputs(), OutputNode::new);
        in_nodes = map(c.getInputs().getInputs(), InputNode::new);
        getChildren().addAll(out_nodes);
        getChildren().addAll(in_nodes);
    }

    public Icon addIcon(FontAwesomeIconName icon, String descriptn) {
        return addIcon(icon, null, descriptn);
    }
    public Icon addIcon(FontAwesomeIconName icon, String text, String descriptn) {
        Icon l = new Icon(icon, icon_size);
        l.setFont(new Font(l.getFont().getName(), 13));
//        l.setText(text);
        boolean drag_activated = false;
        boolean hover_activated = true;
        
        l.scaleYProperty().bind(l.scaleXProperty());
        if(drag_activated) {
            l.addEventHandler(DRAG_ENTERED, e -> description.setText(descriptn));
            l.addEventHandler(DRAG_EXITED, e -> description.setText(""));
            l.addEventHandler(DRAG_ENTERED, e -> l.setScaleX(1.1));
            l.addEventHandler(DRAG_EXITED, e -> l.setScaleX(1));
        }
        if(hover_activated) {
            l.addEventHandler(MOUSE_ENTERED, e -> description.setText(descriptn));
            l.addEventHandler(MOUSE_EXITED, e -> description.setText(""));
            l.addEventHandler(MOUSE_ENTERED, e -> l.setScaleX(1.1));
            l.addEventHandler(MOUSE_EXITED, e -> l.setScaleX(1));
        }
        actionBox.getChildren().add(l);
        return l;
    }
    
    public T getItem() {
        return item;
    }
    
    private final Controller c;
    private final List<OutputNode> out_nodes;
    private final List<InputNode> in_nodes;
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        
        double os = out_nodes.size()+1;
        double h = getHeight();
        double hx = h/os;
        double w = getWidth();
        
        forEachI(out_nodes, (i,o) -> o.relocate(w-o.getWidth()-5, hx*(i+1)-o.getHeight()/2));
        forEachI(in_nodes, (i,o) -> o.relocate(5, hx*(i+1)-o.getHeight()/2));
    }
    
    
    class OutputNode<T> extends HBox {
        Text t = new Text();
        Icon i = new Icon(FontAwesomeIconName.TOGGLE_RIGHT, 10);
        OutputNode(Output<T> o) {
            super(8);
            
            setMaxSize(80,120);
            getChildren().addAll(t,i);
            setAlignment(Pos.CENTER_RIGHT);
            
            Anim a = new Anim(millis(250), at -> util.Util.setScaleXY(t, at));
            i.setOnMouseEntered(e -> a.playOpen());
            t.setOnMouseExited(e -> a.playClose());
            
            i.addEventFilter(DRAG_DETECTED,e -> {
                DragUtil.setWidgetOutput(o,i.startDragAndDrop(TransferMode.LINK));
                e.consume();
            });
            
            o.monitor(v -> t.setText(ClassName.get(o.getType()) + " : " + o.getName() + "\n" + o.getValueAsS()));
        }
    }
    class InputNode<T> extends HBox {
        Text t = new Text();
        Icon i = new Icon(FontAwesomeIconName.TOGGLE_LEFT, 10);
        InputNode(Input<T> in) {
            super(8);
            
            setMaxSize(80,120);
            getChildren().addAll(i,t);
            setAlignment(Pos.CENTER_LEFT);
            
            Anim a = new Anim(millis(250), at -> util.Util.setScaleXY(t, at));
            i.setOnMouseEntered(e -> a.playOpen());
            t.setOnMouseExited(e -> a.playClose());
            
            i.addEventFilter(DRAG_OVER,DragUtil.widgetOutputDragAccepthandler);
            i.addEventFilter(DRAG_DROPPED,e -> {
                if(DragUtil.hasWidgetOutput()) {
                    in.bind(DragUtil.getWidgetOutput(e));
                    e.setDropCompleted(true);
                    e.consume();
                }
            });
            
            t.setText(ClassName.get(in.getType()) + " : " + in.getName());
        }
    }
}
