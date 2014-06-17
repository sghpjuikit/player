/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import utilities.functional.functor.Procedure;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.media.AudioSpectrumListener;
import javafx.util.Duration;

/**
 * @author uranium
 */
public final class PlaybackCore {
    
//******************************** SEEKING ************************************/  
    
    boolean needs_bind = true; // must be initialized to true
    boolean needs_seek = false;
    Duration seekTo;
    
//******************************* SPECTRUM ************************************/
    
    private final List<AudioSpectrumListener> spectrumListeners = new ArrayList<>();
    /**
     * Only one spectrum listener is allowed per playback (MediaPlayer) object. Here
     * reregistering and ditributing of the event is handled.
     * Playback has main spectrum listener registered only if there is at least one
     * listener registered in the listener list.
     */
    final AudioSpectrumListener spectrumListenerDistributor = (double d, double d1, float[] floats, float[] floats1) -> {
        // distribute event to all listeners
        spectrumListeners.forEach(l->l.spectrumDataUpdate(d, d1, floats, floats1));
    };

    void addAudioSpectrumListener(AudioSpectrumListener l) {
        spectrumListeners.add(l);
        if(spectrumListeners.size()==1)
            PLAYBACK.playback.setAudioSpectrumListener(l);
    }
    void removeAudioSpectrumListener(AudioSpectrumListener l) {
        spectrumListeners.remove(l);
        if(spectrumListeners.isEmpty())
            PLAYBACK.playback.setAudioSpectrumListener(null);
    }
    
//*****************************************************************************/

    private final List<Procedure> onStartHandlers = new ArrayList<>();
    
    final Procedure playbackStartDistributor = () -> {
        onStartHandlers.forEach(Procedure::run);
    };
    void addOnPlaybackStart(Procedure b) {
        onStartHandlers.add(b);
    }
    void removeOnPlaybackStart(Procedure b) {
        onStartHandlers.remove(b);
    }
    
//*****************************************************************************/

    private final List<Procedure> onEndHandlers = new ArrayList<>();
    
    final Runnable playbackEndDistributor = () -> { 
        onEndHandlers.forEach(Procedure::run);
    };
    
    void addOnPlaybackEnd(Procedure b) {
        onEndHandlers.add(b);
    }
    void removeOnPlaybackEnd(Procedure b) {
        onEndHandlers.remove(b);
    }
    
//*****************************************************************************/
}
