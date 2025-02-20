package sp.it.util.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static java.util.Objects.requireNonNull;
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
	@SuppressWarnings("ConstantConditions")
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

	/** @return first common parent directory for specified files or the parent if list size is 1 or null if empty or no shared root */
	static File getCommonRoot(Collection<File> files) {
		int size = files.size();
		if (size==0) return null;
		if (size==1) return files.stream().findFirst().map(f -> f.isDirectory() ? f : f.getParentFile()).orElseThrow();

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
		if (size==1) return files.stream().findFirst().orElseThrow();

		File d = null;
		for (File f : files) {
			if (f!=null) {
				if (d==null) d = f;
				if (d.toPath().compareTo(f.toPath())<0) d = f;
			}
		}
		return d;
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
				logger(Util.class).error(e, () -> "Could not copy file " + f);
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
			logger(Util.class).error(e, () -> "Could not copy file " + f);
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
			logger(Util.class).error(e, () -> "Could not copy file " + f);
		}
	}

	/**
	 * Copies file from given url, for example accessed over http protocol, into
	 * new file on a local file system specified by the parameter. Previously
	 * existing file is removed.
	 *
	 * @throws IOException when bad url or input or output file inaccessible
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
	static void saveFileAs(String url, File file) throws IOException {
		if (file.exists()) file.delete();
		if (!requireNonNull(file.getParentFile()).exists()) file.getParentFile().mkdirs();
		try (
			var is = URI.create(url).toURL().openStream();
			var os = new FileOutputStream(file)
		) {
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
	@SuppressWarnings("ResultOfMethodCallIgnored")
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

	private static String getSuffix(URI f) {
		return getSuffix(f.getPath());
	}

	private static String getSuffix(String path) {
		int p = path.lastIndexOf('.');
		return p==-1 ? "" : path.substring(p + 1);
	}

}