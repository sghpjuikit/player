package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import util.functional.Try;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static util.dev.Util.log;
import static util.functional.Try.error;
import static util.functional.Try.ok;

/**
 * Composition of serializers for the application.
 *
 * @author Martin Polakovic
 */
public final class AppSerializer {

    /** XStream serializer that serializes objects into human readable XMLs. */
    public final XStream x;
    private final Charset encoding;

    public AppSerializer(Charset xStream_encoding) {
        encoding = xStream_encoding;
        x = new XStream(new DomDriver(encoding.name()));
    }

    public Try<Void,SerializationException> toXML(Object o, File file) {
        try (
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter ow = new OutputStreamWriter(fos,encoding);
            BufferedWriter w = new BufferedWriter(ow)
        ) {
	        x.toXML(o, w);
            return ok(null);
        } catch(Throwable e) {
        // We need to be absolutely sure we catch everything
        // Apparently XStreamException | IOException is not enough
        // } catch(XStreamException | IOException e) {
	        log(AppSerializer.class).error("Couldn't serialize " + o.getClass() + " to file {}", file, e);
	        return error(new SerializationException("Couldn't serialize to file " + file, e));
        }
    }

	@SuppressWarnings("unchecked")
	public <T> Try<T,SerializationException> fromXML(Class<T> type, File file) {
		// pre-processing
		String varDefinition = "#def ";
		List<String> lines = util.file.Util.readFileLines(file).collect(toList());
		Map<String,String> variables = lines.stream()
            .filter(l -> l.startsWith(varDefinition))
			.map(l -> l.substring(varDefinition.length()))
			.collect(toMap(
			 	  l -> l.substring(0, l.indexOf(" ")),
				  l -> l.substring(l.indexOf(" ")+1)
			));
		String text = lines.stream()
            .filter(l -> !l.startsWith(varDefinition))
			.collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
			.toString();
		for (Map.Entry<String,String> var : variables.entrySet())
			text = text.replace("@" + var.getKey(), var.getValue());

		try {
			return ok((T) x.fromXML(text));
		} catch(Throwable e) {
			// We need to be absolutely sure we catch everything
			// Apparently ClassCastException is not enough
			// } catch(ClassCastException e) {
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