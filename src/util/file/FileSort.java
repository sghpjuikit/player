package util.file;

import util.Sort;

/**
 * Denotes {@link java.io.File} sort: directory first, file first or no preference.
 */
public enum FileSort {
    DIR_FIRST(Sort.DESCENDING),
    FILE_FIRST(Sort.ASCENDING),
    NONE(Sort.NONE);

    public final Sort sort;

    FileSort(Sort s) {
        sort = s;
    }
}