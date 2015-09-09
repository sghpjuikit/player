/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import java.util.*;

import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import AudioPlayer.services.ClickEffect;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.WidgetManager.WidgetSource;
import Layout.Widgets.controller.Controller;
import Layout.Widgets.controller.io.InOutput;
import Layout.Widgets.controller.io.Input;
import Layout.Widgets.controller.io.Output;
import gui.objects.Text;
import gui.objects.icon.Icon;
import main.App;
import util.animation.Anim;
import util.graphics.Util;
import util.graphics.drag.DragUtil;

import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.DragEvent.*;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.util.Duration.millis;
import static util.functional.Util.*;

/**
 <p>
 @author Plutonium_
 */
public class IOPane extends StackPane {

    public IOPane(Controller controller) {
        c = controller;
        out_nodes = map(c.getOutputs().getOutputs(), OutputNode::new);
        in_nodes = map(c.getInputs().getInputs(), InputNode::new);
        getChildren().addAll(out_nodes);
        getChildren().addAll(in_nodes);
    }

    private final Controller c;
    private final List<OutputNode> out_nodes;
    private final List<InputNode> in_nodes;

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        double os = out_nodes.size()+1;
        double is = in_nodes.size()+1;
        double h = getHeight();
        double w = getWidth();
        double ohx = h/os;
        double ihx = h/is;

        forEachWithI(out_nodes, (i,o) -> o.relocate(w-o.getWidth()-5, ohx*(i+1)-o.getHeight()/2));
        forEachWithI(in_nodes, (i,o) -> o.relocate(5, ihx*(i+1)-o.getHeight()/2));
    }


    public static final String INODE_STYLECLASS = "inode";
    public static final String ONODE_STYLECLASS = "onode";
    public static final String IONODE_STYLECLASS = "ionode";
    public static final PseudoClass DRAGOVER_PSEUDOCLASS = getPseudoClass("drag-over");

    static interface XNode {
        Icon getIcon();
        default Point2D getSceneXY() {
            Icon i = getIcon();
            AnchorPane widget_io = App.getWindow().getSwitchPane().widget_io;
            double translation_x = widget_io.getTranslateX();
            double header = widget_io.localToScene(0,0).getY() - 5;
            Point2D p = i.localToScene(i.getLayoutBounds().getMinX(),i.getLayoutBounds().getMinY());
                    p = p.subtract(translation_x-5,header);
                    // start = new Point2D(start.getX()/scale.getX(), start.getY()/scale.getY());
            return p;
        }
    }
    static class OutputNode<T> extends HBox implements XNode {
        Text t = new Text();
        Icon i = new Icon();
        Output output;
        OutputNode(Output<T> o) {
            super(8);
            output = o;

            setMaxSize(80,120);
            getChildren().addAll(t,i);
            setAlignment(Pos.CENTER_RIGHT);
            i.styleclass(ONODE_STYLECLASS);

            Anim a = new Anim(millis(250), at -> Util.setScaleXY(t, at));
            i.setOnMouseEntered(e -> a.playOpen());
            t.setOnMouseExited(e -> a.playClose());

            i.addEventFilter(DRAG_DETECTED,e -> {
                DragUtil.setWidgetOutput(o,i.startDragAndDrop(TransferMode.LINK));
                e.consume();
            });
            i.addEventFilter(DRAG_ENTERED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, true));
            i.addEventFilter(DRAG_EXITED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, false));

            o.monitor(v -> a.playCloseDoOpen(() -> t.setText(oToStr(o))));
            o.monitor(v -> App.use(ClickEffect.class, c -> {
                if(!gui.GUI.isLayoutMode())
                    c.run(getSceneXY());
            }));
        }

        @Override
        public Icon getIcon() {
            return i;
        }
    }
    static class InputNode<T> extends HBox implements XNode {
        Text t = new Text();
        Icon i = new Icon();
        Input input;
        InputNode(Input<T> in) {
            super(8);
            input = in;

            setMaxSize(80,120);
            getChildren().addAll(i,t);
            setAlignment(Pos.CENTER_LEFT);
            i.styleclass(INODE_STYLECLASS);

            Anim a = new Anim(millis(250), at -> Util.setScaleXY(t, at));
            i.setOnMouseEntered(e -> a.playOpen());
            t.setOnMouseExited(e -> a.playClose());

            i.addEventFilter(DRAG_OVER, DragUtil.anyDragAccepthandler);
            i.addEventFilter(DRAG_DROPPED,e -> {
                if(DragUtil.hasWidgetOutput()) {
                    in.bind(DragUtil.getWidgetOutput(e));
                    drawWidgetIO();
                    e.setDropCompleted(true);
                    e.consume();
                } else {
                    Object o = DragUtil.hasComponent() ? DragUtil.getComponent().child : DragUtil.getAny(e);
                    Class c = o.getClass();
                    if(in.getType().isAssignableFrom(c)) {
                        in.setValue((T)o);
                        e.setDropCompleted(true);
                        e.consume();
                    }
                }
            });
            i.addEventFilter(DRAG_ENTERED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, true));
            i.addEventFilter(DRAG_EXITED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, false));

            t.setText(iToStr(input));
        }

        @Override
        public Icon getIcon() {
            return i;
        }
    }
    static class InOutputNode<T> extends VBox implements XNode {
        Text t = new Text();
        Icon i = new Icon();
        InOutput inoutput;
        InOutputNode(InOutput<T> inout) {
            super(8);
            inoutput = inout;

            setMaxSize(80,120);
            getChildren().addAll(i,t);
            setAlignment(Pos.CENTER_LEFT);
            i.styleclass(IONODE_STYLECLASS);

            Anim a = new Anim(millis(250), at -> Util.setScaleXY(t, at));
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
            i.addEventFilter(DRAG_ENTERED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, true));
            i.addEventFilter(DRAG_EXITED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, false));

            Output<T> o = inout.o;
            o.monitor(v -> a.playCloseDoOpen(() -> t.setText(oToStr(o))));
            o.monitor(v -> App.use(ClickEffect.class, c -> {
                if(!gui.GUI.isLayoutMode())
                    c.run(getSceneXY());
            }));
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
            App.getWindow().getSwitchPane().widget_io.getChildren().add(this);

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
            double h = App.getWindow().getSwitchPane().widget_io.getHeight();
            minHeight(h);
            prefHeight(h);
            maxHeight(h);

            getElements().clear();
            getElements().add(new MoveTo(startx, starty));

            double dx = tox-startx;
            double dy = toy-starty;
            if(dx>0) {
                double d = 20; //Math.random()*30/10*10+10;
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

    public static String oToStr(Output o) {
        return App.className.get(o.getType()) + " : " + o.getName() +
               "\n" + App.instanceName.get(o.getValue());
    }
    public static String iToStr(Input i) {
        return App.className.get(i.getType()) + " : " + i.getName() + "\n";
    }
    public static void drawWidgetIO() {
        Map<Input,XNode> is = new HashMap();
        Map<Output,XNode> os = new HashMap();

        WidgetManager.findAll(WidgetSource.ANY).map(w->w.getController().getActivityNode())
            .filter(ISNTØ)
            .forEach(c -> {
                c.in_nodes.forEach(i -> is.put(((InputNode)i).input, ((XNode)i)));
                c.out_nodes.forEach(o -> os.put(((OutputNode)o).output, ((XNode)o)));
            });
        ionodes.getChildren().forEach(n -> {
            is.put(((InOutputNode)n).inoutput.i, (XNode)n);
            os.put(((InOutputNode)n).inoutput.o, (XNode)n);
        });

        AnchorPane widget_io = App.getWindow().getSwitchPane().widget_io;
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
                    Node ni = inputnode.getIcon();
                    Node no = outputnode.getIcon();
                    Point2D scale = new Point2D(widget_io.getParent().getScaleX(),widget_io.getParent().getScaleY());
                    double translation_x = widget_io.getTranslateX();
                    double header = widget_io.localToScene(0,0).getY() - 5;
                    Point2D start = ni.localToScene(ni.getLayoutBounds().getMinX(),ni.getLayoutBounds().getMinY());
                            start = start.subtract(translation_x,header);
//                            start = new Point2D(start.getX()/scale.getX(), start.getY()/scale.getY());
                    Point2D end = no.localToScene(no.getLayoutBounds().getMinX(),no.getLayoutBounds().getMinY());
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