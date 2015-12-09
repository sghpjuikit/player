/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.FieldValue;

import java.io.File;
import java.time.LocalDateTime;

import util.File.FileUtil;
import util.Util;
import util.functional.Functors.Ƒ1;
import util.units.FileSize;

import static AudioPlayer.tagging.Metadata.localDateTimeFromMillis;
import static util.Util.mapEnumConstant;

/**
 *
 * @author Plutonium_
 */
public enum FileField implements ObjectField<File> {

    PATH("Path",File::getPath, String.class),
    NAME("Name",FileUtil::getName, String.class),
    NAME_FULL("Gilename",FileUtil::getNameFull, String.class),
    EXTENSION("Extension",FileUtil::getSuffix, String.class),
    SIZE("Size",FileSize::new, FileSize.class),
    TIME_MODIFIED("Time Modified",f -> localDateTimeFromMillis(f.lastModified()), LocalDateTime.class),
    TYPE("Type",f -> f.isDirectory() ? "Directory" : FileUtil.getSuffix(f), String.class);

    private final String description;
    private final Ƒ1<File,?> mapper;
    private final Class<?> type;

    private <T> FileField(String description, Ƒ1<File,T> mapper, Class<T> type){
        mapEnumConstant(this, Util::enumToHuman);
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