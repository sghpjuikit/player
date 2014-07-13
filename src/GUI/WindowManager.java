/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI;

import Configuration.IsConfig;
import GUI.LayoutAggregators.SwitchPane;
import Layout.Layout;
import Layout.WidgetImpl.Layouter;
import Layout.Widgets.WidgetManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        // get windows
        List<Window> src = new ArrayList<>(ContextManager.windows);
        Log.deb("Serializing " + src.size() + " application windows for next session.");
        
        // remove serialized window files from previous session
        File[] oldFs = App.LAYOUT_FOLDER().listFiles(f->f.getPath().endsWith(".ws"));
        for(File f: oldFs) f.delete();
        Log.deb("Removing " + oldFs.length + " old windows files from previous session.");
        // remove serialized layout files from previous session
        File[] oldLsA = App.LAYOUT_FOLDER().listFiles(f->
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
            File f = new File(App.LAYOUT_FOLDER(), name + ".ws").getAbsoluteFile();
            // serialize
            boolean success = w.serializeSupressed(f);
            if (success) count++;
            // serialize contained layouts
            List<Layout> ls = w.getLayoutAggregator().getLayouts();
            for(int j=0; j<ls.size(); j++) {
                Layout l = ls.get(j);
                // dont continue if layout empty
                if(l.getChild() == null || l.getChild() instanceof Layouter) continue;
                // associate the layout with the window by name & save
                l.setNameAndSave("window" + i + "-layout" + j);
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
        Log.deb("Deserializing application windows from next session.");
        // discover all window files with 'ws extension
        File[] fs = App.LAYOUT_FOLDER().listFiles(f->f.getName().endsWith(".ws"));
        Log.deb("Discovered " + fs.length + " window files to deserialize.");
        
        // prepare layout files
        Map<Integer,List<File>> lmap= new HashMap(); // map windowIndex-layoutFiles
        File[] lfs = App.LAYOUT_FOLDER().listFiles(f->f.getPath().endsWith(".l"));
        for(File f : lfs) {
            // look for the window index for the layout, put 50 as a window amount limit
            for(int i=0; i<50; i++) { // i - window index
                // put layout to map's list with index of the window it belongs to
                if(f.getName().contains("window"+i)) {
                    if(!lmap.containsKey(i)) lmap.put(i, new ArrayList<>());
                    lmap.get(i).add(f);
                    // we found the index, stop searching
                    break;
                }
            }
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
            if(lmap.get(i)==null) lmap.put(i,new ArrayList<>());
            
            // otherwise deserialize layout
            List<Layout> ls = lmap.get(i).stream()
                    .map(lf->new Layout(FileUtil.getName(lf)).deserialize(lf))
                    .collect(Collectors.toList());
            
            SwitchPane la = new SwitchPane();
            for(int j=0; j<ls.size(); j++) la.addTab(j,ls.get(j));
            w.setLayoutAggregator(la);
        }
        
        // make sure there is at least one window
        if(ws.isEmpty()) ws.add(Window.create());
        
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
