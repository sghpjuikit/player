

package unused;

import Configuration.IsConfig;
import Layout.Widgets.ClassWidget;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.ClassController;
import static java.lang.Math.random;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.BoxBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;
import util.access.Accessor;

/**
 *
 * @author Plutonium_
 */
//@IsWidget
@Layout.Widgets.Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Visualisation",
    description = "Provides simple visualisation.",
    howto = "",
    notes = "",
    version = "0.2",
    year = "2014",
    group = Widget.Group.VISUALISATION
)
public class Visualisation extends ClassController {

    private final Rectangle all = new Rectangle(100, 100);
    private final Timeline animation = new Timeline();
    private static final double WIDTH = 500, HEIGHT = 500;
    
    @IsConfig(name="Rate", info="Speed of the animation.",min=0,max=8)
    public final Accessor<Double> rate = new Accessor<>(1d, animation::setRate);
    @IsConfig(name="Background color", info="Background color")
    public final Accessor<Color> bgr_color = new Accessor<>(Color.BLACK, all::setFill);
    @IsConfig(name="Blending mode", info="The blending mode influences how the content is displayed on the screen in ocntext with the background.")
    public final Accessor<BlendMode> blending_mode = new Accessor<>(BlendMode.SRC_OVER, this::setBlendMode);
    
    public Visualisation(ClassWidget widget) {
        super(widget);
        
        initialize();
        play();
    }
 
    private void initialize() {
        this.setPadding(new Insets(5));
        
        Group layer1 = new Group();
        for(int i=0; i<15;i++) {
            Circle circle = new Circle(200,Color.web("white",0.05f));
            circle.setStrokeType(StrokeType.OUTSIDE);
            circle.setStroke(Color.web("white",0.2f));
            circle.setStrokeWidth(4f);
            layer1.getChildren().add(circle);
        }
        // create second list of circles
        Group layer2 = new Group();
        for(int i=0; i<20;i++) {
            Circle circle = new Circle(70,Color.web("white",0.05f));
            circle.setStrokeType(StrokeType.OUTSIDE);
            circle.setStroke(Color.web("white",0.1f));
            circle.setStrokeWidth(2f);
            layer2.getChildren().add(circle);
        }
        // create third list of circles
        Group layer3 = new Group();
        for(int i=0; i<10;i++) {
            Circle circle = new Circle(150,Color.web("white",0.05f));
            circle.setStrokeType(StrokeType.OUTSIDE);
            circle.setStroke(Color.web("white",0.16f));
            circle.setStrokeWidth(4f);
            layer3.getChildren().add(circle);
        }
        // Set a blur effect on each layer
        layer1.setEffect(new BoxBlur(30,30,3));
        layer2.setEffect(new BoxBlur(2,2,2));
        layer3.setEffect(new BoxBlur(10,10,3));
        // create a rectangle size of window with colored gradient
        Rectangle colors = new Rectangle(WIDTH, HEIGHT,
                new LinearGradient(0f,1f,1f,0f,true, CycleMethod.NO_CYCLE, new Stop(0,Color.web("#f8bd55")),
                        new Stop(0.14f,Color.web("#c0fe56")),
                        new Stop(0.28f,Color.web("#5dfbc1")),
                        new Stop(0.43f,Color.web("#64c2f8")),
                        new Stop(0.57f,Color.web("#be4af7")),
                        new Stop(0.71f,Color.web("#ed5fc2")),
                        new Stop(0.85f,Color.web("#ef504c")),
                        new Stop(1,Color.web("#f2660f")))
        );
        colors.setBlendMode(BlendMode.OVERLAY);
        colors.setOpacity(0.7);
        colors.widthProperty().bind(widthProperty());
        colors.heightProperty().bind(heightProperty());
        // create main content
        all.widthProperty().bind(widthProperty());
        all.heightProperty().bind(heightProperty());
        Rectangle clip = new Rectangle();
                  clip.widthProperty().bind(widthProperty());
                  clip.heightProperty().bind(heightProperty());
                  clip.setSmooth(false);
        Group group = new Group(
                all,
                layer1, 
                layer2,
                layer3,
                colors
        );
        group.setClip(clip);
        this.getChildren().add(group);
        // create list of all circles
        List<Node> allCircles = new ArrayList<>();
                   allCircles.addAll(layer1.getChildren());
                   allCircles.addAll(layer2.getChildren());
                   allCircles.addAll(layer3.getChildren());
        // Create a animation to randomly move every circle in allCircles
        for(Node circle: allCircles) {
            animation.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, // set start position at 0s
                    new KeyValue(circle.translateXProperty(),random()*WIDTH),
                    new KeyValue(circle.translateYProperty(),random()*HEIGHT),
                    new KeyValue(circle.opacityProperty(),random())
                ),
                new KeyFrame(new Duration(random()*40000), // set end position at 40s
                    new KeyValue(circle.translateXProperty(),random()*WIDTH),
                    new KeyValue(circle.translateYProperty(),random()*HEIGHT),
                    new KeyValue(circle.opacityProperty(),random())
                )
            );
        }
        animation.setAutoReverse(true);
        animation.setCycleCount(Animation.INDEFINITE);
        
//        CanvasParticleAnimater animater = new CanvasParticleAnimater(800, 600, new BallParticleRenderer());
//        getChildren().add(animater.getCanvas());
        
    }
 
    public void stop() {
        animation.stop();
    }
 
    public void play() {
        animation.play();
    }

    @Override public void refresh() {
        rate.applyValue();
        bgr_color.applyValue();
        blending_mode.applyValue();
    }

}
