
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.widget.impl;

import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.chart.NumberAxis;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import Configuration.Config;
import Layout.widget.IsWidget;
import Layout.widget.Widget;
import Layout.widget.controller.ClassController;
import gui.itemnode.ConfigField;
import util.access.Ѵ;
import util.functional.StrExƑ;

import static Layout.widget.Widget.Group.DEVELOPMENT;
import static java.lang.Math.max;
import static javafx.scene.layout.Priority.ALWAYS;
import static util.graphics.Util.setAnchors;

@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "FunctionPlotter",
    description = "Plots functions",
    howto = "",
    notes = "",
    version = "0.7",
    year = "2015",
    group = DEVELOPMENT
)
public class FunctionPlotter extends ClassController  {
    private final Axes axes = new Axes(400,300,  -1,1,0.2, -1,1,0.2);
    private final Plot plot = new Plot(-1,1, axes);
    
    public FunctionPlotter() {
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        
        Ѵ<StrExƑ> sdf = new Ѵ<>(new StrExƑ("x"),this::plot);
        ConfigField c = ConfigField.create(Config.forProperty("Function", sdf));

        
        StackPane la = new StackPane(new HBox(5,c.getLabel(),c.getNode()));
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
    
    
    private class Axes extends Pane {
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
    private class Plot extends Pane {
        private double xmin;
        private double xmax;
        private Axes a;
        
        public Plot(double xMin, double xMax, Axes axes) {
            xmin = xMin;
            xmax = xMax;
            a = axes;
            
            setMinSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
            setPrefSize(a.getPrefWidth(), a.getPrefHeight());
            setMaxSize(Pane.USE_PREF_SIZE, Pane.USE_PREF_SIZE);
        }
        
        public void plot(Function<Double,Double> ƒ) {
            Path path = new Path();
            path.setStroke(Color.ORANGE);
            path.setStrokeWidth(2);
            path.setClip(new Rectangle(0, 0,a.getPrefWidth(),a.getPrefHeight()));
            double inc = (xmax-xmin)/max(1,getWidth()); // inc by 1 px

            PathElement pe=null;
            double x = xmin+inc;
            while (x < xmax) {
                try {
                    double y = ƒ.apply(x);
                    pe = pe==null ? new MoveTo(mapX(x),mapY(y)) : new LineTo(mapX(x),mapY(y));
                    path.getElements().add(pe);
                    x += inc;
                } catch(ArithmeticException e){}
            }

            getChildren().setAll(a, path);
        }

        private double mapX(double x) {
            double tx = a.getPrefWidth() / 2;
            double sx = a.getPrefWidth() / 
               (a.getXAxis().getUpperBound() - a.getXAxis().getLowerBound());

            return x * sx + tx;
        }

        private double mapY(double y) {
            double ty = a.getPrefHeight() / 2;
            double sy = a.getPrefHeight() / 
                (a.getYAxis().getUpperBound() - a.getYAxis().getLowerBound());

            return -y * sy + ty;
        }
    }
}