package sp.it.pl.gui.objects.hierarchy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javafx.scene.image.Image;
import sp.it.pl.audio.SimpleSong;
import sp.it.pl.audio.tagging.MetadataReader;
import sp.it.pl.gui.objects.image.Thumbnail;
import sp.it.pl.gui.objects.image.cover.Cover.CoverSource;
import sp.it.pl.util.HierarchicalBase;
import sp.it.pl.util.access.fieldvalue.CachingFile;
import sp.it.pl.util.file.AudioFileFormat;
import sp.it.pl.util.file.AudioFileFormat.Use;
import sp.it.pl.util.file.FileType;
import sp.it.pl.util.file.ImageFileFormat;
import sp.it.pl.util.functional.Try;
import sp.it.pl.util.graphics.IconExtractor;
import sp.it.pl.util.graphics.image.Image2PassLoader;
import sp.it.pl.util.graphics.image.ImageSize;
import static sp.it.pl.util.dev.FailKt.failIfFxThread;
import static sp.it.pl.util.file.FileType.DIRECTORY;
import static sp.it.pl.util.file.FileType.FILE;
import static sp.it.pl.util.file.UtilKt.getNameWithoutExtensionOrRoot;
import static sp.it.pl.util.file.UtilKt.listChildren;
import static sp.it.pl.util.functional.Util.list;

/**
 * File wrapper, content of Cell with an image cover.<br/>
 * We cache various stuff in here, including the cover image and children files.
 */
public abstract class Item extends HierarchicalBase<File,Item> {

	public final FileType valType;
	public Set<Item> children;              // filtered files
	public Set<String> all_children;        // all files, cache, use instead File.exists to reduce I/O
	public Image cover;                     // cover cache
	public File cover_file;                 // cover file cache
	public boolean coverFile_loaded;
	public final AtomicBoolean cover_loadedThumb = new AtomicBoolean(false);
	public final AtomicBoolean cover_loadedFull = new AtomicBoolean(false);
	private volatile boolean disposed = false;  // TODO: this inherently can not work, use AtomicReference on fields
	public double loadProgress; // 0-1
	public double lastScrollPosition; // 0-1

	public Item(Item parent, File value, FileType valueType) {
		super(value, parent);
		this.valType = valueType;
		init();
	}

	public List<Item> children() {
		if (children==null) buildChildren();
		return children==null ? list() : list(children);
	}

	private void init() {
		children = null;
		all_children = null;
		cover = null;
		cover_file = null;
		coverFile_loaded = false;
		cover_loadedThumb.set(false);
		cover_loadedFull.set(false);
		loadProgress = 0;
		lastScrollPosition = -1;
	}

	public void dispose() {
		if (children!=null) children.forEach(Item::dispose);
		if (children!=null) children.clear();
		if (all_children!=null) all_children.clear();
		children = null;
		all_children = null;
		cover = null;
		disposed = true;
	}

	public void disposeChildren() {
		if (children!=null) children.forEach(Item::dispose);
		if (children!=null) children.clear();
		if (all_children!=null) all_children.clear();
		init();
	}

	private void buildChildren() {
		if (disposed) return;

		Set<String> all = new HashSet<>();
		List<Item> dirs = new ArrayList<>();
		List<Item> files = new ArrayList<>();
		children_files().forEach(f -> {
			if (!disposed) {
				all.add(f.getPath().toLowerCase());
				FileType type = FileType.of(f);
				if (type==DIRECTORY) {
					dirs.add(createItem(this, f, type));
				} else {
					if (filterChildFile(f)) files.add(createItem(this, f, type));
				}
			}
		});

		if (!disposed) {
			children = new HashSet<>(dirs.size() + files.size());
			children.addAll(dirs);
			children.addAll(files);
			all_children = all;
		}
	}

	protected Stream<File> children_files() {
		return listChildren(val).map(CachingFile::new);
	}

	protected boolean filterChildFile(File f) {
		return true;
	}

	protected abstract Item createItem(Item parent, File value, FileType type);

	protected File getImage(File dir, String name) {
		if (disposed) return null;
		if (dir==null) return null;

		for (ImageFileFormat format : ImageFileFormat.values()) {
			if (format.isSupported()) {
				File f = new File(dir, name + "." + format.toString());

				if (dir==val) {
					return file_exists(this, f) ? f : null;
				} else {
					if (parent!=null && parent.val!=null && parent.val.equals(f.getParentFile())) {
						if (file_exists(parent, f))
							return f;
					} else {
						if (f.exists())
							return f;
					}
				}
			}
		}
		return null;
	}

	protected File getImageT(File dir, String name) {
		if (disposed) return null;
		if (dir==null) return null;

		for (ImageFileFormat format : ImageFileFormat.values()) {
			if (format.isSupported()) {
				File f = new File(dir, name + "." + format.toString());
				if (file_exists(this, f)) return f;
			}
		}
		return null;
	}

	public Try<LoadResult,Void> loadCover(boolean full, ImageSize size) {
		failIfFxThread();
		if (disposed) return Try.error();

		boolean wasCoverFile_loaded = coverFile_loaded;
		File file = getCoverFile();
		if (file==null) {
			if (!wasCoverFile_loaded && cover_file==null && valType==FILE) {
				if (val.getPath().endsWith(".exe") || val.getPath().endsWith(".lnk")) {
					cover = IconExtractor.getFileIcon(val);
					cover_loadedFull.set(true);
					cover_loadedThumb.set(true);
					return Try.ok(new LoadResult(null, cover));
				}
				if (AudioFileFormat.isSupported(val, Use.APP)) {
					cover = MetadataReader.readMetadata(new SimpleSong(val)).getCover(CoverSource.TAG).getImage(size);
					cover_loadedFull.set(true);
					cover_loadedThumb.set(true);
					return Try.ok(new LoadResult(null, cover));
				}
			}
		} else {
			if (full) {
				if (!cover_loadedFull.get()) {
					Image img = Image2PassLoader.INSTANCE.getHq().invoke(file, size);
					if (img!=null) {
						cover = img;
						return Try.ok(new LoadResult(file, cover));
					}
					cover_loadedFull.set(true);
				}
			} else {
				boolean wasLoaded = cover_loadedThumb.get();
				if (!wasLoaded) {
					Image imgCached = Thumbnail.getCached(file, size);
					cover = imgCached!=null ? imgCached : Image2PassLoader.INSTANCE.getLq().invoke(file, size);
					cover_loadedThumb.set(true);
				}
				return Try.ok(new LoadResult(file, cover));
			}
		}
		return Try.error();
	}

	// guaranteed to execute only once
	protected File getCoverFile() {
		if (disposed) return null;

		if (coverFile_loaded) return cover_file;
		coverFile_loaded = true;

		if (valType==DIRECTORY) {
			if (all_children==null) buildChildren();
			cover_file = getImageT(val, "cover");
		} else {
			// image files are their own thumbnail
			if (ImageFileFormat.isSupported(val)) {
				cover_file = val;
			} else {
				File i = getImage(val.getParentFile(), getNameWithoutExtensionOrRoot(val));
				if (i==null && parent!=null) cover_file = parent.getCoverFile();
				else cover_file = i;
			}
		}

//	    if (cover_file==null)
//		    use icons if still no cover

		return cover_file;
	}

	@Override
	public List<Item> getHChildren() {
		return children();
	}

	private static boolean file_exists(Item c, File f) {
		return c!=null && f!=null && !c.disposed && c.all_children.contains(f.getPath().toLowerCase());
	}

	public static class LoadResult {
		public final File file;
		public final Image cover;

		public LoadResult(File file, Image cover) {
			this.file = file;
			this.cover = cover;
		}
	}

}