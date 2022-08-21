package sp.it.pl.audio.tagging

import javafx.util.Duration
import sp.it.pl.main.toUi
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.access.fieldvalue.ObjectFieldRegistry
import sp.it.util.functional.net
import sp.it.util.type.VType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.type
import sp.it.util.units.FileSize
import sp.it.util.units.RangeYear
import sp.it.util.units.toHMSMs

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
   val grouped: LinkedHashSet<Metadata>

   private constructor(field: Metadata.Field<*>, isAll: Boolean, value: Any?, ms: Collection<Metadata>) {
      this.field = field
      this.isAll = isAll
      this.value = value
      grouped = LinkedHashSet(ms)
      itemCount = ms.size.toLong()
      year = RangeYear()
      val albumSet = HashSet<String?>()
      var lengthSum = 0.0
      var sizeSum: Long = 0
      var ratingSum = 0.0
      for (m in ms) {
         albumSet += m.getAlbum()
         lengthSum += m.getLengthInMs()
         sizeSum += m.getFileSizeInB().net { if (it==FileSize.VALUE_NA) 0L else it }
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

   @Suppress("ClassName")
   sealed class Field<T>: ObjectFieldBase<MetadataGroup, T> {

      private constructor(type: VType<T>, extractor: (MetadataGroup) -> T, toUi: (T?, String) -> String, name: String, description: String): super(type, extractor, name, description, toUi)

      fun toString(group: MetadataGroup): String = toString(group.field)

      fun toString(field: Metadata.Field<*>): String = if (this===VALUE) field.toString() else toString()

      fun getMFType(field: Metadata.Field<*>): VType<*> = if (this===VALUE) field.typeGrouped else type

      override fun cVisible(): Boolean = this!==AVG_RATING && this!==YEAR && this!==W_RATING

      override fun cWidth(): Double = if (this===VALUE) 250.0 else 70.0

      object VALUE: Field<Any?>(type(), { it.value }, { o, or -> o?.toUi() ?: or }, "Value", "Song field to group by")
      object ITEMS: Field<Long>(type(), { it.itemCount }, { o, or -> o?.toUi() ?: or }, "Items", "Number of songs in the group")
      object ALBUMS: Field<Long>(type(), { it.albumCount }, { o, or -> o?.toUi() ?: or }, "Albums", "Number of albums in the group")
      object LENGTH: Field<Duration>(type(), { it.getLength() }, { o, or -> o?.toHMSMs(false) ?: or }, "Length", "Total length of the group")
      object SIZE: Field<FileSize>(type(), { it.getFileSize() }, { o, or -> o?.toUi() ?: or }, "Size", "Total file size of the group")
      object AVG_RATING: Field<Double>(type(), { it.avgRating }, { o, or -> o?.toUi() ?: or }, "Avg rating", "Average rating of the group = sum(rating)/items")
      object W_RATING: Field<Double>(type(), { it.weighRating }, { o, or -> o?.toUi() ?: or }, "W rating", "Weighted rating of the group = sum(rating) = avg_rating*items")
      object YEAR: Field<RangeYear>(type(), { it.year }, { o, or -> if (o==null || !o.hasSpecific()) or else o.toUi() }, "Year", "Year or years of songs in the group")

      companion object: ObjectFieldRegistry<MetadataGroup, Field<*>>(MetadataGroup::class) {

         init { registerDeclared() }

         @JvmStatic override fun valueOf(text: String) = super.valueOf(text) ?: VALUE
      }

   }

   companion object {

      fun groupOfUnrelated(ms: Collection<Metadata>) = MetadataGroup(Metadata.Field.PATH, true, null, ms)

      fun groupOf(f: Metadata.Field<*>, ms: Collection<Metadata>) = MetadataGroup(f, true, getAllValue(f), ms)

      fun groupsOf(f: Metadata.Field<*>, ms: Collection<Metadata>): List<MetadataGroup> {
         val groups = HashMap<Any?, ArrayList<Metadata>>()
         val accumulate = f.groupBuildAccumulator { key, m -> groups.getOrPut(key, ::ArrayList) += m }
         ms.forEach(accumulate)
         return groups.map { (key, value) -> MetadataGroup(f, false, key, value) }
      }

      fun ungroup(groups: Collection<MetadataGroup>): Set<Metadata> = groups.flatMapTo(HashSet()) { it.grouped }

      private fun getAllValue(f: Metadata.Field<*>): Any? = if (f.type.isSubclassOf<String>()) "<any>" else null

   }
}