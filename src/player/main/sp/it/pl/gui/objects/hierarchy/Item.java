package sp.it.pl.gui.objects.hierarchy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javafx.scene.image.Image;
import sp.it.pl.audio.SimpleSong;
import sp.it.pl.gui.objects.image.cover.Cover.CoverSource;
import sp.it.util.HierarchicalBase;
import sp.it.util.JavaLegacy;
import sp.it.util.access.fieldvalue.CachingFile;
import sp.it.util.file.FileType;
import sp.it.util.functional.Try;
import sp.it.util.ui.IconExtractor;
import sp.it.util.ui.image.Image2PassLoader;
import sp.it.util.ui.image.ImageSize;
import static java.util.stream.Collectors.toList;
import static sp.it.pl.audio.tagging.SongReadingKt.read;
import static sp.it.pl.main.AppFileKt.getImageExtensionsRead;
import static sp.it.pl.main.AppFileKt.isAudio;
import static sp.it.pl.main.AppFileKt.isImage;
import static sp.it.util.dev.FailKt.failIfFxThread;
import static sp.it.util.dev.FailKt.failIfNotFxThread;
import static sp.it.util.file.FileType.DIRECTORY;
import static sp.it.util.file.FileType.FILE;
import static sp.it.util.file.UtilKt.getNameWithoutExtensionOrRoot;
import static sp.it.util.file.UtilKt.listChildren;
import static sp.it.util.functional.Try.Java.error;
import static sp.it.util.functional.Try.Java.ok;
import static sp.it.util.functional.Util.list;
import static sp.it.util.ui.image.UtilKt.toBuffered;
import static sp.it.util.ui.image.UtilKt.toFX;

/**
 * File wrapper, content of Cell with an image cover.<br/>
 * We cache various stuff in here, including the cover image and children files.
 */
public abstract class Item extends HierarchicalBase<File,Item> {

	private static final String COVER_STRATEGY_KEY = "coverStrategy";

	public final FileType valType;
	public Set<Item> children;              // filtered files
	public Set<String> all_children;        // all files, cache, use instead File.exists to reduce I/O
	public final AtomicBoolean cover_loadedThumb = new AtomicBoolean(false);
	public final AtomicBoolean cover_loadedFull = new AtomicBoolean(false);
	public volatile Image cover;            // cover cache
	public volatile File cover_file;        // cover file cache
	public volatile boolean coverFile_loaded;
	private volatile boolean disposed = false;
	public double loadProgress; // 0-1
	public double lastScrollPosition; // 0-1
	private HashMap<String, Object> properties = new HashMap<>();

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
		failIfNotFxThread();

		if (children!=null) children.forEach(Item::dispose);
		if (children!=null) children.clear();
		if (all_children!=null) all_children.clear();
		children = null;
		all_children = null;
		cover = null;
		disposed = true;
	}

	public void disposeChildren() {
		failIfNotFxThread();

		if (children!=null) children.forEach(Item::dispose);
		if (children!=null) children.clear();
		if (all_children!=null) all_children.clear();
		init();
	}

	private void buildChildren() {
		if (disposed) return;

		var all = new HashSet<String>();
		var dirs = new ArrayList<Item>();
		var files = new ArrayList<Item>();
		childrenFiles().forEach(f -> {
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
			var cs = new HashSet<Item>(dirs.size() + files.size());
			cs.addAll(dirs);
			cs.addAll(files);
			children = cs;
			all_children = all;
		}
	}

	protected Stream<File> childrenFiles() {
		return listChildren(value).map(CachingFile::new);
	}

	protected boolean filterChildFile(File f) {
		return true;
	}

	protected abstract Item createItem(Item parent, File value, FileType type);

	protected File getImage(File dir, String name) {
		if (disposed) return null;
		if (dir==null) return null;

		for (String ext : getImageExtensionsRead()) {
			File f = new File(dir, name + "." + ext);

			if (dir==value) {
				return file_exists(this, f) ? f : null;
			} else {
				if (parent!=null && parent.value!=null && parent.value.equals(f.getParentFile())) {
					if (file_exists(parent, f))
						return f;
				} else {
					if (f.exists())
						return f;
				}
			}
		}
		return null;
	}

	protected File getImageT(File dir, String name) {
		if (disposed) return null;
		if (dir==null) return null;

		for (String ext : getImageExtensionsRead()) {
			File f = new File(dir, name + "." + ext);
			if (file_exists(this, f)) return f;
		}
		return null;
	}

	public Try<LoadResult,Void> loadCover(boolean full, ImageSize size) {
		failIfFxThread();
		if (disposed) return error();

		File file = getCoverFile();
		if (file==null) {
			if (valType==DIRECTORY) {
				if (getCoverStrategy().useComposedDirCover) {
					var subcovers = children.stream()
						.filter(it -> it.valType==FILE)
						.map(it -> it.getCoverFile())
						.filter(it -> it!=null)
						.limit(4)
						.map(it -> Image2PassLoader.INSTANCE.getLq().invoke(it, size.div(2)))
						.filter(it -> it!=null)
						.collect(toList());
					var w = (int) size.width;
					var h = (int) size.height;
					var imgFin = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
					var imgFinGraphics = imgFin.getGraphics();
					var i = 0;
					for (Image img: subcovers) {
						var bi = img==null ? null : toBuffered(img);
						imgFinGraphics.drawImage(bi, w/2*(i%2), h/2*((i+1)%2), null);
						if (bi!=null) bi.flush();
						JavaLegacy.destroyImage(img);
						i++;
					}

					var coverToBe = toFX(imgFin);
					cover = coverToBe;
					cover_loadedFull.set(true);
					cover_loadedThumb.set(true);
					return ok(new LoadResult(null, coverToBe));
				}
			} else if (valType==FILE) {
				if (value.getPath().endsWith(".exe") || value.getPath().endsWith(".lnk")) {
					var coverToBe = IconExtractor.getFileIcon(value);
					cover = coverToBe;
					cover_loadedFull.set(true);
					cover_loadedThumb.set(true);
					if (coverToBe!=null) return ok(new LoadResult(null, coverToBe));
				}
				if (isAudio(value)) {
					var c = read(new SimpleSong(value)).getCover(CoverSource.ANY);
					var coverToBe = c.isEmpty() ? null : c.getImage(size);
					cover = coverToBe;
					cover_loadedFull.set(true);
					cover_loadedThumb.set(true);
					if (coverToBe!=null) return ok(new LoadResult(null, coverToBe));
				}
			}
		} else {
			if (full) {
				if (!cover_loadedFull.get()) {
					var coverToBe = Image2PassLoader.INSTANCE.getHq().invoke(file, size);
					cover = coverToBe;
					cover_loadedFull.set(true);
					if (coverToBe!=null) return ok(new LoadResult(file, coverToBe));
				}
			} else {
				if (!cover_loadedThumb.get()) {
					var coverToBe = Image2PassLoader.INSTANCE.getLq().invoke(file, size);
					cover = coverToBe;
					cover_loadedThumb.set(true);
					if (coverToBe!=null) return ok(new LoadResult(file, coverToBe));
				}
			}
		}

		if (full) cover_loadedFull.set(true);
		if (!full) cover_loadedThumb.set(true);
		return error();
	}

	// guaranteed to execute only once
	protected File getCoverFile() {
		if (disposed) return null;

		if (coverFile_loaded) return cover_file;
		coverFile_loaded = true;

		if (valType==DIRECTORY) {
			if (all_children==null) buildChildren();
			cover_file = getImageT(value, "cover");
		} else {
			if (cover_file==null && isImage(value)) {
				cover_file = value;
			}
			if (cover_file==null && getCoverStrategy().useParentCoverIfNone) {
				File i = getImage(value.getParentFile(), getNameWithoutExtensionOrRoot(value));
				if (i==null && parent!=null) cover_file = parent.getCoverFile();
				else cover_file = i;
			}
		}

		return cover_file;
	}

	@Override
	public List<Item> getHChildren() {
		return children();
	}

	private void property(String key, Object o) {
		failIfNotFxThread();
		var root = getHRoot();
		if (root.properties==null) root.properties = new HashMap<>();
		root.properties.put(key, o);
	}

	private Object property(String key) {
		var root = getHRoot();
		return root.properties==null ? null : root.properties.get(key);
	}

	public void setCoverStrategy(CoverStrategy coverStrategy) {
		property(COVER_STRATEGY_KEY, coverStrategy);
	}

	@SuppressWarnings("unsafe")
	public CoverStrategy getCoverStrategy() {
		var cs = (CoverStrategy) property(COVER_STRATEGY_KEY);
		return cs!=null ? cs : CoverStrategy.DEFAULT;
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

	public static class CoverStrategy {
		public static final CoverStrategy DEFAULT = new CoverStrategy(true, true);

		public boolean useParentCoverIfNone;
		public boolean useComposedDirCover;

		public CoverStrategy(boolean useComposedDirCover, boolean useParentCoverIfNone) {
			this.useParentCoverIfNone = useParentCoverIfNone;
			this.useComposedDirCover = useComposedDirCover;
		}
	}
}