package sp.it.pl.layout.widget.feature;

import java.io.File;
import java.util.Collection;

/**
 * Displays images.
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