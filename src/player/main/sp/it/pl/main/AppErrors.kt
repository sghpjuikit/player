package sp.it.pl.main

import javafx.collections.FXCollections.observableArrayList
import sp.it.pl.plugin.notif.Notifier
import sp.it.util.async.runFX
import sp.it.util.collections.ObservableListRO
import sp.it.util.dev.ThreadSafe
import sp.it.util.functional.Try

data class AppError(val textShort: String, val textFull: String)

object AppErrors {

   const val ERROR_NOTIFICATION_TITLE = "Error"
   private val historyImpl = observableArrayList<AppError>()
   /** Mutable read-only view of error queue. Must be accessed from fx application thread. */
   val history = ObservableListRO<AppError>(historyImpl)

   @ThreadSafe
   fun push(textShort: String, textFull: String) = push(AppError(textShort, textFull))

   @ThreadSafe
   fun push(errorText: String) = push(AppError(errorText, errorText))

   /** Pushes an error to an error queue. Will happen on fx application thread. */
   @ThreadSafe
   fun push(error: AppError) {
      runFX {
         historyImpl += error
         showNotificationForLastError()
      }
   }

   /** Shows notification for last error or does nothing if empty queue. Will happen on fx application thread. */
   @ThreadSafe
   fun showNotificationForLastError() {
      runFX {
         history.lastOrNull()?.let {
            APP.plugins.use<Notifier> { n ->
               n.showTextNotification(ERROR_NOTIFICATION_TITLE, it.textShort + "\n\nClick to show details")
            }
         }
      }
   }

   /** Shows error detail for last error or does nothing if empty queue. Will happen on fx application thread. */
   @ThreadSafe
   fun showDetailForLastError() {
      runFX {
         history.lastOrNull()?.let {
            APP.ui.messagePane.orBuild.show(it)
         }
      }
   }

}

@ThreadSafe
fun <R, E> Try<R, E>.ifErrorNotify(errorSupplier: (E) -> AppError) = ifError { AppErrors.push(errorSupplier(it)) }