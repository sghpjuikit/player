
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
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import gui.objects.Window.stage.Window;
import util.access.V;

import static javafx.application.Platform.runLater;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static main.App.APP;
import static util.graphics.Util.setAnchors;

/**
 *
 * @author uranium
 */
@IsConfigurable
public class ClickEffect extends ServiceBase {

    // configuration
    @IsConfig(name = "Show click effect", info = "Show effect on click.")
    public final V<Boolean> show_clickEffect = new V<>(true,this::applyC);
    @IsConfig(name = "Click effect duration", info = "Duration of the click effect in milliseconds.")
    public final V<Double> DURATION = new V<>(350d,this::apply);
    @IsConfig(name = "Click effect min", info = "Starting scale value of cursor click effect animation.")
    public final V<Double> MIN_SCALE = new V<>(0.2d,this::apply);
    @IsConfig(name = "Click effect max", info = "Ending scale value of cursor click effect animation.")
    public final V<Double> MAX_SCALE = new V<>(0.7d,this::apply);
    @IsConfig(name="Click effect delay", info = "Delay of the click effect in milliseconds.")
    public final V<Double> DELAY = new V<>(0d,this::apply);
    @IsConfig(name="Blend Mode", info = "Blending mode for the effect.")
    public final V<BlendMode> blend_mode = new V<>(BlendMode.SRC_OVER,this::apply);

    private void applyC() {
        List<Window> ws = APP.windowManager.windows;
        if(show_clickEffect.get()) ws.forEach(w -> w.getStage().getScene().getRoot().addEventFilter(MOUSE_PRESSED, clickHandler));
        else ws.forEach(w -> w.getStage().getScene().getRoot().removeEventFilter(MOUSE_PRESSED, clickHandler));
    }

    private void apply() {
        if(APP.initialized)
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
        if(!isRunning) return; // create() must not execute when not running since screen==null
        create().play(x, y);
    }

    public void run(Point2D xy) {
        run(xy.getX(), xy.getY());
    }

    // handlers to display the effect, set to window's root
    private final EventHandler<MouseEvent> clickHandler = e -> run(e.getSceneX(), e.getSceneY());


/******************************************************************************/

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

        runLater(() -> {
            AnchorPane p = (AnchorPane) APP.window.getStage().getScene().getRoot();
            if(p!=null) {
                p.getChildren().add(screen);
                setAnchors(screen,0d);
            }
        });
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void stop() {
        isRunning = false;

        runLater(() -> {
            AnchorPane p = (AnchorPane) APP.window.getStage().getScene().getRoot();
            if(p!=null) {
                p.getChildren().remove(screen);
                screen = null;
            }
        });
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