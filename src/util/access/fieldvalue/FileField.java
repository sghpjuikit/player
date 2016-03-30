/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.fieldvalue;

import java.io.File;
import java.time.LocalDateTime;

import util.file.Util;
import util.functional.Functors.Ƒ1;
import util.units.FileSize;

import static audio.tagging.Metadata.localDateTimeFromMillis;
import static util.type.Util.mapEnumConstantName;

/**
 *
 * @author Martin Polakovic
 */
public enum FileField implements ObjectField<File> {

    PATH("Path",File::getPath, String.class),
    NAME("Name", Util::getName, String.class),
    NAME_FULL("Gilename", Util::getNameFull, String.class),
    EXTENSION("Extension", Util::getSuffix, String.class),
    SIZE("Size",FileSize::new, FileSize.class),
    TIME_MODIFIED("Time Modified",f -> localDateTimeFromMillis(f.lastModified()), LocalDateTime.class),
    TYPE("Type",f -> f.isDirectory() ? "Directory" : Util.getSuffix(f), String.class);

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