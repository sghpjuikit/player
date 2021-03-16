package sp.it.pl.main

import javafx.collections.FXCollections.observableArrayList
import mu.KotlinLogging
import sp.it.pl.plugin.impl.Notifier
import sp.it.util.access.readOnly
import sp.it.util.access.v
import sp.it.util.async.FX
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.collections.readOnly
import sp.it.util.dev.ThreadSafe
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull

data class AppError(val textShort: String, val textFull: String, val action: AppErrorAction? = null)

data class AppErrorAction(val name: String, val action: () -> Unit)

object AppEventLog {

   private val historyImpl = observableArrayList<Any>()
   /** Mutable read-only view of error queue. Must be accessed from fx application thread. */
   val history = historyImpl.readOnly()
   private val hasErrorsImpl = v(false)
   /** True iff [history] contains an error. */
   val hasErrors = hasErrorsImpl.readOnly()

   init {
      APP.actionStream += {
         hasErrorsImpl.value = hasErrorsImpl.value || it is Throwable || it is AppError
         historyImpl += it
      }
   }

   @ThreadSafe
   fun push(textShort: String, textFull: String) = push(AppError(textShort, textFull))

   @ThreadSafe
   fun push(errorText: String) = push(AppError(errorText, errorText))

   /** Pushes an error to an error queue. Will happen on fx application thread. */
   @ThreadSafe
   fun push(error: AppError) {
      runFX {
         APP.actionStream(error)
         showNotificationForLastError()
      }
   }

   /** Shows notification for last error or does nothing if empty queue. Will happen on fx application thread. */
   @ThreadSafe
   fun showNotificationForLastError() {
      runFX {
         history.lastOrNull { it is Throwable || it is AppError }.ifNotNull {
            APP.plugins.use<Notifier> { n -> n.showTextNotification(it.asIs()) }
         }
      }
   }

   /** Shows error detail for last error or does nothing if empty queue. Will happen on fx application thread. */
   @ThreadSafe
   fun showDetailForLastError() {
      runFX {
         history.lastOrNull { it is Throwable || it is AppError }.ifNotNull { APP.ui.errorPane.orBuild.show(it) }
      }
   }

   /** Shows error detail for last error or does nothing if empty queue. Will happen on fx application thread. */
   @ThreadSafe
   fun showDetailForLast() {
      runFX {
         history.lastOrNull().ifNotNull { APP.ui.errorPane.orBuild.show(it) }
      }
   }

}

private val logger = KotlinLogging.logger { }

@ThreadSafe
fun <R, E> Try<R, E>.ifErrorNotify(errorSupplier: (E) -> AppError) = ifError { AppEventLog.push(errorSupplier(it)) }

@ThreadSafe
fun <E: Throwable> E.errorNotify(errorSupplier: (E) -> AppError) = also { Try.error(it).ifErrorNotify(errorSupplier) }

@ThreadSafe
fun <R> Fut<R>.onErrorNotify(errorSupplier: (Throwable) -> AppError) = onError(FX) { it.errorNotify(errorSupplier) }