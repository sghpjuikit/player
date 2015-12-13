/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import AudioPlayer.services.ClickEffect;
import Layout.container.switchcontainer.SwitchPane;
import Layout.widget.WidgetManager;
import Layout.widget.WidgetManager.WidgetSource;
import Layout.widget.controller.Controller;
import Layout.widget.controller.io.InOutput;
import Layout.widget.controller.io.Input;
import Layout.widget.controller.io.Output;
import gui.objects.Text;
import gui.objects.Window.stage.Window;
import gui.objects.icon.Icon;
import util.animation.Anim;
import util.collections.map.Map2D;
import util.collections.map.Map2D.Key;
import util.graphics.Util;
import util.graphics.drag.DragUtil;

import static java.lang.Math.abs;
import static java.lang.Math.random;
import static java.lang.Math.signum;
import static java.util.stream.Collectors.toList;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.DragEvent.*;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.functional.Util.*;
import static util.graphics.drag.DragUtil.installDrag;

/**
 <p>
 @author Plutonium_
 */
public class IOLayer extends StackPane {

    public static final String INODE_STYLECLASS = "inode";
    public static final String ONODE_STYLECLASS = "onode";
    public static final String IONODE_STYLECLASS = "ionode";
    public static final PseudoClass DRAGOVER_PSEUDOCLASS = getPseudoClass("drag-over");

    static public final ObservableSet<Input> all_inputs = FXCollections.observableSet();
    static public final ObservableSet<Output> all_outputs = FXCollections.observableSet();
    static public final ObservableSet<InOutput> all_inoutputs = InOutput.inoutputs;
    static public final Map2D<Input,Output,IOLine> connections = new Map2D<>();
    static public final Map<Input,XNode> inputnodes = new HashMap<>();
    static public final Map<Output,XNode> outputnodes = new HashMap<>();
    static public final Map<InOutput,InOutputNode> inoutputnodes = new HashMap<>();

    public void addController(Controller c) {
        c.getInputs().getInputs().forEach(this::addInput);
        c.getOutputs().getOutputs().forEach(this::addOutput);
    }

    public void remController(Controller c) {
        c.getInputs().getInputs().forEach(this::remInput);
        c.getOutputs().getOutputs().forEach(this::remOutput);
    }

    private void addInput(Input<?> i) {
        all_inputs.add(i);
        inputnodes.computeIfAbsent(i, k -> {
            InputNode<?> in = new InputNode<>(i);
            i.getSources().forEach(o -> connections.computeIfAbsent(new Key(i,o), key -> new IOLine(i,o)));
            getChildren().add(in.graphics);
            return in;
        });
    }

    private void remInput(Input<?> i) {
        all_inputs.remove(i);
        i.getSources().forEach(o -> removeChild(connections.remove2D(new Key(i,o))));
        removeXNode(inputnodes.remove(i));
    }
    private void addOutput(Output<?> o) {
        all_outputs.add(o);
        outputnodes.computeIfAbsent(o, key -> {
            OutputNode<?> on = new OutputNode<>(o);
            getChildren().add(on.graphics);
            return on;
        });
    }
    private void remOutput(Output<?> o) {
        all_outputs.remove(o);
        removeXNode(outputnodes.remove(o));
    }
    private void addInOutput(InOutput<?> io) {
        all_inoutputs.add(io);
        inoutputnodes.computeIfAbsent(io, k -> {
            InOutputNode<?> ion = new InOutputNode<>(io);
            inputnodes.put(io.i, ion);
            outputnodes.put(io.o, ion);
            io.i.getSources().forEach(o -> connections.computeIfAbsent(new Key(io.i,o), key -> new IOLine(io.i,o)));
            getChildren().add(ion.graphics);
            return ion;
        });
    }
    private void remInOutput(InOutput<?> io) {
        all_inoutputs.remove(io);
        io.i.getSources().forEach(o -> removeChild(connections.remove2D(new Key(io.i,o))));
        removeXNode(inoutputnodes.remove(io));
        inputnodes.remove(io.i);
        outputnodes.remove(io.o);
    }


    public static void addConnectionE(Input<?> i, Output<?> o) {
        Window.WINDOWS.stream().map(Window::getSwitchPane).filter(ISNTØ).map(s -> s.widget_io).forEach(wio -> wio.addConnection(i,o));
    }
    public static void remConnectionE(Input<?> i, Output<?> o) {
        Window.WINDOWS.stream().map(Window::getSwitchPane).filter(ISNTØ).map(s -> s.widget_io).forEach(wio -> wio.remConnection(i,o));
    }
    private void addConnection(Input<?> i, Output<?> o) {
        connections.computeIfAbsent(new Key(i,o), key -> new IOLine(i,o));
        drawGraph();
    }
    private void remConnection(Input<?> i, Output<?> o) {
        removeChild(connections.remove2D(i,o));
        drawGraph();
    }

    private final SwitchPane switchpane;
    private final double padding = 15;
    private final DoubleProperty translation;
    private final DoubleProperty scalex;
    private final DoubleProperty scaley;

    public IOLayer(SwitchPane sp) {
        switchpane = sp;
        translation = sp.translateProperty();
        scalex = sp.zoomProperty();
        scaley = sp.zoomProperty();
        scalex.addListener((o,ov,nv) -> layoutChildren());

        setMouseTransparent(false);
        setPickOnBounds(false);
        visibleProperty().bind(gui.GUI.layout_mode);
//        scaleXProperty().bind(scalex);
//        scaleYProperty().bind(scaley);
        translateXProperty().bind(translation.multiply(scalex));

        // set & maintain children
        all_inoutputs.forEach(this::addInOutput);
        all_inputs.addListener((SetChangeListener.Change<? extends Input> c) -> {
            Input ai = c.getElementAdded();
            Input ri = c.getElementRemoved();
            if(ai!=null) addInput(ai);
            if(ri!=null) remInput(ri);
        });
        all_outputs.addListener((SetChangeListener.Change<? extends Output> c) -> {
            Output ao = c.getElementAdded();
            Output ro = c.getElementRemoved();
            if(ao!=null) addOutput(ao);
            if(ro!=null) remOutput(ro);
        });
        all_inoutputs.addListener((SetChangeListener.Change<? extends InOutput> c) -> {
            InOutput aio = c.getElementAdded();
            InOutput rio = c.getElementRemoved();
            if(aio!=null) addInOutput(aio);
            if(rio!=null) remInOutput(rio);
        });
    }

    private void removeChild(Node n) {
        if(n!=null) getChildren().remove(n);
    }
    private void removeXNode(XNode n) {
        if(n!=null) getChildren().remove(n.graphics);
    }

    @Override
    protected void layoutChildren() {
//        super.layoutChildren();
        double W = getWidth();
        double H = getHeight();

//        for(Node n : getChildren()) {
//            if(n instanceof Path)
//                n.resizeRelocate(0,0,W,H);
//        }
        double sx = scalex.get();
        double sy = scaley.get();
        double header_offset = switchpane.getRoot().localToScene(0,0).getY();
        double translation_offset = translation.get();
        double iconhalfsize = 5;
        WidgetManager.findAll(WidgetSource.ANY)
            .map(w -> w.getController()).filter(ISNTØ)
            .forEach(c -> {
                List<XNode> is = c.getInputs().getInputs().stream().map(inputnodes::get).filter(ISNTØ).collect(toList());
                List<XNode> os = c.getOutputs().getOutputs().stream().map(outputnodes::get).filter(ISNTØ).collect(toList());
                Bounds b = c.getWidget().areaTemp.root.localToScene(c.getWidget().areaTemp.root.getBoundsInLocal());
                double basex = b.getMinX()/scalex.doubleValue()-translation_offset;
                double basey = b.getMinY()-header_offset;
                double w = b.getWidth()/scalex.doubleValue();
                double h = b.getHeight();
                double ihx = h/(is.size()+1);
                double ohx = h/(os.size()+1);

                forEachWithI(is, (i,o) -> {
                    o.cx = scaleX(basex + padding);
                    o.cy = scaleY(basey + ihx*(i+1));
                    o.graphics.relocate(o.cx - iconhalfsize,o.cy-o.graphics.getHeight()/2);
                });
                forEachWithI(os, (i,o) -> {
                    o.cx = scaleX(basex + w - padding - 2*iconhalfsize); // not sure why iconhalfsize
                    o.cy = scaleY(basey +  ohx*(i+1));
                    o.graphics.relocate(o.cx + iconhalfsize -o.graphics.getWidth(),o.cy-o.graphics.getHeight()/2);
                });
            });
        forEachWithI(inoutputnodes.values(), (i,o) -> {
            o.cx = (i+1)*200;
            o.cy = H-120;
            o.graphics.relocate(o.cx-iconhalfsize,o.cy-iconhalfsize+24);
        });

        drawGraph();
    }

    public void drawGraph() {
        connections.forEach((input,output,line) -> {
            XNode inputnode = inputnodes.get(input);
            XNode outputnode = outputnodes.get(output);
            if(inputnode!=null && outputnode!=null)
                line.lay(inputnode.cx,inputnode.cy,outputnode.cx,outputnode.cy);
        });
    }


    private double scaleX(double x) {
        return x*scalex.doubleValue();
    }

    private double scaleY(double y) {
        double middle = getHeight()/2;
        return middle + scaley.doubleValue()*(y-middle);
    }


    abstract class XNode<XPUT, P extends Pane> {
        XPUT xput;
        P graphics;
        Text t = new Text();
        Icon i = new Icon();
        double cy = 80 + random()*20;
        double cx = 80 + random()*20;

        Point2D getSceneXY() {
            return new Point2D(i.getLayoutBounds().getMinX()+i.getLayoutBounds().getWidth()/2,i.getLayoutBounds().getMinY()+i.getLayoutBounds().getHeight()/2);
        }
    }
    class OutputNode<T> extends XNode<Output<T>,HBox> {

        OutputNode(Output<T> o) {
            xput = o;

            graphics = new HBox(8, t,i);
            graphics.setMaxSize(80,120);
            graphics.setAlignment(Pos.CENTER_RIGHT);
            i.styleclass(ONODE_STYLECLASS);

            Anim a = new Anim(millis(250), at -> Util.setScaleXY(t, at));
            i.setOnMouseEntered(e -> a.playOpen());
            t.setOnMouseExited(e -> a.playClose());

            // drag&drop
            i.addEventFilter(DRAG_DETECTED,e -> {
                DragUtil.setWidgetOutput(o,i.startDragAndDrop(TransferMode.LINK));
                e.consume();
            });

            o.monitor(v -> a.playCloseDoOpen(() -> t.setText(oToStr(o))));
            o.monitor(v -> APP.use(ClickEffect.class, c -> {
                if(gui.GUI.isLayoutMode())
                    c.run(getSceneXY());
            }));
        }
    }
    class InputNode<T> extends XNode<Input<T>,HBox> {

        InputNode(Input<T> in) {
            xput = in;

            graphics = new HBox(8, i,t);
            graphics.setMaxSize(80,120);
            graphics.setAlignment(Pos.CENTER_LEFT);
            i.styleclass(INODE_STYLECLASS);

            Anim a = new Anim(millis(250), at -> Util.setScaleXY(t, at));
            i.setOnMouseEntered(e -> a.playOpen());
            t.setOnMouseExited(e -> a.playClose());

            // drag&drop
            installDrag(
                i, null, "",
                DragUtil::hasAny,
                e -> {
                    if(DragUtil.hasWidgetOutput(e)) {
                        in.bind(DragUtil.getWidgetOutput(e));
                        drawGraph();
                    } else {
                        Object o = DragUtil.getAny(e);
                        Class c = o.getClass();
                        if(in.getType().isAssignableFrom(c)) {
                            in.setValue((T)o);
                        }
                    }
                }
            );
            i.addEventFilter(DRAG_ENTERED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, true));
            i.addEventFilter(DRAG_EXITED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, false));

            t.setText(iToStr(xput));
        }
    }
    class InOutputNode<T> extends XNode<InOutput<T>,VBox> {

        InOutputNode(InOutput<T> inout) {
            xput = inout;

            graphics = new VBox(8, i,t);
            graphics.setMaxSize(80,120);
            graphics.setAlignment(Pos.CENTER_LEFT);
            i.styleclass(IONODE_STYLECLASS);

            Anim a = new Anim(millis(250), at -> Util.setScaleXY(t, at));
            i.setOnMouseEntered(e -> a.playOpen());
            t.setOnMouseExited(e -> a.playClose());

            // drag&drop
            installDrag(
                i, null, "",
                DragUtil::hasWidgetOutput,
                e -> {
                    Output o = DragUtil.getWidgetOutput(e);
                    if(o!=inout.o) {
                        inout.i.bind(o);
                        drawGraph();
                    }
                }
            );
            i.addEventFilter(DRAG_ENTERED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, true));
            i.addEventFilter(DRAG_EXITED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, false));
            i.addEventFilter(DRAG_DETECTED,e -> {
                DragUtil.setWidgetOutput(inout.o,i.startDragAndDrop(TransferMode.LINK));
                e.consume();
            });

            Output<T> o = inout.o;
            o.monitor(v -> a.playCloseDoOpen(() -> t.setText(oToStr(o))));
            o.monitor(v -> APP.use(ClickEffect.class, c -> {
                if(gui.GUI.isLayoutMode())
                    c.run(getSceneXY());
            }));
        }
    }
    class IOLine extends Path {
        static final double GAP = 20;

        Output output;
        Input input;

        public IOLine(Input i, Output o) {
            input = i;
            output = o;

            IOLayer.this.getChildren().add(this);
            getStyleClass().add("input-output-line");
            IOLine.this.setMouseTransparent(false);
            IOLine.this.setPickOnBounds(false);

            setOnMouseClicked(e -> {
                if(e.getButton()==SECONDARY) i.unbind(o);
                e.consume();
            });
            setOnDragDetected(e -> {
                DragUtil.setWidgetOutput(output, startDragAndDrop(TransferMode.LINK));
                e.consume();
            });
//            hoverProperty().addListener((f,ov,nv) -> {
//            i.addEventFilter(DRAG_ENTERED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, true));
//            i.addEventFilter(DRAG_EXITED, e -> i.pseudoClassStateChanged(DRAGOVER_PSEUDOCLASS, false));
//            });
        }

        public void lay(double startx, double starty, double tox, double toy) {
            getElements().clear();
            getElements().add(new MoveTo(startx, starty));

            double h = IOLayer.this.getHeight();
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
        return APP.className.get(o.getType()) + " : " + o.getName() +
               "\n" + APP.instanceName.get(o.getValue());
    }
    public static String iToStr(Input i) {
        return APP.className.get(i.getType()) + " : " + i.getName() + "\n";
    }

}