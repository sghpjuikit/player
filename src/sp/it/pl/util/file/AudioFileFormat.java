package sp.it.pl.util.file;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;
import javafx.stage.FileChooser;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.util.SwitchException;
import static java.util.stream.Collectors.toList;
import static sp.it.pl.util.dev.FailKt.noNull;

/**
 * All audio file formats known and supported by application except for UNKNOWN that
 * servers for all the other file types.
 * Any operation with unsupported file will produce undefined behavior. They
 * should be discovered and ignored.
 */
public enum AudioFileFormat {
	mp3,
	ogg,
	flac,
	wav,
	m4a,
	mp4,

	spx,
	snd,
	aifc,
	aif,
	au,
	mp1,
	mp2,
	aac,

	UNKNOWN;

	/**
	 * Checks whether this format supported audio format. Unsupported formats
	 * don't get any official support for any of the app's features and by default
	 * are ignored.
	 *
	 * @return true if supported, false otherwise
	 */
	@SuppressWarnings("unused")
	public boolean isSupported(Use use) {
		switch (this) {
			case mp4:
			case m4a:
			case mp3:
			case ogg:
			case wav:
			case flac: return true;
			case spx:
			case snd:
			case aifc:
			case aif:
			case au:
			case mp1:
			case mp2:
			case aac:
			case UNKNOWN: return false;
			default: throw new SwitchException(this);
		}
	}

	public String toExt() {
		return "*." + toString();
	}

	public FileChooser.ExtensionFilter toExtFilter() {
		return new FileChooser.ExtensionFilter(toString(), toExt());
	}

	/** Returns whether writing the field to tag for this format is supported. */
	@SuppressWarnings("unused")
	public boolean isTagWriteSupported(Metadata.Field f) {
		switch (this) {
			case mp4:
			case m4a:
			case mp3:
			case ogg:
			case wav:
			case flac: return true;
			case spx:
			case snd:
			case aifc:
			case aif:
			case au:
			case mp1:
			case mp2:
			case aac:
			case UNKNOWN: return false;
			default: throw new SwitchException(this);
		}
	}

	/**
	 * Checks whether the item is of supported audio format. Unsupported file
	 * don't get any official support for any of the app's features and by default
	 * are ignored.
	 *
	 * @return true if supported, false otherwise
	 */
	public static boolean isSupported(Item item, Use use) {
		return of(item.getUri()).isSupported(use);
	}

	/**
	 * Checks whether the file has supported audio format. Unsupported file
	 * don't get any official support for any of the app's features and by default
	 * are ignored.
	 *
	 * @return true if supported, false otherwise
	 * @throws NullPointerException if param is null
	 */
	public static boolean isSupported(URI uri, Use use) {
		noNull(uri);
		return of(uri).isSupported(use);
	}

	/**
	 * Equivalent to {@code of(file.toURI()).isSupported(use)} using file.toURI().
	 *
	 * @throws NullPointerException if param is null
	 */
	public static boolean isSupported(File file, Use use) {
		noNull(file);
		return of(file.toURI()).isSupported(use);
	}

	/**
	 * Equivalent to {@link #isSupported(java.net.URI, sp.it.pl.util.file.AudioFileFormat.Use)} using URI.create(url). If
	 * the provided url can not be used to construct an URI, false is returned.
	 * On the other hand, if true is returned the validity of the url is guaranteed.
	 *
	 * @throws NullPointerException if param is null
	 */
	public static boolean isSupported(String url, Use use) {
		try {
			URI uri = new URI(url);
			return of(uri).isSupported(use);
		} catch (URISyntaxException e) {
			return false;
		}
	}

	/**
	 * Labels file as one of the audio file types the application recognizes.
	 */
	public static AudioFileFormat of(URI uri) {
		return of(uri.getPath());
	}

	/**
	 * Labels file as one of the audio file types the application recognizes.
	 */
	public static AudioFileFormat of(String path) {
		// do a quick job of it
		for (AudioFileFormat f : values())
			if (path.endsWith(f.name()))
				return f;
		// cover damaged or weird paths
		String suffix = Util.getSuffix(path);
		for (AudioFileFormat f : values())
			if (suffix.equalsIgnoreCase(f.toString()))
				return f;
		// no match
		return UNKNOWN;
	}

	/** Writes up list of all supported values. */
	public static String supportedExtensionsS(Use use) {
		String out = "";
		for (String ft : supportedExtensions(use))
			out = out + ft + "\n";
		return out;
	}

	public static List<AudioFileFormat> supportedValues(Use use) {
		return Stream.of(values()).filter(f -> f.isSupported(use)).collect(toList());
	}

	public static FileChooser.ExtensionFilter filter(Use use) {
		return new FileChooser.ExtensionFilter("Audio files", supportedExtensions(use));
	}

	// List of supported extension strings in the format: '*.extension'
	private static List<String> supportedExtensions(Use use) {
		return supportedValues(use).stream().map(AudioFileFormat::toExt).collect(toList());
	}

	public enum Use {
		APP,
		PLAYBACK,
		DB
	}
}

