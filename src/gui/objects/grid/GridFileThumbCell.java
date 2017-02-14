package gui.objects.grid;

import gui.objects.hierarchy.Item;
import gui.objects.image.ImageNode.ImageSize;
import gui.objects.image.Thumbnail;
import java.io.File;
import java.util.concurrent.ExecutorService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import main.App;
import util.SwitchException;
import util.animation.Anim;
import util.async.Async;
import util.async.executor.EventReducer;
import util.file.Util;
import static javafx.scene.input.MouseButton.PRIMARY;
import static util.async.Async.runFX;
import static util.dev.Util.noØ;
import static util.dev.Util.throwIfNotFxThread;

/**
 * Graphics representing the file. Cells are virtualized just like ListView or TableView does
 * it, but both vertically & horizontally. This avoids loading all files at once and allows
 * unlimited scaling.
 */
public class GridFileThumbCell extends GridCell<Item,File> {
	protected Pane root;
	protected Label name;
	protected Thumbnail thumb;
	protected final ExecutorService executorThumbs;
	protected final ExecutorService executorImage;
	protected final EventReducer<Item> setCoverLater;

	public GridFileThumbCell(ExecutorService executorThumbs, ExecutorService executorImage) {
		noØ(executorThumbs);
		this.executorThumbs = executorThumbs;
		this.executorImage = executorImage;
		setCoverLater = EventReducer.toLast(100, item -> executorThumbs.execute(computeTask(() -> {
			Async.sleep(10); // gives FX thread some space to avoid lag under intense workload
			runFX(() -> {
				if (item==getItem())
					setCoverNow(item);
			});
		})));
	}

	protected String computeName(Item item) {
		return Util.getName(item.val);
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
		if (getItem()==item) return;
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
			boolean loadLater = item.cover_loadedFull.get();
			if (loadLater) setCoverNow(item);
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
			protected Object getRepresentant() {
				return getItem()==null ? null : getItem().val;
			}
		};
		thumb.setBorderVisible(false);
		thumb.getPane().setSnapToPixel(true);
		thumb.getView().setSmooth(true);

		// TODO: remove workaround for fuzzy edges & incorrect layout
		// Problem: OS scaling will change width of the border, for non-integer widths it may produce visual artifacts
		// Solution: We adjust width so it can only scale into integer values.
		double BW = 1;
		double dpiScalingFix = Math.rint(BW*App.APP.windowManager.screenMaxScaling)/App.APP.windowManager.screenMaxScaling;
		BW *= dpiScalingFix;
				;
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
		if (item.cover_loadedFull.get()) {
			setCoverPost(item, true, item.cover_file, item.cover);
		} else {
			ImageSize size = thumb.calculateImageLoadSize();
			double w = size.width, h = size.height;

			// load thumbnail
			if (executorThumbs!=null)
				executorThumbs.execute(computeTask(() ->
						item.loadCover(false, w, h, (was_loaded, file, img) -> setCoverPost(item, was_loaded, file, img))
				));

			// load high quality thumbnail
			if (executorImage!=null)
				executorImage.execute(computeTask(() ->
						item.loadCover(true, w, h, (was_loaded, file, img) -> setCoverPost(item, was_loaded, file, img))
				));
		}
	}

	private void setCoverLater(Item item) {
		throwIfNotFxThread();
		thumb.loadImage((File) null); // prevent displaying old content before cover loads
		setCoverLater.push(item);
	}

	/** Finished loading of the cover. */
	private void setCoverPost(Item item, boolean imgAlreadyLoaded, File imgFile, Image img) {
		runFX(() -> {
			if (item==getItem()) { // prevents content inconsistency
				boolean animate = computeAnimateOn().needsAnimation(this, imgAlreadyLoaded, img);
				thumb.loadImage(img, imgFile);
				if (animate)
					new Anim(thumb.getView()::setOpacity).dur(400).intpl(x -> x*x*x*x).play();
			}
		});
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