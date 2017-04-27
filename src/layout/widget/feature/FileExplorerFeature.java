package layout.widget.feature;

import java.io.File;
import java.util.Collection;
import static util.file.Util.getCommonRoot;

/**
 * File system viewer.
 */
@Feature(
	name = "Song info",
	description = "Displays the metadata information about the song",
	type = FileExplorerFeature.class
)
public interface FileExplorerFeature {

	/**
	 * Explores file in the file system hierarchy.
	 * Expands file tree up to the file.
	 * If file denotes directory, it will expand the directory too.
	 */
	void exploreFile(File f);

	/**
	 * Explores first common parent directory files in the file system hierarchy.
	 *
	 * @param files
	 * <ul>
	 * <li> if empty, does nothing
	 * <li> if has one file, explores the file
	 * <li> if has multiple files, explores their first common parent directory.
	 * </ul>
	 */
	default void exploreFiles(Collection<File> files) {
		File f = getCommonRoot(files);
		if (f!=null) exploreFile(f);
	}

}