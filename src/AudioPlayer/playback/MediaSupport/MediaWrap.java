/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback.MediaSupport;

import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 *
 * @author yoss
 */
public interface MediaWrap {
    void play();

    public Object getMedia();

    public void pause();

    public MediaPlayer.Status getStatus();

    public void stop();

    public void seek(Duration duration);

    
    
    
}
