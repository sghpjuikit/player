package sp.it.util.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static sp.it.util.Util.filenamizeString;
import static sp.it.util.dev.DebugKt.logger;

public interface Util {

	/**
	 * Returns true if for provided File all conditions are met:
	 * - is not null
	 * - exists
	 * - is directory
	 * - is readable
	 * - is writable
	 * <p>
	 * Use this method to see if the directory is not required to be valid.
	 *
	 * @return validity of directory for use
	 */
	static boolean isValidDirectory(File dir) {
		return dir!=null && dir.isDirectory() && dir.canRead() && dir.canWrite();
	}

	/**
	 * Checks whether directory is valid and if it is not, attempts
	 * to make it valid by setting appropriate permissions or creating it.
	 * Returns true if for provided File all conditions are met:
	 * - exists
	 * - is directory
	 * - is readable
	 * - is writable
	 * <p>
	 * Use this method to see if the directory is required to be valid.
	 *
	 * @param dir if not a directory, this method always returns false.
	 * @return whether the directory is usable.
	 * @throws NullPointerException if param null
	 */
	static boolean isValidatedDirectory(File dir) {
		boolean validity = true;
		if (!dir.exists()) validity &= dir.mkdirs();
		if (!dir.isDirectory()) return false;
		if (!dir.canRead()) validity &= dir.setReadable(true);
		if (!dir.canWrite()) validity &= dir.setWritable(true);
		return validity;
	}

	/**
	 * @return true iff file <ul> <li> not null <li> exists <li> is file <li> is readable </ul>
	 */
	static boolean isValidFile(File file) {
		return file!=null && file.isFile() && file.exists() && file.canRead();
	}

	static Stream<File> getFilesR(File dir, int depth) {
		return getFilesR(dir, depth, f -> true);
	}

	static Stream<File> getFilesR(Collection<File> files, int depth) {
		return files.stream().flatMap(d -> getFilesR(d, depth, f -> true));
	}

	/**
	 * @param depth the maximum number of levels of directories to visit. A value of 0 means that only the starting file
	 * is visited. Integer.MAX_VALUE may be used to indicate that all levels should be visited.
	 */
	static Stream<File> getFilesR(File dir, int depth, Predicate<? super File> filter) {
		if (dir.isDirectory()) {
			try {
				return Files.walk(dir.toPath(), depth).map(Path::toFile).filter(filter);
			} catch (IOException ex) {
				return Stream.empty();
			}
		}

		return filter.test(dir) ? Stream.of(dir) : Stream.empty();
	}

	/** @return first common parent directory for specified files or the parent if list size is 1 or null if empty or no shared root */
	static File getCommonRoot(Collection<File> files) {
		int size = files.size();
		if (size==0) return null;
		if (size==1) return files.stream().findFirst().map(f -> f.isDirectory() ? f : f.getParentFile()).get();

		File d = null;
		for (File f : files) {
			if (f!=null) {
				if (d==null) d = f;
				if (d.toPath().compareTo(f.toPath())<0) d = f;
			}
		}
		return d==null ? null : d.isFile() ? d.getParentFile() : d;
	}

	/** @return first common parent directory for specified files or the file if list size is 1 or null if empty or no shared root */
	static File getCommonFile(Collection<File> files) {
		int size = files.size();
		if (size==0) return null;
		if (size==1) return files.stream().findFirst().get();

		File d = null;
		for (File f : files) {
			if (f!=null) {
				if (d==null) d = f;
				if (d.toPath().compareTo(f.toPath())<0) d = f;
			}
		}
		return d==null ? null : d;
	}

	/**
	 * For files name with no extension is returned.
	 * For directories name is returned.
	 * Root directory returns 'X:\' string.
	 *
	 * @return name of the file without suffix
	 * @throws NullPointerException if parameter null
	 */
	private static String getName(File f) {
		String n = f.getName();
		if (n.isEmpty()) return f.toString();
		int i = n.lastIndexOf('.');
		return i==-1 ? n : n.substring(0, i);
	}

	// TODO: make robust and public
	private static void createFileIfNotExists(File file) {
		try {
			file.getParentFile().mkdirs();
			file.createNewFile();
		} catch (IOException e) {
			logger(Util.class).error("Creating file failed: {}", file, e);
		}
	}

	/**
	 * Writes a textual file with specified content, name and location.
	 *
	 * @param file file to create. If exists, it will be overwritten.
	 * @param content Text that will be written to the file.
	 * @return true if no IOException occurs else false
	 * @throws RuntimeException when param is directory
	 */
	static boolean writeFile(File file, String content) {
		if (file.isDirectory()) throw new RuntimeException("File must not be directory.");

		createFileIfNotExists(file);

		try (
				Writer writerF = new FileWriter(file);
				Writer writer = new BufferedWriter(writerF)
		) {
			writer.write(content);
			return true;
		} catch (IOException e) {
			logger(Util.class).error("Could not save file: {}", file, e);
			return false;
		}
	}

	/**
	 * Reads file as a text file and returns all its content as list of all lines,
	 * with newlines removed. Joining the lines with '\n' will build the original
	 * content.
	 *
	 * @return List of lines or empty list (if empty or on error). Never null.
	 */
	static List<String> readFileLines(String filePath) {
		try {
			return Files.readAllLines(Paths.get(filePath));
		} catch (IOException e) {
			boolean noSuchFile = e instanceof NoSuchFileException || e.getCause() instanceof NoSuchFileException;
			if (!noSuchFile) logger(Util.class).error("Problem reading file {}. File was not read.", filePath, e);
			return new ArrayList<>();
		}
	}

	static Stream<String> readFileLines(File f) {
		try {
			return Files.lines(f.toPath());
		} catch (IOException e) {
			boolean noSuchFile = e instanceof NoSuchFileException || e.getCause() instanceof NoSuchFileException;
			if (!noSuchFile) logger(Util.class).error("Problem reading file {}. File was not read.", f, e);
			return Stream.empty();
		}
	}

	/**
	 * Copies provided items to the provided directory.
	 * <p/>
	 * The method consumes I/O exception - that can occur when: an I/O error
	 * occurs when reading or writing.
	 * <p/>
	 * If source file equals the file of its copy, the file will not be copied.
	 *
	 * @param files list of files
	 * @param options optional. See {@link CopyOption} and Files.copy() methods
	 * @return list of files representing the successfully created copies - all copies that didn't throw IOException
	 */
	static List<File> copyFiles(List<File> files, File target, CopyOption... options) {
		List<File> out = new ArrayList<>();
		for (File f : files) {
			try {
				Path nf = target.toPath().resolve(f.toPath().getFileName());
				if (!nf.equals(f.toPath())) {
					Files.copy(f.toPath(), nf, options);
					out.add(new File(target, f.getName()));
				}
			} catch (IOException e) {
				logger(Util.class).error("Could not copy file {}", f, e);
			}
		}
		return out;
	}

	/**
	 * <p/>
	 * If source file equals the file of its copy, the file will not be copied.
	 */
	static void copyFile(File f, File target, String new_name, CopyOption... options) {
		try {
			File nf = new File(target, new_name + "." + getSuffix(f.toURI()));
			Files.copy(f.toPath(), nf.toPath(), options);
		} catch (IOException e) {
			logger(Util.class).error("Could not copy file {}", f, e);
		}
	}

	/**
	 * Equivalent to {@link #copyFile(java.io.File, java.io.File, java.lang.String, java.nio.file.CopyOption...) },
	 * but the copying will always take place and never overwrite existing file,
	 * as if there is any, it is backed up by renaming, utilizing {@link #renameAsOld(java.io.File) }
	 * <p/>
	 * If source file equals the file of its copy, the operation will not take place.
	 */
	static void copyFileSafe(File f, File target, String new_name, CopyOption... options) {
		try {
			String suffix = Util.getSuffix(f.toURI());
			String name = suffix.isEmpty() ? "cover" : "cover." + suffix;
			File nf = new File(target, new_name + "." + suffix);
			// avoid when files are the same (would produce nasty side effect of renaming
			// the file needlessly)
			if (f.equals(nf)) return;

			// backup old file
			Util.renameAsOld(new File(target, name));

			// copy file
			Files.copy(f.toPath(), nf.toPath(), options);
		} catch (IOException e) {
			logger(Util.class).error("Could not copy file {}", f, e);
		}
	}

	/**
	 * Copies file from given url, for example accessed over http protocol, into
	 * new file on a local file system specified by the parameter. Previously
	 * existing file is removed.
	 *
	 * @throws IOException when bad url or input or output file inaccessible
	 */
	static void saveFileAs(String url, File file) throws IOException {
		if (file.exists()) file.delete();
		URL u = new URL(url);
		try (
				InputStream is = u.openStream();
				OutputStream os = new FileOutputStream(file)) {
			byte[] b = new byte[2048];
			int length;
			while ((length = is.read(b))>0)
				os.write(b, 0, length);
		}
	}

	/**
	 * Same as {@link #saveFileAs(java.lang.String, java.io.File)} but instead
	 * destination directory is provided and the destination file will be put
	 * there and named according to the input url file name.
	 *
	 * @param dir directory
	 * @return the file denoting the new file.
	 */
	static File saveFileTo(String url, File dir) throws IOException {
		int i = url.lastIndexOf('/');
		if (i==-1) throw new IOException("url does not contain name. No '/' character found.");
		String name = filenamizeString(url.substring(1 + i));

		File df = new File(dir, name);
		saveFileAs(url, df);
		return df;
	}

	/**
	 * Renames file by suffixing it with a number, utilizing
	 * {@link #findFirstNonExistent(java.io.File, java.lang.String, java.lang.String, int)}
	 */
	static void renameAsOld(File f) {
		if (f!=null && f.exists()) {
			// remove name
			String suffix = getSuffix(f.toURI());
			f.renameTo(findFirstNonExistent(f.getParentFile(), getName(f), suffix, 1));
		}
	}

	/**
	 * For given directory, filename and file suffix, returns first available file
	 * suffixed by an auto-incrementing decimal number.
	 * <p/>
	 * Useful to avoid rewriting files on file move/copy.
	 */
	static File findFirstNonExistent(File location, String name, String suffix, int i) {
		File f = new File(location, name + "-" + i + "." + suffix);
		if (f.exists()) return findFirstNonExistent(location, name, suffix, i + 1);
		else return f;
	}

	/**
	 * Returns suffix after last '.' character of the path of the file or empty
	 * string if it doesn't contain the character.
	 * Note that the suffix will keep the case, which may break comparison.
	 *
	 * @return suffix after last '.' in the path or empty string
	 * @throws java.lang.NullPointerException if parameter null
	 */
	static String getSuffix(File f) {
		return getSuffix(f.getName());
	}

	/**
	 * Returns suffix after last '.' character of the path of the uri or empty
	 * string if it doesn't contain the character.
	 * Note that the suffix will keep the case, which may break comparison.
	 *
	 * @return suffix after last '.' in the path or empty string
	 * @throws java.lang.NullPointerException if parameter null or if uri path not defined
	 */
	static String getSuffix(URI f) {
		return getSuffix(f.getPath());
	}

	/**
	 * Returns suffix after last '.' character or empty string if doesn't contain the character.
	 * Note that the suffix will keep the case, which may break comparison.
	 *
	 * @return suffix after last '.' or empty string if does not contain '.'
	 * @throws java.lang.NullPointerException if parameter null
	 */
	static String getSuffix(String path) {
		int p = path.lastIndexOf('.');
		return (p<=-1) ? "" : path.substring(p + 1);
	}

	/**
	 * Renames file (with extension suffix).
	 *
	 * @param f file to rename, if does not exist nothing happens
	 * @param name new file name without suffix
	 */
	static void renameFile(File f, String name) {
		File rf = f.getParentFile().getAbsoluteFile();
		f.renameTo(new File(rf, filenamizeString(name)));
	}

	/**
	 * Renames file (extension suffix remains the same).
	 *
	 * @param f file to rename, if does not exist nothing happens
	 * @param name new file name without suffix
	 */
	static void renameFileNoSuffix(File f, String name) {
		File rf = f.getParentFile().getAbsoluteFile();
		int dot = f.getPath().lastIndexOf('.');
		String p = f.getPath();
		String ext = dot==-1 ? "" : p.substring(dot, p.length());
		f.renameTo(new File(rf, filenamizeString(name) + ext));
	}

}