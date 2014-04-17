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
import java.util.UUID;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

/**
 *
 * @author uranium
 */
public class PlaybackStateConverter implements Converter {
    
    @Override
    public boolean canConvert(Class type) {
        return type.equals(PlaybackState.class);
    }
    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        PlaybackState state = (PlaybackState) value;
        writer.startNode("id");
        writer.setValue(String.valueOf(state.getId()));
        writer.endNode();
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
        writer.setValue(String.valueOf(state.getDuration().toMillis()));
        writer.endNode();
        writer.startNode("currentTime");
        writer.setValue(String.valueOf(state.getCurrentTime().toMillis()));
        writer.endNode();
        writer.startNode("realTime");
        writer.setValue(String.valueOf(state.getRealTime().toMillis()));
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
        reader.moveDown();
        UUID id = UUID.fromString(reader.getValue());
        PlaybackState s = PlaybackState.getDefault(id);
        reader.moveUp();
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
        s.setDuration(Duration.millis(Double.parseDouble(reader.getValue())));
        reader.moveUp();
        reader.moveDown();
        s.setCurrentTime(Duration.millis(Double.parseDouble(reader.getValue())));
        reader.moveUp();
        reader.moveDown();
        s.setRealTime(Duration.millis(Double.parseDouble(reader.getValue())));
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