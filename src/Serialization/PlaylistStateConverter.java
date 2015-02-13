/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Serialization;

import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.ItemSelection.PlayingItemSelector;
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
        writer.setValue(String.valueOf(state.volume.get()));
        writer.endNode();
        writer.startNode("balance");
        writer.setValue(String.valueOf(state.balance.get()));
        writer.endNode();
        writer.startNode("loopMode");
        writer.setValue(String.valueOf(state.loopMode.get()));
        writer.endNode();
        writer.startNode("status");
        writer.setValue(String.valueOf(state.status.get()));
        writer.endNode();
        writer.startNode("duration");
        writer.setValue(String.valueOf(state.duration.get()));
        writer.endNode();
        writer.startNode("currentTime");
        writer.setValue(String.valueOf(state.currentTime.get()));
        writer.endNode();
        writer.startNode("realTime");
        writer.setValue(String.valueOf(state.realTime.get()));
        writer.endNode();
        writer.startNode("mute");
        writer.setValue(String.valueOf(state.mute.get()));
        writer.endNode();
        writer.startNode("rate");
        writer.setValue(String.valueOf(state.rate.get()));
        writer.endNode();
    }
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        PlaybackState s = PlaybackState.getDefault();
        reader.moveDown();
        s.volume.set(Double.parseDouble(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.balance.set(Double.parseDouble(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.loopMode.set(PlayingItemSelector.LoopMode.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.status.set(Status.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.duration.set(Duration.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.currentTime.set(Duration.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.realTime.set(Duration.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.mute.set(Boolean.valueOf(reader.getValue()));
        reader.moveUp();
        reader.moveDown();
        s.rate.set(Double.valueOf(reader.getValue()));
        reader.moveUp();
        return s;
    }
}
