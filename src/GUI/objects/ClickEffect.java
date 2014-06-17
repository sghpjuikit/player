
package GUI.objects;

import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.ContextManager;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransitionBuilder;
import javafx.animation.ParallelTransition;
import javafx.animation.ParallelTransitionBuilder;
import javafx.animation.ScaleTransitionBuilder;
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
    @IsConfig(name="Click effect intensity", info = "Intensity of the cursor trail effect. Currently logarithmic.", min = 1, max = 100)
    public static double effect_intensity = 50;
    @IsConfig(name="Blend Mode", info = "Blending mode forthe effect.")
    public static BlendMode blend_mode = BlendMode.SRC_OVER;
    
    // loading
    private static final URL fxml = ClickEffect.class.getResource("ClickEffect.fxml");
    private static final FXMLLoader fxmlLoader = new FXMLLoader(fxml);
    
    // pooling
    private static final List<ClickEffect> passive = new ArrayList<>();
    private static int counter = 0;
    
    // fields
    private final AnchorPane root = new AnchorPane();
    private final ParallelTransition effect= ParallelTransitionBuilder.create()
            .node(root)
            .children(
                FadeTransitionBuilder.create()
                    .duration(Duration.millis(DURATION))
                    .delay(Duration.millis(DELAY))
                    .fromValue(0.6)
                    .toValue(0)
                    .build(),
                ScaleTransitionBuilder.create()
                    .duration(Duration.millis(DURATION))
                    .delay(Duration.millis(DELAY))
                    .fromX(MIN_SCALE)
                    .fromY(MIN_SCALE)
                    .toX(MAX_SCALE)
                    .toY(MAX_SCALE)
                    .build()
            )
            .build();


    
    public static ClickEffect create() {
        if (passive.isEmpty())
            return new ClickEffect();
        else {
            ClickEffect c = passive.get(0);
            passive.remove(0);
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
    
    private ClickEffect() {
        try {
            fxmlLoader.setRoot(root);
            fxmlLoader.setController(this);
            fxmlLoader.load();
            initialize();
            ContextManager.gui.overlayPane.getChildren().add(root);
        } catch (IOException ex) {
            Log.err("ClickEffect source data coudlnt be read.");
        }
    }
    
    private void initialize() {
        root.setVisible(false);
        root.setCache(true);
        root.setCacheHint(CacheHint.SPEED);
        root.setBlendMode(blend_mode);
        effect.setOnFinished( e -> {
            passive.add(this);
        });
    }
    
    private void play(double X, double Y) {        
        // center position on run
        root.setLayoutX(X-(root.getWidth()/2));
        root.setLayoutY(Y-(root.getHeight()/2));
        
        // run effect
        root.setVisible(true);
        effect.play();
    }
}
