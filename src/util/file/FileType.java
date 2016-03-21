
package util.file;

import java.io.File;

/**
 * Denotes type of [java.io.File]: directory or file.
 */
public enum FileType {
    FILE, DIRECTORY;

    public static FileType of(File f) {
        return f.isDirectory() ? DIRECTORY : FILE;
    }
}