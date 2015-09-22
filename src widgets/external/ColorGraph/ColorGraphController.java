/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ColorGraph;

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

import AudioPlayer.Player;
import AudioPlayer.tagging.Metadata;
import Layout.widget.controller.FXMLController;
import util.graphics.drag.DragUtil;

import static java.time.Duration.ofMillis;
import static org.reactfx.EventStreams.valuesOf;
import static util.functional.Util.list;

/**
 *
 * @author Plutonium_
 */
public class ColorGraphController extends FXMLController {

    @FXML AnchorPane root;
//    Canvas c = new Canvas();

    private final Map<Color,Metadata> data = new HashMap();
    private final Map<Color,Node> nodes = new HashMap();
    Subscription dataMonitor;
//    EventStream<Number> s = EventStreams.merge(
//            EventStreams.valuesOf(root.widthProperty()),
//            EventStreams.valuesOf(root.heightProperty())
//    ).reduceSuccessions((a,b)->a, Duration.ofMillis(20)).subscribe(n->data.forEach(this::place));
//    public final Accessor<ReadMode>//    EventStream<Number> s = EventStreams.merge(
//            EventStreams.valuesOf(root.widthProperty()),
//            EventStreams.valuesOf(root.heightProperty())
//    ).reduceSuccessions((a,b)->a, Duration.ofMillis(20)).subscribe(n->data.forEach(this::place));
//    public final Accessor<ReadMode>


    @Override
    public void init() {
//        dataMonitor = Player.librarySelectedItemsES.subscribe(this::dataChanged);
        root.setOnDragOver(DragUtil.audioDragAccepthandler);
        root.setOnDragDropped(e -> dataChanged((List)DragUtil.getItemsList(e))); // WTF, !safe !!
        root.setCursor(Cursor.CROSSHAIR);

        EventStreams.merge(valuesOf(root.widthProperty()),
                           valuesOf(root.heightProperty()))
                    .reduceSuccessions((a,b)->a, ofMillis(200))
                    .subscribe(n->data.forEach(this::place));

//        root.heightProperty().addListener(a->data.forEach(this::place));
//        root.widthProperty().addListener(a->data.forEach(this::place));

//        Stop[] s1 = new Stop[] { new Stop(0,RED), new Stop(1/6d,YELLOW), new Stop(1/3d,GREEN), new Stop(2/3d,BLUE), new Stop(1,RED)};
//        LinearGradient f1 = new LinearGradient(0, 0, 1, 0, true, NO_CYCLE, s1);
//        Stop[] s2 = new Stop[] { new Stop(0,BLACK), new Stop(0.5,TRANSPARENT), new Stop(1,WHITE)};
//        LinearGradient f2 = new LinearGradient(0, 0, 0, 1, true, NO_CYCLE, s2);
//        root.setBackground(new Background(new BackgroundFill(f1, CornerRadii.EMPTY, Insets.EMPTY),
//                           new BackgroundFill(f2, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    @Override
    public void refresh() {
        Metadata m = Player.librarySelected.o.getValue();
        if(m!=null) dataChanged(list(m));
    }

    @Override
    public void onClose() {
//        dataMonitor.unsubscribe();
    }


    private void dataChanged(List<Metadata> source) {
//        c.setWidth(max(0,root.getWidth()-10));
//        c.setHeight(max(0,root.getHeight()-10));




        data.clear();
        root.getChildren().clear();
        source.forEach(m -> {
            Color c = m.getColor();
            if(c!=null) data.put(m.getColor(), m);
        });
        data.forEach(this::place);


        double W = root.getWidth();
        double H = root.getHeight();
        for(int j=0; j<=50; j++){
            Shape circle = null;
            for(int i=0; i<=25; i++) {
                circle = new Circle(i*W*0.04, j*H*0.01, 5, Color.hsb(i*0.04*360, 1, j*0.02));
//                circle.setOpacity(0.2);
                root.getChildren().add(circle);
            }
            for(int i=0; i<=25; i++) {
                circle = new Circle(i*W*0.04, j*H*0.01+H/2, 5, Color.hsb(i*0.04*360, 1-j*0.02, 1));
//                circle.setOpacity(0.2);
                root.getChildren().add(circle);
            }
        }

        root.getChildren().addAll(nodes.values());
    }

    private void place(Color c, Metadata m) {
        double W = root.getWidth();System.out.println("W " + W);
        double H = root.getHeight();System.out.println("H " + H);
        double x = c.getHue()/360d * W;System.out.println("x " + x + " " + c.getHue());
        double brightness = c.getBrightness();System.out.println("b " + brightness);
        double y = brightness * H;System.out.println("y " + y);

        Node node = null;
//        if (nodes.containsKey(c)) {
//            node = nodes.get(c);
//        } else {
            node = new Circle(3,3,3,c);
//            node = new Circle(3,3,3,Color.BLACK);
//            node.setEffect(new Blend(DIFFERENCE));
            nodes.put(c, node);
//            root.getChildren().add(node);
//        }
        node.setLayoutX(x);
        node.setLayoutY(y);
    }
}
///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package ColorGraph;
//
//import AudioPlayer.Player;
//import AudioPlayer.tagging.Metadata;
//import Layout.Widgets.FXMLController;
//import static java.time.Duration.ofMillis;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import javafx.fxml.FXML;
//import javafx.scene.Cursor;
//import javafx.scene.Node;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.paint.Color;
//import javafx.scene.shape.Circle;
//import javafx.scene.shape.Shape;
//import org.reactfx.EventStreams;
//import static org.reactfx.EventStreams.valuesOf;
//import org.reactfx.Subscription;
//import util.graphics.drag.DragUtil;
//
///**
// *
// * @author Plutonium_
// */
//public class ColorGraphController extends FXMLController {
//
//    @FXML AnchorPane root;
//
//    private final Map<Color,Metadata> data = new HashMap();
//    private final Map<Color,Node> nodes = new HashMap();
//    Subscription dataMonitor;
////    EventStream<Number> s = EventStreams.merge(
////            EventStreams.valuesOf(root.widthProperty()),
////            EventStreams.valuesOf(root.heightProperty())
////    ).reduceSuccessions((a,b)->a, Duration.ofMillis(20)).subscribe(n->data.forEach(this::place));
////    public final Accessor<ReadMode>
//
//    @Override
//    public void init() {
////        dataMonitor = Player.librarySelectedItemsES.subscribe(this::dataChanged);
//        root.setOnDragOver(DragUtil.audioDragAccepthandler);
//        root.setOnDragDropped(e -> dataChanged((List)DragUtil.getItemsList()));
//        root.setCursor(Cursor.CROSSHAIR);
//
//        EventStreams.merge(valuesOf(root.widthProperty()),
//                           valuesOf(root.heightProperty()))
//                    .reduceSuccessions((a,b)->a, ofMillis(200))
//                    .subscribe(n->data.forEach(this::place));
//
////        root.heightProperty().addListener(a->data.forEach(this::place));
////        root.widthProperty().addListener(a->data.forEach(this::place));
//
////        Stop[] s1 = new Stop[] { new Stop(0,RED), new Stop(1/6d,YELLOW), new Stop(1/3d,GREEN), new Stop(2/3d,BLUE), new Stop(1,RED)};
////        LinearGradient f1 = new LinearGradient(0, 0, 1, 0, true, NO_CYCLE, s1);
////        Stop[] s2 = new Stop[] { new Stop(0,BLACK), new Stop(0.5,TRANSPARENT), new Stop(1,WHITE)};
////        LinearGradient f2 = new LinearGradient(0, 0, 0, 1, true, NO_CYCLE, s2);
////        root.setBackground(new Background(new BackgroundFill(f1, CornerRadii.EMPTY, Insets.EMPTY),
////                           new BackgroundFill(f2, CornerRadii.EMPTY, Insets.EMPTY)));
//    }
//
//    @Override
//    public void refresh() {
//        dataChanged(Player.librarySelectedItemsES.getValue());
//    }
//
//    @Override
//    public void onClose() {
////        dataMonitor.unsubscribe();
//    }
//
//
//    private void dataChanged(List<Metadata> source) {
//        data.clear();
//        root.getChildren().clear();
//        source.forEach(m -> {
//            Color c = m.getColor();
//            if(c!=null) data.put(m.getColor(), m);
//        });
//        data.forEach(this::place);
//
//
//        double W = root.getWidth();
//        double H = root.getHeight();
//        for(int j=0; j<=50; j++){
//            Shape circle = null;
//            for(int i=0; i<=25; i++) {
//                circle = new Circle(i*W*0.04, j*H*0.01, 5, Color.hsb(i*0.04*360, 1, j*0.02));
////                circle.setOpacity(0.2);
//                root.getChildren().add(circle);
//            }
//            for(int i=0; i<=25; i++) {
//                circle = new Circle(i*W*0.04, j*H*0.01+H/2, 5, Color.hsb(i*0.04*360, 1-j*0.02, 1));
////                circle.setOpacity(0.2);
//                root.getChildren().add(circle);
//            }
//        }
//
//        root.getChildren().addAll(nodes.values());
//    }
//
//    private void place(Color c, Metadata m) {
//        double W = root.getWidth();System.out.println("W " + W);
//        double H = root.getHeight();System.out.println("H " + H);
//        double x = c.getHue()/360d * W;System.out.println("x " + x + " " + c.getHue());
//        double brightness = c.getBrightness();System.out.println("b " + brightness);
//        double y = brightness * H;System.out.println("y " + y);
//
//        Node node = null;
////        if (nodes.containsKey(c)) {
////            node = nodes.get(c);
////        } else {
//            node = new Circle(3,3,3,c);
////            node = new Circle(3,3,3,Color.BLACK);
////            node.setEffect(new Blend(DIFFERENCE));
//            nodes.put(c, node);
////            root.getChildren().add(node);
////        }
//        node.setLayoutX(x);
//        node.setLayoutY(y);
//    }
//}
