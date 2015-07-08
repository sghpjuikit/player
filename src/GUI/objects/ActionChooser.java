/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects;

import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.WidgetSource;
import Layout.Widgets.controller.Controller;
import Layout.Widgets.controller.io.InOutput;
import Layout.Widgets.controller.io.Input;
import Layout.Widgets.controller.io.Output;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import gui.LayoutAggregators.SwitchPane;
import gui.objects.icon.Icon;
import java.io.File;
import static java.lang.Math.abs;
import static java.lang.Math.signum;
import java.util.*;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import static javafx.scene.input.DragEvent.*;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import static javafx.util.Duration.millis;
import main.App;
import util.ClassName;
import util.animation.Anim;
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
        
        forEachWithI(out_nodes, (i,o) -> o.relocate(w-o.getWidth()-5, hx*(i+1)-o.getHeight()/2));
        forEachWithI(in_nodes, (i,o) -> o.relocate(5, hx*(i+1)-o.getHeight()/2));
    }
    
    
    static interface XNode {
        Icon getIcon();
    }
    static class OutputNode<T> extends HBox implements XNode {
        Text t = new Text();
        Icon i = new Icon(FontAwesomeIconName.TOGGLE_RIGHT, 10);
        Output output;
        OutputNode(Output<T> o) {
            super(8);
            output = o;
            
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

        @Override
        public Icon getIcon() {
            return i;
        }
    }
    static class InputNode<T> extends HBox implements XNode {
        Text t = new Text();
        Icon i = new Icon(FontAwesomeIconName.TOGGLE_LEFT, 10);
        Input input;
        InputNode(Input<T> in) {
            super(8);
            input = in;
            
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
                    drawWidgetIO();
                    e.setDropCompleted(true);
                    e.consume();
                } else if (in.getType().equals(File.class) && DragUtil.hasFiles(e)) {
                    in.setValue((T)DragUtil.getFiles(e).get(0));
                    e.setDropCompleted(true);
                    e.consume();
                }
            });
            
            t.setText(ClassName.get(in.getType()) + " : " + in.getName() + "\n");
        }

        @Override
        public Icon getIcon() {
            return i;
        }
    }
    static class InOutputNode<T> extends VBox implements XNode {
        Text t = new Text();
        Icon i = new Icon(FontAwesomeIconName.TOGGLE_LEFT, 10);
        InOutput inoutput;
        InOutputNode(InOutput<T> inout) {
            super(8);
            inoutput = inout;
            
            setMaxSize(80,120);
            getChildren().addAll(i,t);
            setAlignment(Pos.CENTER_LEFT);
            
            Anim a = new Anim(millis(250), at -> util.Util.setScaleXY(t, at));
            i.setOnMouseEntered(e -> a.playOpen());
            t.setOnMouseExited(e -> a.playClose());
            
            i.addEventFilter(DRAG_DETECTED,e -> {
                DragUtil.setWidgetOutput(inout.o,i.startDragAndDrop(TransferMode.LINK));
                e.consume();
            });
            i.addEventFilter(DRAG_OVER,DragUtil.widgetOutputDragAccepthandler);
            i.addEventFilter(DRAG_DROPPED,e -> {
                if(DragUtil.hasWidgetOutput()) {
                    Output o = DragUtil.getWidgetOutput(e);
                    if(o!=inout.o) {
                        inout.i.bind(o);
                        drawWidgetIO();
                    }
                    e.setDropCompleted(true);
                    e.consume();
                }
            });
            
            Output<T> o = inout.o;
            o.monitor(v -> t.setText(ClassName.get(o.getType()) + " : " + o.getName() + "\n" + o.getValueAsS()));
        }

        @Override
        public Icon getIcon() {
            return i;
        }
    }
    static class IOLine extends Path {
        static final double GAP = 20;
        
        Output output;
        Input input;
        
        public IOLine(Input i, Output o) {
            input = i;
            output = o;
            
            getStyleClass().add("input-output-line");
            ((SwitchPane)App.getWindow().getLayoutAggregator()).widget_io.getChildren().add(this);
            
            setOnMouseClicked(e -> {
                if(e.getButton()==SECONDARY) {
                    i.unbind(o);
                    drawWidgetIO();
                }
                e.consume();
            });
            setOnDragDetected(e -> {
                DragUtil.setWidgetOutput(output, startDragAndDrop(TransferMode.LINK));
                e.consume();
            });
        }
        
        public void lay(double startx, double starty, double tox, double toy) { // System.out.println(startx + " " + starty + " " + tox + " " + toy);
            setLayoutX(0);
            double h = ((SwitchPane)App.getWindow().getLayoutAggregator()).widget_io.getHeight();
            minHeight(h);
            prefHeight(h);
            maxHeight(h);
        
            getElements().clear();
            getElements().add(new MoveTo(startx, starty));
            
            double dx = tox-startx;
            double dy = toy-starty;
            if(dx>0) {
                double d = 20;
                // enhance start
                starty += d*signum(dy);
                getElements().add(new LineTo(startx,starty));
                // enhance end
                not_finished = true;
                not_finished_x = tox;
                not_finished_y = toy;
                tox -= d*signum(dx);
                toy -= 2*d*signum(dy);
            }
            layTo(startx, starty, tox, toy, h);
        }
        
        private boolean not_finished = false;
        private double not_finished_x;
        private double not_finished_y;
        void layTo(double startx, double starty, double tox, double toy, double h) {
            double dx = tox-startx;
            double dy = toy-starty;
            if(dx==0 || dy==0) {
                getElements().add(new LineTo(tox,toy));
                if(not_finished) {
                    not_finished = false;
                    layTo(tox, toy, not_finished_x, not_finished_y, h);
                }
            } else {
                double d_xy = min(abs(dx),abs(dy));
                double d = d_xy;
                double x = startx+signum(dx)*d;
                double y = starty+signum(dy)*d;
                getElements().add(new LineTo(x, y));
                layTo(x,y, tox, toy, h);
            }
        }
    }
    
    
    public static void drawWidgetIO() {
        Map<Input,XNode> is = new HashMap();
        Map<Output,XNode> os = new HashMap();
        
        WidgetManager.findAll(WidgetSource.ANY).map(w->w.getController().getActivityNode())
            .filter(isNotNULL)
            .map(ActionChooser.class::cast)
            .forEach(c -> {
                c.in_nodes.forEach(i -> is.put(((InputNode)i).input, ((XNode)i)));
                c.out_nodes.forEach(o -> os.put(((OutputNode)o).output, ((XNode)o)));
            });
        ionodes.getChildren().forEach(n -> {
            is.put(((InOutputNode)n).inoutput.i, (XNode)n);
            os.put(((InOutputNode)n).inoutput.o, (XNode)n);
        });
        
        AnchorPane widget_io = ((SwitchPane)App.getWindow().getLayoutAggregator()).widget_io;
        widget_io.getChildren().retainAll(ionodes);
        if(!widget_io.getChildren().contains(ionodes)) {
            widget_io.getChildren().add(ionodes);
            AnchorPane.setBottomAnchor(ionodes, 20.0);
        }
        
        is.forEach((input,inputnode) -> {
            Set<Output> outs = input.getSources();
            outs.forEach(output -> {
                XNode outputnode = os.get(output);
                if(outputnode!=null) {
                    Node ni = inputnode.getIcon().getGraphic();
                    Node no = outputnode.getIcon().getGraphic();
                    Point2D scale = new Point2D(widget_io.getParent().getScaleX(),widget_io.getParent().getScaleY());
                    double translation_x = widget_io.getTranslateX();
                    double header = widget_io.localToScene(0,0).getY() - 5;
                    Point2D start = ni.localToScene(ni.getBoundsInParent().getMinX()+10,ni.getBoundsInParent().getMinY());
                            start = start.subtract(translation_x,header);
//                            start = new Point2D(start.getX()/scale.getX(), start.getY()/scale.getY());
                    Point2D end = no.localToScene(no.getBoundsInParent().getMinX()+10,no.getBoundsInParent().getMinY());
                            end = end.subtract(translation_x,header);
//                            end = new Point2D(end.getX()-translation_x*scale.getX(), end.getY()/scale.getY());
                    new IOLine(input,output).lay(start.getX(),start.getY(),end.getX(),end.getY());
                }
            });
        });
    }
    
    static final HBox ionodes;
    static {
        ionodes = new HBox(15);
        ionodes.setPickOnBounds(false);
        ionodes.setAlignment(Pos.CENTER);
        ionodes.getChildren().addAll(map(InOutput.inoutputs,InOutputNode::new));
        
        InOutput.inoutputs.addListener(new ListChangeListener<InOutput>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends InOutput> c) {
                while(c.next()) {
                    if (c.wasPermutated()) {
                        for (int i = c.getFrom(); i < c.getTo(); ++i) {
                             //permutate
                        }
                    } else if (c.wasUpdated()) {
                             //update item
                    } else {
                        for (InOutput io : c.getRemoved()) {
                            ionodes.getChildren().removeIf(n -> ((InOutputNode)n).inoutput==io);
                        }
                        for (InOutput io : c.getAddedSubList()) {
                            ionodes.getChildren().add(new InOutputNode(io));
                        }
                    }
                }
            }
        });
    }

}
