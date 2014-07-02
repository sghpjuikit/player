
package GUI.objects;

import Configuration.AppliesConfig;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.ContextManager;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import jdk.nashorn.internal.ir.annotations.Immutable;
import utilities.Log;

/**
 * 
 * @author uranium
 */
@Immutable
@IsConfigurable
public class ClickEffect {
    
    // configuration
    @IsConfig(name = "Show click effect", info = "Show effect on click.")
    public static boolean clickEffect = true;
    @IsConfig(name = "Show trail effect", info = "Show cursor trail effect.")
    public static boolean trail_effect = true;
    @IsConfig(name = "Click effect duration", info = "Duration of the click effect in milliseconds.")
    public static double DURATION = 350;
    @IsConfig(name = "Click effect min", info = "Starting scale value of cursor click effect animation.")
    public static double MIN_SCALE = 0.2;
    @IsConfig(name = "Click effect max", info = "Ending scale value of cursor click effect animation.")
    public static double MAX_SCALE = 0.7;
    @IsConfig(name="Click effect delay", info = "Delay of the click effect in milliseconds.")
    public static double DELAY = 0;
    @IsConfig(name="Trail effect intensity", info = "Intensity of the cursor trail effect. Currently logarithmic.", min = 1, max = 100)
    public static double effect_intensity = 50;
    @IsConfig(name="Blend Mode", info = "Blending mode for the effect.")
    public static BlendMode blend_mode = BlendMode.SRC_OVER;
    
    @AppliesConfig(config = "blend_mode")
    @AppliesConfig(config = "DURATION")
    @AppliesConfig(config = "MIN_SCALE")
    @AppliesConfig(config = "MAX_SCALE")
    @AppliesConfig(config = "DELAY")
    private static void applyEffectAttributes() {
        pool.forEach(ClickEffect::apply);
    }
        
    // loading
    private static final URL fxml = ClickEffect.class.getResource("ClickEffect.fxml");
    private static final FXMLLoader fxmlLoader = new FXMLLoader(fxml);
    
    // pooling
    private static final List<ClickEffect> pool = new ArrayList();
    private static int counter = 0;
    
    public static ClickEffect create() {
        if (pool.isEmpty())
            return new ClickEffect();
        else {
            ClickEffect c = pool.get(0);
            pool.remove(0);
            return c;
        }
    }
    
    /**
     * Run at specific coordinates. The effect is centered as if in polar coord
     * system.
     * @param X
     * @param Y 
     */
    public static void run(double X, double Y) {
        counter = counter==100 ? 1 : counter+1;
        if (!clickEffect || ( trail_effect && counter%(101-(int)effect_intensity)!=0)) return;
        create().play(X, Y);
    }
    
/******************************************************************************/
    
    // fields
    private AnchorPane parent;
    private final AnchorPane root = new AnchorPane();
    private final FadeTransition fade = new FadeTransition();
    private final ScaleTransition scale = new ScaleTransition();
    private final ParallelTransition anim = new ParallelTransition(root,fade,scale);
    
    private ClickEffect() {
        try {
            fxmlLoader.setRoot(root);
            fxmlLoader.setController(this);
            fxmlLoader.load();
            initialize();
//            ContextManager.overlayPane.getChildren().add(root);
        } catch (IOException ex) {
            Log.err("ClickEffect source data coudlnt be read.");
        }
    }
    
    private void initialize() {
        root.setVisible(false);
        root.setCache(true);
        root.setCacheHint(CacheHint.SPEED);
        anim.setOnFinished( e -> {
            parent.getChildren().remove(root);
            pool.add(this);
        });
        apply();
    }
    
    private void play(double X, double Y) {
        parent = ContextManager.activeWindow.overlayPane;
        parent.getChildren().add(root);
        // center position on run
        root.setLayoutX(X-(root.getWidth()/2));
        root.setLayoutY(Y-(root.getHeight()/2));
        
        // run effect
        root.setVisible(true);
        anim.play();
    }
    
    void apply() {
        root.setBlendMode(blend_mode);
        anim.setDelay(Duration.millis(DELAY));
        
        fade.setDuration(Duration.millis(DURATION));
        fade.setFromValue(0.6);
        fade.setToValue(0);
        
        scale.setDuration(Duration.millis(DURATION));
        scale.setFromX(MIN_SCALE);
        scale.setFromY(MIN_SCALE);
        scale.setToX(MAX_SCALE);
        scale.setToY(MAX_SCALE);
    }
}
