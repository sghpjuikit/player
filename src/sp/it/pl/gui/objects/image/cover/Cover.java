package sp.it.pl.gui.objects.image.cover;

import java.io.File;
import javafx.scene.image.Image;
import sp.it.pl.util.graphics.image.ImageSize;

public interface Cover {

	Cover EMPTY = new Cover() {
		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public Image getImage(double width, double height) {
			return null;
		}

		@Override
		public File getFile() {
			return null;
		}

		@Override
		public String getDescription() {
			return "";
		}

		@Override
		public boolean isEmpty() {
			return true;
		}
	};

	/**
	 * Returns the cover image.
	 */
	Image getImage();

	/**
	 * Returns the cover image as is if available or loads it from file into
	 * specified size. Doesn't guarantee the resulting size will match the specified.
	 *
	 * @param width requested width
	 * @param height requested height
	 * @return the cover image of at least requested size or smaller if the underlying image is smaller
	 */
	Image getImage(double width, double height);

	/**
	 * @see #getImage(double, double)
	 */
	default Image getImage(ImageSize size) {
		return getImage(size.width, size.height);
	}

	/**
	 * Returns file denoting the image. Only some implementations of Cover will
	 * return non null value. For example cover image obtained from tag will not
	 * have fle available.
	 *
	 * @return file for the image or null if none.
	 */
	File getFile();

	/**
	 * Human readable information about the cover. No guarantees about the
	 * format of the output. Do not parse.
	 * <br/>
	 * Example: "jpg 500x500"
	 *
	 * @return information about the cover or "" if not available. Never null.
	 */
	String getDescription();

	/**
	 * Cover is empty if it doesn't contain any resource that could be turned
	 * into an Image. For nonempty Cover method {@link #getImage()} at least
	 * must not return null value.
	 */
	boolean isEmpty();

	enum CoverSource {
		/** use tag as cover source */
		TAG,
		/** use parent directory image as source */
		DIRECTORY,
		/** use all of the sources in their respective order and return first find */
		ANY
	}
}