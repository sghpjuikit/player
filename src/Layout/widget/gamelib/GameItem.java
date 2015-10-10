/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.gamelib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import gui.objects.image.cover.Cover;
import gui.objects.image.cover.FileCover;
import util.File.ImageFileFormat;

import static util.File.FileUtil.listFiles;
import static util.File.FileUtil.readFileKeyValues;

/**
 <p>
 @author Plutonium_
 */
public class GameItem {
    private String name;
    private File location;
    private boolean portable;
    private File installocation;
    private Map<String,String> settings;

    public GameItem(File f) {
        location = f.getAbsoluteFile();
        name = f.getName();
    }

    public String getName() {
        return name;
    }

    /**
     * @return the location
     */
    public File getLocation() {
        return location;
    }

    public Cover getCover() {
        File dir = getLocation();
        File cf = listFiles(dir).filter(f -> {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if(i == -1) return false;
            String name = filename.substring(0, i);
            return (ImageFileFormat.isSupported(f.toURI()) && name.equalsIgnoreCase("cover"));
        }).findFirst().orElse(null);

        return new FileCover(cf, "");
    }

    /**
     * @return the portable
     */
    public boolean isPortable() {
        return portable;
    }

    /**
     * @return the installocation
     */
    public File getInstallocation() {
        return installocation;
    }

    public File getExe() {
        return new File(location,"play.lnk");
    }

    public Map<String,String> loadMetadata() {
        if(settings==null) {
            File f = new File(location,"settings.cfg");
            settings = f.exists() ? readFileKeyValues(f) : new HashMap<>();
        }
        return settings;
    }

    public String play() {
        loadMetadata();
        List<String> command = new ArrayList();

        try {
            File exe =null ;
            String pathA = settings.get("pathAbs");

            if(pathA!=null) {
                exe = new File(pathA);
            }

            if(exe==null) {
                String pathR = settings.get("path");
                if(pathR==null) return "No path is set up.";
                exe = new File(location,pathR);
            }

            // run this program
            command.add(exe.getAbsolutePath());

            // with optional parameter
            String arg = settings.get("arguments");
            if(arg!=null) {
                arg = arg.replaceAll(", ", ",");
                String[] args = arg.split(",",0);
                for(String a : args) if(!a.isEmpty()) command.add("-" + a);
            }
            // run
            new ProcessBuilder(command).start();
            return "Starting...";
        } catch (IOException ex) {
            // we might have failed due to the program requiring elevation (run
            // as admin) so we use a little utility we package along
            try {
                // use elevate.exe to run what we wanted
                command.add(0, "elevate.exe");
                new ProcessBuilder(command).start();
                return "Starting (as administrator)...";
            } catch (IOException ex1) {
                Logger.getLogger(GameItem.class.getName()).log(Level.SEVERE, null, ex1);
                return ex.getMessage();
            }
        }

    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(obj instanceof GameItem) return name.equals(((GameItem)obj).name);
        else return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.name);
        return hash;
    }



    public static class RunParameter {
        String value;
        String description;
    }

}
