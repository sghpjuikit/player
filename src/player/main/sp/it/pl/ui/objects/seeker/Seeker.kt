package sp.it.pl.ui.objects.seeker

enum class ChapterDisplayMode {
   NONE, POPUP_SHARED, POPUP_EACH;

   fun canBeShown() = this!=NONE

   fun isShownAsPopup() = this==POPUP_EACH || this==POPUP_SHARED

}

enum class ChapterDisplayActivation {
   HOVER, RIGHT_CLICK
}