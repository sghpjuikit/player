package sp.it.pl.audio.tagging

import javafx.util.Duration
import sp.it.pl.audio.Player
import sp.it.pl.util.access.fieldvalue.ObjectFieldBase
import sp.it.pl.util.dev.failCase
import sp.it.pl.util.units.FileSize
import sp.it.pl.util.units.RangeYear
import sp.it.pl.util.units.toHMSMs
import java.util.ArrayList
import java.util.HashSet
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.streams.asSequence

/** Group of [Metadata] by some [Metadata.Field] providing some group data. */
class MetadataGroup {
    val field: Metadata.Field<*>
    val isAll: Boolean
    val value: Any?
    val itemCount: Long
    val albumCount: Long
    val lengthInMs: Double
    val fileSizeInB: Long
    val avgRating: Double
    val weighRating: Double
    val year: RangeYear
    val grouped: List<Metadata>

    private constructor(field: Metadata.Field<*>, isAll: Boolean, value: Any?, ms: Collection<Metadata>) {
        this.field = field
        this.isAll = isAll
        this.value = value
        grouped = ArrayList(ms)
        itemCount = ms.size.toLong()
        year = RangeYear()
        val albumSet = HashSet<String?>()
        var lengthSum = 0.0
        var sizeSum: Long = 0
        var ratingSum = 0.0
        for (m in ms) {
            albumSet += m.getAlbum()
            lengthSum += m.getLengthInMs()
            sizeSum += m.getFileSizeInB()
            ratingSum += m.getRatingPercentOr0()
            year += m.getYearAsInt()
        }
        albumCount = albumSet.size.toLong()
        lengthInMs = lengthSum
        fileSizeInB = sizeSum
        avgRating = ratingSum/itemCount
        weighRating = avgRating*itemCount
    }

    /** @return the length */
    fun getLength() = Duration(lengthInMs)

    /** get total file size */
    fun getFileSize(): FileSize = FileSize(fileSizeInB)

    fun getValueS(empty_val: String): String = Field.VALUE.toS(this, value, empty_val)

    fun getMainField(): Field<*> = Field.VALUE

    /** @return true iff any of the songs belonging to this group is playing */
    fun isPlaying(): Boolean = field.getOf(Player.playingSong.value)==value

    override fun toString() = "$field: $value, items: $itemCount, albums: $albumCount, length: ${getLength()}, size: ${getFileSize()}, avgRating: $avgRating, wighted rating: $weighRating, year: $year"

    class Field<T: Any>: ObjectFieldBase<MetadataGroup, T> {

        internal constructor(type: KClass<T>, extractor: (MetadataGroup) -> T?, name: String, description: String): super(type, extractor, name, description) {
            FIELDS_IMPL.add(this)
        }

        fun toString(group: MetadataGroup): String = toString(group.field)

        fun toString(field: Metadata.Field<*>): String = if (this===VALUE) field.toString() else toString()

        @Suppress("UNCHECKED_CAST")
        fun getType(field: Metadata.Field<*>): Class<out T> = if (this===VALUE) field.type as Class<out T> else type

        override fun toS(o: T?, substitute: String): String {
            return when(this) {
                VALUE -> if (o==null || ""==o) "<none>" else o.toString()
                ITEMS, ALBUMS, LENGTH, SIZE, AVG_RATING, W_RATING -> o!!.toString()
                YEAR -> {
                    val y = o as RangeYear?
                    if (y==null || !y.hasSpecific()) substitute else y.toString()
                }
                else -> failCase(this)
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun toS(v: MetadataGroup, o: T?, substitute: String): String {
            return when(this) {
                VALUE -> if (v.isAll) "<any>" else (v.field as Metadata.Field<Any>).toS(o, "<none>")
                LENGTH -> (o as Duration).toHMSMs()
                else -> toS(o, substitute)
            }
        }

        override fun cVisible(): Boolean = this!==AVG_RATING && this!==YEAR && this!==W_RATING

        override fun cWidth(): Double = if (this===VALUE) 250.0 else 70.0

        companion object {
            private val FIELDS_IMPL = HashSet<Field<*>>()
            @JvmField val FIELDS: Set<Field<*>> = FIELDS_IMPL
            @JvmField val VALUE = Field(Any::class, { it.value }, "Value", "Song field to group by")
            @JvmField val ITEMS = Field(Long::class, { it.itemCount }, "Items", "Number of songs in the group")
            @JvmField val ALBUMS = Field(Long::class, { it.albumCount }, "Albums", "Number of albums in the group")
            @JvmField val LENGTH = Field(Duration::class, { it.getLength() }, "Length", "Total length of the group")
            @JvmField val SIZE = Field(FileSize::class, { it.getFileSize() }, "Size", "Total file size of the group")
            @JvmField val AVG_RATING = Field(Double::class, { it.avgRating }, "Avg rating", "Average rating of the group = sum(rating)/items")
            @JvmField val W_RATING = Field(Double::class, { it.weighRating }, "W rating", "Weighted rating of the group = sum(rating) = avg_rating*items")
            @JvmField val YEAR = Field(RangeYear::class, { it.year }, "Year", "Year or years of songs in the group")

            @JvmStatic fun valueOf(s: String): Field<*> = when (s) {
                ITEMS.name() -> ITEMS
                ALBUMS.name() -> ALBUMS
                LENGTH.name() -> LENGTH
                SIZE.name() -> SIZE
                AVG_RATING.name() -> AVG_RATING
                W_RATING.name() -> W_RATING
                YEAR.name() -> YEAR
                else -> VALUE
            }

        }

    }

    companion object {

        @JvmStatic fun groupOfUnrelated(ms: Collection<Metadata>) = MetadataGroup(Metadata.Field.ALBUM, true, null, ms)

        @JvmStatic
        fun groupOf(f: Metadata.Field<*>, ms: Collection<Metadata>) = MetadataGroup(f, true, getAllValue(f), ms)

        @JvmStatic fun groupsOf(f: Metadata.Field<*>, ms: Collection<Metadata>): Stream<MetadataGroup> {
            val getGroupedOf = f.getGroupedOf()
            return ms.asSequence().groupBy { getGroupedOf(it) }
                    .entries.stream()
                    .map { (key, value) -> MetadataGroup(f, false, key, value) }
        }

        @JvmStatic
        fun ungroup(groups: Collection<MetadataGroup>): Set<Metadata> = groups.asSequence().flatMap { it.grouped.asSequence() }.toSet()

        @JvmStatic
        fun ungroup(groups: Stream<MetadataGroup>): Set<Metadata> = groups.asSequence().flatMap { it.grouped.asSequence() }.toSet()

        // TODO: this may need some work"
        private fun getAllValue(f: Metadata.Field<*>): Any? = if (f.isTypeString) "" else null

    }
}