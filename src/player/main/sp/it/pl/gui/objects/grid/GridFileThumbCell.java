package sp.it.pl.gui.objects.grid;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import sp.it.pl.gui.objects.hierarchy.Item;
import sp.it.pl.gui.objects.image.Thumbnail;
import sp.it.util.JavaLegacy;
import sp.it.util.animation.Anim;
import sp.it.util.dev.ThreadSafe;
import sp.it.util.file.FileType;
import sp.it.util.reactive.Disposer;
import sp.it.util.ui.image.ImageSize;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.util.Duration.millis;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.async.AsyncKt.oneTPExecutor;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.async.AsyncKt.sleep;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.dev.FailKt.failIfNotFxThread;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.file.UtilKt.getNameWithoutExtensionOrRoot;
import static sp.it.util.reactive.UtilKt.doIfImageLoaded;
import static sp.it.util.reactive.UtilKt.maintain;
import static sp.it.util.reactive.UtilKt.sync1IfImageLoaded;

/**
 * GridCell implementation for file using {@link sp.it.pl.gui.objects.hierarchy.Item}
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class GridFileThumbCell extends GridCell<Item,File> {
	protected Pane root;
	protected Label name;
	protected Thumbnail thumb;
	protected final Loader loader;
	protected Anim imgLoadAnimation;
	private Item imgLoadAnimationItem;
	private Disposer onDispose = new Disposer();
	protected volatile boolean disposed = false;
	private volatile Item itemVolatile = null;
	private volatile Parent parentVolatile = null;
	private volatile Integer indexVolatile = null;

	public GridFileThumbCell(Loader imgLoader) {
		loader = noNull(imgLoader);

		onDispose.plusAssign(maintain(parentProperty(), p -> parentVolatile = p==null ? null : p.getParent()));
	}

	protected String computeName(Item item) {
		return item.valType==FileType.DIRECTORY ? item.value.getName() : getNameWithoutExtensionOrRoot(item.value);
	}

	protected double computeCellTextHeight() {
		return 40;
	}

	protected Runnable computeTask(Runnable r) {
		return r;
	}

	@SuppressWarnings("unused")
	protected void onAction(Item i, boolean edit) {}

	@Override
	public void dispose() {
		failIfNotFxThread();

		disposed = true;
		if (imgLoadAnimation!=null) imgLoadAnimation.stop();
		imgLoadAnimation = null;
		imgLoadAnimationItem = null;
		onDispose.invoke();
		if (thumb!=null) {
			var img = thumb.getView().getImage();
			thumb.getView().setImage(null);
			if (img!=null) JavaLegacy.destroyImage(img);
		}
		thumb = null;
		itemVolatile = null;
		parentVolatile = null;
		indexVolatile = null;
	}

	@Override
	protected void updateItem(Item item, boolean empty) {
		if (disposed) return;
		if (item==getItem()) return;
		super.updateItem(item, empty);
		itemVolatile = item;

		if (imgLoadAnimation!=null) {
			imgLoadAnimation.stop();
			imgLoadAnimationItem = item;
			imgLoadAnimation.applyAt(item.loadProgress);
		}

		if (empty || item==null) {
			// empty cell has no graphics
			// we do not clear the content of the graphics however
			setGraphic(null);
		} else {
			if (root==null) {
				// we create graphics only once and only when first requested
				computeGraphics();
				// we set graphics only once (when creating it)
				setGraphic(root);
			}
			// if cell was previously empty, we set graphics back
			// this improves performance compared to setting it every time
			if (getGraphic()!=root) setGraphic(root);

			// set name
			name.setText(computeName(item));
			// set cover
			// The item caches the cover Image, so it is only loaded once. That is a heavy op.
			// This method can be called very frequently and:
			//     - block ui thread when scrolling
			//     - reduce ui performance when resizing
			// Solved by delaying the image loading & drawing, which reduces subsequent
			// invokes into single update (last).
//			boolean loaded = item.cover_loadedFull.get();	// TODO: do this properly

			boolean loaded = item.cover_loadedThumb.get();
			if (loaded) setCoverNow(item);
			else setCoverLater(item);

		}
	}

	@Override
	public void updateSelected(boolean selected) {
		super.updateSelected(selected);
		if (thumb!=null && thumb.image.get()!=null) thumb.animationPlayPause(selected);
	}

	@Override
	public void updateIndex(int i) {
		indexVolatile = i;
		super.updateIndex(i);
	}

	protected void computeGraphics() {
		name = new Label();
		name.setAlignment(Pos.CENTER);

		thumb = new Thumbnail() {
			@Override
			public Object getRepresentant() {
				return getItem()==null ? null : getItem().value;
			}
		};
		thumb.setBorderVisible(false);
		thumb.getPane().setSnapToPixel(true);
		thumb.getView().setSmooth(true);
		onDispose.plusAssign(
			doIfImageLoaded(thumb.getView(), img -> {
				imgLoadAnimation.stop();
				imgLoadAnimationItem = getItem();
				if (img==null) {
					imgLoadAnimation.applyAt(0);
				} else
					imgLoadAnimation.playOpenFrom(imgLoadAnimationItem.loadProgress);
			})
		);

		imgLoadAnimation = new Anim(x -> {
				if (imgLoadAnimationItem!=null) {
					imgLoadAnimationItem.loadProgress = x;
					thumb.getView().setOpacity(x*x*x*x);
				}
			})
			.dur(millis(200));

		// TODO: remove workaround for fuzzy edges & incorrect layout
		// Problem: OS scaling will change width of the border, for non-integer widths it may produce visual artifacts
		// Solution: We adjust width so it can only scale into integer values.
		double BW = 1;
		double dpiScalingFix = Math.rint(BW*APP.windowManager.screenMaxScaling)/APP.windowManager.screenMaxScaling;
		BW *= dpiScalingFix;
		Rectangle r = new Rectangle(1, 1);
		r.setMouseTransparent(true);
		r.setFill(null);
		r.setStroke(Color.BLACK);
		r.setStrokeType(StrokeType.INSIDE);
		r.setStrokeWidth(BW);
		r.setManaged(false);
		r.setSmooth(false);

		root = new Pane(thumb.getPane(), name, r) {
			// Cell layout should be fast - gets called multiple times on grid resize.
			// Why not use custom pane for more speed if we can.
			@Override
			protected void layoutChildren() {
				double x = 0, y = 0, w = getWidth(), h = getHeight(), th = computeCellTextHeight();
				thumb.getPane().resizeRelocate(x, y, w, h - th);
				name.resizeRelocate(x, h - th, w, th);
				r.setX(x);
				r.setY(x);
				r.setWidth(w);
				r.setHeight(h);
			}
		};
		root.setSnapToPixel(true);
		root.setMinSize(-1, -1);
		root.setPrefSize(-1, -1);
		root.setMaxSize(-1, -1);
		Anim a = new Anim(x -> root.setTranslateY(-5*x*x)).dur(millis(200));
		onDispose.plusAssign(maintain(thumb.getView().hoverProperty(), (nv) -> a.playFromDir(nv)));
		root.setOnMouseClicked(e -> {
			if (e.getButton()==PRIMARY && e.getClickCount()==2) {
				onAction(getItem(), e.isShiftDown());
				e.consume();
			}
		});
	}

	/**
	 * @implSpec called on fx application thread, must return positive width and height
	 * @return size of an image to be loaded for the thumbnail
	 */
	protected ImageSize computeThumbSize(Item item) {
		// return thumb.calculateImageLoadSize(); // has potential to cause problems and is less performable than below:
		return new ImageSize(gridView.get().getCellWidth(), gridView.get().getCellHeight()-computeCellTextHeight());
	}

	/**
	 * @implSpec must be thread safe
	 * @return true if the item of this cell is not the same object as the item specified
	 */
	@ThreadSafe
	protected boolean isInvalidItem(Item item) {
		return itemVolatile!=item;
	}

	/**
	 * @implSpec must be thread safe
	 * @return true if this cell is detached from the grid (i.e. not its child)
	 */
	@ThreadSafe
	protected boolean isInvalidVisibility() {
	    return parentVolatile==null || indexVolatile==-1;
	}

	/**
	 * Begins loading cover for the item. If item changes meanwhile, the result is stored
	 * (it will not need to load again) to the old item, but not showed.
	 * <p/>
	 * Thumbnail quality may be decreased to achieve good performance, while loading high
	 * quality thumbnail in the bgr. Each phase uses its own executor.
	 * <p/>
	 * Must be called on FX thread.
	 */
	private void setCoverNow(Item item) {
		failIfNotFxThread();

		if (isInvalidItem(item) || isInvalidVisibility()) return;

		if (item.cover_loadedThumb.get()) {
			setCoverPost(item, item.cover_file, item.cover, null);
		} else {
			ImageSize size = computeThumbSize(item);
			failIf(size.width<=0 || size.height<=0);

			// load thumbnail
			if (loader.executorThumbs!=null)
				loader.executorThumbs.execute(computeTask(() -> {
					RunnableLocked then = new RunnableLocked();
					loader.loadSynchronizerThumb.execute(then);

						if (isInvalidItem(item) || isInvalidVisibility()) {
							then.runNothing();
							return;
						}

					item.loadCover(false, size)
						.ifOkUse(result -> then.run(() -> setCoverPost(item, result.file, result.cover, then)))        // load immediately
//						.ifOkUse(result -> setCoverPost(item, result.file, result.cover, then))                        // load after all previous
						.ifErrorUse(e -> then.runNothing());
				}
			));

			// load high quality thumbnail
			if (loader.executorImage!=null && loader.twoPass.get())
				loader.executorImage.execute(computeTask(() -> {
						RunnableLocked then = new RunnableLocked();
						loader.loadSynchronizerFull.execute(then);

						if (isInvalidItem(item) || isInvalidVisibility())  {
							then.runNothing();
							return;
						}

						item.loadCover(true, size)
							.ifOkUse(result -> then.run(() -> setCoverPost(item, result.file, result.cover, then)))    // load after all previous
//							.ifOkUse(result -> setCoverPost(item, result.file, result.cover, then))                    // load immediately
							.ifErrorUse(e -> then.runNothing())
							.ifErrorUse(e -> System.out.println("fml" + itemVolatile.value));
					}
				));
		}
	}

	private void setCoverLater(Item item) {
		failIfNotFxThread();
		if (disposed) return;

		thumb.loadImage((File) null); // prevent displaying old content before cover loads
		setCoverNow(item);
	}

	private void setCoverPost(Item item, File imgFile, Image img, RunnableLocked then) {
		runFX(() -> {
			if (disposed) return;

			if (isInvalidItem(item) || isInvalidVisibility() || img==null) {
				if (then!=null) then.finish();
				return;
			}

			thumb.loadImage(img, imgFile);
			if (then!=null)
				sync1IfImageLoaded(img, then::finish);
		});
	}

	public static class Loader {
		public final ExecutorService executorThumbs;
		public final ExecutorService executorImage;
		public final AtomicBoolean twoPass = new AtomicBoolean(false);
		public final ExecutorService loadSynchronizerThumb = oneTPExecutor();
		public final ExecutorService loadSynchronizerFull = oneTPExecutor();

		public Loader(ExecutorService executorThumbs, ExecutorService executorImage) {
			this.executorThumbs = executorThumbs;
			this.executorImage = executorImage;
		}
	}

	public static class RunnableLocked implements Runnable {
		private final AtomicReference<Runnable> action = new AtomicReference<>(null);
		private final AtomicBoolean waitPre = new AtomicBoolean(true);
		private final AtomicBoolean waitPost = new AtomicBoolean(true);

		public void runNothing() {
			run(null);
			finish();
		}

		public void run(Runnable r) {
			action.set(r);
			waitPre.set(false);
		}

		public void finish() {
			waitPost.set(false);
		}

		@Override
		public void run() {
			while(waitPre.get()) sleep(2);
			Runnable r = action.get();
			if (r!=null) r.run();
			while(waitPost.get()) sleep(2);
		}
	}

}