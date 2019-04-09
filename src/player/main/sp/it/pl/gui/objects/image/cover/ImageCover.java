package sp.it.pl.gui.objects.image.cover;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Objects;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import static sp.it.util.dev.FailKt.noNull;

/**
 * Denotes Cover represented by a {@link javafx.scene.image.Image} or {@link java.awt.image.BufferedImage}.
 * <p/>
 * This class is fully polymorphic
 * Should never be used directly but instead use the {@link sp.it.pl.gui.objects.image.cover.Cover} interface
 * and leverage polymorphism.
 */
public class ImageCover implements Cover {
	private final Image imageI;
	private final BufferedImage imageB;
	private final String info;

	public ImageCover(Image image, String description) {
		noNull(description);

		this.imageI = image;
		this.imageB = null;
		this.info = description;
	}

	public ImageCover(BufferedImage image, String description) {
		noNull(description);

		this.imageB = image;
		this.imageI = null;
		this.info = description;
	}

	@Override
	public Image getImage() {
		return imageB==null ? imageI : SwingFXUtils.toFXImage(imageB, null);
	}

	@Override
	public Image getImage(double width, double height) {
		return getImage();
	}

	@Override
	public File getFile() {
		return null;
	}

	@Override
	public boolean isEmpty() {
		return imageB==null && imageI==null;
	}

	@Override
	public String getDescription() {
		return info;
	}

	@Override
	public boolean equals(Object o) {
		if (o==this) return true;

		if (o!=null && o instanceof ImageCover) {
			ImageCover other = (ImageCover) o;
			if (imageB!=null && other.imageB!=null)
				return imageB.equals(other.imageB);
			if (imageI!=null && other.imageI!=null)
				return imageI.equals(other.imageI);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 37*hash + Objects.hashCode(this.imageI);
		hash = 37*hash + Objects.hashCode(this.imageB);
		return hash;
	}

}