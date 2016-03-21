package gui.objects.hierarchy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javafx.scene.image.Image;

import gui.objects.image.Thumbnail;
import unused.TriConsumer;
import util.Util;
import util.file.FileType;
import util.file.FileUtil;
import util.file.ImageFileFormat;

import static util.file.FileType.DIRECTORY;
import static util.file.FileUtil.listFiles;
import static util.functional.Util.list;

/**
 * File wrapper, content of Cell.
 * We cache various stuff in here, including the cover Image and children files.
 */
public abstract class Item {
    public final File val;
    public final FileType valtype;
    public final Item parent;
    public Set<Item> children = null;        // filtered files
    public Set<String> all_children = null;  // all files, cache, use instead File.exists to reduce I/O
    public Image cover = null;               // cover cache
    public File cover_file = null;           // cover file cache
    public boolean coverFile_loaded = false;
    public boolean cover_loadedThumb = false;
    public volatile boolean cover_loadedFull = false;
    public double last_gridposition = -1;

    public Item(Item parent, File value, FileType valtype) {
        this.val = value;
        this.valtype = valtype;
        this.parent = parent;
    }

    public List<Item> children() {
        if (children == null) buildChildren();
        return list(children);
    }

    public void dispose() {
        if(children!=null) children.forEach(Item::dispose);
        cover = null;
        if(children!=null) children.clear();
        if(all_children!=null) all_children.clear();
    }

    private void buildChildren() {
        all_children = new HashSet<>();
        children = new HashSet<>();
        List<Item> files = new ArrayList<>();
        children_files().forEach(f -> {
            all_children.add(f.getPath().toLowerCase());
            FileType type = FileType.of(f);
            if(type==DIRECTORY) {
                children.add(createItem(this, f, type));
            } else {
                if(filterChildFile(f))
                    files.add(createItem(this, f, type));
            }
        });
        children.addAll(files);
    }

    protected Stream<File> children_files() {
        return listFiles(val);
    }
    
    protected boolean filterChildFile(File f) {
        return true;
    }

    protected abstract Item createItem(Item parent, File value, FileType type);

    private File getImage(File dir, String name) {
        if(dir==null) return null;
        for(ImageFileFormat format: ImageFileFormat.values()) {
            if (format.isSupported()) {
                File f = new File(dir,name + "." + format.toString());
                if(dir==val ? file_exists(this,f) : file_exists(parent,f)) return f;
            }
        }
        return null;
    }

    private File getImageT(File dir, String name) {
        if(dir==null) return null;

        for(ImageFileFormat format: ImageFileFormat.values()) {
            if (format.isSupported()) {
                File f = new File(dir,name + "." + format.toString());
                if(file_exists(this,f)) return f;
            }
        }
        return null;
    }

    public void loadCover(boolean full, double width, double height, TriConsumer<Boolean,File,Image> action) {
        File file = getCoverFile();
        if(full) {
            boolean was_loaded = cover_loadedFull;
            if(!cover_loadedFull) {
                Image img = Util.loadImageFull(file, width, height);
                if(img!=null) {
                    cover = img;
                    action.accept(was_loaded,file,cover);
                }
                cover_loadedFull = true;
            }
        } else {
            boolean was_loaded = cover_loadedThumb;
            if(!cover_loadedThumb) {
                Image imgc = Thumbnail.getCached(file, width, height);
                cover = imgc!=null ? imgc : Util.loadImageThumb(file, width, height);
                cover_loadedThumb = true;
            }
            action.accept(was_loaded,file,cover);
        }
    }

    // guaranteed to execute only once
    protected File getCoverFile() {
        if(coverFile_loaded) return cover_file;
        coverFile_loaded = true;

        if(all_children==null) buildChildren();
        if(valtype== DIRECTORY) {
            cover_file = getImageT(val,"cover");
            return cover_file;
        } else {
            // image files are their own thumbnail
            if(ImageFileFormat.isSupported(val)) {
                cover_file = val;
                return cover_file;
            } else {
                File i = getImage(val.getParentFile(), FileUtil.getName(val));
                if(i==null && parent!=null) return parent.getCoverFile(); // return the parent image if available, needs some work
                cover_file = i;
                return cover_file;
            }
        }
    }


    private static boolean file_exists(Item c, File f) {
        return c!=null && f!=null && c.all_children.contains(f.getPath().toLowerCase());
    }
}
