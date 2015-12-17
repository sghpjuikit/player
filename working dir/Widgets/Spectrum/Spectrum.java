/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Spectrum;

import javafx.scene.canvas.Canvas;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.paint.Color;

import AudioPlayer.playback.PLAYBACK;
import Layout.widget.Widget;
import Layout.widget.controller.ClassController;

/**
 *
 * @author Plutonium_
 */
@Layout.widget.Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Spectrumator",
    description = "PDisplays real time audio spectrum of playback",
    howto = "",
    notes = "",
    version = "0.6",
    year = "2014",
    group = Widget.Group.VISUALISATION
)
public class Spectrum extends ClassController  {
    private final Spectr spectr = new Spectr();

    public Spectrum() {
        getChildren().add(spectr);
        spectr.heightProperty().bind(heightProperty());
        spectr.widthProperty().bind(widthProperty());

        // we only need to start listening once, so do it here rather than in
        // refresh(). In fact that would have an effect of multiplying the
        // listener
        spectr.startListening();
    }

    @Override
    public void onClose() {
        spectr.stopListening();
    }

    class Spectr extends Canvas implements AudioSpectrumListener {
//        Loop loop = new util.animation.Loop(() -> {});

        /** Starts listening to the playback */
        public void startListening() {
            PLAYBACK.addAudioSpectrumListener(this);
        }

        /** Stops listening to the playback */
        public void stopListening() {
            PLAYBACK.removeAudioSpectrumListener(this);
        }

        @Override
        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
            getGraphicsContext2D().clearRect(0,0,getWidth(),getHeight());
            double midH = getHeight()/2;
            double barGap = 5;
            double bars = magnitudes.length;
            double barW = (getWidth()+barGap)/bars-barGap;
            for (int i = 0; i < bars; i++) {
//                double barH = 60*phases[i];
                double barH = 60 + magnitudes[i];
                       barH *=8;
                       barH +=10;
                getGraphicsContext2D().setFill(Color.ORANGE);
                getGraphicsContext2D().fillRect(i*(barW+barGap), midH-barH/2, barW, barH);
            }
        }
    }
}