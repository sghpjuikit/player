package sp.it.pl.ui.objects.grid

import java.io.File
import javafx.scene.image.Image
import sp.it.util.async.future.Fut
import sp.it.util.functional.Option
import sp.it.util.functional.orNull

/** Image loading state */
sealed interface ImageLoad {
   /** Image result or null if null result or loading is error, in progress or haven't started yet */
   val image: Image? get() = null
   /** File result or null if null result or loading is error, in progress or haven't started yet */
   val file: File? get() = null

   /** Image loading state - initial */
   object NotStarted: ImageLoad
   /** Image loading state - loading in progress */
   data class Loading(val loading: Fut<ImageLoad>): ImageLoad
   /** Image loading state - loading finished with error */
   object DoneErr: ImageLoad
   /** Image loading state - loading finished with result */
   data class DoneOk(override val image: Image?, override val file: File?): ImageLoad
   /** Image loading state - loading interrupted as no longer necessary */
   data class DoneInterrupted(val fileOpt: Option<File?>): ImageLoad {
      override val file = fileOpt.orNull()
   }
}