package sp.it.pl.ui.objects.image;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.image.ImageStandardLoader;
import sp.it.util.access.V;
import sp.it.util.animation.Anim;
import sp.it.util.async.executor.EventReducer;
import sp.it.util.async.future.Fut;
import sp.it.util.dev.Dependency;
import sp.it.util.math.P;
import sp.it.util.ui.image.FitFrom;
import sp.it.util.ui.image.ImageSize;
import static java.lang.Double.min;
import static java.lang.Math.signum;
import static javafx.scene.input.DataFormat.FILES;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.TransferMode.ANY;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static sp.it.pl.audio.playlist.PlaylistReaderKt.toAbsoluteURIOrNull;
import static sp.it.pl.main.AppBuildersKt.contextMenuFor;
import static sp.it.pl.main.AppFileKt.isImage;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.ui.objects.hierarchy.Item.CoverStrategy.VT_IMAGE;
import static sp.it.util.Util.clip;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.runOn;
import static sp.it.util.dev.FailKt.failIfNotFxThread;
import static sp.it.util.file.UtilKt.toFileOrNull;
import static sp.it.util.functional.Util.ISNT0;
import static sp.it.util.functional.Util.stream;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.UtilKt.sync1If;
import static sp.it.util.ui.ContextMenuExtensionsKt.show;
import static sp.it.util.ui.MouseDragKt.initMouseDrag;
import static sp.it.util.ui.UiDelegateKt.setUiDelegate;
import static sp.it.util.ui.UtilKt.initClip;
import static sp.it.util.ui.UtilKt.setScaleXYByTo;
import static sp.it.util.ui.image.FitFrom.INSIDE;
import static sp.it.util.units.FactoriesKt.uri;

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
public class Thumbnail {

	private static final Logger logger = LoggerFactory.getLogger(Thumbnail.class);
	private static final String styleclassBgr = "thumbnail";
	private static final String styleclassBorder = "thumbnail-border";
	private static final String styleclassImage = "thumbnail-image";

	protected final ImageView imageView = new ImageView();
	protected final StackPane root = new StackPane(imageView) {
		private P xyOld = new P(-1, -1);

		@SuppressWarnings("UnnecessaryLocalVariable")
		@Override
		protected void layoutChildren() {
			if (getChildren().isEmpty()) return;

			double W = getWidth();
			double H = getHeight();
			double imgW = min(W, maxImgW.get());
			double imgH = min(H, maxImgH.get());

			ratioTHUMB.setValue(H==0.0 ? 1 : W/H);

			needsApplyViewport = xyOld.getX()!=W || xyOld.getY()!=H;
			xyOld = new P(W, H);
			applyViewPortImpl(imageView.getImage());

			// resize thumbnail
			if (fitFrom.getValue()==INSIDE) {
				imageView.setFitWidth(imgW);
				imageView.setFitHeight(imgH);
			} else {
				if (isImgSmallerW && isImgSmallerH) {
					imageView.setFitWidth(imgW);
					imageView.setFitHeight(imgH);
				} else {
					imageView.setFitWidth(W);
					imageView.setFitHeight(H);
				}
			}

			// lay out other children (maybe introduced in subclass)
			super.layoutChildren();

			// lay out border
			if (isBorderVisible) {
				if (borderToImage && imageView.getImage()!=null) {
					var imgWf = fitFrom.getValue()==INSIDE ? min(imgW, W) : imgW;
					var imgHf = fitFrom.getValue()==INSIDE ? min(imgH, H) : imgH;
					if (ratioIMG.get()>ratioTHUMB.get()) {
						double borderW = imgWf;
						double borderWGap = (W - borderW)/2;
						double borderH = imgWf/ratioIMG.get();
						double borderHGap = (H - borderH)/2;
						border.resizeRelocate(borderWGap, borderHGap, borderW, borderH);
					} else {
						double borderW = imgHf*ratioIMG.get();
						double borderWGap = (W - borderW)/2;
						double borderH = imgHf;
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
	public final @NotNull ObjectProperty<@Nullable Image> image = new V<>(null);
	private File imageFile = null;
	public final @NotNull V<@NotNull FitFrom> fitFrom = new V<>(INSIDE);

	private boolean isImgSmallerW = false;
	private boolean isImgSmallerH = false;

	/** {@link sp.it.pl.ui.objects.image.Thumbnail#Thumbnail(double, double)}} using {@link javafx.scene.layout.Region#USE_COMPUTED_SIZE} */
	public Thumbnail() {
		this(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
	}

	/** {@link sp.it.pl.ui.objects.image.Thumbnail#Thumbnail()}} using the specified size */
	public Thumbnail(P size) {
		this(size.getX(), size.getY());
	}

	/**
	 * Use if you need different size than default thumbnail size and the image
	 * is expected to change during life cycle.
	 */
	public Thumbnail(double width, double height) {
		root.getStyleClass().add("thumbnail-root");
		root.setMinSize(width, height);
		root.setPrefSize(width, height);
		root.setMaxSize(width, height);
		imageView.getStyleClass().add(styleclassImage);
		imageView.setFitHeight(-1);
		imageView.setFitWidth(-1);
		fitFrom.addListener((o, ov, nv) -> needsApplyViewport = true);
		fitFrom.addListener((o, ov, nv) -> root.requestLayout());
		viewportEditable.addListener((o, ov, nv) -> root.requestLayout());

		// initialize values
        imageView.setCache(false);
		imageView.setSmooth(false);
		imageView.setPreserveRatio(true);
		setBorderToImage(false);
		setBorderVisible(false);
		setBackgroundVisible(true);
		setDragEnabled(true);
		setContextMenuOn(true);

		var hoverState = new V<>(false);
            hoverState.attachC(it -> animationPlayPause(it));
		var hoverRaw = EventReducer.toLast(150.0, consumer(hoverState::setValue));
		root.addEventFilter(MOUSE_ENTERED, e -> hoverRaw.push(true));
		root.addEventFilter(MOUSE_EXITED, e -> hoverRaw.push(false));
		root.addEventFilter(MOUSE_EXITED, e -> hoverState.setValue(false));

		setUiDelegate(root, this);
	}

	public final ObservableList<String> getStyleClass() {
		return root.getStyleClass();
	}

	/** Loads image and sets it to show, null sets no image. */
	public void loadImage(@Nullable Image img) {
		imageFile = null;
		if (Objects.equals(image.getValue(), img)) return;
		animHelper.animDispose();
		setImgAsync(img);
	}

	public void loadImage(@Nullable Image img, @Nullable File file) {
		imageFile = file;
		if (Objects.equals(image.getValue(), img)) return;
		animHelper.animDispose();
		setImgAsync(img);
	}

	public void loadFile(@Nullable File img) {
		animHelper.animDispose();
		imageFile = null;
		if (img==null) {
			loadImage(null, null);
		} else if (image.getValue()==null || image.getValue().getUrl()==null || !img.getAbsoluteFile().toURI().equals(toAbsoluteURIOrNull(image.getValue().getUrl()))) {
			ImageSize size = calculateImageLoadSize();
			if (image.getValue()==null || image.getValue().getUrl()==null || !img.getAbsoluteFile().toURI().equals(toAbsoluteURIOrNull(image.getValue().getUrl())) || size.width-5.0>image.getValue().getWidth() || size.height-5.0>image.getValue().getHeight()) {
				runOn(VT_IMAGE, () -> ImageStandardLoader.INSTANCE.invoke(img, size, fitFrom.getValue())).useBy(FX, this::setImgAsync);
			}
		}
	}

	public void loadCover(@Nullable Cover img) {
		animHelper.animDispose();
		if (img==null) {
			loadImage(null, null);
		} else {
			imageFile = null;
			var size = calculateImageLoadSize();
			if (img.getFile()==null || image.getValue()==null || image.getValue().getUrl()==null || !img.getFile().getAbsoluteFile().toURI().equals(toAbsoluteURIOrNull(image.getValue().getUrl())) || size.width-5.0>image.getValue().getWidth() || size.height-5.0>image.getValue().getHeight()) {
				runOn(VT_IMAGE, () -> img.getImage(size, fitFrom.getValue())).useBy(FX, i -> loadImage(i, img.getFile()));
			}
		}
	}

	private long loadId = 0;    // prevents wasteful set image operations

	private void setImgAsync(@Nullable Image i) {
		failIfNotFxThread();
		loadId++;   // load next image
		final long id = loadId; // expected id (must match when load finishes)
		if (i==null) {
			setImg(null, id);
		} else {
			sync1If(i.progressProperty(), p -> p.doubleValue()==1, p -> { setImg(i, id); return Unit.INSTANCE; });
		}
	}

	private void setImg(@Nullable Image i, long id) {
		// ignore outdated loadings
		if (id!=loadId) return;
		ratioIMG.setValue(i==null || i.getHeight()==0 ? 1.0 : i.getWidth()/i.getHeight());
		applyViewPort(i);
		needsApplyViewport = true;
		imageView.setImage(i);
		image.setValue(i);

		if (i!=null) {
			maxImgW.set(i.getWidth());
			maxImgH.set(i.getHeight());
		}

		root.requestLayout();
	}

	void setImgFrame(@Nullable Image i) {
		applyViewPort(i);
		needsApplyViewport = true;
		imageView.setImage(i);
		root.requestLayout();
	}

	/**
	 * Calculates size of the image to load. Returns recommended size for the image.
	 * <p/>
	 * The image normally loads 1:1 with the resolution size of the file, but it
	 * is often wasteful, particularly for big images and even more if the size of
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

	public final @NotNull V<@NotNull Boolean> viewportEditable = new V<>(false);
	private boolean needsApplyViewport = false;
	private @Nullable Rectangle2D viewport = null;
	private @Nullable P viewportMin = null;
	private @Nullable P viewportMax = null;
	private @Nullable P viewportOffset = null;

	{
		initMouseDrag(
			root,
			null,
			consumer(it -> viewportOffset = imageView.getViewport()==null ? null : new P(imageView.getViewport().getMinX(), imageView.getViewport().getMinY())),
			consumer(it -> {
				if (viewport!=null && viewportEditable.getValue()) {
					imageView.setViewport(
						new Rectangle2D(
							clip(viewportMin.getX(), viewportOffset.getX() - signum(imageView.getScaleX()) * it.getDiff().getX(), viewportMax.getX()),
							clip(viewportMin.getY(), viewportOffset.getY() - signum(imageView.getScaleY()) * it.getDiff().getY(), viewportMax.getY()),
							viewport.getWidth(),
							viewport.getHeight()
						)
					);
				}
			})
		);
	}

	public void viewportShift(P by) {
		if (viewport!=null && viewportEditable.getValue())
			imageView.setViewport(
				new Rectangle2D(
					clip(viewportMin.getX(), imageView.getViewport().getMinX() + signum(imageView.getScaleX()) * signum(by.getX()) * imageView.getFitWidth()/30, viewportMax.getX()),
					clip(viewportMin.getY(), imageView.getViewport().getMinY() + signum(imageView.getScaleY()) * signum(by.getY()) * imageView.getFitWidth()/30, viewportMax.getY()),
					viewport.getWidth(),
					viewport.getHeight()
				)
			);
	}

	private void applyViewPort(@Nullable Image i) {
		needsApplyViewport = true;
		applyViewPortImpl(i);
	}

	private void applyViewPortImpl(@Nullable Image i) {
		if (!needsApplyViewport) return;
	    needsApplyViewport = false;

		if (i==null) {
			viewport = null;
			viewportMin = null;
			viewportMax = null;
		} else {
			var ratioI = i.getHeight()==0 ? 1.0 : i.getWidth()/i.getHeight();

			if (fitFrom.get()==INSIDE) {
				viewport = null;
				viewportMin = null;
				viewportMax = null;
				imageView.setViewport(viewport);
			} else {
				isImgSmallerW = i.getWidth()<=imageView.getLayoutBounds().getWidth();
				isImgSmallerH = i.getHeight()<=imageView.getLayoutBounds().getHeight();
				if (isImgSmallerW && isImgSmallerH) {
					viewport = null;
					viewportMin = null;
					viewportMax = null;
					imageView.setViewport(viewport);
				} else
				if (ratioTHUMB.getValue()<ratioI) {
					double uiImgWidth = i.getHeight()*ratioTHUMB.getValue();
					double x = i.getWidth() - uiImgWidth;
					viewport = new Rectangle2D(x/2, 0, uiImgWidth, i.getHeight());
					viewportMin = new P(0, 0);
					viewportMax = new P(x, 0);
					imageView.setViewport(viewport);
				} else
				if (ratioTHUMB.getValue()>ratioI) {
					double uiImgHeight = i.getWidth()/ratioTHUMB.getValue();
					double y = i.getHeight() - uiImgHeight;
					viewport = new Rectangle2D(0, y/2, i.getWidth(), uiImgHeight);
					viewportMin = new P(0, 0);
					viewportMax = new P(0, y);
					imageView.setViewport(viewport);
				} else
				if (ratioTHUMB.getValue()==ratioI) {
					viewport = null;
					viewportMin = null;
					viewportMax = null;
					imageView.setViewport(viewport);
				}
			}
		}
	}

/* ---------- ANIMATION --------------------------------------------------------------------------------------------- */

	private ThumbnailAnim animHelper = new ThumbnailAnim(this);

	/** @return whether the current image has animation (animation must be initialized before this call) */
	public Fut<Boolean> isAnimated() {
		return animHelper.isAnimated();
	}

	/** @return whether the current image has animation and it is playing */
	public boolean isAnimating() {
		return animHelper.isAnimating();
	}

	/** Plays or pauses currently loaded image animation */
	public void animationPlayPause(boolean play) {
		animHelper.animationPlayPause(play);
	}

	/** Plays currently loaded image animation */
	public void animationPlay() {
		animHelper.animationPlay();
	}

	/** Plays currently loaded image animation once and returns future that ends when the animation ends or as soon as possible if not animated or error loading animation or image */
	public Fut<Unit> animationPlayOnceAndWait() {
		return animHelper.animationPlayOnceAndWait();
	}

	/** Pauses currently loaded image animation */
	public void animationPause() {
		animHelper.animationPause();
	}

/* ---------- DATA -------------------------------------------------------------------------------------------------- */

	/** File of the displayed image or null if no image displayed or not a file (e.g. over http). */
	public File getFile() {
		// Since we delay image loading or something, image.get() can be null, in that case we fall
		// back to imageFile
		String url = image.getValue()==null ? null : image.getValue().getUrl();
		try {
			return url==null ? imageFile : toFileOrNull(uri(url));
		} catch (IllegalArgumentException e) {
			logger.warn("Uri={} is not valid file", url, e);
			return null;
		}
	}

	/** @return object for which this thumbnail displays the thumbnail, by default equivalent to {@link #getFile()} */
	public Object getRepresentant() {
		return getFile();
	}

	/** @return image. Null if no image. */
	public @Nullable Image getImage() {
		return image.getValue();
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

/* ---------- RADIUS ------------------------------------------------------------------------------------------------ */

	{
		// TODO optimize
		initClip(root, Insets.EMPTY);
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

	public boolean isDragEnabled() {
		return dragHandler!=null;
	}

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

	private @NotNull EventHandler<MouseEvent> buildDH() {
		return e -> {
			if (e.getButton()==PRIMARY && (!viewportEditable.getValue() || e.isShiftDown())) {
				var representant = getRepresentant();
				var file = getFile();
				var files = stream(representant instanceof File f ? f : file).filter(ISNT0).toList();

				if (!files.isEmpty()) {
					Dragboard db = root.startDragAndDrop(ANY);
					if (getImage()!=null) db.setDragView(getImage());
					db.setContent(Map.of(FILES, files));
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
	public final V<Duration> durationOnHover = new V<>(APP.ui.getThumbnailAnimDur().getValue());
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

	private final EventHandler<MouseEvent> contextMenuHandler = e -> {
		if (e.getButton()==SECONDARY) {
			show(contextMenuFor(new ContextMenuData()), root, e);
			e.consume();
		}
	};

	public class ContextMenuData {
		public final Object representant = getRepresentant();
		public final File file = representant instanceof File f ?f : null;
		public final File iFile = getFile();
		public final File fsImageFile = iFile!=null ? iFile : file;
		public final Image image = getImage();
		public final boolean fsDisabled = fsImageFile==null || !isImage(fsImageFile);
		public final Thumbnail thumbnail = Thumbnail.this;
	}

}