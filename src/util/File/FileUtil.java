/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.File;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import main.App;
import util.File.AudioFileFormat.Use;
import util.Util;
import util.dev.Log;
import static util.functional.Util.isNotNULL;

/**
 * Provides file operations.
 * 
 * This class provides bunch of fancy methods that are arguably redundant, but
 * can make your life hell of a lot easier. They clean the code from low level
 * machinations.
 * 
 */
public final class FileUtil {
    
    /**
     * Empty file. Use where null is not desired. There is only one instance of
     * empty file - this one. Do not use equals() for comparing, intead use
     * == operator.
     * <p>
     * Current implementation is file denoting application location directory.
     */
    public static final URI EMPTY_URI = URI.create("empty://empty");
    
    /**
     * Empty color. Fully transparent black. Substitute for null in some
     * situations.
     */
    public static final Color EMPTY_COLOR = new Color(0, 0, 0, 0);
    
    /**
     * Returns true if for provided File all conditions are met:
     * - is not null
     * - exists
     * - is directory
     * - is readable
     * - is writable
     * 
     * Use this method to see if the diretory is not required to be valid.
     * @param dir
     * @return validity of directory for use
     */
    public static boolean isValidDirectory(File dir){
        if (dir==null) {
            return false;
        }
        if (!dir.exists()) {
            Log.info("Directory " + dir.getAbsolutePath() + " doesnt exist");
            return false;
        }
        if (!dir.isDirectory()) {
            Log.err("Path " + dir.getAbsolutePath() + " is not directory");
            return false;
        }
        if (!dir.canRead()) {
            Log.info("Directory " + dir.getAbsolutePath() + " is not readable");
            return false;            
        }
        if (!dir.canWrite()) {
            Log.info("Directory " + dir.getAbsolutePath() + " is not writable");
            return false;            
        }
        return true;
    }
    
    /**
     * Checks whether directory is valid and if it isnt, attempts
     * to make it valid by setting appropriate permissions or creating it.
     * Returns true if for provided File all conditions are met:
     * - exists
     * - is directory
     * - is readable
     * - is writable
     * 
     * Use this method to see if the diretory is required to be valid.
     * @param dir if not a directory, this method always returns false.
     * @return whether the directory is usable.
     * @throws NullPointerException if param null
     */
    public static boolean isValidatedDirectory(File dir) {
        boolean validity = true;
        if (!dir.exists())
            validity = dir.mkdirs();
        if (!dir.isDirectory()) {
            Log.err("Path " + dir.getAbsolutePath() + " is not directory");
            return false;
        }
        if (!dir.canRead())
            validity = dir.setReadable(true);  
        if (!dir.canWrite())
            validity = dir.setWritable(true);
        
        if (!validity)
            Log.err("Directory " + dir.getAbsolutePath() + " is not valid");
        return validity;
    }

    /**
     * Returns true if for provided File all conditions are met:
     * - exists
     * - is file
     * - is readable
     * 
     * @param file
     * @return validity of file for use
     * @throws NullPointerException if parameter null.
     */
    public static boolean isValidFile(File file) {
        if (!file.exists()) {
            Log.err("File " + file.getAbsolutePath() + " doesnt exist");
            return false;
        } else 
        if (!file.isFile()) {
            Log.err("Path " + file.getAbsolutePath() + " is not directory");
            return false;
        } else
        if (!file.canRead()) {
            Log.err("File " + file.getAbsolutePath() + " is not readable");
            return false;            
        } else {
            return true;
        }
    }
    
    /**
     * Checks validity of a file to be a skin. True return file means the file
     * can be used as a skin (the validity of the skin itself is not included).
     * For files returning false this application will not allow skin change.
     * Valid skin file checks out the following:
     * - not null
     * - isValidFile()
     * - is located in Skins folder set for this application
     * - is .css
     * - is located in its own folder with the same name
     * example: /Skins/MySkin/MySkin.css
     * @param f
     * @return true if parameter is valid skin file. False otherwise or if null.
     */
    public static boolean isValidSkinFile(File f) {
        String name = FileUtil.getName(f);
        String path = App.SKIN_FOLDER().getPath() + File.separator + name +
                      File.separator + name + ".css";
        File test = new File(path);
        return (f != null && FileUtil.isValidFile(f) &&   // is valid file
                    f.getPath().endsWith(".css") &&       // is .css file
                        f.equals(test));                  // is located in skins folder in its rightful folder
    }
    
    public static boolean isValidWidgetFile(File f) {
        File p1 = f.getParentFile();
        File p2 = p1==null ? null : p1.getParentFile();
        return (f != null && FileUtil.isValidFile(f) &&   // is valid file
                    f.getPath().endsWith(".fxml") &&      // is .css file
                        App.WIDGET_FOLDER().equals(p2));  // is located in skins folder in its rightful folder
    }
    
    /**
     @param depth the maximum number of levels of directories to visit. A value
     of 0 means that only the starting file is visited. Integer.MAX_VALUE
     may be used to indicate that all levels should be visited.
     */
    public static Stream<File> getFilesAudio(File dir, Use use, int depth) {
        if(dir.isDirectory()) {
            try {
                return Files.walk(dir.toPath(),depth).map(Path::toFile)
                            .filter(f->AudioFileFormat.isSupported(f,use));
            } catch (IOException ex) {
                return Stream.empty();
            }
        }
        
        if(dir.isFile()) {
            if(AudioFileFormat.isSupported(dir,use))
                return Stream.of(dir);
        }
        
        return Stream.empty();
    }
    
    public static Stream<File> getFilesAudio(List<File> files, Use use, int depth) {
        return files.stream().flatMap(f -> getFilesAudio(f, use, depth));
    }
    
    public static Stream<File> getFilesImage(File dir, int depth) {
        if(dir.isDirectory()) {
            try {
                return Files.walk(dir.toPath(),depth).map(Path::toFile)
                            .filter(ImageFileFormat::isSupported);
            } catch (IOException ex) {
                return Stream.empty();
            }
        }
        
        if(dir.isFile()) {
            if(ImageFileFormat.isSupported(dir))
                return Stream.of(dir);
        }
        
        return Stream.empty();
    }
    
    public static Stream<File> getFilesImage(List<File> files, int depth) {
        return files.stream().flatMap(f -> getFilesImage(f, depth));
    }
    
    /**
     * Constructs list of Images from provided file list. Filters out unsupported
     * types.
     * @param files
     * @return Empty if null or empty parameter or no results.
     */
    public static List<Image> FilesToImages(List<File> files) {
        List<Image> list = new ArrayList<>();
        for(File f: files) {
            if(ImageFileFormat.isSupported(f.toURI())) {
                Image img = new Image(f.toURI().toString());
                list.add(img);
            }
        }
        return list;
    }
    
    /**
     * Checks if there is at least one supported audio file in the list.
     * @param files
     * @return true if the list contains at least one supported audio file.
     */
    public static boolean containsAudioFiles(List<File> files, Use use) {
        for(File f : files) 
            if(AudioFileFormat.isSupported(f, use)) return true;
        return false;
    }
    
    static boolean containsImageFiles(List<File> files) {
        for(File f : files) 
            if(ImageFileFormat.isSupported(f)) return true;
        return false;
    }
    
    public static List<File> getImageFiles(List<File> files) {
        return files.stream().filter(isNotNULL)
                .filter(ImageFileFormat::isSupported)
                .collect(Collectors.toList());
    }
    
    /**
     * Returns common root directory for specified files.
     * 
     * @param files
     * @return common root
     */
    public static File getCommonRoot(Collection<File> files) {
            File d = null;
            for (File f : files) {
                if (f !=null) {
                    if(d==null) d = f;
                    if(d.toPath().compareTo(f.toPath())<0) d=f;
                }
            }
            return d==null ? null : d.isFile() ? d.getParentFile() : d;
    }
    
    /**
     * Returns name of the file without suffix. If the file denotes a directory
     * its name will be returned.
     * @param f
     * @return name of the file without suffix
     * @throws NullPointerException if parameter null
     */
    public static String getName(File f) {
        String whole_name = f.getName();
        int i = whole_name.lastIndexOf('.');
        return i==-1 ? whole_name : whole_name.substring(0,i);
    }
    /**
     * Returns name of the file without suffix denoted by this URI. This is just
     * the last name in the pathname's name sequence.
     * <p>
     * If the URI denotes a directory its name will be returned. If the uri doesnt denote
     * a file its path will still be parsed and last name in the pathname's
     * sequence will be attempted to be returned. Therefore if the URI denotes 
     * file accessed by http protocol the returned string will be the name of
     * the file without suffix - consistent result with file system based URIs.
     * However that doesnt have to be true for all schemes and URIs.
     * <p>
     * For file based URIs, this method is equivalent to 
     * {@link #getName(java.io.File)}.
     * <p>
     * If the path part of the URI is empty or null empty string will be returned.
     * @param u
     * @return name of the file without suffix
     * @throws NullPointerException if parameter null
     * @throws IllegalArgumentException if uri param scheme not file - if uri
     * doesnt represent a file
     */
    public static String getName(URI u) {
        String p = u.getPath();
        if(p==null || p.isEmpty()) return "";   // shouldnt happen ever, but just in case some badly damaged http URL gets through here
        int i = p.lastIndexOf('/');
        if(i==-1 || p.length()<2) return p;     // another exceptional state check
        p = p.substring(i+1);       // remove leading '/' character
        i = p.lastIndexOf('.');     // remove extension
        return (i==-1) ? p : p.substring(0, i);
    }
    
     /**
      * Writes a textual file with specified content, name and location. 
      * @param filepath Path to the file. The extension is part of the file. Do
      * not use .txt extension as it can cause problems with newline characters.
      * In case the file exists, it will be completely overwritten.
      * @param content Text that will be written to the file.
      * @throws RuntimeException when param is directory
      */
     public static void writeFile(String filepath, String content) {
        File file = new File(filepath);
        if (file.isDirectory()) throw new RuntimeException("File must not be directory.");
        Writer writer;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException ex) {
            Log.err("Couldnt save file: " + filepath);
        }
     }
     
     /**
      * Reads file as a text file and returns all its content as list of all lines,
      * with newlines removed. Joining the lines with '\n' will build the original
      * content.
      * @param filepath
      * @return List of lines or empty list (if empty or on error). Never null.
      */
     public static List<String> readFileLines(String filepath) {
        File file = new File(filepath);
        try {
            return Files.readAllLines(Paths.get(filepath), Charset.defaultCharset());
        } catch (IOException ex) {
            Log.err("Problems reading file " + file.getPath() + ". File wasnt read.");
            return new ArrayList<>();
        }
     }
     
     /**
      * Reads files as key-value storage. Empty lines or lines starting with '#'
      * (comment) will be ignored.
      * <pre>{@code
      * File format per line (input):
      *     "key : value"
      * Map of lines (output):
      *     <String key, String value>
      * }</pre>
      *
      * @param file file to read
      * @return map of key-value pairs
      */
     public static Map<String,String> readFileKeyValues(File file) {
        Map<String,String> m = new HashMap<>();
        readFileLines(file.getAbsolutePath())
             .forEach(line -> {
                String l = Util.emptifyString(line);
                if (!l.isEmpty() && !l.startsWith("#")) {
                    String key = l.substring(0, l.indexOf(" : "));
                    String value = l.substring(l.indexOf(" : ")+3);
                    m.put(key, value);                
                }    
             });
        return m;
     }
     
    public static void deleteFile(File file) {
        if (!file.exists()) return;
        try {
           boolean success = file.delete();
           if (!success) {
               Log.err("The file " + file.getPath() + " couldnt be deleted."
                       + " Will attempt to delete on application shutdown.");
               file.deleteOnExit();
           }
        } catch(SecurityException e) {
            Log.err("The file " + file.getPath() + " couldnt be deleted. "
                    + e.getMessage());
        }
    }
    
    /**
     * Saves image as a file, both being provided as parameters. If
     * the file is of type that is not supported by the application, the operation
     * will not take place.
     * @see ImageFileFormat for specifications
     * @param img 
     * @param file
     * @throws NullPointerException if any of the parameters null
     */
    public static void writeImage(Image img, File file) {
        Objects.requireNonNull(img);
        Objects.requireNonNull(file);
        
        ImageFileFormat t = ImageFileFormat.of(file.toURI());
        if (!t.isSupported()) {
            Log.err("Error during saving image " + file.getPath() + ". Format "
                    + t.name() + " not supported.");
            return;
        }
        
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", file);
        } catch (IOException e) {
            Log.err("Error during saving image " + file.getPath());
        }
    }
    
    /**
     * Copies provided items to the provided directory.
     * <p>
     * The method consumes I/O exception - that can occur when: an I/O error 
     * occurs when reading or writing.
     * <p>
     * If source file equals the file of its copy, the file will not be copied.
     * 
     * @param files
     * @param target
     * @param options optional. See {@link CopyOption} and Files.copy() methods
     * 
     * @return list of files representing the successfully created copies - all
     * copies that didnt throw IOException
     */
    public static List<File> copyFiles(List<File> files, File target, CopyOption... options) {
        List<File> out = new ArrayList(); 
        for(File f : files) {
            try {
                Path nf = target.toPath().resolve(f.toPath().getFileName());
                if(!nf.equals(f.toPath())) {
                    Files.copy(f.toPath(), nf, options);
                    out.add(new File(target, f.getName()));
                }
            } catch(IOException ex) {
                // ignore
            }
        }
        return out;
    }
    
    /**
     * <p>
     * If source file equals the file of its copy, the file will not be copied.
     * 
     * @param f
     * @param target
     * @param new_name
     * @param options 
     */
    public static void copyFile(File f, File target, String new_name, CopyOption... options) {
        try {
            File nf = new File(target, new_name + "." + getSuffix(f.toURI()));
            Files.copy(f.toPath(), nf.toPath(), options);
        } catch(IOException ex) {
            // ignore
        }
    }
    
    /**
     * Equivalent to {@link #copyFile(java.io.File, java.io.File, java.lang.String, java.nio.file.CopyOption...) },
     * but the copying will always take place and never overwrite existing file,
     * as if there is any, it is backed up by renaming, utilizing {@link #renameAsOld(java.io.File) }
     * <p>
     * If source file equals the file of its copy, the operation will not take place.
     * 
     * @param f
     * @param target
     * @param new_name
     * @param options 
     */
    public static void copyFileSafe(File f, File target, String new_name, CopyOption... options) {
        try {
            String suffix = FileUtil.getSuffix(f.toURI());
            String name = suffix.isEmpty() ? "cover" : "cover."+suffix;
            File nf = new File(target, new_name + "." + suffix);
            // avoid when files are the same (would produce nasty side effect of renaming
            // the file needlessly)
            if (f.equals(nf)) return;
            
            // backup old file
            FileUtil.renameAsOld(new File(target, name));
            // copy file
            Files.copy(f.toPath(), nf.toPath(), options);
        } catch(IOException ex) {
            // ignore
        }
    }
    
    /**
     * Copies file from given url, for example accessed over http protocol, into
     * new file on a local file system specified by the parameter.
     * 
     * @param url
     * @param destinationFile
     * @throws IOException when bad url or input or output file inaccessible
     */
    public static void saveFileAs(String url, File destinationFile) throws IOException {
        URL u = new URL(url);
        InputStream is = u.openStream();
        OutputStream os = new FileOutputStream(destinationFile);

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        is.close();
        os.close();
    }
    
    /**
     * Same as {@link #saveFileAs(java.lang.String, java.io.File)} but instead
     * destination directory is provided and the destination file will be put
     * there and named according to the input url file name.
     * @param url
     * @param destination
     * 
     * @return the file denoting the new file.
     * 
     * @throws IOException 
     */
    public static File saveFileTo(String url, File destination) throws IOException {
        int i = url.lastIndexOf('/');
        if(i==-1) throw new IOException("url does not contain name. No '/' character found.");
        String name = url.substring(1+i); 
        
        File df = new File(destination, name);
        saveFileAs(url, df);
        return df;
    }
    
    /**
     * Renames flle by sufixing it with a number, utilizing 
     * {@link #getFirstAvailableOld(java.io.File, java.lang.String, java.lang.String, int)}
     * 
     * @param f 
     */
    public static void renameAsOld(File f) {
        if(f!= null && f.exists()) {
            // remove name
            String suffix = getSuffix(f.toURI());
            f.renameTo(getFirstAvailableOld(f.getParentFile(), getName(f), suffix, 1));
        }
    }
    
    /**
     * For given directory, filename and file suffix, returns first available file
     * suffixed by an auto-incrementing decadic number.
     * <p>
     * Useful to avoid rewriting files on file move/copy.
     * 
     * @param location
     * @param name
     * @param suffix
     * @param i
     * @return 
     */
    public static File getFirstAvailableOld(File location, String name, String suffix, int i) {
        File f = new File(location, name + "-" + i + "."+suffix);
        if(f.exists()) return getFirstAvailableOld(location, name, suffix, i+1);
        else return f;
    }
    
    public static String getSuffix(URI f) {
        String n = f.getPath();
        int p = n.lastIndexOf('.');
        return (p == - 1) ? "" : n.substring(p + 1);
    }
    
    public static String getSuffix(String path) {
        int p = path.lastIndexOf('.');
        return (p == - 1) ? "" : path.substring(p + 1);
    }
    
    
    
    /**
     * Recursively deletes sub files and sub directories. along with the 
     * specified directory
     * @param dir 
     */
    public static void removeDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    removeDir(aFile);
                }
            }
            dir.delete();
        } else {
            dir.delete();
        }
    }
    
    /**
     * does not delete the main directory but all sub files and directories, 
     * results in the main directory being empty
     * @param dir 
     */
    public static void removeDirContent(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    removeDir(aFile);
                }
            }
        }
    }
}
