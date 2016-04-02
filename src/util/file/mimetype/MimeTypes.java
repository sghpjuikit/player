package util.file.mimetype;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import one.util.streamex.StreamEx;

import static util.file.Util.getSuffix;

/**
 * A utility registry of mime types, with lookups by mime type and by file
 * extensions.
 * <p>
 * <p>The constructors, factory methods and load methods are not thread safe,
 * the exception to this is the {@link #getInstance()} method. BLookup methods
 * ({@link #ofType(String)} and {@link #ofExtension(String)}) are
 * thread-safe. Therefore, once initialized, instances may be used concurrently
 * by multiple threads.
 *
 * @author https://github.com/amr/mimetypes
 */
public class MimeTypes {
	private static final String COMMENT_PREFIX = "#";
	private static MimeTypes singleton = null;
	private final static Object singletonMonitor = new Object();

	/**
	 * Get the default instance which is initialized with the built-in mime
	 * types definitions on the first access to this method.
	 * <p>
	 * <p>This is thread-safe.
	 *
	 * @return default singleton instance with built-in mime types definitions
	 */
	public static MimeTypes getInstance() {
		if (singleton == null) {
			synchronized (singletonMonitor) {
				if (singleton == null) {
					singleton = new MimeTypes();
				}
			}
		}

		return singleton;
	}

	/**
	 * Get path to the default included mime types definition file.
	 *
	 * @return Standard path to the included mime types definitions
	 */
	public static Path getDefaultMimeTypesDefinition() {
		URL defaultDefinition = MimeTypes.class.getResource("mime.types");
		if (defaultDefinition == null) {
			throw new IllegalStateException("Could not find the built-in mime.types definition file");
		}

		try {
			if (defaultDefinition.getProtocol().startsWith("jar")) {
				URI uri = defaultDefinition.toURI();

				Map<String, String> env = new HashMap<>(1);
				env.put("create", "true");

				FileSystems.newFileSystem(uri, env);
			}
			return Paths.get(defaultDefinition.toURI());
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException("Error occurred while initializing from the default mime types definitions, " +
				                           "this is a bug", e);
		}
	}

	/**
	 * Create a new instance not initialized with any mime types definitions.
	 *
	 * @return New blank instance
	 */
	public static MimeTypes blank() {
		return new MimeTypes(new Path[0]);
	}


	private final Map<String, MimeType> types = new ConcurrentHashMap<>();
	private final Map<String, MimeType> extensions = new ConcurrentHashMap<>();

	public MimeTypes() {
		this(getDefaultMimeTypesDefinition());
	}

	/**
	 * Initialize the mime types definitions with given one or more mime
	 * types definition files in standard /etc/mime.types format.
	 *
	 * @param mimeTypesDefinitions Paths to mime types definition files
	 */
	public MimeTypes(Path... mimeTypesDefinitions) {
		for (Path f : mimeTypesDefinitions) {
			load(f);
		}
	}

	/**
	 * Parse and register mime type definitions from given path.
	 *
	 * @param def Path of mime type definitions file to load and register
	 * @return This instance of Mimetypes
	 */
	public MimeTypes load(Path def) {
		try {
			Files.readAllLines(def, StandardCharsets.US_ASCII).forEach(this::loadOne);
			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
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
		if (def.startsWith(COMMENT_PREFIX)) return this;

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
	 * @return Instance of MimeType for the given mime type identifier or null
	 * if none was found
	 */
	public MimeType ofType(String type) {
		return types.get(type);
	}

	/**
	 * Get a @{link MimeType} instance for the given extension from the loaded
	 * mime type definitions.
	 *
	 * @param extension lower-case extension
	 * @return Instance of MimeType for the given ext or null if none was found
	 */
	public MimeType ofExtension(String extension) {
		return extensions.get(extension.toLowerCase());
	}

	public MimeType ofFile(File file) {
		return extensions.get(getSuffix(file).toLowerCase());
	}

	public MimeType ofURI(URI url) {
		return extensions.get(getSuffix(url).toLowerCase());
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