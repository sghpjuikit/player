
package Serialization;

import Configuration.Config;
import Configuration.Configuration;
import GUI.Window;
import Layout.Layout;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import utilities.FileUtil;
import utilities.Log;
import utilities.Parser.Parser;

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
public final class Serializator {
    
    /**
     * Serializes this object into file according to application design. 
     * @param o 
     */
    public static void serialize(Serializes o) {
        try {
            Method m = Serializator.class.getDeclaredMethod("serialize", o.getClass());
            m.invoke(new Serializator(), o);
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
            Method m = Serializator.class.getDeclaredMethod("deserialize", o.getClass());
            return m.invoke(new Serializator(), o);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            Log.err(ex.getMessage());
            return null;
        }
    }
    public static Object deserializeFrom(Class<? extends SerializesFile> type, File f) {
        try {
            Method m = Serializator.class.getDeclaredMethod("deserialize", type.getClass(), File.class);
            return m.invoke(new Serializator(), type, f);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            Log.err(ex.getMessage());
            return null;
        }
    }
    
    private void serialize(Configuration o) {
        Log.mess("Saving configuration");
        String content="";
               content += "# " + Configuration.application_Name + " configuration file.\n";
               content += "# " + java.time.LocalTime.now() + "\n";
        
        for (Config f: o.getFields())
            content += f.name + " : " + Parser.toS(f.value) + "\n";
        
        FileUtil.writeFile("Settings.cfg", content);
    }
    private Object deserialize(Configuration o) {
        File file= new File("Settings.cfg").getAbsoluteFile();
        Map<String,String> lin = FileUtil.parseFile(file);
        if(lin.isEmpty())
            Log.mess("Configuration couldnt be set. No content. Using old settings.");
        
        Configuration c = Configuration.getDefault();
        lin.forEach(c::setField);
        return c;
    }
    
    private void serialize(Layout o) {
        File f = new File(Configuration.LAYOUT_FOLDER + File.separator + o.getName() + ".l").getAbsoluteFile();
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.autodetectAnnotations(true);
            
            o.getAllWidgets().stream().filter(w->w!=null).forEach(w -> w.configs.addAll(w.getFields()));
            xstream.toXML(o, new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            Log.err("Unable to save gui layout '" + o.getName() + "' into the file: " + f.toPath());
        }
    }
    private Object deserialize(Layout o) {
        File f = new File(Configuration.LAYOUT_FOLDER + File.separator + o.getName() + ".l").getAbsoluteFile();
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.autodetectAnnotations(true);
            Layout l = (Layout) xstream.fromXML(f);
                   l.setName(FileUtil.getName(f));
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
            return l;
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load gui layout from the file: " + "Layout.l" +
                    ". The file not found or content corrupted. ");
            return null;
        }
    }
    
    private void serialize(Window o) {
        String name = "window";
        File f = new File(Configuration.LAYOUT_FOLDER + File.separator + name + ".w").getAbsoluteFile();
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.autodetectAnnotations(true);
            xstream.registerConverter(new Window.WindowConverter());
            xstream.toXML(o, new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            Log.err("Unable to save window '" + name + "' into the file: " + f.getPath());
        }
    }
    private Object deserialize(Window o) {
        String name = "window";
        File f = new File(Configuration.LAYOUT_FOLDER + File.separator + name + ".w").getAbsoluteFile();
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.autodetectAnnotations(true);
                    xstream.registerConverter(new Window.WindowConverter());
                    
            return xstream.fromXML(f);
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load window from the file: " + f.getPath() +
                    ". The file not found or content corrupted. ");
            return null;
        }
    }
    
    
    //************************ CONVENIENCE METHODS *****************************
    
    public static void serializeLayout(Layout l) {
        (new Serializator()).deserialize(l);
    }
    public static Layout deserializeLayout(File f) {
        return (Layout) deserializeFrom(Layout.class, f);
    }
    

    public static void serializeWindow(Window o) {
        String name = "window";
        File f = new File(Configuration.LAYOUT_FOLDER + File.separator + name + ".w").getAbsoluteFile();
        try {
            XStream xstream = new XStream(new DomDriver());
            xstream.autodetectAnnotations(true);
            xstream.registerConverter(new Window.WindowConverter());
            xstream.toXML(o, new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            Log.err("Unable to save window '" + name + "' into the file: " + f.getPath());
        }
    }
    public static Window deserializeWindow() {
        String name = "window";
        File f = new File(Configuration.LAYOUT_FOLDER + File.separator + name + ".w").getAbsoluteFile();
        try {
            XStream xstream = new XStream(new DomDriver());
                    xstream.autodetectAnnotations(true);
                    xstream.registerConverter(new Window.WindowConverter());
                    
            return (Window) xstream.fromXML(f);
        } catch (ClassCastException | StreamException ex) {
            Log.err("Unable to load window from the file: " + f.getPath() +
                    ". The file not found or content corrupted. ");
            return null;
        }
    }
}
