
package spectrum;

import java.util.Arrays;

import javafx.scene.canvas.Canvas;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.paint.Color;

import audio.playback.PLAYBACK;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.animation.Loop;

/**
 *
 * @author Martin Polakovic
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Spectrum",
    description = "Displays real time audio spectrum of playback",
    howto = "",
    notes = "",
    version = "0.6",
    year = "2015",
    group = Widget.Group.VISUALISATION
)
public class Spectrum extends ClassController  {
    private final SpectrumNode n = new SpectrumNode();

    public Spectrum() {
        getChildren().add(n);
        n.heightProperty().bind(heightProperty());
        n.widthProperty().bind(widthProperty());

        n.startListening();
    }

    @Override
    public void onClose() {
        n.stopListening();
    }

    private static class SpectrumNode extends Canvas implements AudioSpectrumListener {
        private static final double SPECTRUM_INTERVAL = 100; // seconds(0.1).toMillis();
        private static final double FRAMES = SPECTRUM_INTERVAL*60; // SPECTRUM_INTERVAL at 60 fps
        double[] heights = new double[128];
        double[] heights_target = new double[128];
        long lastUpdate = 0;
        Loop loop = new util.animation.Loop(this::draw);

        /** Starts listening to the playback */
        public void startListening() {
            loop.start();
            PLAYBACK.spectrumListeners.add(this);
        }

        /** Stops listening to the playback */
        public void stopListening() {
            loop.stop();
            PLAYBACK.spectrumListeners.remove(this);
        }

        void draw() {
            // this cause bars go to 0 when paused, instead of remaining frozen
            // cool effect(makes it feel more alive) + more natural
            if(System.currentTimeMillis()- lastUpdate > SPECTRUM_INTERVAL)
                Arrays.fill(heights_target,0);

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
                       barH *=8;
                       barH +=10;
                heights_target[i] = barH;
            }
        }
    }
}