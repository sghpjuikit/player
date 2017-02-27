package util.file;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import one.util.streamex.StreamEx;
import static util.dev.Util.noØ;
import static util.functional.Util.stream;

/**
 * All image file types known and supported by application except for UNKNOWN that
 * serves as a marker for all the other file types.
 * Any operation with unsupported file will produce undefined behavior.
 */
public enum ImageFileFormat {
	jpg,
	jpeg,
	bmp,
	png,
	gif,
	psd,
	UNKNOWN;

	public boolean isSupported() {
		return this!=UNKNOWN;
	}

	public String toExt() {
		return "*." + toString();
	}

	public ExtensionFilter toExtFilter() {
		return new FileChooser.ExtensionFilter(toString(), toExt());
	}

	public static StreamEx<ImageFileFormat> formats() {
		return stream(values()).without(UNKNOWN);
	}

	/**
	 * Checks whether the format is supported image format. Unsupported formats
	 * don't get any official support for any of the app's features and by default
	 * are ignored.
	 *
	 * @return true if supported, false otherwise
	 */
	public static boolean isSupported(ImageFileFormat f) {
		return f.isSupported();
	}

	/**
	 * Checks whether the file has supported image format. Unsupported formats
	 * don't get any official support for any of the app's features and by default
	 * are ignored.
	 *
	 * @return true if supported, false otherwise
	 */
	public static boolean isSupported(URI uri) {
		Objects.requireNonNull(uri);
		return of(uri).isSupported();
	}

	/**
	 * Equivalent to {@link #isSupported(java.net.URI)} using file.toURI().
	 */
	public static boolean isSupported(File file) {
		noØ(file);
		return of(file.toURI()).isSupported();
	}

	/**
	 * Equivalent to {@link #isSupported(java.net.URI)} using URI.create(url). If
	 * the provided url can not be used to construct an URI, false is returned.
	 * On the other hand, if true is returned the validity of the url is guaranteed.
	 *
	 * @throws NullPointerException if param is null
	 */
	public static boolean isSupported(String url) {
		try {
			URI uri = URI.create(url);
			return of(uri).isSupported();
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Labels file as one of the image file types the application recognizes.
	 */
	public static ImageFileFormat of(URI uri) {
		String suffix = Util.getSuffix(uri);
		for (ImageFileFormat f : values())
			if (suffix.equalsIgnoreCase(f.toString()))
				return f;
		return UNKNOWN;
	}

	/** Writes up list of all supported values. */
	public static String supportedExtensionsS() {
		String out = "";
		for (String ft : supportedExtensions())
			out = out + ft + "\n";
		return out;
	}

	public static List<ImageFileFormat> supportedValues() {
		List<ImageFileFormat> ext = new ArrayList<>();
		for (ImageFileFormat format : values()) {
			if (format.isSupported())
				ext.add(format);
		}
		return ext;
	}

	public static ExtensionFilter filter() {
		return new ExtensionFilter("Image files", supportedExtensions());
	}

	// List of supported extension strings in the format: '*.extension'
	private static List<String> supportedExtensions() {
		List<String> ext = new ArrayList<>();
		for (ImageFileFormat format : supportedValues()) {
			if (format.isSupported())
				ext.add(format.toExt());
		}
		return ext;
	}

}