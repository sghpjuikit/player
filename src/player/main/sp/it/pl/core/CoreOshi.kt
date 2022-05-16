package sp.it.pl.core

import oshi.util.GlobalConfig

class CoreOshi: Core {

   override fun init() {
      // Fixes an issue where corrupted registers cause crashes
      // https://github.com/oshi/oshi/issues/2015#issuecomment-1105638671
      GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PERFOS_DIABLED, false)
      GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PERFPROC_DIABLED, false)
      GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_PERFDISK_DIABLED, false)
   }

}