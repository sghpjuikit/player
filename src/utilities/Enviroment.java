/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.awt.Desktop;
import static java.awt.Desktop.Action.BROWSE;
import static java.awt.Desktop.Action.EDIT;
import static java.awt.Desktop.Action.OPEN;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides methods to handle external often platform specific tasks. Browsing
 * files, opening files in external apps etc.
 * 
 * @author uranium
 */
@TODO("printing, mailing if needed (but can be easily implemented")
public class Enviroment {
    
    /**
     * Browses file's parent directory or directory.
     * <p>
     * On some platforms the operation may be unsupported.
     * @param uri to brose, for files call file.toURI()
     */
    @TODO("make this work so the file is selected in the explorer")
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
     * @param file
     */
    public static void open(File file) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(OPEN)) {
            Log.unsupported("unsupported operation");
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            Log.err(ex.getMessage());
        }
    }
}
