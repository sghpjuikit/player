
package main;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;

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


    public void toXML(Object o, File file) throws IOException {
        x.toXML(o, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),encoding)));
    }

    public <T> T fromXML(Class<T> type, File file) throws StreamException {
        try {
            return (T) x.fromXML(file);
        } catch(ClassCastException e) {
            throw new StreamException(e);
        }
    }

}
