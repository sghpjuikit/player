package sp.it.pl.core

import com.sun.jna.LastErrorException
import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary
import java.io.File
import org.jetbrains.annotations.Blocking
import sp.it.pl.main.APP
import sp.it.pl.main.Events.AppEvent.SystemSleepEvent
import sp.it.util.async.future.awaitFxOrBlock
import sp.it.util.async.runFX
import sp.it.util.dev.fail
import sp.it.util.functional.Try
import sp.it.util.functional.runTry
import sp.it.util.system.Os
import sp.it.util.system.execRaw

/** API to access OS. Use only on `JavaFX Application Thread` unless specified otherwise. */
object CoreOs: Core {

   /** Sleeps OS. Fires [SystemSleepEvent.Pre] and waits till listeners complete, then sleeps and fires [SystemSleepEvent.Start]. */
   fun sleep(): Try<Unit, Throwable> = runTry {
      runFX { APP.actionStream(SystemSleepEvent.Pre) }.awaitFxOrBlock()
      when (Os.current) {
         Os.WINDOWS -> Runtime.getRuntime().execRaw("rundll32.exe powrprof.dll,SetSuspendState 0,1,0")
         Os.OSX -> Runtime.getRuntime().execRaw("pmset sleepnow")
         Os.UNIX -> Runtime.getRuntime().execRaw("systemctl suspend")
         Os.UNKNOWN -> fail { "Unsupported OS" }
      }
   }

   /** Hibernates OS. Fires [SystemSleepEvent.Pre] and waits till listeners complete, then sleeps and fires [SystemSleepEvent.Start]. */
   fun hibernate(): Try<Unit, Throwable> = runTry {
      runFX { APP.actionStream(SystemSleepEvent.Pre) }.awaitFxOrBlock()
      when (Os.current) {
         Os.WINDOWS -> Runtime.getRuntime().execRaw("rundll32.exe powrprof.dll,SetSuspendState Hibernate")
         Os.OSX -> Runtime.getRuntime().execRaw("pmset hibernatemode 25; pmset sleepnow")
         Os.UNIX -> Runtime.getRuntime().execRaw("systemctl hibernate")
         Os.UNKNOWN -> fail { "Unsupported OS" }
      }
   }

   /** Shuts down OS. */
   fun shutdown(): Try<Unit, Throwable> = runTry {
      when (Os.current) {
         Os.WINDOWS -> Runtime.getRuntime().execRaw("shutdown /s /t 0")
         Os.OSX -> Runtime.getRuntime().execRaw("/sbin/shutdown -h now")
         Os.UNIX -> Runtime.getRuntime().execRaw("shutdown -h now")
         Os.UNKNOWN -> fail { "Unsupported OS" }
      }
   }

   /** Restarts OS. */
   fun restart(): Try<Unit, Throwable> = runTry {
      when (Os.current) {
         Os.WINDOWS -> Runtime.getRuntime().execRaw("shutdown -r -t 0")
         Os.OSX -> Runtime.getRuntime().execRaw("/sbin/shutdown -r now")
         Os.UNIX -> Runtime.getRuntime().execRaw("shutdown -r now")
         Os.UNKNOWN -> fail { "Unsupported OS" }
      }
   }

   /** Logs off OS user. */
   fun logOff(): Try<Unit, Throwable> = runTry {
      when (Os.current) {
         Os.WINDOWS -> Runtime.getRuntime().execRaw("shutdown -l -t 0")
         Os.OSX -> Runtime.getRuntime().execRaw("osascript -e 'tell app \"System Events\" to log out'")
         Os.UNIX -> Runtime.getRuntime().execRaw("dm-tool switch-to-greeter")
         Os.UNKNOWN -> fail { "Unsupported OS" }
      }
   }

   /** Locks OS screen. */
   fun lock(): Try<Unit, Throwable> = runTry {
      when (Os.current) {
         Os.WINDOWS -> Runtime.getRuntime().execRaw("Rundll32.exe user32.dll,LockWorkStation")
         Os.OSX -> Runtime.getRuntime().execRaw("/System/Library/CoreServices/'Menu Extras'/User.menu/Contents/Resources/CGSession -suspend")
         Os.UNIX -> Runtime.getRuntime().execRaw("dm-tool lock || xscreensaver-command -lock")
         Os.UNKNOWN -> fail { "Unsupported OS" }
      }
   }

   /** Changes OS screewallpaper. */
   @Blocking
   @Suppress("LocalVariableName", "SpellCheckingInspection")
   fun changeWallpaper(file: File): Try<Unit, Throwable> = runTry {
      when (Os.current) {
         Os.WINDOWS -> {
            val MY_SPI_WALLPAPER = 0x0014 // Change wallpaper flag.
            val MY_SENDCHANGE = 1 // Send winini change
            val MY_UNUSED = 0 // unused parameter
            JnaWallpaper.user32.SystemParametersInfoA(MY_SPI_WALLPAPER, MY_UNUSED, file.absolutePath, MY_SENDCHANGE)
         }
         Os.OSX -> fail { "Unsupported OS" }
         Os.UNIX -> fail { "Unsupported OS" }
         Os.UNKNOWN -> fail { "Unsupported OS" }
      }
   }

   interface JnaWallpaper: StdCallLibrary {

      /**
       * Map function (Based on JNA/Microsoft document).
       * @param theUiAction Action to perform on UI
       * @param theUiParam Not used
       * @param thePath Path of a picture for desktop wallpaper
       * @param theFWinIni Not used
       * @return a boolean, not used
       */
      @Throws(LastErrorException::class)
      fun SystemParametersInfoA(theUiAction: Int, theUiParam: Int, thePath: String, theFWinIni: Int): Boolean

      companion object {
         @JvmStatic
         val user32 by lazy { Native.load("user32", JnaWallpaper::class.java) }
      }
   }

}