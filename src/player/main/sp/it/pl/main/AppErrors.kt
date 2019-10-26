package sp.it.pl.main

import javafx.collections.FXCollections.observableArrayList
import mu.KotlinLogging
import sp.it.pl.plugin.notif.Notifier
import sp.it.util.async.FX
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.collections.ObservableListRO
import sp.it.util.dev.ThreadSafe
import sp.it.util.functional.Try
import sp.it.util.functional.ifNotNull

class AppError(val textShort: String, val textFull: String, val action: AppErrorAction? = null)

class AppErrorAction(val name: String, val action: () -> Unit)

object AppErrors {

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
         history.lastOrNull().ifNotNull {
            APP.plugins.use<Notifier> { n -> n.showTextNotification(it) }
         }
      }
   }

   /** Shows error detail for last error or does nothing if empty queue. Will happen on fx application thread. */
   @ThreadSafe
   fun showDetailForLastError() {
      runFX {
         history.lastOrNull().ifNotNull { APP.ui.errorPane.orBuild.show(it) }
      }
   }

}

private val logger = KotlinLogging.logger { }

@ThreadSafe
fun <R, E> Try<R, E>.ifErrorNotify(errorSupplier: (E) -> AppError) = ifError {
   val e = errorSupplier(it)
   if (it is Throwable) logger.error(it) { "Error occurred: ${e.textShort}" }
   AppErrors.push(e)
}

@ThreadSafe
fun <E: Throwable> E.errorNotify(errorSupplier: (E) -> AppError) = also { Try.error(it).ifErrorNotify(errorSupplier) }

@ThreadSafe
fun <R> Fut<R>.onErrorNotify(errorSupplier: (Throwable) -> AppError) = onError(FX) { it.errorNotify(errorSupplier) }