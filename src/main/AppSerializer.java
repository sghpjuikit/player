package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import util.functional.Try;

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
        try {
            return ok((T) x.fromXML(file));
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