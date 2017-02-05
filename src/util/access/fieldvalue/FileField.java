package util.access.fieldvalue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.Date;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.xmp.XmpDirectory;

import main.App;
import util.file.FileType;
import util.file.Util;
import util.file.mimetype.MimeType;
import util.functional.Functors.Ƒ1;
import util.units.FileSize;

import static util.Util.localDateTimeFromMillis;
import static util.type.Util.mapEnumConstantName;

/**
 *
 * @author Martin Polakovic
 */
public enum FileField implements ObjectField<File> {

    PATH("Path", File::getPath, String.class),
    NAME("Name", Util::getName, String.class),
    NAME_FULL("Filename", Util::getNameFull, String.class),
    EXTENSION("Extension", Util::getSuffix, String.class),
    SIZE("Size", FileSize::new, FileSize.class),
    TIME_MODIFIED("Time Modified", f -> localDateTimeFromMillis(f.lastModified()), LocalDateTime.class),
    TIME_CREATED("Time Created", f -> {
        try {
            return Files.readAttributes(f.toPath(), BasicFileAttributes.class)
	                    .creationTime();
        } catch (IOException e) {
            return null;
        }
    }, FileTime.class),
	TIME_CREATED_FROM_TAG("Time Created (metadata)", f -> {
		try {
			return ImageMetadataReader
				       .readMetadata(f)
					   .getFirstDirectoryOfType(XmpDirectory.class)
					   .getDate(XmpDirectory.TAG_CREATE_DATE);
		} catch (IOException | ImageProcessingException | NullPointerException e) {
//			log(FileField.class).error("Could not read image xmp metadata for {}", f, e);
			return null;
		}
	}, Date.class),
    TIME_ACCESSED("Time Accessed", f -> {
        try {
            return Files.readAttributes(f.toPath(), BasicFileAttributes.class)
	                    .lastAccessTime();
        } catch (IOException e) {
            return null;
        }
    }, FileTime.class),
    TYPE("Type", FileType::of, FileType.class),
    MIME("Mime Type", App.APP.mimeTypes::ofFile, MimeType.class),
    MIME_GROUP("Mime Group", f -> App.APP.mimeTypes.ofFile(f).getGroup(), String.class);

    private final String description;
    private final Ƒ1<File,?> mapper;
    private final Class<?> type;

    <T> FileField(String description, Ƒ1<File,T> mapper, Class<T> type){
        mapEnumConstantName(this, util.Util::enumToHuman);
        this.mapper = mapper;
        this.description = description;
        this.type = type;
    }

    @Override
    public Object getOf(File value) {
        return mapper.apply(value);
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String toS(Object o, String empty_val) {
        return o==null ? empty_val : o.toString();
    }

    @Override
    public Class getType() {
        return type;
    }
}