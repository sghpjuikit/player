package util.access.fieldvalue;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.xmp.XmpDirectory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import main.App;
import util.SwitchException;
import util.file.FileType;
import util.file.Util;
import util.file.mimetype.MimeType;
import util.functional.Functors.Ƒ1;
import util.units.FileSize;
import static util.Util.localDateTimeFromMillis;

/**
 * @author Martin Polakovic
 */
public class FileField<T> implements ObjectField<File,T> {

	public static final Set<FileField<?>> FIELDS = new HashSet<>();
	public static final FileField<String> PATH = new FileField<>("Path", "Path", File::getPath, String.class);
	public static final FileField<String> NAME = new FileField<>("Name", "Name", Util::getName, String.class);
	public static final FileField<String> NAME_FULL = new FileField<>("Filename", "Filename", Util::getNameFull, String.class);
	public static final FileField<String> EXTENSION = new FileField<>("Extension", "Extension", Util::getSuffix, String.class);
	public static final FileField<FileSize> SIZE = new FileField<>("Size", "Size", FileSize::new, FileSize.class);
	public static final FileField<LocalDateTime> TIME_MODIFIED = new FileField<>("Time Modified", "Time Modified", f -> localDateTimeFromMillis(f.lastModified()), LocalDateTime.class);
	public static final FileField<FileTime> TIME_CREATED = new FileField<>("Time Created", "Time Created", f -> {
		try {
			return Files.readAttributes(f.toPath(), BasicFileAttributes.class)
					.creationTime();
		} catch (IOException e) {
			return null;
		}
	}, FileTime.class);
	public static final FileField<Date> TIME_CREATED_FROM_TAG = new FileField<>("Time Created (tag)", "Time Created (metadata)", f -> {
		try {
			return ImageMetadataReader
					.readMetadata(f)
					.getFirstDirectoryOfType(XmpDirectory.class)
					.getDate(XmpDirectory.TAG_CREATE_DATE);
		} catch (IOException|ImageProcessingException|NullPointerException e) {
//			log(FileField.class).error("Could not read image xmp metadata for {}", f, e);
			return null;
		}
	}, Date.class);
	public static final FileField<FileTime> TIME_ACCESSED = new FileField<>("Time Accessed", "Time Accessed", f -> {
		try {
			return Files.readAttributes(f.toPath(), BasicFileAttributes.class)
					.lastAccessTime();
		} catch (IOException e) {
			return null;
		}
	}, FileTime.class);
	public static final FileField<FileType> TYPE = new FileField<>("Type", "Type", FileType::of, FileType.class);
	public static final FileField<MimeType> MIME = new FileField<>("Mime Type", "Mime Type", App.APP.mimeTypes::ofFile, MimeType.class);
	public static final FileField<String> MIME_GROUP = new FileField<>("Mime Group", "Mime Group", f -> App.APP.mimeTypes.ofFile(f).getGroup(), String.class);

	private final String name;
	private final String description;
	private final Ƒ1<? super File,? extends T> mapper;
	private final Class<T> type;

	FileField(String name, String description, Ƒ1<? super File,? extends T> extractor, Class<T> type) {
		this.name = name;
		this.description = description;
		this.mapper = extractor;
		this.type = type;
		FIELDS.add(this);
	}

	public static FileField<?> valueOf(String s) {
		if (PATH.name().equals(s)) return PATH;
		if (NAME.name().equals(s)) return NAME;
		if (NAME_FULL.name().equals(s)) return NAME_FULL;
		if (EXTENSION.name().equals(s)) return EXTENSION;
		if (SIZE.name().equals(s)) return SIZE;
		if (TIME_MODIFIED.name().equals(s)) return TIME_MODIFIED;
		if (TIME_CREATED.name().equals(s)) return TIME_CREATED;
		if (TIME_CREATED_FROM_TAG.name().equals(s)) return TIME_CREATED_FROM_TAG;
		if (TIME_ACCESSED.name().equals(s)) return TIME_ACCESSED;
		if (TYPE.name().equals(s)) return TYPE;
		if (MIME.name().equals(s)) return MIME;
		if (MIME_GROUP.name().equals(s)) return MIME_GROUP;
		throw new SwitchException(s);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public T getOf(File value) {
		return mapper.apply(value);
	}

	@Override
	public String description() {
		return description;
	}

	@Override
	public String toS(T o, String empty_val) {
		return o==null ? empty_val : o.toString();
	}

	@Override
	public Class<T> getType() {
		return type;
	}
}