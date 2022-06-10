package sp.it.util.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Optional;

/**
 * Represents a Windows shortcut (typically visible to Java only as a '.lnk' file).
 * <p/>
 * Retrieved 2011-09-23 from <a href="http://stackoverflow.com/questions/309495/windows-shortcut-lnk-parser-in-java/672775#672775">...</a>
 * Originally called LnkParser
 * <p/>
 * Written by: (the stack overflow users, obviously!)
 * <br/>
 * Apache Commons VFS dependency removed by crysxd (why were we using that!?) <a href="https://github.com/crysxd">...</a>
 * <br/>
 * Header added, refactored and commented by Code Bling <a href="http://stackoverflow.com/users/675721/code-bling">...</a>
 * <br/>
 * Network file support added by Stefan Cordes <a href="http://stackoverflow.com/users/81330/stefan-cordes">...</a>
 * Adapted by Sam Brightman <a href="http://stackoverflow.com/users/2492/sam-brightman">...</a>
 * <br/>
 * Based on information in 'The Windows Shortcut File Format' by Jesse Hager &lt;jessehager@iname.com&gt;
 * <br/>
 * And somewhat based on code from the book 'Swing Hacks: Tips and Tools for Killer GUIs' by
 * Joshua Marinacci and Chris Adamson, SBN: 0-596-00907-0, <a href="http://www.oreilly.com/catalog/swinghks/">...</a>
 */
public class WindowsShortcut {
	private boolean isDirectory;
	private boolean isLocal;
	private String real_file;

	/**
	 * Provides a quick test to see if this could be a valid link !
	 * If you try to instantiate a new WindowShortcut and the link is not valid,
	 * Exceptions may be thrown and Exceptions are extremely slow to generate,
	 * therefore any code needing to loop through several files should first check this.
	 *
	 * @param file the potential link
	 * @return true if it may be a link, false otherwise
	 * @throws IOException if an IOException is thrown while reading from the file
	 */
	public static boolean isPotentialValidLink(File file) throws IOException {
		final int minimum_length = 0x64;
		try (InputStream fis = new FileInputStream(file)) {
			return file.isFile()
					&& file.getName().toLowerCase().endsWith(".lnk")
					&& fis.available()>=minimum_length
					&& isMagicPresent(getBytes(fis, 32));
		}
	}

	@SuppressWarnings("TryWithIdenticalCatches")
	public static WindowsShortcut of(File f) {
		try {
			return new WindowsShortcut(f);
		} catch (IOException e) {
			return null;
		} catch (ParseException e) {
			return null;
		}
	}

	@SuppressWarnings("TryWithIdenticalCatches")
	public static Optional<File> targetedFile(File f) {
		try {
			return Optional.of(new File(new WindowsShortcut(f).real_file));
		} catch (IOException e) {
			return Optional.empty();
		} catch (ParseException e) {
			return Optional.empty();
		}
	}

	public WindowsShortcut(File file) throws IOException, ParseException {
		try (InputStream in = new FileInputStream(file)) {
			parseLink(getBytes(in));
		}
	}

	/**
	 * @return the name of the filesystem object pointed to by this shortcut
	 */
	public String getRealFilename() {
		return real_file;
	}

	/**
	 * Tests if the shortcut points to a local resource.
	 *
	 * @return true if the 'local' bit is set in this shortcut, false otherwise
	 */
	public boolean isLocal() {
		return isLocal;
	}

	/**
	 * Tests if the shortcut points to a directory.
	 *
	 * @return true if the 'directory' bit is set in this shortcut, false otherwise
	 */
	public boolean isDirectory() {
		return isDirectory;
	}

	/**
	 * Gets all the bytes from an InputStream
	 *
	 * @param in the InputStream from which to read bytes
	 * @return array of all the bytes contained in 'in'
	 * @throws IOException if an IOException is encountered while reading the data from the InputStream
	 */
	private static byte[] getBytes(InputStream in) throws IOException {
		return getBytes(in, null);
	}

	/**
	 * Gets up to max bytes from an InputStream
	 *
	 * @param in the InputStream from which to read bytes
	 * @param max maximum number of bytes to read
	 * @return array of all the bytes contained in 'in'
	 * @throws IOException if an IOException is encountered while reading the data from the InputStream
	 */
	private static byte[] getBytes(InputStream in, Integer max) throws IOException {
		// read the entire file into a byte buffer
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte[] buff = new byte[256];
		while (max==null || max>0) {
			int n = in.read(buff);
			if (n==-1) {
				break;
			}
			bout.write(buff, 0, n);
			if (max!=null)
				max -= n;
		}
		in.close();
		return bout.toByteArray();
	}

	private static boolean isMagicPresent(byte[] link) {
		final int magic = 0x0000004C;
		final int magic_offset = 0x00;
		return link.length>=32 && bytesToDWord(link, magic_offset)==magic;
	}

	/**
	 * Gobbles up link data by parsing it and storing info in member fields
	 *
	 * @param link all the bytes from the .lnk file
	 */
	@SuppressWarnings("SpellCheckingInspection")
	private void parseLink(byte[] link) throws ParseException {
		try {
			if (!isMagicPresent(link))
				throw new ParseException("Invalid shortcut; magic is missing", 0);

			// get the flags byte
			byte flags = link[0x14];

			// get the file attributes byte
			final int file_atts_offset = 0x18;
			byte file_atts = link[file_atts_offset];
			byte is_dir_mask = (byte) 0x10;
			isDirectory = (file_atts&is_dir_mask)>0;

			// if the shell settings are present, skip them
			final int shell_offset = 0x4c;
			final byte has_shell_mask = (byte) 0x01;
			int shell_len = 0;
			if ((flags&has_shell_mask)>0) {
				// the plus 2 accounts for the length marker itself
				shell_len = bytesToWord(link, shell_offset) + 2;
			}

			// get to the file settings
			int file_start = 0x4c + shell_len;

			final int file_location_info_flag_offset_offset = 0x08;
			int file_location_info_flag = link[file_start + file_location_info_flag_offset_offset];
			isLocal = (file_location_info_flag&2)==0;
			// get the local volume and local system values
			//final int localVolumeTable_offset_offset = 0x0C;
			final int basename_offset_offset = 0x10;
			final int networkVolumeTable_offset_offset = 0x14;
			final int finalname_offset_offset = 0x18;
			int finalname_offset = link[file_start + finalname_offset_offset] + file_start;
			String finalname = getNullDelimitedString(link, finalname_offset);
			if (isLocal) {
				int basename_offset = link[file_start + basename_offset_offset] + file_start;
				String basename = getNullDelimitedString(link, basename_offset);
				real_file = basename + finalname;
			} else {
				int networkVolumeTable_offset = link[file_start + networkVolumeTable_offset_offset] + file_start;
				int shareName_offset_offset = 0x08;
				int shareName_offset = link[networkVolumeTable_offset + shareName_offset_offset]
						+ networkVolumeTable_offset;
				String shareName = getNullDelimitedString(link, shareName_offset);
				real_file = shareName + "\\" + finalname;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ParseException("Could not be parsed, probably not a valid WindowsShortcut", 0);
		}
	}

	private static String getNullDelimitedString(byte[] bytes, int off) {
		int len = 0;
		// count bytes until the null character (0)
		while (bytes[off + len]!=0) {
			len++;
		}
		return new String(bytes, off, len);
	}

	/*
	 * convert two bytes into a short note, this is little endian because it's
	 * for an Intel only OS.
	 */
	@SuppressWarnings("SameParameterValue")
	private static int bytesToWord(byte[] bytes, int off) {
		return ((bytes[off + 1]&0xff)<<8)|(bytes[off]&0xff);
	}

	@SuppressWarnings("SameParameterValue")
	private static int bytesToDWord(byte[] bytes, int off) {
		return (bytesToWord(bytes, off + 2)<<16)|bytesToWord(bytes, off);
	}

}