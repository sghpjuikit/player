package sp.it.pl.gui.objects.grid;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
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
import static sp.it.util.async.AsyncKt.oneTPExecutor;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.dev.FailKt.failIfNotFxThread;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.file.UtilKt.getNameWithoutExtensionOrRoot;
import static sp.it.util.reactive.UtilKt.doIfImageLoaded;
import static sp.it.util.reactive.UtilKt.sync1IfImageLoaded;
import static sp.it.util.reactive.UtilKt.syncC;

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

		onDispose.plusAssign(syncC(parentProperty(), p -> parentVolatile = p==null ? null : p.getParent()));
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
		if (item==getItem()) {
			if (!empty) setCoverNow(item);
			return;
		}
		super.updateItem(item, empty);
		itemVolatile = item;

		if (imgLoadAnimation!=null) {
			imgLoadAnimation.stop();
			imgLoadAnimationItem = item;
			imgLoadAnimation.applyAt(item==null ? 0 : item.loadProgress);
		}

		if (empty) {
			setGraphic(null);   // do not discard contents of the graphics
		} else {
			if (root==null) computeGraphics();  // create graphics lazily and only once
			if (getGraphic()!=root) setGraphic(root);   // set graphics only when necessary
		}

		if (getGraphic()!=null) {
			name.setText(item==null ? null : computeName(item));
			setCoverNow(item);
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
				if (img==null)
					imgLoadAnimation.applyAt(0);
				else
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

		Rectangle r = new Rectangle(1, 1);
		r.getStyleClass().add("grid-cell-stroke");
		r.setMouseTransparent(true);

		root = new Pane(thumb.getPane(), name, r) {
			// Cell layout should be fast - gets called multiple times on grid resize.
			// Why not use custom pane for more speed if we can.
			@Override
			protected void layoutChildren() {
				double x = 0, y = 0, w = getWidth(), h = getHeight(), th = computeCellTextHeight();
				thumb.getPane().resizeRelocate(x, y, w, h - th);
				name.resizeRelocate(x, h - th, w, th);
				r.setX(x);
				r.setY(y);
				r.setWidth(w);
				r.setHeight(h);
			}
		};
		root.setSnapToPixel(true);
		root.setMinSize(-1, -1);
		root.setPrefSize(-1, -1);
		root.setMaxSize(-1, -1);
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
		return new ImageSize(gridView.getValue().cellWidth.getValue(), gridView.getValue().cellHeight.getValue()-computeCellTextHeight());
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
	    return parentVolatile==null;
	}

	/**
	 * @implSpec must be thread safe
	 * @return true if the index of this cell is not the same as the index specified
	 */
	@ThreadSafe
	protected boolean isInvalidIndex(int i) {
		return indexVolatile!=i;
	}

	@ThreadSafe
	private boolean isInvalid(Item item, int i) {
		return isInvalidItem(item) || isInvalidIndex(i) || isInvalidVisibility();
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
		var i = indexVolatile;

		if (item.isLoadedCover()) {
			if (thumb.getImage()!=item.cover) {
				setCoverPost(item, i, item.coverFile, item.cover, null);
			}
		} else {
			thumb.loadImage((File) null); // prevent displaying old content before cover loads

			ImageSize size = computeThumbSize(item);
			failIf(size.width<=0 || size.height<=0);

			if (loader.executorThumbs!=null)
				loader.executorThumbs.execute(computeTask(() -> {
					if (isInvalid(item, i)) return;

					RunnableLocked then = new RunnableLocked();
					loader.loadSynchronizerThumb.execute(then);

					item.loadCover(size)
						.ifOkUse(result -> then.run(() -> setCoverPost(item, i, result.file, result.cover, then)))        // load immediately
//						.ifOkUse(result -> setCoverPost(item, i, result.file, result.cover, then))                        // load after all previous
						.ifErrorUse(e -> then.runNothing());
				}
			));
		}
	}

	@ThreadSafe
	private void setCoverPost(Item item, int i, File imgFile, Image img, RunnableLocked then) {
		runFX(() -> {
			if (disposed || isInvalid(item, i) || img==null) {
				if (then!=null) then.runNothing();
				return;
			}

			sync1IfImageLoaded(img, () -> {
				thumb.loadImage(img, imgFile);
				if (then!=null) then.finish();
			});
		});
	}

	public static class Loader {
		public final ExecutorService executorThumbs;
		public final AtomicBoolean twoPass = new AtomicBoolean(false);
		public final ExecutorService loadSynchronizerThumb = oneTPExecutor();

		public Loader(ExecutorService executorThumbs) {
			this.executorThumbs = executorThumbs;
		}

		public void shutdown() {
			if (executorThumbs!=null) executorThumbs.shutdownNow();
			loadSynchronizerThumb.shutdownNow();
		}
	}

	public static class RunnableLocked implements Runnable {
		private final AtomicReference<Runnable> action = new AtomicReference<>(null);
		private final CountDownLatch waitPre = new CountDownLatch(1);
		private final CountDownLatch waitPost = new CountDownLatch(1);

		public void runNothing() {
			run(null);
			finish();
		}

		public void run(Runnable r) {
			action.set(r);
			waitPre.countDown();
		}

		public void finish() {
			waitPost.countDown();
		}

		@Override
		public void run() {
			try { waitPre.await(); } catch (InterruptedException e) {}
			Runnable r = action.get();
			if (r!=null) r.run();
			try { waitPost.await(); } catch (InterruptedException e) {}
		}
	}

}