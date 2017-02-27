package util.serialize.xstream;

import audio.playlist.Playlist;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.UUID;

public class PlaylistConverter extends CollectionConverter {

	public PlaylistConverter(Mapper mapper) {
		super(mapper);
	}

	@Override
	public boolean canConvert(Class type) {
		return type!=null && Playlist.class.isAssignableFrom(type);
	}

	@Override
	protected Object createCollection(Class type) {
		throw new AssertionError("Method forbidden");
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		Playlist p = (Playlist) source;
		writer.addAttribute("id", p.id.toString());
		writer.addAttribute("playing", String.valueOf(p.playingI.get()));
		super.marshal(source, writer, context);
	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		String id = reader.getAttribute("id");
		String playing = reader.getAttribute("playing");

		Playlist p = new Playlist(UUID.fromString(id));
		populateCollection(reader, context, p);
		p.updatePlayingItem(Integer.parseInt(playing)); // collection must be populated at this point
		return p;
	}

}