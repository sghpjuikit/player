/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.File;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

import AudioPlayer.playlist.PlaylistManager;
import Layout.Widgets.WidgetManager;
import Layout.Widgets.feature.ImageDisplayFeature;
import Layout.Widgets.feature.ImagesDisplayFeature;
import gui.GUI;
import main.App;
import unused.Log;
import util.File.AudioFileFormat.Use;
import util.Util;
import util.dev.TODO;

import static Layout.Widgets.WidgetManager.WidgetSource.NO_LAYOUT;
import static java.awt.Desktop.Action.*;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.dev.TODO.Severity.MEDIUM;
import static util.functional.Util.filter;
import static util.functional.Util.map;

/**
 * Provides methods to handle external often platform specific tasks. Browsing
 * files, opening files in external apps etc.
 * 
 * @author uranium
 */
@TODO(purpose = FUNCTIONALITY,
      note = "printing, mailing if needed (but can be easily implemented")
public class Environment {

    /** Equivalent to {@code browse(f.toURI()); } */
    public static void browse(File f) {
        browse(f.toURI());
    }
    
    /** Equivalent to {@code browse(URI.create(uri)); } */
    public static void browse(String uri) {
        browse(URI.create(uri));
    }
    
    /**
     * Browses file's parent directory or directory.
     * <p>
     * On some platforms the operation may be unsupported.
     * @param uri to brose, for files call file.toURI()
     */
    @TODO(purpose = FUNCTIONALITY, severity = MEDIUM,
          note = "make this work so the file is selected in the explorer")
    public static void browse(URI uri) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(BROWSE)) {
            Log.unsupported("Unsupported operation : " + BROWSE + " uri.");
            return;
        }
        try {
            // if uri denotes a file open its parent directory instead
            // 'bug' fix where the file opens anyway which makes this browse()
            // method the same as open() which we avoid here
            try {
                File file = new File(uri);
                if (file.isFile()) {
                    // get parent
                    File parent = file.getParentFile();
                    // change uri if uri file and has a parent 
                    uri = parent==null ? uri : parent.toURI();
                }
            } catch (IllegalArgumentException e) {
                // ignore exception, it just means the uri does not denote a
                // file which is fine
            }
            
            Desktop.getDesktop().browse(uri);
        } catch (IOException ex) {
            Log.err(ex.getMessage());
        }
    }
    
    /**
     * Browses file or directory. On some platforms the operation may be unsupported.
     * @param files
     * @param uniqify None of the resulting locations is browsed twice, if the
     * file list will be filtered, which can be specified by setting the filter
     * parameter to true.
     */
    public static void browse(List<File> files, boolean uniqify) {
        List<File> to_browse = new ArrayList();
        if (uniqify)
            for (File f: files)
                if (!to_browse.contains(f))
                    to_browse.add(f);
        else
            to_browse.addAll(files);
        
        to_browse.stream().forEach( f -> browse(f.toURI()));
    }
    
    /**
     * Edits file in default associated editor program.
     * On some platforms the operation may be unsupported.
     * 
     * @param file
     */
    public static void edit(File file) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(EDIT)) {
            Log.unsupported("unsupported operation");
            return;
        }
        try {
            Desktop.getDesktop().edit(file);
        } catch (IOException ex) {
            Log.err(ex.getMessage());
        }
    }
    
    /**
     * Opens file in default associated program.
     * On some platforms the operation may be unsupported.
     * 
     * @param f
     */
    public static void open(File f) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(OPEN)) {
            try {
                Desktop.getDesktop().open(f);
            } catch (IOException ex) {
                Log.err(ex.getMessage());
            }
        } else
            Log.unsupported("unsupported operation - open");
    }
    
    public static boolean isOpenableInApp(File f) {
        return ((f.isDirectory() && App.SKIN_FOLDER().equals(f.getParentFile())) || FileUtil.isValidSkinFile(f)) ||
               ((f.isDirectory() && App.WIDGET_FOLDER().equals(f.getParentFile())) || FileUtil.isValidWidgetFile(f)) ||
               AudioFileFormat.isSupported(f,Use.PLAYBACK) || ImageFileFormat.isSupported(f);
    }
    
    public static void openIn(File f, boolean inApp) {
        // open skin - always in app
        if((f.isDirectory() && App.SKIN_FOLDER().equals(f.getParentFile())) || FileUtil.isValidSkinFile(f)) {
            GUI.setSkin(FileUtil.getName(f));
        }
        
        // open widget
        else if((f.isDirectory() && App.WIDGET_FOLDER().equals(f.getParentFile())) || FileUtil.isValidWidgetFile(f)) {
            String n = FileUtil.getName(f);
            WidgetManager.find(wi -> wi.name().equals(n), NO_LAYOUT);
        }
        
        // open audio file
        else if (inApp && AudioFileFormat.isSupported(f,Use.PLAYBACK)) {
            PlaylistManager.use(p -> p.addUri(f.toURI()));
        }
        
        // open image file
        else if (inApp && ImageFileFormat.isSupported(f)) {
            WidgetManager.use(ImageDisplayFeature.class, NO_LAYOUT, w->w.showImage(f));
        }
        
        // delegate to native app cant handle
        else open(f);
    }
    
    public static void openIn(List<File> files, boolean inApp) {
        if(files.isEmpty()) return;
        if(files.size()==1) {
            openIn(files.get(0), inApp);
        } else {
            if(inApp) {
                List<File> audio = filter(files, f->AudioFileFormat.isSupported(f, Use.PLAYBACK));
                List<File> images = filter(files, f->ImageFileFormat.isSupported(f));

                if(!audio.isEmpty())
                    PlaylistManager.use(p -> p.addUris(map(audio,File::toURI)));

                if(images.size()==1) {
                    WidgetManager.use(ImageDisplayFeature.class,NO_LAYOUT, w->w.showImage(images.get(0)));
                } else if (images.size()>1) {
                    WidgetManager.use(ImagesDisplayFeature.class,NO_LAYOUT, w->w.showImages(images));
                }
            } else {
                browse(files, true);
            }
        }
    }
    
    public static File chooseFile(String title, boolean dir, File initial, Window w, ExtensionFilter... exts) {
        if(dir) {
            DirectoryChooser c = new DirectoryChooser();
            c.setTitle(title);
            c.setInitialDirectory(Util.getExistingParent(initial,App.getLocation()));
            return c.showDialog(w);
        } else {
            FileChooser c = new FileChooser();
            c.setTitle(title);
            c.setInitialDirectory(Util.getExistingParent(initial,App.getLocation()));
            if (exts !=null) c.getExtensionFilters().addAll(exts);
            return c.showOpenDialog(w);
        }
    }
    
    public static List<File> chooseFiles(String title, File initial, Window w, ExtensionFilter... exts) {
        FileChooser c = new FileChooser();
        c.setTitle(title);
        c.setInitialDirectory(Util.getExistingParent(initial,App.getLocation()));
        if (exts !=null) c.getExtensionFilters().addAll(exts);
        return c.showOpenMultipleDialog(w);
    }
    
    public static void saveFile(String title, File initial, String initialName, Window w, ExtensionFilter... exts) {
        FileChooser c = new FileChooser();
        c.setTitle(title);
        c.setInitialDirectory(Util.getExistingParent(initial,App.getLocation()));
        c.setInitialFileName(title);
        if (exts !=null) c.getExtensionFilters().addAll(exts);
        c.showSaveDialog(w);
    }
    
}