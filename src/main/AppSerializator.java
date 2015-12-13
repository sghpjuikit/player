
package main;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Composition of serializators for the application.
 *
 * @author uranium
 */
public final class AppSerializator {

    /** Xstream serializator that serializes objects into human readable XMLs. */
    public final XStream x = new XStream(new DomDriver());

}
