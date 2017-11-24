package sp.it.pl.main;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import javafx.beans.Observable;
import org.atteo.classindex.ClassIndex;
import sp.it.pl.audio.playlist.Playlist;
import sp.it.pl.audio.playlist.PlaylistItem;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.util.functional.Try;
import sp.it.pl.util.serialize.xstream.BooleanPropertyConverter;
import sp.it.pl.util.serialize.xstream.DoublePropertyConverter;
import sp.it.pl.util.serialize.xstream.IntegerPropertyConverter;
import sp.it.pl.util.serialize.xstream.LongPropertyConverter;
import sp.it.pl.util.serialize.xstream.ObjectPropertyConverter;
import sp.it.pl.util.serialize.xstream.ObservableListConverter;
import sp.it.pl.util.serialize.xstream.StringPropertyConverter;
import sp.it.pl.util.serialize.xstream.VConverter;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.file.Util.readFileLines;
import static sp.it.pl.util.functional.Try.error;
import static sp.it.pl.util.functional.Try.ok;

/**
 * Composition of serializers for the application.
 */
public final class AppSerializer {

	/** XStream serializer that serializes objects into human readable XMLs. */
	public final XStream x;
	private final Charset encoding;

	public AppSerializer(Charset outputEncoding) {
		encoding = outputEncoding;
		x = new XStream(new DomDriver(encoding.name()));

		// configure security
		XStream.setupDefaultSecurity(x);
		x.allowTypeHierarchy(Observable.class);
		x.allowTypesByWildcard(new String[] {
			sp.it.pl.PackageInfo.class.getPackageName()+".**"
		});

		// configure serialization
		Mapper xm = x.getMapper();
		x.autodetectAnnotations(true);
		x.registerConverter(new StringPropertyConverter(xm));
		x.registerConverter(new BooleanPropertyConverter(xm));
		x.registerConverter(new DoublePropertyConverter(xm));
		x.registerConverter(new LongPropertyConverter(xm));
		x.registerConverter(new IntegerPropertyConverter(xm));
		x.registerConverter(new ObjectPropertyConverter(xm));
		x.registerConverter(new ObservableListConverter(xm));
		x.registerConverter(new VConverter(xm));
		x.alias("Component", Component.class);
		x.alias("Playlist", Playlist.class);
		x.alias("item", PlaylistItem.class);
		ClassIndex.getSubclasses(Component.class).forEach(c -> x.alias(c.getSimpleName(),c));
		x.useAttributeFor(Component.class, "id");
		x.useAttributeFor(Widget.class, "name");
	}

	public Try<Void,SerializationException> toXML(Object o, File file) {
		try (
				FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter ow = new OutputStreamWriter(fos, encoding);
				BufferedWriter w = new BufferedWriter(ow)
		) {
			x.toXML(o, w);
			return ok(null);
		} catch (Throwable e) { // XStreamException | IOException is not enough
			log(AppSerializer.class).error("Couldn't serialize " + o.getClass() + " to file {}", file, e);
			return error(new SerializationException("Couldn't serialize to file " + file, e));
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Try<T,SerializationException> fromXML(Class<T> type, File file) {
		// pre-processing
		String varDefinition = "#def ";
		List<String> lines = readFileLines(file).collect(toList());
		Map<String,String> variables = lines.stream()
				.filter(l -> l.startsWith(varDefinition))
				.map(l -> l.substring(varDefinition.length()))
				.collect(toMap(
						l -> l.substring(0, l.indexOf(" ")),
						l -> l.substring(l.indexOf(" ") + 1)
				));
		String text = lines.stream()
				.filter(l -> !l.startsWith(varDefinition))
				.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
				.toString();
		for (Map.Entry<String,String> var : variables.entrySet())
			text = text.replace("@" + var.getKey(), var.getValue());

		try {
			return ok((T) x.fromXML(text));
		} catch (Throwable e) { // ClassCastException | XStreamException | IOException is not enough
			log(AppSerializer.class).error("Couldn't deserialize " + type + " from file {}", file, e);
			return error(new SerializationException("Couldn't deserialize " + type + " from file " + file, e));
		}
	}

	public static class SerializationException extends Exception {
		SerializationException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}