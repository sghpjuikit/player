
package main;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Application serializator.
 * 
 * @author uranium
 */
public final class AppSerializator {

    public final XStream x = new XStream(new DomDriver());

}
