package sp.it.util.system

import java.io.File
import sp.it.util.JavaLegacy
import sp.it.util.functional.Try
import sp.it.util.functional.runTry

object Windows {

   fun sleep() = suspend(false, false, true)

   fun hibernate() = suspend(true, false, true)

   fun suspend(hibernate: Boolean, forceCritical: Boolean, disableWakeEvent: Boolean): Try<Unit, Throwable> = runTry {
      JavaLegacy.WindowsSuspend.SetSuspendState(hibernate, forceCritical, disableWakeEvent)
   }

   fun shutdown(): Try<Unit, Throwable> = runTry {
      Runtime.getRuntime().execRaw("shutdown -s -t 0")
   }

   fun restart(): Try<Unit, Throwable> = runTry {
      Runtime.getRuntime().execRaw("shutdown -r -t 0")
   }

   fun logOff(): Try<Unit, Throwable> = runTry {
      Runtime.getRuntime().execRaw("shutdown -l -t 0")
   }

   fun lock(): Try<Unit, Throwable> = runTry {
      Runtime.getRuntime().execRaw("Rundll32.exe user32.dll,LockWorkStation")
   }

   @Suppress("LocalVariableName", "SpellCheckingInspection")
   fun changeWallpaper(file: File): Try<Unit, Throwable> = runTry {
      val MY_SPI_WALLPAPER = 0x0014 // Change wallpaper flag.
      val MY_SENDCHANGE = 1 // Send winini change
      val MY_UNUSED = 0 // unused parameter
      JavaLegacy.JnaWallpaper.INSTANCE.SystemParametersInfoA(MY_SPI_WALLPAPER, MY_UNUSED, file.absolutePath, MY_SENDCHANGE)
   }

}