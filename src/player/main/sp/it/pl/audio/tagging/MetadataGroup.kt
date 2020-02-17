package sp.it.pl.audio.tagging

import javafx.util.Duration
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.fieldvalue.ObjectFieldRegistry
import sp.it.util.dev.failCase
import sp.it.util.functional.asIs
import sp.it.util.type.VType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.units.FileSize
import sp.it.util.units.RangeYear
import sp.it.util.units.toHMSMs
import java.util.ArrayList
import java.util.HashSet
import java.util.stream.Stream
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

   override fun toString() = "$field: $value, items: $itemCount, albums: $albumCount, length: ${getLength()}, size: ${getFileSize()}, avgRating: $avgRating, wighted rating: $weighRating, year: $year"

   class Field<T>: ObjectFieldBase<MetadataGroup, T> {

      private constructor(type: VType<T>, extractor: (MetadataGroup) -> T, name: String, description: String): super(type, extractor, name, description)

      fun toString(group: MetadataGroup): String = toString(group.field)

      fun toString(field: Metadata.Field<*>): String = if (this===VALUE) field.toString() else toString()

      @Suppress("UNCHECKED_CAST")
      fun getMFType(field: Metadata.Field<*>): VType<T> = if (this===VALUE) field.type.asIs() else type

      override fun toS(o: T?, substitute: String): String {
         return when (this) {
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
         return when (this) {
            VALUE -> if (v.isAll) "<any>" else (v.field as Metadata.Field<Any>).toS(o, "<none>")
            LENGTH -> (o as Duration).toHMSMs()
            else -> toS(o, substitute)
         }
      }

      override fun cVisible(): Boolean = this!==AVG_RATING && this!==YEAR && this!==W_RATING

      override fun cWidth(): Double = if (this===VALUE) 250.0 else 70.0

      @Suppress("RemoveExplicitTypeArguments")
      companion object: ObjectFieldRegistry<MetadataGroup, Field<*>>(MetadataGroup::class) {
         @JvmField val VALUE = this + Field(type<Any>(), { it.value }, "Value", "Song field to group by")
         @JvmField val ITEMS = this + Field(type<Long>(), { it.itemCount }, "Items", "Number of songs in the group")
         @JvmField val ALBUMS = this + Field(type<Long>(), { it.albumCount }, "Albums", "Number of albums in the group")
         @JvmField val LENGTH = this + Field(type<Duration>(), { it.getLength() }, "Length", "Total length of the group")
         @JvmField val SIZE = this + Field(type<FileSize>(), { it.getFileSize() }, "Size", "Total file size of the group")
         @JvmField val AVG_RATING = this + Field(type<Double>(), { it.avgRating }, "Avg rating", "Average rating of the group = sum(rating)/items")
         @JvmField val W_RATING = this + Field(type<Double>(), { it.weighRating }, "W rating", "Weighted rating of the group = sum(rating) = avg_rating*items")
         @JvmField val YEAR = this + Field(type<RangeYear>(), { it.year }, "Year", "Year or years of songs in the group")

         @JvmStatic override fun valueOf(text: String) = super.valueOf(text) ?: VALUE
      }

   }

   companion object {

      @JvmStatic
      fun groupOfUnrelated(ms: Collection<Metadata>) = MetadataGroup(Metadata.Field.PATH, true, null, ms)

      @JvmStatic
      fun groupOf(f: Metadata.Field<*>, ms: Collection<Metadata>) = MetadataGroup(f, true, getAllValue(f), ms)

      @JvmStatic
      fun groupsOf(f: Metadata.Field<*>, ms: Collection<Metadata>): Stream<MetadataGroup> {
         val getGroupedOf = f.getGroupedOf()
         return ms.asSequence().groupBy { getGroupedOf(it) }
            .entries.stream()
            .map { (key, value) -> MetadataGroup(f, false, key, value) }
      }

      fun ungroup(groups: Collection<MetadataGroup>): Set<Metadata> = groups.asSequence().flatMap { it.grouped.asSequence() }.toSet()

      fun ungroup(groups: Stream<MetadataGroup>): Set<Metadata> = groups.asSequence().flatMap { it.grouped.asSequence() }.toSet()

      // TODO: this may need some work"
      private fun getAllValue(f: Metadata.Field<*>): Any? = if (f.type.isSubclassOf<String>()) "" else null

   }
}