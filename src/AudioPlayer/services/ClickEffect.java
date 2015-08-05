
package AudioPlayer.services;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.CacheHint;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import AudioPlayer.services.Service.ServiceBase;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import gui.objects.Window.stage.Window;
import jdk.nashorn.internal.ir.annotations.Immutable;
import main.App;
import util.access.Accessor;

import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static util.Util.setAnchors;

/**
 * 
 * @author uranium
 */
@Immutable
@IsConfigurable
public class ClickEffect extends ServiceBase {
    
    // configuration
    @IsConfig(name = "Show click effect", info = "Show effect on click.")
    public final Accessor<Boolean> show_clickEffect = new Accessor<>(true,this::applyC);
    @IsConfig(name = "Click effect duration", info = "Duration of the click effect in milliseconds.")
    public final Accessor<Double> DURATION = new Accessor<>(350d,this::apply);
    @IsConfig(name = "Click effect min", info = "Starting scale value of cursor click effect animation.")
    public final Accessor<Double> MIN_SCALE = new Accessor<>(0.2d,this::apply);
    @IsConfig(name = "Click effect max", info = "Ending scale value of cursor click effect animation.")
    public final Accessor<Double> MAX_SCALE = new Accessor<>(0.7d,this::apply);
    @IsConfig(name="Click effect delay", info = "Delay of the click effect in milliseconds.")
    public final Accessor<Double> DELAY = new Accessor<>(0d,this::apply);
    @IsConfig(name="Blend Mode", info = "Blending mode for the effect.")
    public final Accessor<BlendMode> blend_mode = new Accessor<>(BlendMode.SRC_OVER,this::apply);
    
    private void applyC() {
        if(show_clickEffect.get()) Window.windows.forEach(w -> w.getStage().getScene().getRoot().addEventFilter(MOUSE_PRESSED, clickHandler));
        else Window.windows.forEach(w -> w.getStage().getScene().getRoot().removeEventFilter(MOUSE_PRESSED, clickHandler));
    }
    
    private void apply() {
        if(App.isInitialized())
        pool.forEach(Effect::apply);
    }
    
    
    // pooling
    private final List<Effect> pool = new ArrayList();
    
    // creating
    public Effect create() {
        if (pool.isEmpty())
            return new Effect();
        else {
            Effect c = pool.get(0);
            pool.remove(0);
            return c;
        }
    }
    
    /**
     * Run at specific coordinates. The graphics of the effect is centered - [0,0]
     * is at its center
     * system.
     * @param x
     * @param y 
     */
    public void run(double x, double y) {
        create().play(x, y);
    }
    
    public void run(Point2D xy) {
        run(xy.getX(), xy.getY());
    }
    
    // handlers to display the effect, set to window's root
    private final EventHandler<MouseEvent> clickHandler = e -> run(e.getSceneX(), e.getSceneY());
    
    
/******************************************************************************/
    
//    void moveTo(MouseEvent e) {
//        lastx = (int) e.getSceneX();
//        lasty = (int) e.getSceneY();
//        if(!t.isRunning() && e1.root.getLayoutY()!=lasty) t.restart();
//    }
//    void move() {
//        e1.root.relocate(50, e1.root.getLayoutY()+(lasty-e1.root.getLayoutY())/7 - e1.root.getBoundsInParent().getHeight()/2);
//        e2.root.relocate(e2.root.getLayoutX()+(lastx-e2.root.getLayoutX())/7 - e2.root.getBoundsInParent().getWidth()/2,50);
//        if(lasty==(int)e1.root.getLayoutY()) {
//            t.stop();System.out.println("stoping");
//        }
//    }
//        
//    Effect e1,e2;
//    int lastx=0,lasty=0;
//    util.async.executor.FxTimer t = new FxTimer(20,-1, this::move);
    

    AnchorPane screen;
    boolean isRunning = false;

    public ClickEffect() {
        super(false);
    }
    
    @Override
    public void start() {
        isRunning = true;
        
        screen = new AnchorPane();
        screen.setMouseTransparent(true);
        screen.setStyle("-fx-background-color: null;");
        screen.setPickOnBounds(false);

        AnchorPane p = (AnchorPane) App.getWindow().getStage().getScene().getRoot();
        p.getChildren().add(screen);
        setAnchors(screen,0);
    
    
//        e1 = new Effect();
//        e1.apply();
//        e1.root.setVisible(true);
//        e2 = new Effect();
//        e2.apply();
//        e2.root.setVisible(true);
//
//        Window.windows.forEach(w -> w.getStage().getScene().getRoot().addEventFilter(MOUSE_MOVED, this::moveTo));
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void stop() {
        isRunning = false;
        
        AnchorPane p = (AnchorPane) App.getWindow().getStage().getScene().getRoot();
        p.getChildren().remove(screen);
        screen = null;
    }
    
    
    public class Effect {
        
        private final Circle root = new Circle();
        private final FadeTransition fade = new FadeTransition();
        private final ScaleTransition scale = new ScaleTransition();
        private final ParallelTransition anim = new ParallelTransition(root,fade,scale);
        private double scaleB = 1;

        private Effect() {
            root.setRadius(15);
            root.setFill(null);
            root.setEffect(new GaussianBlur(5.5));
            root.setStroke(Color.AQUA);
            root.setStrokeWidth(4.5);

            root.setVisible(false);
            root.setCache(true);
            root.setCacheHint(CacheHint.SPEED);
            root.setMouseTransparent(true);
            anim.setOnFinished( e -> pool.add(this));

            screen.getChildren().add(root);

            apply();
        }
        
        public Effect setScale(double s) {
            scaleB = s;
            return this;
        }
        
        public void play(double X, double Y) {
            // center position on run
            root.setLayoutX(X);
            root.setLayoutY(Y);
            // run effect
            root.setVisible(true);
            anim.play();
        }

        public void apply() {
            root.setBlendMode(blend_mode.get());
            anim.setDelay(Duration.millis(DELAY.get()));

            fade.setDuration(Duration.millis(DURATION.get()));
            fade.setFromValue(0.6);
            fade.setToValue(0);

            scale.setDuration(Duration.millis(DURATION.get()));
            scale.setFromX(scaleB*MIN_SCALE.get());
            scale.setFromY(scaleB*MIN_SCALE.get());
            scale.setToX(scaleB*MAX_SCALE.get());
            scale.setToY(scaleB*MAX_SCALE.get());
        }
    }
}