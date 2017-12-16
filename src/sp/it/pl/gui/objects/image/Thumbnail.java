package sp.it.pl.gui.objects.image;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import sp.it.pl.gui.objects.contextmenu.ImprovedContextMenu;
import sp.it.pl.gui.objects.image.cover.Cover;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.ref.SingleR;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfigurable;
import sp.it.pl.util.dev.Dependency;
import sp.it.pl.util.file.ImageFileFormat;
import sp.it.pl.util.graphics.image.ImageSize;
import sp.it.pl.util.graphics.image.ImageStandardLoader;
import static java.lang.Double.min;
import static java.util.stream.Collectors.toList;
import static javafx.scene.input.DataFormat.FILES;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.file.UtilKt.toFileOrNull;
import static sp.it.pl.util.functional.Util.ISNTØ;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.UtilKt.setScaleXYByTo;
import static sp.it.pl.util.reactive.Util.doOnceIf;
import static sp.it.pl.util.type.Util.getFieldValue;

/**
 * Thumbnail.
 * <p/>
 * Resizable image container with additional features.
 * <p/>
 * Features:
 * <ul>
 * <li> Resizable. The image resizes in layout automatically. For manual resize
 * set preferred, minimal and maximal size of the root {@link #getPane()}.
 * <p/>
 * This size is applied on the root of this thumbnail, which contains the image.
 * The image will try to use maximum space available, depending on the aspect
 * ratios of the image and this thumbnail.
 * <p/>
 * The image retains aspect ratio and is always fully visible inside this thumbnail.
 * If the aspect ratio of this thumbnail differs from that of the image, there
 * will be empty space left on left+right or top+bottom. Image is center - aligned.
 * <li> Data aware. Object the image is representing (file or any domain object, i.e.: album) can be set and queried.
 * <li> File aware. The underlying file of the image (if available) can be provided.
 * <li> Aspect ratio aware. See {@link #fitFrom}, {@link #ratioTHUMB} and {@link #ratioIMG}. Used for layout.
 * <li> Resolution aware. The images are loaded only up to required size to reduce memory, see
 * {@link #calculateImageLoadSize() }.
 * <li> Drag support. Dragging the image will put the the file in the
 * clipboard (if it is available).
 * <li> Background can be used. Background is visible on the empty space resulting
 * from different aspect ratio of this thumbnail and image.
 * <p/>
 * Define background style in css.
 * <li> Border. Can be set to frame whole thumbnail or image respectively. Framing
 * whole thumbnail will frame also the empty areas and background.
 * Useful for small thumbnails to give them 'rectangular' size by setting both
 * background and border framing to whole thumbnail. Big image may do the opposite
 * - display no background and frame image only.
 * <p/>
 * Define border style in css.
 * <li> Context menu. Shown on right click. Has additional menu items if the
 * file is set.
 * <p/>
 * Image can be opened in native application, edited in native editor,
 * browsed in native file system, exported as file or viewed fullscreen inside
 * the application, and more.
 * </ul>
 */
@IsConfigurable("Images")
public class Thumbnail {

	private static final String styleclassBgr = "thumbnail";
	private static final String styleclassBorder = "thumbnail-border";
	private static final String styleclassImage = "thumbnail-image";

	@IsConfig(name = "Image caching", info = "Will keep every loaded image in "
		+ "memory. Reduces image loading (except for the first time) but "
		+ "increases memory. For large images (around 5000px) the effect "
		+ "is very noticeable. Not worth it if you do not browse large images "
		+ "or want to minimize RAM usage.")
	public static boolean cache_images = false;

	@IsConfig(name = "Thumbnail anim duration", info = "Preferred hover scale animation duration for thumbnails.")
	public static Duration animDur = millis(100);

	protected final ImageView imageView = new ImageView();
	protected final StackPane root = new StackPane(imageView) {
		@SuppressWarnings("UnnecessaryLocalVariable")
		@Override
		protected void layoutChildren() {
			// lay out image
			double W = getWidth();
			double H = getHeight();
			double imgW = min(W, maxImgW.get());
			double imgH = min(H, maxImgH.get());

			applyViewPort(imageView.getImage());

			// fixes scaling when one side with FitFrom.OUTSIDE when one side of the image is smaller than thumbnail
			boolean needsScaleH = !(isImgSmallerW && fitFrom.get()==FitFrom.OUTSIDE);
			boolean needsScaleW = !(isImgSmallerH && fitFrom.get()==FitFrom.OUTSIDE);

			// resize thumbnail
			if (needsScaleH) imageView.setFitWidth(imgW);
			if (needsScaleW) imageView.setFitHeight(imgH);


			// lay out other children (maybe introduced in subclass)
			super.layoutChildren();

			// lay out border
			if (isBorderVisible) {
				if (borderToImage && imageView.getImage()!=null) {
					if (ratioIMG.get()>ratioTHUMB.get()) {
						double borderW = imgW;
						double borderWGap = (W - borderW)/2;
						double borderH = imgW/ratioIMG.get();
						double borderHGap = (H - borderH)/2;
						border.resizeRelocate(borderWGap, borderHGap, borderW, borderH);
					} else {
						double borderW = imgH*ratioIMG.get();
						double borderWGap = (W - borderW)/2;
						double borderH = imgH;
						double borderHGap = (H - borderH)/2;
						border.resizeRelocate(borderWGap, borderHGap, borderW, borderH);
					}
				} else {
					border.resizeRelocate(0, 0, W, H);
				}
			}
		}
	};

	/**
	 * Displayed image. Editable, but it is recommended to use one of the load
	 * methods instead. Note, that those load the image on bgr thread and setting
	 * this property is delayed until the image fully loads. Until then this
	 * thumbnail will keep showing the previous image and this property will
	 * reflect that. Thus calling getM() on this property may not provide the
	 * expected result.
	 */
	public final ObjectProperty<Image> image = imageView.imageProperty();
	private File imageFile = null;
	public final V<FitFrom> fitFrom = new V<>(FitFrom.INSIDE);
	private boolean isImgSmallerW = false;
	private boolean isImgSmallerH = false;

	/**
	 * Constructor.
	 * Use if you need  default thumbnail size and the image is expected to
	 * change during life cycle.
	 */
	public Thumbnail() {
		this(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
	}

	/**
	 * Use when you want to use default sized thumbnail no post-initial changes
	 * of the image are expected. In other words situations, where thumbnail object
	 * is viewed as immutable create-destroy type.
	 *
	 * @param img initial image, null if none
	 */
	public Thumbnail(Image img) {
		this(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
		loadImage(img);
	}

	/**
	 * Use if you need different size than default thumbnail size and the image
	 * is expected to change during life cycle.
	 */
	public Thumbnail(double width, double height) {
		root.setMinSize(width, height);
		root.setPrefSize(width, height);
		root.setMaxSize(width, height);
		imageView.getStyleClass().add(styleclassImage);
		imageView.setFitHeight(-1);
		imageView.setFitWidth(-1);

		// update ratios
		ratioTHUMB.bind(root.widthProperty().divide(root.heightProperty()));
		imageView.imageProperty().addListener((o, ov, nv) ->
				ratioIMG.set(nv==null ? 1 : nv.getWidth()/nv.getHeight())
		);

		fitFrom.addListener((o, ov, nv) -> applyViewPort(imageView.getImage()));

		// initialize values
//        imageView.setCache(false);
		imageView.setSmooth(true);
		imageView.setPreserveRatio(true);
		setBorderToImage(false);
		setBorderVisible(false);
		setBackgroundVisible(true);
		setDragEnabled(true);
		setContextMenuOn(true);
		root.addEventFilter(MOUSE_ENTERED, e -> animationPlay());
		root.addEventFilter(MOUSE_EXITED, e -> animationPause());
	}

	public final ObservableList<String> getStyleClass() {
		return root.getStyleClass();
	}

	/** Loads image and sets it to show, null sets no image. */
	public void loadImage(Image img) {
		imageFile = null;
		setImgA(img);
	}

	public void loadImage(Image img, File imageFile) {
		loadImage(img);
		this.imageFile = imageFile;
	}

	public void loadImage(File img) {
		imageFile = img;
		ImageSize size = calculateImageLoadSize();
		Image c = getCached(img, size);
		Image i = c!=null ? c : ImageStandardLoader.INSTANCE.invoke(img, size);
		setImgA(i);
	}

	public void loadImage(Cover img) {
		imageFile = img==null ? null : img.getFile();
		if (img==null) {
			setImgA(null);
		} else {
			ImageSize size = calculateImageLoadSize();
			Image i = img.getImage(size);
			setImgA(i);
		}
	}

	private long loadId = 0;    // prevents wasteful set image operations
	private static Map<String,Image> IMG_CACHE = new ConcurrentHashMap<>();   // caches images

	public static Image getCached(String url, double w, double h) {
		Image ci = url==null ? null : IMG_CACHE.get(url);
		return ci!=null && (ci.getWidth()>=w || ci.getHeight()>=h) ? ci : null;
	}

	public static Image getCached(File file, ImageSize size) {
		return getCached(file, size.width, size.height);
	}

	public static Image getCached(File file, double w, double h) {
		String url;
		try {
			url = file.toURI().toURL().toString();
		} catch (Exception e) {
			url = null;
		}
		return getCached(url, w, h);
	}

	// set asynchronously
	private void setImgA(Image i) {
		loadId++;   // load next image
		final long id = loadId; // expected id (must match when load finishes)
		if (i==null) {
			setImg(null, id);
		} else {
			doOnceIf(i.progressProperty(), p -> p.doubleValue()==1, p -> setImg(i, id));
		}
	}

	// set synchronously
	private void setImg(Image i, long id) {
		// cache
		if (i!=null && cache_images) {
			Image ci = IMG_CACHE.get(i.getUrl());
			if (ci==null || ci.getHeight()*ci.getWidth()<i.getHeight()*i.getWidth())
				IMG_CACHE.put(i.getUrl(), i);
		}

		// ignore outdated loadings
		if (id!=loadId) return;

		applyViewPort(i);
		imageView.setImage(i);

		if (i!=null) {
			maxImgW.set(i.getWidth());
			maxImgH.set(i.getHeight());
		}

		root.layout();

		// animation
		if (i!=null) {
			Object animWrapper = getFieldValue(i, "animation");
			animation = animWrapper==null ? null : getFieldValue(animWrapper, "timeline");
			animationPause();
		}
	}

	/**
	 * Calculates size of the image to load. Returns recommended size for the image.
	 * <p/>
	 * The image normally loads 1:1 with the resolution size of the file, but it
	 * is often wasteful, particularly for big images and even more if the size
	 * the image will be used with is rather small.
	 * <p/>
	 * In order to limit memory consumption
	 * the size of the specified component will be assumed to be an upper bound
	 * of image's loaded size.
	 *
	 * @return recommended size of the image
	 */
	public ImageSize calculateImageLoadSize() {
		// sample both size and prefSize to avoid getting 0 when source not yet initialized (part of scene graph)
		double w = APP.windowManager.screenMaxScaling*Math.max(root.getWidth(), root.getPrefWidth());
		double h = APP.windowManager.screenMaxScaling*Math.max(root.getHeight(), root.getPrefHeight());
		return new ImageSize(w, h);
	}

/* ---------- VIEWPORT ---------------------------------------------------------------------------------------------- */

	private void applyViewPort(Image i) {
		if (i!=null) {
			if (fitFrom.get()==FitFrom.INSIDE) {
				imageView.setViewport(null);
			} else {
				isImgSmallerW = i.getWidth()<=imageView.getLayoutBounds().getWidth();
				isImgSmallerH = i.getHeight()<=imageView.getLayoutBounds().getHeight();
				if (ratioTHUMB.get()<ratioIMG.get()) {
					double uiImgWidth = i.getHeight()*ratioTHUMB.get();
					double x = (i.getWidth() - uiImgWidth)/2;
					imageView.setViewport(new Rectangle2D(x, 0, uiImgWidth, i.getHeight()));
				} else
				if (ratioTHUMB.get()>ratioIMG.get()) {
					double uiImgHeight = i.getWidth()/ratioTHUMB.get();
					double y = (i.getHeight() - uiImgHeight)/2;
					imageView.setViewport(new Rectangle2D(0, y, i.getWidth(), uiImgHeight));
				} else
				if (ratioTHUMB.get()==ratioIMG.get()) {
					imageView.setViewport(null);
				}
			}
		}
	}

/* ---------- ANIMATION --------------------------------------------------------------------------------------------- */

	Timeline animation = null;

	public boolean isAnimated() {
		return animation!=null; // same impl as Image.isAnimation(), which is not public
	}

	public boolean isAnimating() {
		return animation!=null && animation.getCurrentRate()!=0;
	}

	public void animationPlayPause(boolean play) {
		if (play) animationPlay();
		else animationPause();
	}

	public void animationPlay() {
		if (animation!=null) animation.play();
	}

	public void animationPause() {
		if (animation!=null) animation.pause();
	}

/* ---------- DATA -------------------------------------------------------------------------------------------------- */

	/** File of the displayed image or null if no image displayed or not a file (e.g. over http). */
	public File getFile() {
		// Since we delay image loading or something, image.get() can be null, in that case we fall
		// back to imageFile
		String url = image.get()==null ? null : image.get().getUrl();
		try {
			return url==null ? imageFile : toFileOrNull(URI.create(url));
		} catch (IllegalArgumentException e) {
			log(Thumbnail.class).warn("Uri={} is not valid file", url, e);
			return null;
		}
	}

	/** @return object for which this thumbnail displays the thumbnail, by default equivalent to {@link #getFile()} */
	public Object getRepresentant() {
		return getFile();
	}

	/** @return image. Null if no image. */
	public Image getImage() {
		return imageView.getImage();
	}

	/**
	 * For internal use only!
	 * This method allows shifting the image methods common for all implementations
	 * of this class to be defined here and prevent from need to implement them
	 * individually.
	 * Note, that implementations using multiple ImageViews or different way of
	 * displaying the image will have to use their own implementation of all
	 * methods that call this method.
	 *
	 * @return ImageView displaying the image.
	 */
	public ImageView getView() {
		return imageView;
	}

	/**
	 * Returns root pane. Also image drag gesture source {@see #setDragEnabled(boolean)}.
	 * <p/>
	 * Note that in order for the image to resize prefSize of this pane
	 * must be used!
	 * <p/>
	 * {@inheritDoc }
	 */
	@Dependency("Must return image drag gesture root")
	public Pane getPane() {
		return root;
	}

/* ---------- BORDER ------------------------------------------------------------------------------------------------ */

	/**
	 * Whether border envelops thumbnail or image specifically.
	 * <p/>
	 * This is important for when the picture does not have the same aspect ratio
	 * as the thumbnail. Setting the border for thumbnail (false) will frame
	 * the thumbnail without respect for image size. Conversely, setting the border
	 * to image (true) will frame image itself, but the thumbnail itself will not
	 * be resized to image size, therefore leaving empty space either horizontally
	 * or vertically.
	 */
	private boolean borderToImage = false;
	private boolean isBorderVisible = false;
	private Pane border;

	/** Returns value of {@link #borderToImage}. */
	public boolean isBorderToImage() {
		return borderToImage;
	}

	/** Sets the {@link #borderToImage}. */
	public void setBorderToImage(boolean val) {
		if (borderToImage==val) return;
		borderToImage = val;
		if (isBorderVisible) root.layout();
	}

	public boolean getBorderVisible() {
		return isBorderVisible;
	}

	public void setBorderVisible(boolean val) {
		if (isBorderVisible==val) return;
		isBorderVisible = val;

		if (val) {
			border = createBorder();
			root.getChildren().add(border);
		} else {
			root.getChildren().remove(border);
			border = null;
		}
	}

	private Pane createBorder() {
		Pane b = new Pane();
		b.setMouseTransparent(true);
		b.setManaged(false);
		b.getStyleClass().add(styleclassBorder);
		return b;
	}

/* ---------- BACKGROUND -------------------------------------------------------------------------------------------- */

	// TODO: use custom style classes for this

	public boolean isBackgroundVisible() {
		return root.getStyleClass().contains(styleclassBgr);
	}

	/**
	 * Sets visibility of the background. The bgr is visible only when the image
	 * size ratio and thumbnail size ratio does not match.
	 * Default value is true. Invisible background becomes transparent.
	 * Styleable with css.
	 */
	public void setBackgroundVisible(boolean val) {
		if (val) {
			if (!isBackgroundVisible())
				root.getStyleClass().add(styleclassBgr);
		} else {
			root.getStyleClass().remove(styleclassBgr);
		}
	}

/* ---------- properties -------------------------------------------------------------------------------------------- */

	/**
	 * Allow image file drag from this thumbnail.
	 * <p/>
	 * Dragging is done with left button and only possible if this thumbnail has file set.
	 * The file will be put into dragboard, use {@link javafx.scene.input.DataFormat#FILES} to retrieve it.
	 * <p/>
	 * The gesture source can be obtained by {@link #getPane()}
	 * <p/>
	 * Default true.
	 */
	public void setDragEnabled(boolean val) {
		if (val) {
			dragHandler = buildDH();
			root.addEventHandler(DRAG_DETECTED, dragHandler);
		} else {
			root.removeEventHandler(DRAG_DETECTED, dragHandler);
			dragHandler = null;
		}
	}

	private EventHandler<MouseEvent> dragHandler;

	private EventHandler<MouseEvent> buildDH() {
		return e -> {
			if (e.getButton()==PRIMARY) {
				Object representant = getRepresentant();
				File file = getFile();
				List<File> files = stream(representant instanceof File ? (File) representant : file)
						.filter(ISNTØ)
						.collect(toList());

				if (!files.isEmpty()) {
					Dragboard db = root.startDragAndDrop(TransferMode.ANY);
					if (getImage()!=null) db.setDragView(getImage());

					HashMap<DataFormat,Object> c = new HashMap<>();
					c.put(FILES, files);
					db.setContent(c);
				}

				e.consume();
			}
		};
	}

	/**
	 * Set whether thumbnail context menu should be used for thumbnail.
	 * Default true.
	 */
	public void setContextMenuOn(boolean val) {
		if (val) root.addEventHandler(MOUSE_CLICKED, contextMenuHandler);
		else root.removeEventHandler(MOUSE_CLICKED, contextMenuHandler);
	}

/* ---------- HOVER ------------------------------------------------------------------------------------------------- */

	/** Duration of the scaling animation effect when transitioning to hover state. */
	public final V<Duration> durationOnHover = new V<>(animDur);
	private final Anim hoverAnimation = new Anim(durationOnHover.get(), at -> setScaleXYByTo(root, at, 0, 2));
	private final EventHandler<MouseEvent> hoverHandler = e -> {
		hoverAnimation.dur(durationOnHover.get());
		if (isHoverable())
			hoverAnimation.playFromDir(e.getEventType().equals(MOUSE_ENTERED));
	};
	/** Hover scaling effect on/off. Default false. */
	public final BooleanProperty hoverable = new SimpleBooleanProperty(this, "hoverable", false) {
		@Override
		public void set(boolean v) {
			super.set(v);
			if (v) {
				root.addEventFilter(MOUSE_ENTERED, hoverHandler);
				root.addEventFilter(MOUSE_EXITED, hoverHandler);
			} else {
				root.removeEventFilter(MOUSE_ENTERED, hoverHandler);
				root.removeEventFilter(MOUSE_EXITED, hoverHandler);
			}
		}
	};

	private boolean isHoverable() {
		return hoverable.get();
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Image aspect ratio. Image width/height.
	 */
	public final DoubleProperty ratioIMG = new SimpleDoubleProperty(1);
	/**
	 * Thumbnail aspect ratio. Root width/height.
	 */
	public final DoubleProperty ratioTHUMB = new SimpleDoubleProperty(1);
	private final DoubleProperty maxImgW = new SimpleDoubleProperty(Double.MAX_VALUE);
	private final DoubleProperty maxImgH = new SimpleDoubleProperty(Double.MAX_VALUE);

	public double getRatioPane() {
		return ratioTHUMB.get();
	}

	public double getRatioImg() {
		return ratioIMG.get();
	}

	public DoubleProperty ratioPaneProperty() {
		return ratioTHUMB;
	}

	public DoubleProperty ratioImgProperty() {
		return ratioIMG;
	}

/* --------------------- CONTEXT MENU ------------------------------------------------------------------------------- */

	private static final SingleR<ImprovedContextMenu<ContextMenuData>,Thumbnail> contextMenu = new SingleR<>(
			ImprovedContextMenu::new,
			(menu, thumbnail) -> menu.setValueAndItems(thumbnail.new ContextMenuData())
	);

	private final EventHandler<MouseEvent> contextMenuHandler = e -> {
		if (e.getButton()==SECONDARY) {
			contextMenu.getM(this).show(root, e);
			e.consume();
		}
	};

	public class ContextMenuData {
		public final Object representant = getRepresentant();
		public final File file = representant instanceof File ? (File) representant : null;
		public final File iFile = getFile();
		public final File fsImageFile = iFile!=null ? iFile : file;
		public final Image image = getImage();
		public final boolean fsDisabled = fsImageFile==null || !ImageFileFormat.isSupported(fsImageFile);
		public final Thumbnail thumbnail = Thumbnail.this;
	}

	public enum FitFrom {
		INSIDE, OUTSIDE
	}
}