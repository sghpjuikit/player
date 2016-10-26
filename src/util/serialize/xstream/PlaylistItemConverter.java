package util.serialize.xstream;

import java.net.URI;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import audio.playlist.PlaylistItem;

/**
 * @author Martin Polakovic
 * 
 * Converter class used for de/serializing PlaylistItem class with XStream.
 */
public class PlaylistItemConverter implements Converter {
	@Override
	public boolean canConvert(Class type) {
		return type.equals(PlaylistItem.class);
	}

	@Override
	public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
		PlaylistItem item = (PlaylistItem) value;
		writer.startNode("uri");
		writer.setValue(item.getURI().toString());
		writer.endNode();
		writer.startNode("time");
		writer.setValue(String.valueOf(item.getTime().toMillis()));
		writer.endNode();
		writer.startNode("artist");
		writer.setValue(item.getArtist());
		writer.endNode();
		writer.startNode("title");
		writer.setValue(item.getTitle());
		writer.endNode();
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		reader.moveDown();
		URI uri = URI.create(reader.getValue());
		reader.moveUp();
		reader.moveDown();
		double time = Double.parseDouble(reader.getValue());
		reader.moveUp();
		reader.moveDown();
		String artist = reader.getValue();
		reader.moveUp();
		reader.moveDown();
		String title = reader.getValue();
		reader.moveUp();
		return new PlaylistItem(uri, artist, title, time);
	}
}