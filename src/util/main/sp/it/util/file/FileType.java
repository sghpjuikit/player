package sp.it.util.file;

import java.io.File;
import sp.it.util.access.CyclicEnum;

/**
 * Denotes type of [java.io.File]: directory or file.
 */
public enum FileType implements CyclicEnum<FileType> {
	FILE, DIRECTORY;

	public static FileType of(File f) {
		return f.isDirectory() ? DIRECTORY : FILE;
	}
}