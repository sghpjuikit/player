package sp.it.pl.core

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.JoranException
import ch.qos.logback.core.util.StatusPrinter2
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.lang.Thread.UncaughtExceptionHandler
import java.util.logging.LogManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import sp.it.util.async.runFX
import sp.it.util.reactive.Handler1

class CoreLogging(val loggingConfigurationFile: File, val loggingOutputDir: File, val events: Handler1<Any>): Core {
   val uncaughtExceptionHandler = UncaughtExceptionHandler { _, e -> runFX { events(e) } }
   val statusPrinter = StatusPrinter2()
   
   override fun init() {
      // redirect java util logging to sl4j
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()

      // configure slf4 logging
      val lc = LoggerFactory.getILoggerFactory() as LoggerContext
      try {
         JoranConfigurator().apply {
            context = lc.apply {
               reset()
               putProperty("LOG_DIR", loggingOutputDir.path)
            }
            doConfigure(loggingConfigurationFile)    // override default configuration
         }
      } catch (ex: JoranException) {
         logger.error { ex.message }
      }
      statusPrinter.printInCaseOfErrorsOrWarnings(lc)

      // log uncaught thread termination exceptions
      Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler)
   }

   fun changeLogBackLoggerAppenderLevel(appenderName: String, level: Level) {
      LogManager.getLogManager().reset()

      val logger = (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) ?: null)
         as? ch.qos.logback.classic.Logger
      val filter = logger
         ?.getAppender(appenderName)
         ?.copyOfAttachedFiltersList
         ?.firstOrNull()
         as? ch.qos.logback.classic.filter.ThresholdFilter
      filter?.setLevel(level.toString())
   }

   companion object {
      private val logger = KotlinLogging.logger { }
   }
}