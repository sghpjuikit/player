/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.File;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import main.App;
import util.File.AudioFileFormat.Use;
import util.Util;

import static org.slf4j.LoggerFactory.getLogger;
import static util.Util.filenamizeString;
import static util.dev.Util.noØ;
import static util.functional.Util.ISNTØ;
import static util.functional.Util.stream;

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
            return false;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        if (!dir.canRead()) {
            return false;
        }
        if (!dir.canWrite()) {
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
            return false;
        }
        if (!dir.canRead())
            validity = dir.setReadable(true);
        if (!dir.canWrite())
            validity = dir.setWritable(true);

        return validity;
    }

    /**
     * @return true iff file
     * <ul>
     * <li> not null
     * <li> exists
     * <li> is file
     * <li> is readable
     * </ul>
     */
    public static boolean isValidFile(File file) {
        return file != null && file.isFile() && file.exists() && file.canRead();
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
        return (isValidFile(f) &&                   // is valid
                f.getPath().endsWith(".css") &&     // is .css
                f.equals(test));                    // is located in skins folder
    }

    public static boolean isValidWidgetFile(File f) {
        File p1 = f.getParentFile();
        File p2 = p1==null ? null : p1.getParentFile();
        return (isValidFile(f) &&                   // is valid file
                f.getPath().endsWith(".fxml") &&    // is .fxml file
                App.WIDGET_FOLDER().equals(p2));    // is located in skins folder in its rightful folder
    }

    /**
     * Same as {@link File#listFiles() }, but never returns null (instead, empty
     * list) and returns stream.
     * <p>
     * Normally, the method in File returns null if parameter is not a directory, but also when I/O
     * error occurs. For example when parameter refers to a directory on a non existent partition,
     * e.g., residing on hdd that has been disconnected temporarily. Returning null instead of
     * collection is never a good idea anyway!
     *
     * @throws SecurityException - If a security manager exists and its
     * SecurityManager.checkRead(String) method denies read access to the
     * directory
     *
     * @return unmodifiable list of files in the directory, it is empty if
     * parameter null, not a directory or I/O error occurs
     */
    public static Stream<File> listFiles(File dir) {
        File[] l = dir==null ? null : dir.listFiles();
        return l==null ? stream() : stream(l);
    }

    /**
     * Multiple parameter version of {@link #listFiles(java.io.File)} returning an union of the
     * respective results with no order guarantees.
     *
     * @param dirs
     * @return stream of children
     */
    public static Stream<File> listFiles(File... dirs) {
        return listFiles(stream(dirs));
    }

    /** Stream parameter version of {@link #listFiles(java.io.File...)}. */
    public static Stream<File> listFiles(Stream<File> dirs) {
        return dirs.filter(ISNTØ).flatMap(d -> listFiles(d));
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

    public static boolean containsAudioFileOrDir(List<File> files, Use use) {
        for(File f : files)
            if(f.isDirectory() || AudioFileFormat.isSupported(f, use)) return true;
        return false;
    }

    public static boolean containsImageFiles(List<File> files) {
        for(File f : files)
            if(ImageFileFormat.isSupported(f)) return true;
        return false;
    }

    public static List<File> getImageFiles(List<File> files) {
        return files.stream().filter(ISNTØ)
                .filter(ImageFileFormat::isSupported)
                .collect(Collectors.toList());
    }

    /**
     * Returns first common parent directory for specified files.
     *
     * @param files
     * @return common parent directory or null if list empty or its elements in
     * multiple partitions
     */
    public static File getCommonRoot(Collection<File> files) {
        int size = files.size();
        if(size==0) return null;
        if(size==1) return files.stream().findFirst().get();

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
     * For files name with no extension is returned.
     * For directories name is returned.
     * Root directory returns 'X:\' string.
     *
     * @param f
     * @return name of the file without suffix
     * @throws NullPointerException if parameter null
     */
    public static String getName(File f) {
        String n = f.getName();
        if(f.isDirectory()) return n;
        if(n.isEmpty()) return f.toString();
        int i = n.lastIndexOf('.');
        return i==-1 ? n : n.substring(0,i);
    }

    /**
     * For files 'filename.extension' is returned.
     * For directories only name is returned.
     * Root directory returns 'X:\' string.
     * <p>
     * Use instead of {@link File#getName()} which returns empty string for root
     * directories.
     *
     * @return name of the file with suffix
     */
    public static String getNameFull(File f) {
        String n = f.getName();
        return n.isEmpty() ? f.toString() : n;
    }

    /**
     * Returns name of the file without suffix denoted by this URI. This is just
     * the last name in the pathname's name sequence.
     * <p>
     * If the URI denotes a directory its name will be returned. If the uri doesnt denote
     * a file its path will still be parsed and last name in the pathname's
     * sequence will be attempted to be returned. Therefore if the URI denotes
     * file accessed by http protocol the returned string will be the name of
     * the file without suffix - consistent with file based URIs.
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
      *
      * @param filepath file to create. If exists, it will be overwritten.
      * Do not use .txt extension as it can cause problems with newline characters.
      * @param content Text that will be written to the file.
      * @return true if no IOException occurs else false
      * @throws RuntimeException when param is directory
      */
     public static boolean writeFile(String filepath, String content) {
          return writeFile(new File(filepath), content);
     }

     public static boolean writeFile(File file, String content) {
        if (file.isDirectory()) throw new RuntimeException("File must not be directory.");
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            return true;
        } catch (IOException e) {
            getLogger(Util.class).error("Couldnt save file: {}", file,e);
            return false;
        } finally {
            try {
                if(writer!=null) writer.close();
            } catch (IOException e) {
                getLogger(Util.class).error("Couldnt save fclose file writer", e);
            }
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
            return Files.readAllLines(Paths.get(filepath));
        } catch (IOException e) {
            getLogger(Util.class).error("Problems reading file {}. File wasnt read.", filepath,e);
            return new ArrayList<>();
        }
     }

     public static Stream<String> readFileLines(File f) {
        try {
            return Files.lines(f.toPath());
        } catch (IOException e) {
            getLogger(Util.class).error("Problems reading file {}. File wasnt read.", f,e);
            return Stream.empty();
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

    public static void deleteFile(File f) {
        if (!f.exists()) return;
        try {
           boolean success = f.delete();
           if (!success) {
               getLogger(Util.class).error("Coud not delete file {}. Will attempt to delete on app shutdown. ", f);
               f.deleteOnExit();
           }
        } catch(SecurityException e) {
            getLogger(Util.class).error("Coud not delete file {}", f,e);
        }
    }

    /**
     * Saves image as a file, both being provided as parameters. If
     * the file is of type that is not supported by the application, the operation
     * will not take place.
     * @see ImageFileFormat for specifications
     * @param img
     * @param f
     * @throws NullPointerException if any of the parameters null
     */
    public static void writeImage(Image img, File f) {
        noØ(img,f);

        ImageFileFormat t = ImageFileFormat.of(f.toURI());
        if (!t.isSupported()) {
            getLogger(Util.class).error("Could not save image to file {}. Format {} not supported.", f,t);
            return;
        }

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
        } catch (IOException e) {
            getLogger(Util.class).error("Could not save image to file {}", f,e);
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
     * @param files list of files
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
            } catch(IOException e) {
                getLogger(Util.class).error("Could not copy file {}", f,e);
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
        } catch(IOException e) {
            getLogger(Util.class).error("Could not copy file {}", f,e);
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
        } catch(IOException e) {
            getLogger(Util.class).error("Could not copy file {}", f,e);
        }
    }

    /**
     * Copies file from given url, for example accessed over http protocol, into
     * new file on a local file system specified by the parameter. Previously
     * existing file is removed.
     *
     * @param url
     * @param file
     * @throws IOException when bad url or input or output file inaccessible
     */
    public static void saveFileAs(String url, File file) throws IOException {
        if(file.exists()) file.delete();

        URL u = new URL(url);
        InputStream is = u.openStream();
        OutputStream os = new FileOutputStream(file);

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
     * @param dirdirectory
     *
     * @return the file denoting the new file.
     *
     * @throws IOException
     */
    public static File saveFileTo(String url, File dir) throws IOException {
        int i = url.lastIndexOf('/');
        if(i==-1) throw new IOException("url does not contain name. No '/' character found.");
        String name = url.substring(1+i);

        File df = new File(dir, name);
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

    public static String getSuffix(File f) {
        return getSuffix(f.getPath());
    }

    public static String getSuffix(URI f) {
        return getSuffix(f.getPath());
    }

    public static String getSuffix(String path) {
        int p = path.lastIndexOf('.');
        return (p == - 1) ? "" : path.substring(p + 1);
    }

    /** Deletes the file and if it denotes a directory, all its content too. */
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
     * Deletes content of the directory, but not directory itself. Does nothing
     * when not a directory.
     */
    public static void removeDirContent(File dir) {
        listFiles(dir).forEach(FileUtil::removeDir);
    }

    /**
     * Renames file (with extension suffix).
     *
     * @param f file to rename, if doesnt exist nothing happens
     * @param name new file name without suffix
     */
    public static void renameFile(File f, String name) {
        File rf = f.getParentFile().getAbsoluteFile();
        f.renameTo(new File(rf, filenamizeString(name)));
    };

    /**
     * Renames file (extension suffix remains the same).
     *
     * @param f file to rename, if doesnt exist nothing happens
     * @param name new file name without suffix
     */
    public static void renameFileNoSuffix(File f, String name) {
        File rf = f.getParentFile().getAbsoluteFile();
        int dot = f.getPath().lastIndexOf('.');
        String p = f.getPath();
        String ext = dot==-1 ? "" : p.substring(dot,p.length());
        f.renameTo(new File(rf, filenamizeString(name)+ext));
    };
}
