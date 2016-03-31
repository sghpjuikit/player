package audio.playback.player;

import javafx.util.Duration;

import org.gstreamer.ClockTime;
import org.gstreamer.elements.PlayBin2;
import org.reactfx.Subscription;

import audio.Item;
import audio.playback.PlaybackState;

import static util.reactive.Util.maintain;

/**
 * GStreamer implementation
 *
 * @author Martin Polakovic
 */
public class GPlayer implements Play {
    // TODO: figure out how to deploy gstreamer native files with the app
    PlayBin2 p;
    Subscription s1, s2, s3, s4, s5;

    @Override
    public void play() {
        if(p!=null) p.play();
    }

    @Override
    public void pause() {
        if(p!=null) p.pause();
    }

    @Override
    public void resume() {
        if(p!=null) p.play();
    }

    @Override
    public void seek(Duration duration) {
        if(p!=null) p.seek(ClockTime.fromMillis((long)duration.toMillis()));
    }

    @Override
    public void stop() {
        if(p!=null) p.stop();
    }

    @Override
    public void createPlayback(Item item, PlaybackState state, Runnable onOK, Runnable onFail) {
        p = new PlayBin2(item.getPath(), item.getURI());
        p.setAutoFlushBus(true);

        s1 = maintain(state.volume, v -> p.setVolume(v.doubleValue()));

        onOK.run();
    }

    @Override
    public void dispose() {
        if(s1!=null) s1.unsubscribe();
        if(s2!=null) s2.unsubscribe();
        if(s3!=null) s3.unsubscribe();
        if(s4!=null) s4.unsubscribe();
        if(s5!=null) s5.unsubscribe();
        s1 = s2 = s3 = s4 = s5 = null;

        if(p!=null) {
            p.disown();
            p.dispose();
            p = null;
        }
    }
}