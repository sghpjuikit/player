package util.file.mimetype;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import one.util.streamex.StreamEx;
import static util.dev.Util.log;
import static util.file.Util.getSuffix;
import static util.file.mimetype.MimeType.UNKNOWN;

/**
 * A utility registry of mime types, with lookups by mime type and by file
 * extensions.
 *
 * @author https://github.com/amr/mimetypes
 */
public class MimeTypes {

	/** Create empty mime type repository. */
	public static MimeTypes blank() {
		return new MimeTypes(false);
	}

	/** Create empty mime type repository. */
	public static MimeTypes standard() {
		return new MimeTypes(true);
	}

	private final Map<String,MimeType> types = new ConcurrentHashMap<>();
	private final Map<String,MimeType> extensions = new ConcurrentHashMap<>();

	private MimeTypes(boolean addStandardTypes) {
		if (addStandardTypes) {
			try (
					InputStream file = MimeTypes.class.getResourceAsStream("mime.types");
					InputStreamReader ir = new InputStreamReader(file, "UTF-8");
					BufferedReader br = new BufferedReader(ir)
			) {
				String line;
				while ((line = br.readLine())!=null)
					if (!line.isEmpty())
						loadOne(line);
			} catch (Exception e) {
				log(MimeTypes.class).error("Failed to load default mime types", e);
			}
		}
	}

	/**
	 * Load and register a single line that starts with the mime type proceeded
	 * by any number of whitespaces, then a whitespace separated list of
	 * valid extensions for that mime type.
	 *
	 * @param def Single mime type definition to load and register
	 * @return this
	 */
	public MimeTypes loadOne(String def) {
		if (def.startsWith("#")) return this;

		String[] halves = def.toLowerCase().split("\\s", 2);
		MimeType mimeType = new MimeType(halves[0], halves[1].trim().split("\\s"));
		return register(mimeType);
	}

	/**
	 * Register the given {@link MimeType} so it can be looked up later by mime
	 * type and/or extension.
	 *
	 * @param mimeType MimeType instance to register
	 * @return this
	 */
	public MimeTypes register(MimeType mimeType) {
		types.put(mimeType.getMimeType(), mimeType);
		for (String ext : mimeType.getExtensions()) {
			extensions.put(ext, mimeType);
		}
		return this;
	}

	/**
	 * Get a @{link MimeType} instance for the given mime type identifier from
	 * the loaded mime type definitions.
	 *
	 * @param type lower-case mime type identifier string
	 * @return Instance of MimeType for the given mime type identifier or null if none was found
	 */
	public MimeType ofType(String type) {
		return types.getOrDefault(type, UNKNOWN);
	}

	/**
	 * Get a @{link MimeType} instance for the given extension from the loaded
	 * mime type definitions.
	 *
	 * @param extension lower-case extension
	 * @return Instance of MimeType for the given ext or null if none was found
	 */
	public MimeType ofExtension(String extension) {
		return extensions.getOrDefault(extension.toLowerCase(), UNKNOWN);
	}

	public MimeType ofFile(File file) {
		return extensions.getOrDefault(getSuffix(file).toLowerCase(), UNKNOWN);
	}

	public MimeType ofURI(URI url) {
		return extensions.getOrDefault(getSuffix(url).toLowerCase(), UNKNOWN);
	}

	public Set<String> setOfGroups() {
		return StreamEx.ofValues(types).map(MimeType::getGroup).toSet();
	}

	public Set<MimeType> setOfMimeTypes() {
		return StreamEx.ofValues(types).toSet();
	}

	public Set<String> setOfExtensions() {
		return StreamEx.ofKeys(extensions).toSet();
	}

}