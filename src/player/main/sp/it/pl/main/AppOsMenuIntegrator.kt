package sp.it.pl.main

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import sp.it.util.functional.Try
import sp.it.util.functional.runTry
import sp.it.util.system.Os
import sp.it.util.system.Os.WINDOWS

object AppOsMenuIntegrator {

   fun integrate(): Try<Unit, Throwable> = runTry {
      when (Os.current) {
         WINDOWS -> {
            Advapi32Util.registryCreateKey(WinReg.HKEY_CLASSES_ROOT, "*\\shell\\SpitPlayer\\command")
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CLASSES_ROOT, "*\\shell\\SpitPlayer", "", "Inspect in SpitPlayer")
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CLASSES_ROOT, "*\\shell\\SpitPlayer", "Icon", """"${APP.location.spitplayer_exe.absolutePath}"""")
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CLASSES_ROOT, "*\\shell\\SpitPlayer\\command", "", """"${APP.location.spitplayer_exe.absolutePath} inspect" "%1"""")
         }
         else -> throw UnsupportedOperationException("Supported only on Windows")
      }
   }

   fun disintegrate(): Try<Unit, Throwable> = runTry {
      when (Os.current) {
         WINDOWS -> Advapi32Util.registryDeleteKey(WinReg.HKEY_CLASSES_ROOT, "*\\shell\\SpitPlayer")
         else -> throw UnsupportedOperationException("Supported only on Windows")
      }
   }

}