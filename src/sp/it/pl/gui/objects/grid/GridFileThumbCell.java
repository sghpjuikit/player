package sp.it.pl.gui.objects.grid;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import sp.it.pl.gui.objects.hierarchy.Item;
import sp.it.pl.gui.objects.image.Thumbnail;
import sp.it.pl.util.SwitchException;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.async.executor.EventReducer;
import sp.it.pl.util.async.future.Fut;
import sp.it.pl.util.file.FileType;
import sp.it.pl.util.graphics.image.ImageSize;
import static javafx.scene.input.MouseButton.PRIMARY;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.AsyncKt.newSingleDaemonThreadExecutor;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.sleep;
import static sp.it.pl.util.dev.Util.noØ;
import static sp.it.pl.util.dev.Util.throwIf;
import static sp.it.pl.util.dev.Util.throwIfFxThread;
import static sp.it.pl.util.dev.Util.throwIfNotFxThread;
import static sp.it.pl.util.file.UtilKt.getNameWithoutExtensionOrRoot;
import static sp.it.pl.util.reactive.Util.doOnceIfImageLoaded;

/**
 * {@link sp.it.pl.gui.objects.grid.GridCell} implementation for file using {@link sp.it.pl.gui.objects.hierarchy.Item}
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
public class GridFileThumbCell extends GridCell<Item,File> {
	protected Pane root;
	protected Label name;
	protected Thumbnail thumb;
	protected final Loader loader;
	protected final EventReducer<Item> setCoverLater;	// TODO: is this necessary?
	protected Anim imgLoadAnimation;

	public GridFileThumbCell(Loader imgLoader) {
		noØ(imgLoader);
		loader = imgLoader;
		setCoverLater = EventReducer.toLast(10, this::setCoverNow);
	}

	protected String computeName(Item item) {
		return item.valType==FileType.DIRECTORY ? item.val.getName() : getNameWithoutExtensionOrRoot(item.val);
	}

	protected AnimateOn computeAnimateOn() {
		return AnimateOn.IMAGE_CHANGE_1ST_TIME;
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
	protected void updateItem(Item item, boolean empty) {
		if (item==getItem()) return;
		super.updateItem(item, empty);

		if (item==null) {
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

	protected void computeGraphics() {
		name = new Label();
		name.setAlignment(Pos.CENTER);

		thumb = new Thumbnail() {
			@Override
			public Object getRepresentant() {
				return getItem()==null ? null : getItem().val;
			}
		};
		thumb.setBorderVisible(false);
		thumb.getPane().setSnapToPixel(true);
		thumb.getView().setSmooth(true);

		imgLoadAnimation = new Anim(thumb.getView()::setOpacity).dur(200).intpl(x -> x*x*x*x);

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
		root.hoverProperty().addListener((o, ov, nv) -> thumb.getView().setEffect(nv ? new ColorAdjust(0, 0, 0.15, 0) : null));
		root.setOnMouseClicked(e -> {
			if (e.getButton()==PRIMARY && e.getClickCount()==2) {
				onAction(getItem(), e.isShiftDown());
				e.consume();
			}
		});
	}

	/**
	 * @return true if the item of this cell is not the same object as the item specified
	 */
	protected boolean isInvalidItem(Item item) {
		return getItem()!=item;
	}

	/**
	 * @return true if this cell is detached from the grid (i.e. not its child)
	 */
	protected boolean isInvalidVisibility() {
		// TODO: improve code, move to skin?
		// this.parent = row
		// this.parent.parent = grid
		return getParent()==null || getParent().getParent()==null;
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
		throwIfNotFxThread();

		if (isInvalidItem(item) || isInvalidVisibility()) return;

		if (item.cover_loadedThumb.get()) {
			setCoverPost(item, true, item.cover_file, item.cover, null);
		} else {
			ImageSize size = thumb.calculateImageLoadSize();
			throwIf(size.width<0 || size.height<0);

			// load thumbnail
			if (loader.executorThumbs!=null)
				loader.executorThumbs.execute(computeTask(() -> {
					RunnableLocked then = new RunnableLocked();
					loader.loadSynchronizerThumb.execute(then);

					if (isInvalidItem(item) || isInvalidVisibility()) {
						then.runNothing();
						return;
					}

					ImageSize IS = computeImageSize(item);
					boolean isInvisible = IS.width==-1 && IS.height==-1;
					if (isInvisible) {
						then.runNothing();
						return;
					}

					item.loadCover(false, IS)
						.ifOk(result -> then.run(() -> setCoverPost(item, result.wasLoaded, result.file, result.cover, then)))
						.ifError(e -> then.runNothing());
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
						};

						ImageSize IS = computeImageSize(item);
						boolean isInvisible = IS.width==-1 && IS.height==-1;
						if (isInvisible)  {
							then.runNothing();
							return;
						};

						if (isInvalidItem(item) || isInvalidVisibility())  {
							then.runNothing();
							return;
						};

						item.loadCover(true, size)
							.ifOk(result -> then.run(() -> setCoverPost(item, result.wasLoaded, result.file, result.cover, then)))
							.ifError(e -> then.runNothing());
					}
				));
		}
	}

	// TODO: remove, this should not be necessary
	// Sometimes cells are invisible (or not properly laid out?), so delay size calculation and block
	private ImageSize computeImageSize(Item item) {
		throwIfFxThread();
		return Stream.generate(() -> {
					ImageSize is =  Fut.fut()
							.supply(FX, () -> thumb.calculateImageLoadSize())
							.getDone();
					boolean isReady = is.width>0 || is.height>0;
					if (!isReady && getIndex()<0 || getIndex()>=gridView.get().getItemsShown().size()) return new ImageSize(-1,-1);
					if (!isReady) new RuntimeException("Image request size=" + is.width + "x" + is.height + " not valid").printStackTrace();
					if (!isReady) sleep(20);
					return isReady ? is : null;
				})
				.filter(Objects::nonNull)
				.limit(200)
				.findFirst().orElseThrow(() -> new RuntimeException("Could not determine requested image size " + item.cover_file));
	}

	private void setCoverLater(Item item) {
		throwIfNotFxThread();
		thumb.loadImage((File) null); // prevent displaying old content before cover loads
		setCoverLater.push(item);
	}

	private void setCoverPost(Item item, boolean imgAlreadyLoaded, File imgFile, Image img, RunnableLocked then) {
		runFX(() -> {
			if (isInvalidItem(item) || isInvalidVisibility() || img==null) {
				if (then!=null) then.finish();
				return;
			}

			boolean animate = computeAnimateOn().needsAnimation(this, imgAlreadyLoaded, img);
			thumb.loadImage(img, imgFile);
			if (then!=null) {
				if (img==null) then.finish();
				else doOnceIfImageLoaded(img, then::finish);
			}

			// stop any previous animation & revert visuals to normal
			imgLoadAnimation.stop();
			imgLoadAnimation.applyAt(1.0);

			// animate when image appears
			if (animate && img!=null) doOnceIfImageLoaded(img, imgLoadAnimation::play);
		});
	}

	public static class Loader {
		public final ExecutorService executorThumbs;
		public final ExecutorService executorImage;
		public final AtomicBoolean twoPass = new AtomicBoolean(false);
		public final ExecutorService loadSynchronizerThumb = newSingleDaemonThreadExecutor();
		public final ExecutorService loadSynchronizerFull = newSingleDaemonThreadExecutor();

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
//			if (r!=null) sleep(5);
		}
	}

	public enum AnimateOn {
		IMAGE_CHANGE, IMAGE_CHANGE_1ST_TIME, IMAGE_CHANGE_FROM_EMPTY;

		public boolean needsAnimation(GridFileThumbCell cell, boolean imgAlreadyLoaded, Image img) {
			if (this==IMAGE_CHANGE)
				return cell.thumb.image.get()!=img;
			else if (this==IMAGE_CHANGE_FROM_EMPTY)
				return cell.thumb.image.get()==null && img!=null;
			else if (this==IMAGE_CHANGE_1ST_TIME)
				return !imgAlreadyLoaded && img!=null;
			else
				throw new SwitchException(this);
		}
	}

}