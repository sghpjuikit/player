
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Layout.WidgetImpl;

import Layout.Widgets.Controller;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.AnchorPane;
import util.Util;

@IsWidget
@Layout.Widgets.Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "FunctionPlotter",
    description = "Plots functions",
    howto = "",
    notes = "",
    version = "0.6",
    year = "2014",
    group = Widget.Group.VISUALISATION
)
public class FunctionPlotter extends AnchorPane implements Controller<Widget>  {
    
    ScatterChart<Number,Number> chart = new ScatterChart<>(new NumberAxis(0, 1, 0.1),new NumberAxis(0, 1, 0.1));
    
    public FunctionPlotter() {
        initialize();
    }
    
    private void initialize() {
        chart.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        chart.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        chart.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        
        
        this.getChildren().add(chart);
        Util.setAnchors(chart, 0);
        
//        Series<Number,Number> s = new XYChart.Series<>();
//        for(double x = 0; x<=1; x+=0.001)
//            s.getData().add(new XYChart.Data<>(x, Anim.Interpolators.isAroundMin1(1, 0.4,0.5,0.7).apply(x)));
//        chart.getData().add(s);
//        
        Series<Number,Number> s2 = new XYChart.Series<>();
        for(double x = 0; x<=1; x+=0.001)
            s2.getData().add(new XYChart.Data<>(x, x*x));
        chart.getData().add(s2);
        
        Series<Number,Number> s3 = new XYChart.Series<>();
        for(double x = 0; x<=1; x+=0.001)
            s3.getData().add(new XYChart.Data<>(x, x));
        chart.getData().add(s3);
    }
    
    
    
    private Widget widget;
    
    @Override public void refresh() {        
    }

    @Override
    public void close() {}

    @Override public void setWidget(Widget w) {
        widget = w;
    }

    @Override public Widget getWidget() {
        return widget;
    }
    
}
