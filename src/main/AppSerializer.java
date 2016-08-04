package main;

import java.io.*;
import java.nio.charset.Charset;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;

import static util.dev.Util.log;

/**
 * Composition of serializers for the application.
 *
 * @author Martin Polakovic
 */
public final class AppSerializer {

    /** Xstream serializator that serializes objects into human readable XMLs. */
    public final XStream x;
    private final Charset encoding;

    public AppSerializer(Charset xStream_encoding) {
        encoding = xStream_encoding;
        x = new XStream(new DomDriver(encoding.name()));
    }

    public void toXML(Object o, File file) throws SerializationException {
        try (
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter ow = new OutputStreamWriter(fos,encoding);
            BufferedWriter w = new BufferedWriter(ow)
        ) {
            x.toXML(o, w);
        } catch(XStreamException | IOException e) {
	        log(AppSerializer.class).error("Couldn't serialize to file " + file, e);
            throw new SerializationException("Couldn't serialize to file " + file, e);
        } catch(Throwable e) {
	        // We need to be absolutely sure we catch everything
	        // Apparently XStreamException | IOException is not enough
	        log(AppSerializer.class).error("Couldn't serialize to file " + file, e);
            throw new SerializationException("Couldn't serialize to file " + file, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T fromXML(Class<T> type, File file) throws SerializationException {
        try {
            return (T) x.fromXML(file);
        } catch(ClassCastException e) {
	        log(AppSerializer.class).error("Couldn't deserialize " + type + " from file " + file, e);
            throw new SerializationException("Couldn't deserialize " + type + " from file " + file, e);
        } catch(Throwable e) {
	        // We need to be absolutely sure we catch everything
	        // Apparently ClassCastException is not enough
	        log(AppSerializer.class).error("Couldn't deserialize " + type + " from file " + file, e);
            throw new SerializationException("Couldn't deserialize " + type + " from file " + file, e);
        }
    }

    public static class SerializationException extends Exception {
        SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}