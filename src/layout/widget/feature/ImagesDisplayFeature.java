package layout.widget.feature;

import java.io.File;
import java.util.Collection;

/**
 * Displays images.
 *
 * @author Martin Polakovic
 */
@Feature(
	name = "Images display",
	description = "Displays images",
	type = ImagesDisplayFeature.class
)
public interface ImagesDisplayFeature {

	/**
	 * Displays the images.
	 */
	void showImages(Collection<File> imgFiles);

}