package sp.it.pl.core;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;
import javafx.beans.Observable;
import javafx.geometry.Orientation;
import sp.it.pl.audio.playlist.Playlist;
import sp.it.pl.audio.playlist.PlaylistSong;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.BiContainer;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.FreeFormContainer;
import sp.it.pl.layout.container.Layout;
import sp.it.pl.layout.container.SwitchContainer;
import sp.it.pl.layout.container.UniContainer;
import sp.it.pl.layout.widget.Widget;
import sp.it.util.dev.Blocks;
import sp.it.util.file.properties.PropVal.PropVal1;
import sp.it.util.file.properties.PropVal.PropValN;
import sp.it.util.functional.Try;
import sp.it.util.serialize.xstream.BooleanPropertyConverter;
import sp.it.util.serialize.xstream.DoublePropertyConverter;
import sp.it.util.serialize.xstream.IntegerPropertyConverter;
import sp.it.util.serialize.xstream.LongPropertyConverter;
import sp.it.util.serialize.xstream.ObjectPropertyConverter;
import sp.it.util.serialize.xstream.ObservableListConverter;
import sp.it.util.serialize.xstream.StringPropertyConverter;
import sp.it.util.serialize.xstream.VConverter;
import static java.util.stream.Collectors.toMap;
import static sp.it.util.dev.DebugKt.logger;
import static sp.it.util.file.UtilKt.readTextTry;
import static sp.it.util.file.UtilKt.writeSafely;
import static sp.it.util.functional.Try.Java.error;
import static sp.it.util.functional.Try.Java.ok;
import static sp.it.util.functional.TryKt.getOr;

public final class CoreSerializerXml implements Core {

	private final Charset encoding = StandardCharsets.UTF_8;
	private XStream x;

	@Override
	public void init() {
		// requires --add-opens=java.desktop/java.awt.font=ALL-UNNAMED
		x = new XStream(new DomDriver(encoding.name()));

		// configure security
		XStream.setupDefaultSecurity(x);
		x.allowTypeHierarchy(Observable.class);
		x.allowTypesByWildcard(new String[] { sp.it.pl.PackageInfo.class.getPackageName()+".**" });
		x.allowTypesByWildcard(new String[] { sp.it.util.PackageInfo.class.getPackageName()+".**" });

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
		x.alias("Playlist", Playlist.class);
		x.alias("item", PlaylistSong.class);
		x.alias("Component", Component.class);
		x.alias("Container", Container.class);
		x.alias("UniContainer", UniContainer.class);
		x.alias("BiContainer", BiContainer.class);
		x.alias("FreeFormContainer", FreeFormContainer.class);
		x.alias("SwitchContainer", SwitchContainer.class);
		x.alias("Layout", Layout.class);
		x.alias("Widget", Widget.class);
		x.alias("Loading", Widget.LoadType.class);
		x.alias("Orientation", Orientation.class);
		x.alias("val1", PropVal1.class);
		x.alias("valN", PropValN.class);
		x.useAttributeFor(Component.class, "id");
		x.useAttributeFor(Widget.class, "name");
	}

	@SuppressWarnings({"unchecked", "PlaceholderCountMatchesArgumentCount"})
	@Blocks
	public Try<Void,Throwable> toXML(Object o, File file) {
		return writeSafely(file, f -> {
			try (
				FileOutputStream fos = new FileOutputStream(f);
				OutputStreamWriter ow = new OutputStreamWriter(fos, encoding);
				BufferedWriter w = new BufferedWriter(ow)
			) {
				x.toXML(o, w);
				return ok((Void) null);
			} catch (Throwable e) { // XStreamException | IOException is not enough
				return error(e);
			}
		}).ifErrorUse(e ->
			logger(CoreSerializerXml.class).error("Couldn't serialize " + o.getClass() + " to file {}", file, e)
		);
	}

	@SuppressWarnings("unchecked")
	@Blocks
	public <T> Try<T,Throwable> fromXML(Class<T> type, File file) {
		if (!file.exists())
			return error(new Exception("Couldn't deserialize " + type + " from file " + file, new FileNotFoundException(file.getAbsolutePath())));

		// pre-processing
		String varDefinition = "#def ";
		var lines = getOr(readTextTry(file), "").split("\n");
		Map<String,String> variables = Stream.of(lines)
				.filter(l -> l.startsWith(varDefinition))
				.map(l -> l.substring(varDefinition.length()))
				.collect(toMap(
						l -> l.substring(0, l.indexOf(" ")),
						l -> l.substring(l.indexOf(" ") + 1)
				));
		String text = Stream.of(lines)
				.filter(l -> !l.startsWith(varDefinition))
				.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
				.toString();
		for (Map.Entry<String,String> var : variables.entrySet())
			text = text.replace("@" + var.getKey(), var.getValue());

		try {
			return ok((T) x.fromXML(text));
		} catch (Throwable e) { // ClassCastException | XStreamException | IOException is not enough
			logger(CoreSerializerXml.class).error("Couldn't deserialize " + type + " from file {}", file, e);
			return error(new Exception("Couldn't deserialize " + type + " from file " + file, e));
		}
	}

}