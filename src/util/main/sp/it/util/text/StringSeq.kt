package sp.it.util.text

interface StringSeq {
   val seq: Sequence<String>

   fun anyContains(text: String, ignoreCase: Boolean = false): Boolean = seq.any { it.contains(text, ignoreCase) }

   fun isEmpty() = size()==0

   fun size() = seq.count()

}

fun Sequence<String>.toStringSeq() = object: StringSeq {
   override val seq get() = this@toStringSeq
}