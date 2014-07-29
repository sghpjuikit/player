
package Serialization;

import Layout.Layout;
import Layout.Widgets.Widget;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import main.App;
import utilities.FileUtil;
import utilities.Log;
import static utilities.Util.NotNULL;

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
    
    /**
     * Serializes this object into file according to application design. 
     * @param o 
     */
    public static void serialize(Serializes o) {
        try {
            Method m = Serializattion.class.getDeclaredMethod("serialize", o.getClass());
            m.invoke(new Serializattion(), o);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            Log.err(ex.getMessage());
        }
    }
    /**
     * Deserializes this object from file according to application design.
     * @param o
     * @return deserialized object. 
     */
    public static Object deserialize(Serializes o) {
        try {
            Method m = Serializattion.class.getDeclaredMethod("deserialize", o.getClass());
            return m.invoke(new Serializattion(), o);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            Log.err(ex.getMessage());
            return null;
        }
    }
    public static Object deserializeFrom(Class<? extends SerializesFile> type, File f) {
        try {
            Method m = Serializattion.class.getDeclaredMethod("deserialize", type.getClass(), File.class);
            return m.invoke(new Serializattion(), type, f);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            Log.err(ex.getMessage());
            return null;
        }
    }
    
    private void serialize(Layout o) {
        File f = new File(App.LAYOUT_FOLDER(), o.getName() + ".l");
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.autodetectAnnotations(true);
            
            // serialize widget properties as well
            o.getAllWidgets().filter(NotNULL).forEach(Widget::rememberConfigs);
            xstream.toXML(o, new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            Log.err("Unable to save gui layout '" + o.getName() + "' into the file: " + f.toPath());
        }
    }
    private Object deserialize(Layout o) {
        File f = new File(App.LAYOUT_FOLDER(), o.getName() + ".l").getAbsoluteFile();
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.autodetectAnnotations(true);
            Layout l = (Layout) xstream.fromXML(f);
                   l.setName(FileUtil.getName(f));
//                   l.getAllWidgets()
//                        .filter(NotNULL)
//                        .forEach(Widget::restoreConfigs);
            return l;
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load gui layout from the file: " + "Layout.l" +
                    ". The file not found or content corrupted. ");
            return null;
        }
    }
    private Object deserialize(Class<Layout> type, File f) {
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.autodetectAnnotations(true);
            Layout l = (Layout) xstream.fromXML(f);
                   l.setName(FileUtil.getName(f));
//                   l.getAllWidgets()
//                        .filter(NotNULL)
//                        .forEach(Widget::restoreConfigs);
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
            XStream xstream = new XStream(new DomDriver());
            xstream.autodetectAnnotations(true);
            
            // serialize widget properties as well
            l.getAllWidgets().filter(NotNULL).forEach(Widget::rememberConfigs);
            xstream.toXML(l, new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            Log.err("Unable to save gui layout '" + l.getName() + "' into the file: " + f.toPath());
        }
    }
    
}
