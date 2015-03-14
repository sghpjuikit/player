
package Layout.WidgetImpl;

import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import Configuration.IsConfig;
import Layout.Widgets.Controller;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import static util.functional.Util.isNotNULL;

/**
 *
 * @author Plutonium_
 */
@IsWidget
public final class Graphs extends AnchorPane implements Controller<Widget>  {
    
    ObservableList<Metadata> metadatas = FXCollections.observableArrayList();
    Task<List<Metadata>> reader;
     
    AnchorPane chart = new AnchorPane();

    
    public Graphs() {
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
        AnchorPane.setBottomAnchor(chart,0.0);
        AnchorPane.setTopAnchor(chart,0.0);
        AnchorPane.setRightAnchor(chart,0.0);
        AnchorPane.setLeftAnchor(chart,0.0);
        
        Button b = new Button("bgbg");
        b.setOnMouseClicked(e-> readData(PlaylistManager.selectedItemsES.getValue()));
        
        this.getChildren().add(b);
        
        metadatas.addListener((ListChangeListener.Change<? extends Metadata> arg0) -> populate());
        grid = new Grid();
        
        // change camera on resize
        chart.heightProperty().addListener(l->redraw());
        chart.widthProperty().addListener(l->redraw());
        
        // change camera on mouse
        chart.setOnScroll( e -> {
            double dx = e.getX() - chart.getWidth()/2;
            double dy = e.getY() - chart.getHeight()/2;
            
            els.forEach(el->{
//                el.n.setTranslateX(el.Z *(el.X+dx));
//                el.n.setTranslateY(el.Z *(el.Y+dy));
                el.n.setTranslateX(el.Z *dx);
                el.n.setTranslateY(el.Z *dy);
            });
            
            line.draw();
            grid.grid.setTranslateX(grid.Z * dx);
            grid.grid.setTranslateY(grid.Z * dy);
        });
    }
    
    private void readData(List<?extends Item> items) {
        // cancel previous reading if ongoing
        if (reader != null && reader.isRunning()) reader.cancel();
        
        // read new data
        reader = MetadataReader.readMetadata(items, (success,result) ->{
            if(success){metadatas.setAll(result);
            }
        });
    }

    // initialize data to draw, called on data change
    private void populate() {    
        // initialize chart min/max
        double min = 0;
        double maxx = metadatas.stream().filter(isNotNULL).mapToDouble(Metadata::getRatingPercent).max().orElse(1);
        double maxy = metadatas.stream().filter(isNotNULL).mapToInt(Metadata::getPlaylistIndex).max().orElse(1);
        double maxz = metadatas.stream().filter(isNotNULL).map(Metadata::getLength).mapToDouble(d->d.toMinutes()).max().orElse(0)+1;
        
        // initialize elements
        els.clear();
        metadatas.forEach(m-> {
            double x = m.getRatingPercent();
            double y = m.getPlaylistIndex()/maxy;
            double z = m.getLength().toMinutes()/maxz;
            new El(x, y, z, m); // handles everything
        });

        // initialize path points
        line.points.clear();
        line.points.addAll(els);
        
        // draw
        redraw();
    }
    
    // draw data on screen, called after data change and camera change
    private void redraw() {
        chart.heightProperty().addListener(l->els.forEach(El::draw));
        chart.widthProperty().addListener(l->els.forEach(El::draw));
        if (show_path) line.draw();
//        grid.draw();
    }
    
    
    private static final double minZ = 0.2;
    Grid grid;
    List<El> els = new ArrayList<>();
    Line line = new Line();
    
    private final class El {
        
        Shape n = new Circle(5, Color.AQUAMARINE);
        /** 0-1 */
        double X;
        /** 0-1 */
        double Y;
        /** 0-1 */  // + minZ so min != 0
        double Z;
        URI u;
        
        /**
         * 
         * @param x 0-1
         * @param y 0-1
         * @param z 0-1
         */
        El(double x, double y, double z, Metadata m) {
            if (x>1 || y>1 || z>1 || x<0 || y<0 || z<0) throw new IllegalArgumentException();
            u = m.getURI();
            X = x;
            Y = y;
            Z = z+minZ;
            
            chart.getChildren().add(n);
            els.add(this);
            
            n.setCache(true);
            n.setCacheHint(CacheHint.SPEED);
            
            n.setOpacity(0.7);
            double i = Math.abs(5*z);
            n.setEffect(new BoxBlur(i,i,1));
            n.setStroke(path_color);
            n.setStrokeType(StrokeType.INSIDE);
            n.setStrokeWidth(4);
            n.setFill(Color.rgb(180,190,220,0.35));
            
            draw();
        }
        
        void draw() {
            n.relocate(X*chart.getWidth()-2.5, Y*chart.getHeight()-2.5);
            n.setScaleX(Z);
            n.setScaleY(Z);
            n.setScaleY(Z);
            
            n.setTranslateX(Z*X);
            n.setTranslateY(Z*Y);
        }
        
    }
    private final class Line {
        Polyline line;
        List<El> points = new ArrayList<>();

        public Line() {
            line = new Polyline();
            line.setVisible(true);
            chart.getChildren().add(line);
        }
        
        void add(El e) {
            points.add(e);
        }
        
        void draw(){
            line.getPoints().clear();
            for(int i=0; i<points.size();i++) {
                line.getPoints().add(2*i, points.get(i).X*chart.getWidth()+points.get(i).n.getTranslateX());
                line.getPoints().add(2*i+1, points.get(i).Y*chart.getHeight()+points.get(i).n.getTranslateY());
            }
        }
    }
    private final class Grid {
        GridPane grid;
        double Z = 0.0+minZ;
        
        Grid() {
            grid = new GridPane();
            grid.setGridLinesVisible(true);
            grid.setOpacity(0.6);
            for (int i=0; i<10; i++) {
                ColumnConstraints c = new ColumnConstraints();
                                  c.setPercentWidth(10);
                grid.getColumnConstraints().add(c);
                RowConstraints r = new RowConstraints();
                               r.setPercentHeight(10);
                grid.getRowConstraints().add(r);
            }
            
            chart.getChildren().add(grid);
            AnchorPane.setBottomAnchor(grid, 0.0);
            AnchorPane.setLeftAnchor(grid, 0.0);
            AnchorPane.setRightAnchor(grid, 0.0);
            AnchorPane.setTopAnchor(grid, 0.0);
            
        }
        void draw() {
            grid.setScaleX(Z);
            grid.setScaleY(Z);
            grid.setScaleZ(Z);
        }
    }
    
    // element properties
    
    
    // path properties
    @IsConfig(name="Path show", info="Show the path connecting playing items.")
    public boolean show_path = true;
    @IsConfig(name="Path opacity", info="Opacity of the path connecting playing items.", min=0.0, max=1.0)
    public double path_opacity = 1;
    @IsConfig(name="Path blur intensity", info="Blur intensity of the path connecting playing items.", min=0.0, max=20.0)
    public double path_blur = 1;
    @IsConfig(name="Path shadow intensity", info="Shadow intensity of the path connecting playing items.", min=0.0, max=20.0)
    public double path_shadow = 1;
    @IsConfig(name="Path shadow color", info="Shadow color of the path connecting playing items.")
    public Color path_shadow_color = Color.DODGERBLUE;
    @IsConfig(name="Path color", info="Color of the path connecting playing items.")
    public Color path_color = Color.CADETBLUE;
    @IsConfig(name="Path width", info="Width of the path connecting playing items.", min=0.5, max=5.0)
    public double path_width = 1.5;
    @IsConfig(name="Path blend mode", info="Blend mode of the path connecting playing items.")
    public BlendMode path_blend_mode = BlendMode.SRC_OVER;
    @IsConfig(name="Path stroke type", info="Stroke type of the path connecting playing items.")
    public StrokeType path_stroke_type = StrokeType.INSIDE;
    @IsConfig(name="Path stroke mitter", info="Stroke mitter of the path connecting playing items.")
    public double path_stroke_mitter = 2;
    @IsConfig(name="Path stroke line join", info="Stroke line join of the path connecting playing items.")
    public StrokeLineJoin path_stroke_lineJoin = StrokeLineJoin.MITER;
    @IsConfig(name="Path line cap", info="Line cap of the path connecting playing items.")
    public StrokeLineCap path_stroke_lineCap = StrokeLineCap.BUTT;
    @IsConfig(name="Path dash offset", info="Dash offset of the path connecting playing items.")
    public double path_stroke_dashOffset = 2;
    
/******************************************************************************/
    
    private Widget widget;
    
    @Override public void refresh() {
        
        
        
        
        line.line.setVisible(show_path);
        line.line.setOpacity(path_opacity);
        line.line.setEffect(new BoxBlur(path_blur, path_blur, 1));
        line.line.setEffect(new DropShadow(path_shadow, path_shadow_color));
        line.line.setStroke(path_color);
        line.line.setStrokeWidth(path_width);
        line.line.setStrokeType(path_stroke_type);
        line.line.setStrokeMiterLimit(path_stroke_mitter);
        line.line.setStrokeLineJoin(path_stroke_lineJoin);
        line.line.setStrokeLineCap(path_stroke_lineCap);
        line.line.setStrokeDashOffset(path_stroke_dashOffset);
        line.line.setBlendMode(path_blend_mode);
        
    }

    @Override public void setWidget(Widget w) {
        widget = w;
    }

    @Override public Widget getWidget() {
        return widget;
    }
}


























//
//package GUI.Components;
//
//import AudioPlayer.playlist.Item;
//import AudioPlayer.playlist.PlaylistManager;
//import AudioPlayer.tagging.Metadata;
//import Layout.Controller;
//import Layout.Widget;
//import PseudoObjects.TaskRunner;
//import java.util.List;
//import java.util.stream.Collectors;
//import javafx.collections.FXCollections;
//import javafx.collections.ListChangeListener;
//import javafx.collections.ObservableList;
//import javafx.concurrent.Task;
//import javafx.scene.Group;
//import javafx.scene.Node;
//import javafx.scene.chart.NumberAxis;
//import javafx.scene.chart.ScatterChart;
//import javafx.scene.chart.XYChart;
//import javafx.scene.control.Button;
//import javafx.scene.effect.Reflection;
//import javafx.scene.layout.AnchorPane;
//
///**
// *
// * @author Plutonium_
// */
//public final class Graphs extends AnchorPane implements Controller {
//    
//    final NumberAxis xAxis = new NumberAxis(0, 10, 1);
//    final NumberAxis yAxis = new NumberAxis(0, 500, 100);  
//    final private ScatterChart chart = new ScatterChart(xAxis, yAxis);
//    
//    ObservableList<Metadata> metadatas = FXCollections.observableArrayList();
//    Task<List<Metadata>> loader;
//     
//    Group g1 = new Group();
//    Group g2 = new Group();
//    Group g3 = new Group();
//    Group g4 = new Group();
//
//    
//    public Graphs() {
//        initialize();
//    }
//    
//    private void initialize() {getChildren().addAll(g1,g2,g3,g4);
//        
//        chart.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
//        chart.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
//        chart.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
//        this.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
//        this.setMaxSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
//        this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
//        
//        this.getChildren().add(chart);
//        AnchorPane.setBottomAnchor(chart,0.0);
//        AnchorPane.setTopAnchor(chart,0.0);
//        AnchorPane.setRightAnchor(chart,0.0);
//        AnchorPane.setLeftAnchor(chart,0.0);
//        
//        Button b = new Button("bgbg");
//        b.setOnMouseClicked(e-> readData(PlaylistManager.getSelectedItems()));
//        
//        this.getChildren().add(b);
//        metadatas.addListener((ListChangeListener.Change<? extends Metadata> arg0) -> populate());
//        
//    }
//    
//    private void readData(List<?extends Item> items) {
//        // cancel previous reading if ongoing
//        if (loader != null && loader.isRunning())
//            loader.cancel();
//        
//        // read new data
//        loader = Metadata.readMetadata(items, new TaskRunner<List<Metadata>>() {
//            @Override public void success(List<Metadata> result) {
//                metadatas.addAll(result);
//            }
//            @Override public void failure() {
//            }
//        });
//    }
//    
//    private void populate() {
//        XYChart.Series series1 = new XYChart.Series();
//        XYChart.Series series2 = new XYChart.Series();
//        XYChart.Series series3 = new XYChart.Series();
//        XYChart.Series series4 = new XYChart.Series();
//        
//        double min = 0;
//        double max = metadatas.stream().mapToInt(Metadata::getPlaylistIndex).max().orElse(0);
//        
//        yAxis.setLowerBound(min);
//        yAxis.setUpperBound(max);
//        yAxis.setTickUnit((max-min)/10);
//        
//        metadatas.forEach(m-> {
//            series1.getData().add(new XYChart.Data(m.getRatingToStars(10), m.getPlaylistIndex()));
//            series2.getData().add(new XYChart.Data(m.getRatingToStars(9), m.getPlaylistIndex()+1));
//            series3.getData().add(new XYChart.Data(m.getRatingToStars(8), m.getPlaylistIndex()+2));
//            series4.getData().add(new XYChart.Data(m.getRatingToStars(7), m.getPlaylistIndex()+3));
//        });
// 
//        chart.getData().setAll(series1,series2,series3,series4);
//        
////        if (PlaylistManager.isItemPlaying()){
////            XYChart.Data pl = (XYChart.Data)series1.getData().get(PlaylistManager.indexOfPlaying());
////            pl.getNode().setEffect(new Reflection());
////        } 
//        
//        chart.setOnScroll( e -> {
//            double byX = e.getX() - chart.getWidth()/2;
//            double byY = e.getY() - chart.getHeight()/2;
//            
//            List<Node> nodes1 = (List) series1.getData().stream().mapB(n->((XYChart.Data)n).getNode()).collect(Collectors.toList());
//            nodes1.forEach(n->{
//                n.setTranslateX(byX/10);
//                n.setTranslateY(byY/10);
//            });
//            
//            List<Node> nodes2 = (List) series2.getData().stream().mapB(n->((XYChart.Data)n).getNode()).collect(Collectors.toList());
//            nodes2.forEach(n->{
//                n.setTranslateX(byX/15);
//                n.setTranslateY(byY/15);
//            });
//            
//            List<Node> nodes3 = (List) series3.getData().stream().mapB(n->((XYChart.Data)n).getNode()).collect(Collectors.toList());
//            nodes3.forEach(n->{
//                n.setTranslateX(byX/12);
//                n.setTranslateY(byY/15);
//            });
//            
//            List<Node> nodes4 = (List) series4.getData().stream().mapB(n->((XYChart.Data)n).getNode()).collect(Collectors.toList());
//            nodes4.forEach(n->{
//                n.setTranslateX(byX/18);
//                n.setTranslateY(byY/18);
//            });
//        });
//        
//    }
//
//    
//    
//    private Widget widget;
//    
//    @Override public void refresh() {
//        chart.setLegendVisible(false);
//        chart.setVerticalGridLinesVisible(false);
//        chart.setHorizontalGridLinesVisible(false);
//    }
//
//    @Override public void setWidget(Widget w) {
//        widget = w;
//    }
//
//    @Override public Widget getWidget() {
//        return widget;
//    }
//}
