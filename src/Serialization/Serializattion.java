
package Serialization;

import Layout.Layout;
import Layout.Widgets.Widget;
import Serialization.xstream.*;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.*;
import main.App;
import util.File.FileUtil;
import util.dev.Log;
import static util.functional.Util.isNotNULL;

/**
 * Serializes objects.
 * 
 * - further helps separating serializable and non serializable classes
 * - agregates serialization code in one place
 * - functionally separates serialization code from other code
 * - prevents serialization code to spread to various classes
 * - reduces imports and code complexity of other classes
 * 
 * 
 * It provides one serializing and one deserializing method that all Serializes
 * object share. Reflection is used to inspect the type of object and proper
 * serialization/deserialization methodology is used.
 * 
 * Note, that the processes of serializing and deserializing can vary greatly,
 * but thats all inner logic. The important fact is that all objects implementing
 * Serializes interface are predefined and set how they get processed during
 * de/serialization by the application design.
 * 
 * 
 * @author uranium
 */
public final class Serializattion {

    
    private void serialize(Layout l) {
        File f = new File(App.LAYOUT_FOLDER(), l.getName() + ".l");
        serialize(l, f);
    }
    
    private Object deserialize(Class<Layout> type, File f) {
        try {
            XStream x = new XStreamFX(new DomDriver());
                    x.autodetectAnnotations(true);
            Layout l = (Layout) x.fromXML(f);
                   l.setName(FileUtil.getName(f));
            return l;
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load gui layout from the file: " + "Layout.l" +
                    ". The file not found or content corrupted. ");
            return null;
        }
    }    
    
    //************************ CONVENIENCE METHODS *****************************
    
    public static void serializeLayout(Layout l) {
        (new Serializattion()).serialize(l);
    }
    public static Layout deserializeLayout(File f) {
        return (Layout) new Serializattion().deserialize(Layout.class, f);
    }
    public static void serialize(Layout l, File f) {
        try {
            XStream x = new XStreamFX(new DomDriver());
            x.autodetectAnnotations(true);
            
            // dont forget to serialize widget properties as well
            l.getAllWidgets().filter(isNotNULL).forEach(Widget::prepareForSerialization);
            x.toXML(l, new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            Log.err("Unable to save gui layout '" + l.getName() + "' into the file: " + f.toPath());
        }
    }
    
}
