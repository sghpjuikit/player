
package main;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;

import util.dev.TODO;

import static util.dev.TODO.Purpose.READABILITY;

/**
 * Composition of serializators for the application.
 *
 * @author uranium
 */
public final class AppSerializator {

    /** Xstream serializator that serializes objects into human readable XMLs. */
    public final XStream x;
    private final Charset encoding;

    public AppSerializator(Charset xstream_encoding) {
        encoding = xstream_encoding;
        x = new XStream(new DomDriver(encoding.name()));
    }

    public void toXML(Object o, File file) throws SeriallizationException {
        try (
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter ow = new OutputStreamWriter(fos,encoding);
            BufferedWriter w = new BufferedWriter(ow);
        ) {
            x.toXML(o, w);
        // We need to be absolutely sure sp catch everything
        // Apparently XStreamException | IOException is not enough
        } catch(Throwable e) {
            throw new SeriallizationException("Couldn't serialize to file " + file, e);
        }
    }

    public <T> T fromXML(Class<T> type, File file) throws SeriallizationException {
        try {
            return (T) x.fromXML(file);
        // We need to be absolutely sure sp catch everything
        // Apparently ClassCastException is not enough
        } catch(Throwable e) {
            throw new SeriallizationException("Couldn't deserialize " + type + " from file " + file, e);
        }
    }

    public static class SeriallizationException extends Exception {

        public SeriallizationException(String message, Throwable cause) {
            super(message, cause);
        }

        public SeriallizationException(Throwable cause) {
            super(cause);
        }

    }

}