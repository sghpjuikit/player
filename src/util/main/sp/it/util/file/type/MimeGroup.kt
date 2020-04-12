package sp.it.util.file.type

data class MimeGroup(val name: String) {

   companion object {
      const val audio = "audio"
      const val video = "video"
   }

}