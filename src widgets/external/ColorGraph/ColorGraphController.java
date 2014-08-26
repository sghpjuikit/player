/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ColorGraph;

import AudioPlayer.Player;
import AudioPlayer.tagging.Metadata;
import Layout.Widgets.FXMLController;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;

/**
 *
 * @author Plutonium_
 */
public class ColorGraphController extends FXMLController {
    
    @FXML AnchorPane root;
    
    private final Map<Color,Metadata> data = new HashMap();
    private final Map<Color,Node> nodes = new HashMap();
    Subscription dataMonitor;
//    EventStream<Number> s = EventStreams.merge(
//            EventStreams.valuesOf(root.widthProperty()),
//            EventStreams.valuesOf(root.heightProperty())
//    ).reduceSuccessions((a,b)->a, Duration.ofMillis(20)).subscribe(n->data.forEach(this::place));
//    public final Accessor<ReadMode>

    @Override
    public void init() {
        dataMonitor = Player.librarySelectedItemsES.subscribe(this::dataChanged);
        root.setCursor(Cursor.CROSSHAIR);
        
        EventStreams.merge(EventStreams.valuesOf(root.widthProperty()),
                           EventStreams.valuesOf(root.heightProperty()))
                .reduceSuccessions((a,b)->a, Duration.ofMillis(200))
                .subscribe(n->data.forEach(this::place));
        
//        root.heightProperty().addListener(a->data.forEach(this::place));
//        root.widthProperty().addListener(a->data.forEach(this::place));
    }

    @Override
    public void refresh() {
        data.forEach(this::place);
    }
    
    
    private void dataChanged(List<Metadata> source) {
        source.forEach(m -> {
            data.put(m.getColor(), m);
        });
        data.forEach(this::place);
        
        
        double W = root.getWidth();
        double H = root.getHeight();
        for(int j=0; j<=50; j++){
            Shape circle = null;
            for(int i=0; i<=25; i++) {
                circle = new Circle(i*W*0.04, j*H*0.04, 5, Color.hsb(i*0.04*360, 1, j*0.08));
                circle.setOpacity(0.2);
                root.getChildren().add(circle);
            }
            for(int i=0; i<=25; i++) {
                circle = new Circle(i*W*0.04, j*H*0.04+H/2, 5, Color.hsb(i*0.04*360, 1-j*0.08, 1));
                circle.setOpacity(0.2);
                root.getChildren().add(circle);
            }

        }
    }
    
    private void place(Color c, Metadata m) {
        double W = root.getWidth();
        double H = root.getHeight();
        double x = c.getHue()/360 * W;
        double brightness = c.getBrightness();
        double y = brightness * H;
        
        Node node = null;
        if (nodes.containsKey(c)) {
            node = nodes.get(c);
        } else {
            node = new Circle(x, y, 5, c);
            nodes.put(c, node);
            root.getChildren().add(node);
        }
        node.setLayoutX(x);
        node.setLayoutY(y);
    }
}
