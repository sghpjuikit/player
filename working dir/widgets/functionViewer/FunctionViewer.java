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

import gui.itemnode.ConfigField;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.access.V;
import util.conf.Config;
import util.functional.StrExF;

import static java.lang.Math.max;
import static javafx.scene.layout.Priority.ALWAYS;
import static layout.widget.Widget.Group.DEVELOPMENT;
import static util.graphics.Util.setAnchors;

@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Function Viewer",
    description = "Plots functions",
    howto = "",
    notes = "",
    version = "0.7",
    year = "2015",
    group = DEVELOPMENT
)
public class FunctionViewer extends ClassController  {
    private final Axes axes = new Axes(400,300,  -1,1,0.2, -1,1,0.2);
    private final Plot plot = new Plot(-1,1, axes);

    public FunctionViewer() {
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);

        V<StrExF> sdf = new V<>(new StrExF("x"),this::plot);
        ConfigField<StrExF> c = ConfigField.create(Config.forProperty(StrExF.class, "Function", sdf));


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
        plot.plot(x->x);
    }


    private static class Axes extends Pane {
        private NumberAxis xAxis;
        private NumberAxis yAxis;

        public Axes(int width, int height, double xLow, double xHi, double xTickUnit, double yLow, double yHi, double yTickUnit) {
            setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
            setPrefSize(width, height);
            setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);

            xAxis = new NumberAxis(xLow, xHi, xTickUnit);
            xAxis.setSide(Side.BOTTOM);
            xAxis.setMinorTickVisible(false);
            xAxis.setPrefWidth(width);
            xAxis.setLayoutY(height / 2);

            yAxis = new NumberAxis(yLow, yHi, yTickUnit);
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

        public NumberAxis getXAxis() {
            return xAxis;
        }

        public NumberAxis getYAxis() {
            return yAxis;
        }
    }
    private static class Plot extends Pane {
        private double xMin;
        private double xMax;
        private Axes axe;

        public Plot(double xMin, double xMax, Axes axes) {
            this.xMin = xMin;
            this.xMax = xMax;
            axe = axes;

            setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
            setPrefSize(axe.getPrefWidth(), axe.getPrefHeight());
            setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        }

        public void plot(Function<Double,Double> ƒ) {
            Path path = new Path();
            path.setStroke(Color.ORANGE);
            path.setStrokeWidth(2);
            path.setClip(new Rectangle(0, 0, axe.getPrefWidth(), axe.getPrefHeight()));
            double inc = (xMax - xMin)/max(1,getWidth()); // inc by 1 px in 2D

            PathElement pe = null;
            double x = xMin +inc;
            while (x < xMax) try {
                double y = ƒ.apply(x);
                pe = pe == null ? new MoveTo(mapX(x), mapY(y)) : new LineTo(mapX(x), mapY(y));
                path.getElements().add(pe);
                x += inc;
            } catch (ArithmeticException e) {
                // When function is not continuous in point x
                continue;
            }

            getChildren().setAll(axe, path);
        }

        private double mapX(double x) {
            double tx = axe.getPrefWidth() / 2;
            double sx = axe.getPrefWidth() /
               (axe.getXAxis().getUpperBound() - axe.getXAxis().getLowerBound());

            return x * sx + tx;
        }

        private double mapY(double y) {
            double ty = axe.getPrefHeight() / 2;
            double sy = axe.getPrefHeight() /
                (axe.getYAxis().getUpperBound() - axe.getYAxis().getLowerBound());

            return -y * sy + ty;
        }
    }
}