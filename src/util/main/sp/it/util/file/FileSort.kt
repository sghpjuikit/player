package sp.it.util.file

import sp.it.util.Sort

/** Denotes [java.io.File] by type sort: directory first, file first or no preference. */
enum class FileSort constructor(val sort: Sort) {
   DIR_FIRST(Sort.DESCENDING),
   FILE_FIRST(Sort.ASCENDING),
   NONE(Sort.NONE)
}