package functionViewer;

import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.chart.NumberAxis;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import sp.it.pl.gui.itemnode.ConfigField;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.ClassController;
import sp.it.pl.util.access.V;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.math.StrExF;

import static java.lang.Math.max;
import static javafx.scene.layout.Priority.ALWAYS;
import static sp.it.pl.layout.widget.Widget.Group.DEVELOPMENT;
import static sp.it.pl.util.graphics.Util.setAnchors;

@Widget.Info(
    author = "Martin Polakovic",
    name = "Function Viewer",
    description = "Plots functions",
    version = "0.8",
    year = "2015",
    group = DEVELOPMENT
)
public class FunctionViewer extends ClassController  {
    private final Axes axes = new Axes(400,300,  -1,1,0.2, -1,1,0.2);
    private final Plot plot = new Plot(-1,1, axes);
	private final V<StrExF> function = new V<>(StrExF.fromString("x").getOrThrow(), this::plot);

    public FunctionViewer() {
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);

        ConfigField<StrExF> c = ConfigField.create(Config.forProperty(StrExF.class, "Function", function));
        StackPane la = new StackPane(new HBox(5,c.createLabel(),c.getNode()));
        StackPane lb = new StackPane(plot);
        VBox l = new VBox(5,la,lb);
             l.setPadding(new Insets(20));
        VBox.setVgrow(lb, ALWAYS);
        this.getChildren().add(l);
        setAnchors(l, 0d);
    }

    public void plot(Function<Double,Double> ƒ) {
        plot.plot(ƒ);
    }

    @Override public void refresh() {
        plot.plot(function.get());
    }

    private static class Axes extends Pane {
        public final NumberAxis xAxis;
        public final NumberAxis yAxis;

        public Axes(int width, int height, double xMin, double xMax, double xBy, double yMin, double yMax, double yBy) {
            setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
            setPrefSize(width, height);
            setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);

            xAxis = new NumberAxis(xMin, xMax, xBy);
            xAxis.setSide(Side.BOTTOM);
            xAxis.setMinorTickVisible(false);
            xAxis.setPrefWidth(width);
            xAxis.setLayoutY(height / 2);

            yAxis = new NumberAxis(yMin, yMax, yBy);
            yAxis.setSide(Side.LEFT);
            yAxis.setMinorTickVisible(false);
            yAxis.setPrefHeight(height);
            yAxis.layoutXProperty().bind(
                Bindings.subtract(
                    (width / 2) + 1,
                    yAxis.widthProperty()
                )
            );

            getChildren().setAll(xAxis, yAxis);
        }
    }
    private static class Plot extends Pane {
        private final double xMin;
        private final double xMax;
        private final Axes axes;

        public Plot(double xMin, double xMax, Axes axe) {
            this.xMin = xMin;
            this.xMax = xMax;
            this.axes = axe;

            setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
            setPrefSize(axes.getPrefWidth(), axes.getPrefHeight());
            setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        }

        public void plot(Function<Double,Double> ƒ) {
            Path path = new Path();
            path.setStroke(Color.ORANGE);
            path.setStrokeWidth(2);
            path.setClip(new Rectangle(0, 0, axes.getPrefWidth(), axes.getPrefHeight()));
            double inc = (xMax - xMin)/max(1,getWidth()); // inc by 1 px in 2D

            PathElement pe = null;
            double x = xMin +inc;
            while (x < xMax) try {
                double y = ƒ.apply(x);
                pe = pe == null ? new MoveTo(mapX(x), mapY(y)) : new LineTo(mapX(x), mapY(y));
                path.getElements().add(pe);
                x += inc;
            } catch (ArithmeticException e) {
                // Function is not continuous in point x, just continue on
            }

            getChildren().setAll(axes, path);
        }

        private double mapX(double x) {
            double tx = axes.getPrefWidth() / 2;
            double sx = axes.getPrefWidth() /
               (axes.xAxis.getUpperBound() - axes.xAxis.getLowerBound());

            return x * sx + tx;
        }

        private double mapY(double y) {
            double ty = axes.getPrefHeight() / 2;
            double sy = axes.getPrefHeight() /
                (axes.yAxis.getUpperBound() - axes.yAxis.getLowerBound());

            return -y * sy + ty;
        }
    }
}