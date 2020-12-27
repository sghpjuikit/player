package sp.it.pl.ui.objects.hierarchy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.image.Image;
import kotlin.Unit;
import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sp.it.pl.audio.SimpleSong;
import sp.it.pl.ui.objects.image.Cover.CoverSource;
import sp.it.pl.image.Image2PassLoader;
import sp.it.util.HierarchicalBase;
import sp.it.util.JavaLegacy;
import sp.it.util.access.fieldvalue.CachingFile;
import sp.it.util.async.future.Fut;
import sp.it.util.dev.ThreadSafe;
import sp.it.util.file.FileType;
import sp.it.util.file.UtilKt;
import sp.it.util.functional.Util;
import sp.it.util.ui.IconExtractor;
import sp.it.util.ui.image.ImageSize;
import static java.util.stream.Collectors.toList;
import static kotlin.io.FilesKt.getNameWithoutExtension;
import static kotlin.streams.jdk8.StreamsKt.asStream;
import static sp.it.pl.audio.tagging.SongReadingKt.read;
import static sp.it.pl.main.AppFileKt.getImageExtensionsRead;
import static sp.it.pl.main.AppFileKt.isAudio;
import static sp.it.pl.main.AppFileKt.isImage;
import static sp.it.pl.main.AppFileKt.isVideo;
import static sp.it.util.async.AsyncKt.runIO;
import static sp.it.util.dev.FailKt.failIfFxThread;
import static sp.it.util.dev.FailKt.failIfNotFxThread;
import static sp.it.util.file.FileType.DIRECTORY;
import static sp.it.util.file.FileType.FILE;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.ui.image.UtilKt.toBuffered;
import static sp.it.util.ui.image.UtilKt.toFX;

/**
 * File wrapper, content of Cell with an image cover.<br/>
 * We cache various stuff in here, including the cover image and children files.
 */
public abstract class Item extends HierarchicalBase<File,Item> {

	private static final String COVER_STRATEGY_KEY = "coverStrategy";

	public final @NotNull FileType valType;
	/** Children representing filtered files. Must not be accessed outside fx application thread. */
	protected volatile List<Item> children = null;
	/** All children files. Super set of {@link #children}. Must not be accessed outside fx application thread. */
	protected volatile Set<String> all_children = null;        // all files, cache, use instead File.exists to reduce I/O
	public volatile Fut<Unit> coverLoading = null;
	public volatile Image cover = null;           // cover cache
	public volatile File coverFile = null;        // cover file cache
	private volatile boolean coverFileLoaded = false;
	private volatile boolean disposed = false;
	public double loadProgress = 0;         // 0-1
	public double lastScrollPosition = -1;  // -1
	public int lastSelectedChild = -1;   // -1-N
	private HashMap<String, Object> properties = new HashMap<>();

	public Item(Item parent, File value, FileType valueType) {
		super(value, parent);
		this.valType = valueType;
	}

	/** Returns children items. Evaluates children lazily at first invocation at most once */
	public @NotNull List<@NotNull Item> children() {
		if (children==null) buildChildren();
		return children==null ? list() : list(children);
	}

	/** Returns children items as are - null if not yet evaluated. See {@link #children()}. */
	public @Nullable List<@NotNull Item> childrenRO() {
		return children;
	}

	public void removeChild(Item item) {
		failIfNotFxThread();
		if (disposed) return;
		if (children == null) return;
		children.remove(item);
		all_children.remove(item);
	}

	/** Dispose of this as to never be used again. */
	public void dispose() {
		failIfNotFxThread();

		if (children!=null) children.forEach(Item::dispose);
		if (children!=null) children.clear();
		if (all_children!=null) all_children.clear();
		children = null;
		all_children = null;
		cover = null;
		coverFile = null;
		coverFileLoaded = false;
		coverLoading = null;
		disposed = true;
	}

	/** Dispose of the cover as to be able to load it again. */
	public void disposeCover() {
		failIfNotFxThread();
		cover = null;
		coverLoading = null;
	}

	/** Dispose of this as to be fully usable, but children will be {@link #disposeContent()}. */
	public void disposeChildrenContent() {
		failIfNotFxThread();

		if (children!=null) children.forEach(Item::disposeContent);
	}

	/** Dispose of this as to be usable again. Children will be {@link #dispose()}. */
	public void disposeContent() {
		failIfNotFxThread();

		if (children!=null) children.forEach(Item::dispose);
		if (children!=null) children.clear();
		children = null;
		if (all_children!=null) all_children.clear();
		all_children = null;
		cover = null;
		coverFile = null;
		coverFileLoaded = false;
		coverLoading = null;
		loadProgress = 0;
		lastScrollPosition = -1;
		lastSelectedChild = -1;
	}

	private void buildChildren() {
		if (disposed) return;

		var all = new HashSet<String>();
		var dirs = new ArrayList<Item>();
		var files = new ArrayList<Item>();
		asStream(childrenFiles()).forEach(f -> {
			if (!disposed) {
				all.add(f.getPath().toLowerCase());
				FileType type = FileType.Companion.invoke(f);
				if (type==DIRECTORY) {
					dirs.add(createItem(this, f, type));
				} else {
					if (filterChildFile(f)) files.add(createItem(this, f, type));
				}
			}
		});

		if (!disposed) {
			var cs = new ArrayList<Item>(dirs.size() + files.size());
			cs.addAll(dirs);
			cs.addAll(files);
			children = cs;
			all_children = all;
		}
	}

	protected Sequence<File> childrenFiles() {
		return SequencesKt.map(UtilKt.children(value), CachingFile::new);
	}

	protected boolean filterChildFile(File f) {
		return true;
	}

	protected abstract Item createItem(Item parent, File value, FileType type);

	protected File getImage(File dir, String name) {
		if (disposed) return null;
		if (dir==null) return null;
		if (name==null) return null;

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

	@ThreadSafe
	public boolean isLoadedCover() {
		failIfNotFxThread();
		var cl = coverLoading;
		return cl!=null && cl.isDone();
	}

	public Fut<Unit> loadCover(ImageSize size) {
		if (disposed) {
			var f = Fut.fut();
			coverLoading = Fut.fut();
			return f;
		}

		var cl = coverLoading;
		if (cl!=null) {
			return cl;
		} else {
			var f = runIO(runnable(() -> {
				File file = getCoverFile();
				if (file==null) {
					var strategy = getCoverStrategy();
					if (valType==DIRECTORY) {
						if (strategy.useComposedDirCover) {
							var subCovers = (children==null ? Util.<Item>list() : list(children)).stream()
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
							for (Image img: subCovers) {
								var bi = img==null ? null : toBuffered(img);
								imgFinGraphics.drawImage(bi, w/2*(i%2), h/2*(i/2), null);
								if (bi!=null) bi.flush();
								JavaLegacy.destroyImage(img);
								i++;
							}
							cover = toFX(imgFin);
						}
					} else if (valType==FILE) {
						if (isVideo(value) && getCoverStrategy().useVideoFrameCover) {
							cover = Image2PassLoader.INSTANCE.getLq().invoke(value, size);
						} else if (isAudio(value)) {
							var c = read(new SimpleSong(value)).getCover(CoverSource.ANY);
							cover = c.isEmpty() ? null : c.getImage(size);
						} else if (strategy.useNativeIconIfNone)  {
							cover = IconExtractor.INSTANCE.getFileIcon(value);
						}
					}
				} else {
					cover = Image2PassLoader.INSTANCE.getLq().invoke(file, size);
				}
			}));

			coverLoading = f;
			return f;
		}

	}

	protected File getCoverFile() {
		failIfFxThread();
		if (disposed) return null;

		if (coverFileLoaded) return coverFile;
		coverFileLoaded = true;

		if (valType==DIRECTORY) {
			if (children==null) buildChildren();
			coverFile = getImageT(value, "cover");
		} else {
			if (coverFile==null && isImage(value)) {
				coverFile = value;
			}
			if (coverFile==null) {
				File i = null;
				if (i==null)
					i = getImage(value.getParentFile(), getNameWithoutExtension(value));
				if (i==null && parent!=null && getCoverStrategy().useParentCoverIfNone)
					i = parent.getCoverFile();
				coverFile = i;
			}
			if (coverFile==null && isVideo(value) && getHParent()!=null) {
				var n = getNameWithoutExtension(value);
				if (n.endsWith(")")) {
					var i = n.lastIndexOf("(");
					var coverName = i==-1 ? null : n.substring(0, i) + "(BQ)";
					coverFile = getImage(value.getParentFile(), coverName);
				}
			}
		}

		return coverFile;
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

	public static class CoverStrategy {
		public static final CoverStrategy DEFAULT = new CoverStrategy(true, true, false, true);

		public boolean useParentCoverIfNone;
		public boolean useComposedDirCover;
		/** Has no effect if {@link #useParentCoverIfNone} is true. */
		public boolean useNativeIconIfNone;
		public boolean useVideoFrameCover = true;

		public CoverStrategy(boolean useComposedDirCover, boolean useParentCoverIfNone, boolean useNativeIconIfNone, boolean useVideoFrameCover) {
			this.useParentCoverIfNone = useParentCoverIfNone;
			this.useComposedDirCover = useComposedDirCover;
			this.useNativeIconIfNone = useNativeIconIfNone;
			this.useVideoFrameCover = useVideoFrameCover;
		}
	}
}