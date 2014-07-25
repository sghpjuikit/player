/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI;

import Configuration.IsConfig;
import GUI.LayoutAggregators.SwitchPane;
import Layout.Layout;
import Layout.Widgets.WidgetManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.stage.Screen;
import main.App;
import utilities.FileUtil;
import utilities.Log;

/**
 *
 * @author Plutonium_
 */
public class WindowManager {
    
    @IsConfig(name="Docked mode", info="Whether application layout is in mini - docked mode.")
    public static boolean mini = false;
    
    static Window miniWindow;
    
    public static void toggleMini() {
        setMini(!mini);
    }
    public static void setMini(boolean val) {
        mini = val;
            App.getWindow().hide();
        if(val) {
            String name = "mini-window";
            File f = new File(App.LAYOUT_FOLDER(), name + ".w");
            Window tmp = Window.deserializeSuppressed(f);
            miniWindow = tmp!=null ? tmp : Window.create();
            miniWindow.setSize(Screen.getPrimary().getBounds().getWidth(), 50);
            miniWindow.setContent(WidgetManager.getFactory("PlayerControlsTiny").create().load());
            miniWindow.show();
            miniWindow.setShowHeader(false);
            miniWindow.update();
        } else {
            String name = "mini-window";
            File f = new File(App.LAYOUT_FOLDER(), name + ".w");
            miniWindow.serializeSupressed(f);
            App.getWindow().show();
            miniWindow.close();
            miniWindow=null;
        }
    }

    
    public static void serialize() {
        // make sure directory is accessible
        File dir = App.LAYOUT_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Serialization of windows and layouts failed. " + dir.getPath() +
                    " could not be accessed.");
            return;
        }
        
        // get windows
        List<Window> src = new ArrayList<>(ContextManager.windows);
        Log.deb("Serializing " + src.size() + " application windows for next session.");
        
        // remove serialized window files from previous session
        File[] oldFs = dir.listFiles(f->f.getPath().endsWith(".ws"));
        for(File f: oldFs) f.delete();
        Log.deb("Removing " + oldFs.length + " old windows files from previous session.");
        // remove serialized layout files from previous session
        File[] oldLsA = dir.listFiles(f->
            f.getPath().contains("window")&&f.getPath().contains("-layout"));
        // but we have to delay deleting or we can delete new files (a bug)
        // if a layout is going to be serialized into one of the to-be-deleted
        // files it will be deleted for some reason so we need to hold a list
        // of to-delete files and remove to-be-serialized files from that list
        List<File> oldLs = new ArrayList();
        for (File f : oldLsA) oldLs.add(f);
        
        // serialize - for now each window to its own file with .ws extension
        int count = 0;
        for(int i=0; i<src.size(); i++) {
            // ret resources
            Window w = src.get(i);
            String name = "window" + i;
            File f = new File(dir, name + ".ws").getAbsoluteFile();
            // serialize
            boolean success = w.serializeSupressed(f);
            if (success) count++;
            // serialize contained layouts
            for(Map.Entry<Integer,Layout> e : w.getLayoutAggregator().getLayouts().entrySet()) {
                int index = e.getKey();
                Layout l = e.getValue();
                // dont continue if layout empty
                if(l.getChild() == null) continue;
                // associate the layout with the window by name & save
                l.setName("window" + i + "-layout" + index);
                l.serialize();
                // remove file from the list of files from previous session
                oldLs.remove(l.getFile());
            }
        }
        
        // remove serialized layout files from previous session - now its safe
        oldLs.forEach(File::delete);
        Log.deb("Removing " + oldLs.size() + " old layout files from previous session.");
        
        Log.deb("Serialized " + count + " windows.");
    }
    
    public static void deserialize() {
        // make sure directory is accessible
        File dir = App.LAYOUT_FOLDER();
        if (!FileUtil.isValidatedDirectory(dir)) {
            Log.err("Deserialization of windows and layouts failed. " + dir.getPath() +
                    " could not be accessed.");
            return;
        }
        
        Log.deb("Deserializing application windows from next session.");
        // discover all window files with 'ws extension
        File[] fs = dir.listFiles(f->f.getName().endsWith(".ws"));
        Log.deb("Discovered " + fs.length + " window files to deserialize.");
        
        // prepare layout files
        Map<Integer,Map<Integer,File>> lmap= new HashMap(); // map windowIndex-layoutFiles
        File[] lfs = dir.listFiles(f->f.getPath().endsWith(".l"));
        for(File f : lfs) {
            // parse string and get window index
            String name = f.getName();
            int from = name.indexOf("window");
            int to = name.indexOf('-');
            String number = name.substring(from+6, to);
            int wIndex;
            try{
                wIndex = new Integer(number);
            } catch(NumberFormatException e) {
                // ignore file if damaged name
                continue;
            }
            
            // parse string and get layout index
            from = name.indexOf("layout");
            to = name.indexOf(".");
            number = name.substring(from+6, to);
            int lIndex;
            try{
                lIndex = new Integer(number);
            } catch(NumberFormatException e) {
                // ignore file if damaged name
                continue;
            }
            
            // put layout to map's list with index of the window it belongs to
            if(!lmap.containsKey(wIndex)) lmap.put(wIndex, new HashMap());
            lmap.get(wIndex).put(lIndex,f);
        }
        
        // deserialize windows
        List<Window> ws = new ArrayList();
        for(int i=0; i<fs.length; i++) {
            File f = fs[i];
            Window w = Window.deserializeSuppressed(f);
            
            // handle next window if this was not successfully deserialized
            if(w==null) continue;
            ws.add(w);
            
            // avoid null if no layout for this window
            if(lmap.get(i)==null) lmap.put(i,new HashMap<>());
            
            
            SwitchPane la = new SwitchPane();
            // otherwise deserialize layout
            lmap.get(i).forEach( (at,lf) -> {
                Layout l = new Layout(FileUtil.getName(lf)).deserialize(lf);
                la.addTab(at,l);
            });
            
            w.setLayoutAggregator(la);
        }
        
        // make sure there is at least one window
        if(ws.isEmpty()) {
            Window w = Window.create();
                   w.setSizeAndLocationToInitial();
                   w.setLayoutAggregator(new SwitchPane());
            ws.add(w);
        }
        
        // grab main window and initialize it
        ws.get(0).setAsMain();
               
        // show
        ws.forEach(w->{
            w.show();
            w.update();
        });
        Log.deb("Deserialized " + ws.size() + " windows.");
    }
}
