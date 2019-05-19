package sp.it.pl.gui.objects.image;

import de.jensd.fx.glyphs.GlyphIcons;
import java.io.File;
import java.util.function.Consumer;
import sp.it.pl.gui.objects.placeholder.DragPane;
import sp.it.pl.main.AppDragKt;
import sp.it.util.async.future.Fut;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.DragEvent.DRAG_OVER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static sp.it.pl.main.AppDragKt.getImageFile;
import static sp.it.pl.main.AppDragKt.hasImageFileOrUrl;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.async.future.Fut.fut;
import static sp.it.util.file.FileType.FILE;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.system.EnvironmentKt.chooseFile;

/**
 * Thumbnail which can accept a file. A custom action invoked afterwards can be
 * defined. Thumbnail has a highlight mode showed on hover.
 * <p/>
 * File can be accepted either by using file chooser opened by clicking on this
 * thumbnail, or by file drag&drop.
 */
public class ThumbnailWithAdd extends Thumbnail {

	private final DragPane.Data dragData;
	/**
	 * Action for when image file is dropped or received from file chooser.
	 * Default does nothing. Null indicates no action.
	 * <p/>
	 * Obtaining the image file may be blocking operation (hence the {@link Fut}). The future will
	 * never be null, but the obtained image can be null!
	 */
	public Consumer<Fut<File>> onFileDropped = null;
	/**
	 * Action for when image is highlighted.
	 * Default does nothing. Must not be null.
	 */
	public Consumer<Boolean> onHighlight = v -> {};

	public ThumbnailWithAdd() {
		this(DETAILS, "Set Image");
	}

	public ThumbnailWithAdd(GlyphIcons dragIcon, String dragDescription) {
		super();
		dragData = new DragPane.Data(() -> dragDescription, dragIcon);

		// highlight on hover | drag
		root.addEventHandler(MOUSE_EXITED, e -> highlight(false));
		root.addEventHandler(MOUSE_ENTERED, e -> highlight(true));
		root.addEventHandler(DRAG_OVER, e -> { if (hasImageFileOrUrl(e.getDragboard())) onHighlight.accept(true); });
		root.addEventHandler(DRAG_EXITED, e -> onHighlight.accept(false));

		// add image on click
		root.addEventHandler(MOUSE_CLICKED, e -> {
			if (e.getButton()==PRIMARY) {
				chooseFile("Select image to add to tag", FILE, APP.DIR_APP, root.getScene().getWindow())
						.ifOkUse(file -> {
							if (onFileDropped!=null)
								onFileDropped.accept(fut(file));
						});
				e.consume();
			}
		});

		// drag&drop
		installDrag(
				root, dragIcon, dragDescription,
				e -> hasImageFileOrUrl(e.getDragboard()),
				e -> {
	                // Fut<File> fi = getImage(e);
	                // File i = fi.isDone() ? fi.getDone() : null;
	                // boolean same = i!=null && i.equals(except.get());
					File i = getImageFile(e.getDragboard());
					return i!=null && i.equals(getFile());  // false if image file is already displayed
				},
				consumer(e -> {
					if (onFileDropped!=null)
						onFileDropped.accept(AppDragKt.getImageFileOrUrl(e.getDragboard()));
				})
		);
	}

	private void highlight(boolean v) {
		if (v) DragPane.PANE.getM(dragData).showFor(root);
		else DragPane.PANE.get().hide();

		onHighlight.accept(v);
	}

}