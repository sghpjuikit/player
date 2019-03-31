package sp.it.pl.core

import sp.it.pl.main.APP
import sp.it.pl.util.file.div
import sp.it.pl.util.system.EnvironmentContext
import sp.it.pl.util.system.Os

object CoreEnv: Core {
    override fun init() {
        EnvironmentContext.defaultChooseFileDir = APP.DIR_APP
        EnvironmentContext.runAsProgramArgsTransformer = {
            when {
                Os.WINDOWS.isCurrent -> listOf((APP.DIR_APP/"elevate.exe").absolutePath)+it   // use elevate.exe to run command
                else -> it
            }
        }
    }
}