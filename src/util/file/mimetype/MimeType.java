package util.file.mimetype;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a mimetype with its possible extension. If multiple
 * extensions are provided, the first is the default.
 * <p/>
 * See more at:
 * <ul>
 * <li>http://stackoverflow.com/questions/51438/getting-a-files-mime-type-in-java
 * <li>http://stackoverflow.com/questions/7904497/is-there-an-enum-with-mime-types-in-java
 * <li>http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/net/MediaType.html
 * </ul>
 *
 * @author https://github.com/amr/mimetypes
 */
public class MimeType {

	public static final MimeType UNKNOWN = new MimeType("Unknown");

	private String mimeType;
	private String[] extensions;

	public MimeType(String mimeType, String... extensions) {
		this.mimeType = mimeType;
		this.extensions = extensions;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getGroup() {
		int i = mimeType.indexOf('/');
		return i<0 ? mimeType : mimeType.substring(0,i);
	}

	public boolean isOfType(String type) {
		return mimeType.equalsIgnoreCase(type);
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String getExtension() {
		if (extensions != null && extensions.length > 0) {
			return extensions[0];
		}

		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MimeType)) {
			return false;
		}
		MimeType mimeType1 = (MimeType) o;
		return Objects.equals(mimeType, mimeType1.mimeType) && Arrays.equals(extensions, mimeType1.extensions);
	}

	@Override
	public int hashCode() {
		int result = mimeType.hashCode();
		result = 47 * result + Arrays.hashCode(extensions);
		return result;
	}

	@Override
	public String toString() {
		return mimeType;
	}
}