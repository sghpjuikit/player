package sp.it.pl.ui.objects.hierarchy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import javafx.scene.image.Image;
import kotlin.sequences.Sequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sp.it.pl.audio.SimpleSong;
import sp.it.pl.image.ImageLoader;
import sp.it.pl.image.ImageStandardLoader;
import sp.it.pl.ui.objects.grid.GridViewSkin;
import sp.it.pl.ui.objects.grid.ImageLoad;
import sp.it.pl.ui.objects.grid.ImageLoad.DoneErr;
import sp.it.pl.ui.objects.grid.ImageLoad.DoneInterrupted;
import sp.it.pl.ui.objects.grid.ImageLoad.DoneOk;
import sp.it.pl.ui.objects.grid.ImageLoad.Loading;
import sp.it.pl.ui.objects.grid.ImageLoad.NotStarted;
import sp.it.pl.ui.objects.image.Cover.CoverSource;
import sp.it.util.HierarchicalBase;
import sp.it.util.JavaLegacy;
import sp.it.util.access.fieldvalue.CachingFile;
import sp.it.util.async.future.Fut;
import sp.it.util.file.FileType;
import sp.it.util.file.UtilKt;
import sp.it.util.functional.Option;
import sp.it.util.functional.Option.None;
import sp.it.util.functional.Option.Some;
import sp.it.util.ui.IconExtractor;
import sp.it.util.ui.image.FitFrom;
import sp.it.util.ui.image.ImageSize;
import sp.it.util.ui.image.Interrupts;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static kotlin.io.FilesKt.getNameWithoutExtension;
import static kotlin.sequences.SequencesKt.forEach;
import static kotlin.sequences.SequencesKt.map;
import static sp.it.pl.audio.tagging.SongReadingKt.read;
import static sp.it.pl.main.AppFileKt.getImageExtensionsRead;
import static sp.it.pl.main.AppFileKt.isAudio;
import static sp.it.pl.main.AppFileKt.isImage;
import static sp.it.pl.main.AppFileKt.isVideo;
import static sp.it.pl.ui.objects.hierarchy.Item.CoverStrategy.VT_IMAGE;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.VT;
import static sp.it.util.async.AsyncKt.runOn;
import static sp.it.util.async.ExecutorExtensionsKt.limitParallelism;
import static sp.it.util.async.future.Fut.fut;
import static sp.it.util.dev.FailKt.failIfFxThread;
import static sp.it.util.dev.FailKt.failIfNotFxThread;
import static sp.it.util.file.FileType.DIRECTORY;
import static sp.it.util.file.FileType.FILE;
import static sp.it.util.functional.OptionKt.getOrSupply;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.ui.image.FitFrom.OUTSIDE;
import static sp.it.util.ui.image.UtilKt.toBuffered;
import static sp.it.util.ui.image.UtilKt.toFX;

/**
 * File wrapper, content of Cell with an image cover.<br/>
 * We cache various stuff in here, including the cover image and children files.
 */
public abstract class Item extends HierarchicalBase<File,Item> {

	public final @NotNull FileType valType;
	/** Children representing filtered files. Must not be accessed outside fx application thread. */
	protected volatile @Nullable List<Item> children = null;
	/** All file children. Super set of {@link #children}. Must not be accessed outside fx application thread. */
	protected volatile @Nullable Set<String> all_children = null;        // all files, cache, use instead File.exists to reduce I/O
	public volatile @NotNull ImageLoad cover = NotStarted.INSTANCE;
	protected volatile @NotNull Option<@Nullable File> coverFile = Option.Companion.invoke(null);
	protected volatile @Nullable Thread loadingThread = null;
	protected volatile boolean loadingPreInterrupted = false;
	protected volatile boolean disposed = false;
	protected final @NotNull HashMap<String, Object> properties = new HashMap<>();
	public double loadProgress = 0;         // 0-1
	public double lastScrollPosition = -1;  // -1 || 0-1
	public int lastSelectedChild = GridViewSkin.NO_SELECT;   // GridViewSkin.NO_SELECT || 0-N

	public Item(Item parent, File value, @NotNull FileType valueType) {
		super(value, parent);
		this.valType = valueType;
	}

	/** Returns children items. Evaluates children lazily at first invocation at most once */
	public @NotNull List<@NotNull Item> children() {
		if (children==null) children = computeChildren();
		return children;
	}

	/** Returns children items as are - null if not yet evaluated. See {@link #children()}. */
	public @Nullable List<@NotNull Item> childrenRO() {
		failIfNotFxThread();

		return children;
	}

	public void removeChild(@NotNull Item item) {
		failIfNotFxThread();

		if (disposed) return;
		if (children == null) return;
		children.remove(item);
		if (item.value!=null) all_children.remove(item.value.getName().toLowerCase());
	}

	/** Dispose of this as to never be used again. */
	public void dispose() {
		failIfNotFxThread();

		if (children!=null) children.forEach(Item::dispose);
		children = null;
		all_children = null;
		cover = DoneErr.INSTANCE;
		coverFile = Option.Companion.invoke(null);
		computeCoverInterrupt();
		loadingThread = null;
		disposed = true;
	}

	/** Dispose of the cover as to be able to load it again. */
	public void disposeCover() {
		failIfNotFxThread();

		cover = NotStarted.INSTANCE;
		coverFile = Option.Companion.invoke(null);
		computeCoverInterrupt();
		loadingThread = null;
		loadProgress = 0;
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
		children = null;
		all_children = null;
		cover = NotStarted.INSTANCE;
		coverFile = Option.Companion.invoke(null);
		loadProgress = 0;
		lastScrollPosition = -1;
		lastSelectedChild = GridViewSkin.NO_SELECT;
	}

	protected List<Item> computeChildren() {
		failIfFxThread();

		var all = new HashSet<String>();
		var dirs = new ArrayList<Item>();
		var files = new ArrayList<Item>();

		forEach(childrenFiles(), consumer(f -> {
			FileType type = FileType.Companion.invoke(f);
			if (type==DIRECTORY) {
				dirs.add(createItem(this, f, type));
			} else {
				all.add(f.getName().toLowerCase());
				if (filterChildFile(f)) files.add(createItem(this, f, type));
			}
		}));

		var css = new ArrayList<Item>(dirs.size() + files.size());
		css.addAll(dirs);
		css.addAll(files);

		if (!disposed) {
			children = css;
			all_children = all;
			return children;
		} else {
			return List.of();
		}
	}

	protected Sequence<File> childrenFiles() {
		return map(UtilKt.children(value), CachingFile::new);
	}

	protected boolean filterChildFile(File f) {
		return true;
	}

	protected abstract Item createItem(Item parent, File value, FileType type);

	protected File getImage(File dir, String name) {
		if (dir==null) return null;
		if (name==null) return null;
		var all_children = this.all_children;

		for (var ext: getImageExtensionsRead()) {
			var n = name + "." + ext;
			if (dir.equals(value)) {
				return file_exists(n, this, all_children)
					? new File(dir, n)
					: null;
			} else if (parent!=null && dir.equals(parent.value)) {
				if (file_exists(n, parent, parent.all_children))
					return new File(dir, n);
			} else {
				var f = new File(dir, n);
				if (f.exists()) return f;
			}
		}
		return null;
	}

	protected File getImageT(File dir, String name) {
		if (dir==null) return null;
		var all_children = this.all_children;

		for (var ext: getImageExtensionsRead()) {
			var n = name + "." + ext;
			if (file_exists(n, this, all_children))
				return new File(dir, n);
		}
		return null;
	}

	public void computeCoverInterrupt() {
		loadingPreInterrupted = true;
		Interrupts.INSTANCE.interrupt(loadingThread);
	}

	@SuppressWarnings("unchecked")
	public Fut<ImageLoad> computeCover(ImageSize size, FitFrom fit) {
		failIfNotFxThread();

		return switch (cover) {
			case DoneOk l -> fut(l);
			case DoneErr l -> fut(l);
			case Loading l -> l.getLoading();
			case NotStarted l -> computeCoverAsync(None.INSTANCE, size, fit);
			case DoneInterrupted l -> computeCoverAsync(l.getFileOpt(), size, fit);
			default -> throw new AssertionError("Illegal cover type " + cover.getClass());
		};
	}

	private Fut<ImageLoad> computeCoverAsync(Option<@Nullable File> imgFile, ImageSize size, FitFrom fit) {
		if (disposed) {
			cover = DoneErr.INSTANCE;
			return fut(DoneErr.INSTANCE);
		}
		loadingPreInterrupted = false;

		var strategy = getCoverStrategy();
		var cl = runOn(VT_IMAGE, () -> {
			loadingThread = Thread.currentThread();
			if (loadingPreInterrupted) return new DoneInterrupted(imgFile);
			if (Interrupts.INSTANCE.isInterrupted()) return new DoneInterrupted(imgFile);

			var ci = (Image) null;
			var cf = getOrSupply(imgFile, () -> computeCoverFile(strategy));
			if (Interrupts.INSTANCE.isInterrupted()) return new DoneInterrupted(new Some<>(cf));

			try {
				var ch = children;
				if (cf!=null) {
					ci = strategy.loader.invoke(cf, size, fit);
				} else {
					if (valType==DIRECTORY) {
						if (strategy.useComposedDirCover) {
							List<Image> subCovers = ch==null ? list() : ch.stream()
								.filter(it -> it.valType==FILE)
								.map(it -> it.computeCoverFile(strategy))
								.filter(it -> it!=null)
								.limit(4)
								.map(it -> strategy.loader.invoke(it, size.div(2), OUTSIDE, true))
								.filter(it -> it!=null)
								.toList();
							var w = (int) size.width;
							var h = (int) size.height;
							var imgFin = new BufferedImage(w, h, TYPE_INT_ARGB);
							var imgFinGraphics = imgFin.getGraphics();
							var i = 0;
							for (var img : subCovers) {
								var bi = img==null ? null : toBuffered(img);
								imgFinGraphics.drawImage(bi, w/2*(i%2), h/2*(i/2), null);
								if (bi!=null) bi.flush();
								JavaLegacy.destroyImage(img);
								i++;
							}
							ci = toFX(imgFin);
						}
					} else if (valType==FILE) {
						if (isVideo(value) && strategy.useVideoFrameCover) {
							ci = strategy.loader.invoke(value, size, fit);
						} else if (isAudio(value)) {
							var c = read(new SimpleSong(value)).getCover(CoverSource.ANY);
							ci = c.isEmpty() ? null : c.getImage(size, fit);
						} else if (value.getPath().endsWith(".pdf")) {
							ci = strategy.loader.invoke(value, size, fit);
						} else if (strategy.useNativeIconIfNone) {
							ci = IconExtractor.INSTANCE.getFileIcon(value);
						}
					}
				}

				if (ci==null && Interrupts.INSTANCE.isInterrupted()) return new DoneInterrupted(new Some<>(cf));
				return new DoneOk(ci, cf);
			} catch (Throwable e) {
				if (Interrupts.INSTANCE.isInterrupted()) return new DoneInterrupted(new Some<>(cf));
				else return DoneErr.INSTANCE;
			}
		}).then(FX, it -> {
			Interrupts.INSTANCE.dispose(loadingThread);
			loadingThread = null;
			cover = it;
			return it;
		});
		cover = new Loading(cl);
		return cl;
	}

	protected @Nullable File computeCoverFile(CoverStrategy strategy) {
		failIfFxThread();

		if (coverFile instanceof Some<@Nullable File> f) {
			return f.getValue();
		} else {
			var cf = (File) null;

			if (valType==DIRECTORY) {
				if (children==null) computeChildren();
				cf = getImageT(value, "cover");
			} else {
				if (cf==null && isImage(value)) {
					cf = value;
				}
				if (cf==null) {
					File i;
						i = getImage(value.getParentFile(), getNameWithoutExtension(value));
					if (i==null && parent!=null && strategy.useParentCoverIfNone)
						i = parent.computeCoverFile(strategy);
					if (i!=null)
						cf = i;
				}
				if (cf==null && isVideo(value) && getHParent()!=null) {
					var n = getNameWithoutExtension(value);
					if (n.endsWith(")")) {
						var i = n.lastIndexOf("(");
						var coverName = i==-1 ? null : n.substring(0, i) + "(BQ)";
						cf = getImage(value.getParentFile(), coverName);
					}
				}
			}

			coverFile = new Some<>(cf);
			return cf;
		}

	}

	@Override
	public List<Item> getHChildren() {
		return children();
	}

	@SuppressWarnings("SameParameterValue")
	private void property(String key, Object o) {
		failIfNotFxThread();

		var root = getHRoot();
		root.properties.put(key, o);
	}

	@SuppressWarnings("SameParameterValue")
	private Object property(String key) {
		failIfNotFxThread();

		var root = getHRoot();
		return root.properties==null ? null : root.properties.get(key);
	}

	public void setCoverStrategy(CoverStrategy coverStrategy) {
		property("coverStrategy", coverStrategy);
	}

	@SuppressWarnings("unsafe")
	public CoverStrategy getCoverStrategy() {
		var cs = (CoverStrategy) property("coverStrategy");
		return cs!=null ? cs : CoverStrategy.DEFAULT;
	}

	private static boolean file_exists(@NotNull String f, @NotNull Item c, @Nullable Set<String> all_children) {
		return all_children!=null && all_children.contains(f);
	}

	public static final class CoverStrategy {
		/** Enable using parent directory cover if file has no cover */
		public final boolean useParentCoverIfNone;
		/** Enable using directory covers composed of child file covers */
		public final boolean useComposedDirCover;
		/** Enables using small native OS icon for the file cover. Has no effect if {@link #useParentCoverIfNone} is true. */
		public final boolean useNativeIconIfNone;
		/** Enables video frame extraction to display cover for video files */
		public final boolean useVideoFrameCover;
		/** Enables {@link ImageLoader#memoized(java.util.UUID)} */
		public final @Nullable UUID diskCache;
		/** The actual cover image loader */
		private final ImageLoader loader;

		public CoverStrategy(boolean useParentCoverIfNone, boolean useComposedDirCover, boolean useNativeIconIfNone, boolean useVideoFrameCover, @Nullable UUID diskCache) {
			this.useParentCoverIfNone = useParentCoverIfNone;
			this.useComposedDirCover = useComposedDirCover;
			this.useNativeIconIfNone = useNativeIconIfNone;
			this.useVideoFrameCover = useVideoFrameCover;
			this.diskCache = diskCache;
			var loaderRaw = ImageStandardLoader.INSTANCE;
			loader = diskCache!=null ? loaderRaw.memoized(diskCache) : loaderRaw;
		}

		public static final CoverStrategy DEFAULT = new CoverStrategy(true, true, false, true, null);
		public static final Executor VT_IMAGE = limitParallelism(VT, 6);
	}
}