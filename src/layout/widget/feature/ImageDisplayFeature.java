package layout.widget.feature;

import java.io.File;
import java.util.List;

/**
 * Displays image.
 *
 * @author Martin Polakovic
 */
@Feature(
	name = "Image display",
	description = "Displays image",
	type = ImageDisplayFeature.class
)
public interface ImageDisplayFeature {

	/**
	 * Displays the image.
	 *
	 * @param imgFile to display
	 */
	void showImage(File imgFile);

	/**
	 * Attempts to displays the images. Depends on implementation. By default
	 * 1st image is displayed if available.
	 *
	 * @param images to display
	 */
	default void showImages(List<File> images) {
		if (!images.isEmpty())
			showImage(images.get(0));
	}

}
