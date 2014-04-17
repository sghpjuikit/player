/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Serialization;

import AudioPlayer.playback.LoopMode;
import AudioPlayer.playback.PlaybackState;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

/**
 *
 * @author uranium
 */
public class PlaylistStateConverter implements Converter {
    
    @Override
    public boolean canConvert(Class type) {
        return type.equals(PlaybackState.class);
    }
    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        PlaybackState state = (PlaybackState) value;
        writer.startNode("volume");
        writer.setValue(String.valueOf(state.getVolume()));
        writer.endNode();
        writer.startNode("balance");
        writer.setValue(String.valueOf(state.getBalance()));
        writer.endNode();
        writer.startNode("loopMode");
        writer.setValue(String.valueOf(state.getLoopMode()));
        writer.endNode();
        writer.startNode("status");
        writer.setValue(String.valueOf(state.getStatus()));
        writer.endNode();
        writer.startNode("duration");
        writer.setValue(String.valueOf(state.getDuration()));
        writer.endNode();
        writer.startNode("currentTime");
        writer.setValue(String.valueOf(state.getCurrentTime()));
        writer.endNode();
        writer.startNode("realTime");
        writer.setValue(String.valueOf(state.getRealTime()));
        writer.endNode();
        writer.startNode("mute");
        writer.setValue(String.valueOf(state.getMute()));
        writer.endNode();
        writer.startNode("rate");
        writer.setValue(String.valueOf(state.getRate()));
        writer.endNode();
    }
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        PlaybackState s = PlaybackState.getDefault();
        reader.moveDown();
        s.setVolume(Double.parseDouble(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.setBalance(Double.parseDouble(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.setLoopMode(LoopMode.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.setStatus(Status.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.setDuration(Duration.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.setCurrentTime(Duration.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.setRealTime(Duration.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.setMute(Boolean.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.setRate(Double.valueOf(reader.getValue()));
        reader.moveUp();
        return s;
    }
}
