package spectrum;

import java.util.Arrays;
import javafx.scene.canvas.Canvas;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.Effect;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.paint.Color;
import sp.it.pl.audio.Player;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.util.animation.Loop;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static javafx.util.Duration.seconds;

/**
 * Shows audio frequency bars. Animated at 60 fps.
 */
@Widget.Info(
    author = "Martin Polakovic",
    name = "Spectrum",
    description = "Displays real time audio spectrum of playback",
    howto = "",
    notes = "",
    version = "0.7",
    year = "2016",
    group = Widget.Group.VISUALISATION
)
public class Spectrum extends SimpleController {

	public Spectrum(Widget<?> widget) {
    	super(widget);

		SpectrumNode n = new SpectrumNode();
        n.heightProperty().bind(heightProperty());
        n.widthProperty().bind(widthProperty());
		getChildren().add(n);

        n.start();
        onClose.plusAssign(n::stop);
    }

    private static class SpectrumNode extends Canvas implements AudioSpectrumListener {
        private static final double FPS = 60; // Hz
        private static final double SPECTRUM_INTERVAL = seconds(0.1).toMillis(); // ms
        private static final double FRAMES = SPECTRUM_INTERVAL*FPS;
		private static final Effect effect = new Bloom(0);

        private final double[] heights = new double[128];
        private final double[] heights_target = new double[128];
        private long lastUpdate = 0;
        private final Loop loop = new Loop(this::doLoop);

        /** Starts listening to the playback */
        public void start() {
            loop.start();
	        Player.spectrumListeners.add(this);
        }

        /** Stops listening to the playback */
        public void stop() {
            loop.stop();
	        Player.spectrumListeners.remove(this);
        }

        private void doLoop() {
	        // causes bars to go to 0 when paused, instead of standing still
	        // cool effect(makes it feel more alive) + more natural
	        if (System.currentTimeMillis()-lastUpdate > SPECTRUM_INTERVAL)
		        Arrays.fill(heights_target,0);

        	draw();
        }
	    private void draw() {
		    getGraphicsContext2D().setEffect(effect);
		    getGraphicsContext2D().setFill(Color.rgb(0,0,0,0.1));
		    getGraphicsContext2D().fillRect(0,0,getWidth(),getHeight());
		    double midW = getWidth()/2,
				   midH = getHeight()/2;
		    double bars = heights.length;
		    double barGap = 2;
		    double radius = min(midW,midH)/5;
		    double barLine = 2*PI*radius;
		    double barW = (barLine+barGap)/bars-barGap;
		    double dAngle = 2*PI/bars;
		    for (int i = 0; i < bars; i++) {
			    double angle = i*dAngle,
				    angleSin = sin(angle),
				    angleCos = cos(angle);
			    double speed = (heights_target[i]-heights[i])*0.05; // exp
			    heights[i] += speed;
			    double barH = heights[i];
			    getGraphicsContext2D().setLineWidth(barW);
			    getGraphicsContext2D().setStroke(Color.WHITE.interpolate(Color.DEEPPINK, min(1,barH/300)));
			    getGraphicsContext2D().strokeLine(midW+(radius-barH/10)*angleCos, midH+(radius-barH/10)*angleSin, midW+(barH+radius)*angleCos, midH+(barH+radius)*angleSin);
			    getGraphicsContext2D().setLineWidth(20);
		    }
	    }
	    private void draw2() {
		    getGraphicsContext2D().clearRect(0,0,getWidth(),getHeight());
		    double bars = heights.length;
		    double midH = getHeight()/2;
		    double barGap = 5;
		    double barW = (getWidth()+barGap)/bars-barGap;
		    for (int i = 0; i < bars; i++) {
			    double speed =  // choose implementation below
				    (heights_target[i]-heights[i])*0.05; // exp
			    // (heights_target[i]-heights[i])/FRAMES; // linear
			    heights[i] += speed;
			    double barH = heights[i];
			    getGraphicsContext2D().setFill(Color.ORANGE);
			    getGraphicsContext2D().fillRect(i*(barW+barGap), midH-barH/2, barW, barH);
		    }
	    }

        @Override
        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
            lastUpdate = System.currentTimeMillis();
            double bars = heights.length;
            double midH = getHeight()/2;
            double barGap = 5;
            double barW = (getWidth()+barGap)/bars-barGap;
            for (int i = 0; i < bars; i++) {
//                double barH = 60*phases[i];
                double barH = 60 + magnitudes[i]; // i so don't understand this line
                       barH *= max(1,i/10.0); // tries to adjusts higher frequencies having uninsignificant values
                       barH *=8;
                       barH +=10;
                heights_target[i] = barH;
            }
        }
    }

}