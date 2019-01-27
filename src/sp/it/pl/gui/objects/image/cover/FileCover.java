package sp.it.pl.gui.objects.image.cover;

import java.io.File;
import java.util.Objects;
import javafx.scene.image.Image;
import sp.it.pl.gui.objects.image.Thumbnail;
import sp.it.pl.util.graphics.image.ImageSize;
import sp.it.pl.util.graphics.image.ImageStandardLoader;
import static sp.it.pl.util.dev.FailKt.noNull;

/**
 * Denotes Cover represented by a {@link java.io.File}.
 * <p/>
 * This class is fully polymorphic
 * Should never be used directly but instead use the {@link sp.it.pl.gui.objects.image.cover.Cover} interface
 * and leverage polymorphism.
 */
public class FileCover implements Cover {
	private final File file;
	private final String info;

	public FileCover(File image, String description) {
		noNull(description);

		this.file = image;
		this.info = description;
	}

	@Override
	public Image getImage() {
		return file==null ? null : new Image(file.toURI().toString());
	}

	@Override
	public Image getImage(double width, double height) {
		if (file==null) return null;

		Image cached = Thumbnail.getCached(file, width, width);
		if (cached!=null) return cached;

		return ImageStandardLoader.INSTANCE.invoke(file, new ImageSize(width, height));
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public boolean isEmpty() {
		return file==null;
	}

	@Override
	public String getDescription() {
		return info;
	}

	@Override
	public boolean equals(Object o) {
		if (this==o) return true;

		if (o!=null && o instanceof FileCover) {
			FileCover other = (FileCover) o;
			return file.equals(other.file);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 43*hash + Objects.hashCode(this.file);
		return hash;
	}

}