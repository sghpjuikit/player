package sp.it.pl.layout.area;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javafx.animation.PathTransition;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import sp.it.pl.gui.objects.Text;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.layout.container.switchcontainer.SwitchPane;
import sp.it.pl.layout.widget.controller.Controller;
import sp.it.pl.layout.widget.controller.io.InOutput;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.layout.widget.controller.io.XPut;
import sp.it.pl.main.AppDragKt;
import sp.it.pl.main.Df;
import sp.it.util.animation.Anim;
import sp.it.util.collections.map.Map2D;
import sp.it.util.collections.map.Map2D.Key;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.random;
import static java.lang.Math.signum;
import static java.util.stream.Collectors.toList;
import static javafx.scene.input.DragEvent.DRAG_ENTERED;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;
import static javafx.util.Duration.millis;
import static sp.it.pl.layout.area.IOLayerUtilKt.xPutToStr;
import static sp.it.pl.layout.widget.WidgetSource.OPEN_LAYOUT;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.functional.Util.ISNTØ;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.forEachWithI;
import static sp.it.util.functional.Util.min;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.ui.UtilKt.pseudoclass;
import static sp.it.util.ui.UtilKt.setScaleXY;

/**
 * Display for {@link sp.it.pl.layout.widget.controller.io.XPut} of components, displaying their relations as am editable graph.
 */
public class IOLayer extends StackPane {
    public static final String INODE_STYLECLASS = "inode";
    public static final String ONODE_STYLECLASS = "onode";
    public static final String IONODE_STYLECLASS = "ionode";
    public static final String IOLINE_STYLECLASS = "ioline";
    public static final String IOLINE_RUNNER_STYLECLASS = "ioline-runner";
    public static final PseudoClass XNODE_DRAGOVER = pseudoclass("drag-over");
    public static final PseudoClass XNODE_SELECTED = pseudoclass("selected");
    private static final Object XNODE_KEY = new Object();

    static public final ObservableSet<Input<?>> all_inputs = FXCollections.observableSet();
    static public final ObservableSet<Output<?>> all_outputs = FXCollections.observableSet();
    static public final ObservableSet<InOutput<?>> all_inoutputs = FXCollections.observableSet();
    static public final Map2D<Input<?>,Output<?>,IOLine> connections = new Map2D<>();
    static public final Map<Input<?>,XNode<?,?,?>> inputnodes = new HashMap<>();
    static public final Map<Output<?>,XNode<?,?,?>> outputnodes = new HashMap<>();
    static public final Map<InOutput<?>,InOutputNode> inoutputnodes = new HashMap<>();


    public static void relayout() {
        APP.windowManager.windows.stream().map(Window::getSwitchPane).filter(ISNTØ).map(s -> s.widget_io)
              .forEach(IOLayer::requestLayout);
    }

    public static void addConnectionE(Input<?> i, Output<?> o) {
        APP.windowManager.windows.stream().map(Window::getSwitchPane).filter(ISNTØ).map(s -> s.widget_io)
              .forEach(wio -> wio.addConnection(i,o));
    }

    public static void remConnectionE(Input<?> i, Output<?> o) {
        APP.windowManager.windows.stream().map(Window::getSwitchPane).filter(ISNTØ).map(s -> s.widget_io)
              .forEach(wio -> wio.remConnection(i,o));
    }



    public void addController(Controller c) {
        c.getOwnedInputs().getInputs().forEach(this::addInput);
        c.getOwnedOutputs().getOutputs().forEach(this::addOutput);
    }

    public void remController(Controller c) {
        c.getOwnedInputs().getInputs().forEach(this::remInput);
        c.getOwnedOutputs().getOutputs().forEach(this::remOutput);
    }

    @SuppressWarnings("unchecked")
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
        i.getSources().forEach(o -> removeChild(connections.remove2D(new Key<>(i,o))));
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
        removeChildren(connections.removeIfKey2(o));
    }
    @SuppressWarnings("unchecked")
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
        io.i.getSources().forEach(o -> removeChild(connections.remove2D(new Key<>(io.i,o))));
        removeXNode(inoutputnodes.remove(io));
        removeChildren(connections.removeIfKey2(io.o));
        inputnodes.remove(io.i);
        outputnodes.remove(io.o);
    }

    @SuppressWarnings("unchecked")
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

    private EditIOLine<?> edit = null;
    private XNode selected = null;

    public IOLayer(SwitchPane sp) {
        switchpane = sp;
        translation = sp.translateProperty();
        scalex = sp.zoomProperty();
        scaley = sp.zoomProperty();
        scalex.addListener((o,ov,nv) -> layoutChildren());

        parentProperty().addListener((o,ov,nv) ->
            nv.addEventFilter(MOUSE_CLICKED, e -> selectNode(null))
        );

        setMouseTransparent(false);
        setPickOnBounds(false);
        visibleProperty().bind(APP.ui.getLayoutMode());
//        scaleXProperty().bind(scalex);
//        scaleYProperty().bind(scaley);
        translateXProperty().bind(translation.multiply(scalex));

        // set & maintain children
        all_inoutputs.forEach(this::addInOutput);
        all_inputs.addListener((SetChangeListener.Change<? extends Input> c) -> {
            Input ai = c.getElementAdded();
            Input ri = c.getElementRemoved();
            if (ai!=null) addInput(ai);
            if (ri!=null) remInput(ri);
        });
        all_outputs.addListener((SetChangeListener.Change<? extends Output> c) -> {
            Output ao = c.getElementAdded();
            Output ro = c.getElementRemoved();
            if (ao!=null) addOutput(ao);
            if (ro!=null) remOutput(ro);
        });
        all_inoutputs.addListener((SetChangeListener.Change<? extends InOutput> c) -> {
            InOutput aio = c.getElementAdded();
            InOutput rio = c.getElementRemoved();
            if (aio!=null) addInOutput(aio);
            if (rio!=null) remInOutput(rio);
        });
    }

    private void removeChild(Node n) {
        if (n!=null) getChildren().remove(n);
    }
    private void removeXNode(XNode n) {
        if (n!=null) getChildren().remove(n.graphics);
    }
    private void removeChildren(Stream<? extends Node> ns) {
        ns.forEach(this::removeChild);
    }

    private XNode editFrom = null; // editFrom == edit.node, editFrom.output == edit.output
    private XNode editTo = null;

    @SuppressWarnings("unchecked")
    private void editBegin(XNode n) {
        if (n==null) return;

        editFrom = n;
        edit = new EditIOLine<>(n);

        // start effect: disable & visually differentiate bindable & unbindable nodes
        outputnodes.forEach((input, node) -> node.onEditActive(true, false));
        inputnodes.forEach((input, node) -> node.onEditActive(true, node.input.isAssignable(editFrom.output)));
        connections.forEach((inOutput, line) -> line.onEditActive(true));
    }

    private void editMove(MouseEvent e) {
        if (edit==null || editFrom ==null) return;

        edit.lay(editFrom.cx, editFrom.cy, e.getX(), e.getY());

        XNode n = inputnodes.values().stream()
               .filter(in -> pow(in.cx-e.getX(),2)+pow(in.cy-e.getY(),2)<8*8)
               .filter(in -> in.input.isAssignable(editFrom.output))
               .findAny().orElse(null);

        if (editTo!=n) {
            if (editTo!=null) editTo.select(false);
            editTo = n;
            if (editTo!=null) editTo.select(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void editEnd() {
        if (edit==null) return;

        if (editTo!=null) editTo.input.bind(editFrom.output);
        getChildren().remove(edit);
        edit = null;

        if (editTo!=null) editTo.select(false);
        editTo = null;

        // stop effect: disable & visually differentiate bindable nodes
        outputnodes.forEach((input,node) -> node.onEditActive(false,true));
        inputnodes.forEach((input,node) -> node.onEditActive(false,true));
        connections.forEach((input_and_output,line) -> line.onEditActive(false));
    }

    private void selectNode(XNode n) {
        if (selected!=null) selected.select(false);
        selected = n;
        if (selected!=null) selected.select(true);
    }

    @Override
    protected void layoutChildren() {
	    var W = getWidth();
	    var H = getHeight();

        double header_offset = switchpane.getRoot().localToScene(0,0).getY();
        double translation_offset = translation.get();
        APP.widgetManager.widgets.findAll(OPEN_LAYOUT)
            .filter(w -> w!=null && w.getController()!=null)
            .forEach(w -> {
                Controller c = w.getController();
                var is = c.getOwnedInputs().getInputs().stream().map(inputnodes::get).filter(ISNTØ).collect(toList());
	            var os = c.getOwnedOutputs().getOutputs().stream().map(outputnodes::get).filter(ISNTØ).collect(toList());

                if (w.areaTemp==null || w.areaTemp.getRoot()==null) return; // TODO: during initiaization we are not ready yet, try to remove

                Node wr = w.areaTemp.getRoot();
                Bounds b = wr.localToScene(wr.getBoundsInLocal());
                double basex = b.getMinX()/scalex.doubleValue()-translation_offset;
                double basey = b.getMinY()-header_offset;
                double ww = b.getWidth()/scalex.doubleValue();
                double wh = b.getHeight();
                double ihx = wh/(is.size()+1);
                double ohx = wh/(os.size()+1);

                forEachWithI(is, (i,o) -> {
                    o.graphics.autosize(); // necessary before asking for size
                    var is2 = o.i.getLayoutBounds().getWidth()/2.0;
                    o.cx = calcScaleX(basex + padding);
                    o.cy = calcScaleY(basey + ihx*(i+1));
                    o.graphics.relocate(o.cx - is2, o.cy-o.graphics.getHeight()/2);
                });
                forEachWithI(os, (i,o) -> {
                    o.graphics.autosize(); // necessary before asking for size
                    var is2 = o.i.getLayoutBounds().getWidth()/2.0;
                    o.cx = calcScaleX(basex + ww - padding - 2*is2);
                    o.cy = calcScaleY(basey +  ohx*(i+1));
                    o.graphics.relocate(o.cx - o.graphics.getLayoutBounds().getWidth()+is2, o.cy-o.graphics.getLayoutBounds().getHeight()/2);
                });
            });

        var ioMinWidthX = 200.0;
        var ioGapX = 10.0;
        var ioOffsetX = 0.0;
        var ioOffsetYShift = -10.0;
        var ioOffsetY = H-150.0;
        var ios = inoutputnodes.values().stream().sorted(by(it -> it.inoutput.o.id.carrier_id)).collect(toList());
        for (InOutputNode<?> io: ios) {
            io.graphics.autosize(); // necessary before asking for size
            var is2 = io.i.getLayoutBounds().getWidth()/2.0;
            io.cx = ioOffsetX;
            io.cy = ioOffsetY;
            io.graphics.relocate(io.cx-is2,io.cy-is2);
            ioOffsetX += max(io.graphics.getLayoutBounds().getWidth(), ioMinWidthX) + ioGapX;
            ioOffsetY += ioOffsetYShift;
        }

        drawGraph();
    }

    public void drawGraph() {
        connections.forEach((input,output,line) -> {
            XNode inputnode = inputnodes.get(input);
            XNode outputnode = outputnodes.get(output);
            if (inputnode!=null && outputnode!=null)
                line.lay(inputnode.cx, inputnode.cy, outputnode.cx, outputnode.cy);
        });
    }

    private double calcScaleX(double x) {
        return x*scalex.doubleValue();
    }

    private double calcScaleY(double y) {
        double middle = getHeight()/2;
        return middle + scaley.doubleValue()*(y-middle);
    }

    abstract class XNode<X extends XPut<T>,T,P extends Pane> {
        final X xput;
        final Input<T> input;
        final Output<T> output;
        final InOutput<T> inoutput;
        P graphics;
        Text t = new Text();
        Icon i = new Icon();
        double cy = 80 + random()*20;
        double cx = 80 + random()*20;
        boolean selected = false;

        @SuppressWarnings("unchecked")
        XNode(X xPut) {
            this.xput = xPut;

            if (xPut instanceof Input) {
                input = (Input<T>) xPut;
                output = null;
                inoutput = null;
            } else
            if (xPut instanceof Output) {
                input = null;
                output = (Output<T>) xPut;
                inoutput = null;
            } else
            if (xPut instanceof InOutput) {
                input = ((InOutput<T>) xPut).i;
                output = ((InOutput<T>) xPut).o;
                inoutput = (InOutput<T>) xPut;
            } else {
                throw new IllegalArgumentException("Not a valid type");
            }

            i.addEventHandler(MOUSE_CLICKED, e -> {
            	if (e.getClickCount()==1)
	                selectNode(e.getButton()==SECONDARY ? null : this);
            	else if (e.getClickCount()==2 && output!=null && output.getValue()!=null)
            		APP.actionPane.show(output.getValue());
                e.consume();
            });


            Anim a = new Anim(millis(250), at -> {
                t.setOpacity(at);
                setScaleXY(t, 0.8+0.2*at);
            });
            var valuePut = xPut instanceof Input ? input : output;
            valuePut.sync(consumer(v -> a.playCloseDoOpen(runnable(() -> t.setText(xPutToStr(valuePut))))));

            if (output!=null) {
                output.sync(consumer(v ->
                    inputnodes.values().stream().map(in -> in.input).filter(i -> i.getSources().contains(output)).forEach(input ->
                        connections.getOpt(new Key<>(input,output)).ifPresent(c -> c.send())
                    )
                ));
            }
        }

        Point2D getSceneXY() {
            return new Point2D(i.getLayoutBounds().getMinX()+i.getLayoutBounds().getWidth()/2,i.getLayoutBounds().getMinY()+i.getLayoutBounds().getHeight()/2);
        }

        void select(boolean v) {
            if (selected==v) return;
            selected = v;
            i.pseudoClassStateChanged(XNODE_SELECTED, v);
        }

        void onEditActive(boolean active, boolean canAccept) {
            graphics.setDisable(active && !canAccept);
        }

    }

    class InputNode<T> extends XNode<Input<T>,T,HBox> {

        @SuppressWarnings("unchecked")
        InputNode(Input<T> input_) {
            super(input_);

            graphics = new HBox(8, i,t);
            graphics.setMaxSize(80,120);
            graphics.setAlignment(Pos.CENTER_LEFT);
            graphics.getProperties().put(XNODE_KEY, this);
            graphics.setMouseTransparent(false);
            i.styleclass(INODE_STYLECLASS);

            // drag&drop
            installDrag(
                i, null, "",
                e -> true,
                consumer(e -> {
                    if (AppDragKt.contains(e.getDragboard(), Df.WIDGET_OUTPUT)) {
                        input.bind((Output) AppDragKt.get(e.getDragboard(), Df.WIDGET_OUTPUT));
                        drawGraph();
                    } else {
                        Object o = AppDragKt.getAny(e.getDragboard());
                        Class<?> c = o==null ? Void.class : o.getClass();
                        if (input.type.isAssignableFrom(c)) {
                            input.setValueAny(o);
                        }
                    }
                })
            );
            i.addEventFilter(DRAG_ENTERED, e -> i.pseudoClassStateChanged(XNODE_DRAGOVER, true));
            i.addEventFilter(DRAG_EXITED, e -> i.pseudoClassStateChanged(XNODE_DRAGOVER, false));

            t.setText(xPutToStr(input));
        }

    }

    class OutputNode<T> extends XNode<Output<T>,T,HBox> {

        OutputNode(Output<T> output_) {
            super(output_);

            graphics = new HBox(8, t,i);
            graphics.setMaxSize(80,120);
            graphics.setAlignment(Pos.CENTER_RIGHT);
            graphics.getProperties().put(XNODE_KEY, this);
            graphics.setMouseTransparent(false);
            i.styleclass(ONODE_STYLECLASS);

            // drag&drop
            i.addEventFilter(DRAG_DETECTED,e -> {
                if (selected) AppDragKt.set(i.startDragAndDrop(TransferMode.LINK), Df.WIDGET_OUTPUT, output);
                else editBegin(this);
                e.consume();
            });
        }

    }

    class InOutputNode<T> extends XNode<InOutput<T>,T,VBox> {

        @SuppressWarnings("unchecked")
        InOutputNode(InOutput<T> inoutput_) {
            super(inoutput_);

            graphics = new VBox(8, i,t);
            graphics.setMaxSize(80,120);
            graphics.setAlignment(Pos.CENTER_LEFT);
            graphics.getProperties().put(XNODE_KEY, this);
            graphics.setMouseTransparent(false);
            i.styleclass(IONODE_STYLECLASS);

            // drag&drop
            installDrag(
                i, null, "",
                e -> AppDragKt.contains(e.getDragboard(), Df.WIDGET_OUTPUT),
                consumer(e -> {
                    Output o = AppDragKt.get(e.getDragboard(), Df.WIDGET_OUTPUT);
                    if (o!=output) {
                        input.bind(o);
                        drawGraph();
                    }
                })
            );
            i.addEventFilter(DRAG_ENTERED, e -> i.pseudoClassStateChanged(XNODE_DRAGOVER, true));
            i.addEventFilter(DRAG_EXITED, e -> i.pseudoClassStateChanged(XNODE_DRAGOVER, false));
            i.addEventFilter(DRAG_DETECTED, e -> {
                if (selected) AppDragKt.set(i.startDragAndDrop(TransferMode.LINK), Df.WIDGET_OUTPUT, output);
                else editBegin(this);
                e.consume();
            });
        }

    }

    class IOLine<T> extends Path {

        Output<T> output;
        Input<T> input;

        public IOLine(Input<T> i, Output<T> o) {
            input = i;
            output = o;

            IOLayer.this.getChildren().add(this);
            getStyleClass().add(IOLINE_STYLECLASS);
            IOLine.this.setMouseTransparent(false);
            IOLine.this.setPickOnBounds(false);

            setOnMouseClicked(e -> {
                if (e.getButton()==SECONDARY) i.unbind(o);
                e.consume();
            });
            setOnDragDetected(e -> {
                editBegin(outputnodes.get(output));
                e.consume();
            });
        }

        void onEditActive(boolean active) {
            setDisable(active);
        }

        public void lay(double startx, double starty, double tox, double toy) {
            getElements().clear();
            getElements().add(new MoveTo(startx, starty));

            double h = IOLayer.this.getHeight();
            double dx = tox-startx;
            double dy = toy-starty;
            if (dx>0) {
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
        private void layTo(double startx, double starty, double tox, double toy, double h) {
            double dx = tox-startx;
            double dy = toy-starty;
            if (dx==0 || dy==0) {
                getElements().add(new LineTo(tox,toy));
                if (not_finished) {
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

        public void send() {
            // TODO: figure out speed = f(length) or speed = constant
            // Do not know how to obtain path length anyway...
            // naive attempt below
            // double length = sqrt(getWidth()*getWidth()+getHeight()*getHeight());
            Circle n = new Circle(3);
                   n.getStyleClass().add(IOLINE_RUNNER_STYLECLASS);
            IOLayer.this.getChildren().add(n);
            PathTransition a = new PathTransition(millis(2000), this, n);
            a.setRate(-1);
            a.setOnFinished(e -> IOLayer.this.getChildren().remove(n));
            a.playFrom(a.getDuration());
        }

    }

    class EditIOLine<T> extends IOLine<T> {

        final XNode<?,T,?> node;

        public EditIOLine(XNode<?,T,?> n) {
            super(n.input,n.output);
            node = n;

            EventHandler<MouseEvent> editDrawer = this::layToMouse;
            EventHandler<MouseEvent> editCanceler = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    editEnd();
                    IOLayer.this.removeEventFilter(MOUSE_CLICKED, this);
                    IOLayer.this.removeEventFilter(MouseEvent.ANY, editDrawer);
                }
            };
            IOLayer.this.addEventFilter(MouseEvent.ANY, editDrawer);
            IOLayer.this.addEventFilter(MOUSE_RELEASED, editCanceler);
        }

        public void layToMouse(MouseEvent e) {
            editMove(e);
        }

    }

}