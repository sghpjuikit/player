/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides methods to handle external (possibly platform specific) tasks. Browsing
 * files, opening files in external apps etc.
 * @TODO: printing, mailing if needed (but can be easily implemented)
 * @author uranium
 */
public class Enviroment {
    
    /**
     * Browses file or directory. On some platforms the operation may be unsupported.
     * @param file
     * @note: 
     * audio file URI fails to browse - access denied. Use parent folder  uri instead
     * image file URI opens file in associated program instead of win explorer
     * which is unfortunately default windows explorer behavior - therefore use
     * file's parent folder's URI to browse it.
     * @TODO: make this work so the file is selected 
     */
    public static void browse(File file) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Log.unsupported("unsupported operation");
            return;
        }
        try {
            if (file.isFile()) // hack to avoid opening instead of browsing.
                file = file.getParentFile();
            if (file != null)
                Desktop.getDesktop().browse(file.toURI());
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
        List<File> to_browse = new ArrayList<File>();
        if (uniqify)
            for (File f: files)
                if (!to_browse.contains(f))
                    to_browse.add(f);
        else
            to_browse.addAll(files);
        
        for (File f: to_browse)
            Enviroment.browse(f);
    }
    /**
     * Edits file in default associated program. On some platforms the operation may be unsupported.
     * @param file
     */
    public static void edit(File file) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
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
     * Opens file in default associated program. On some platforms the operation may be unsupported.
     * @param file
     */
    public static void open(File file) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
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
