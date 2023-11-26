@file:Suppress("RemoveRedundantBackticks", "unused", "ClassName", "ObjectPropertyName", "SpellCheckingInspection")

package sp.it.pl.main

import java.io.File

/** [File] that always [isFile] and never [isDirectory]. */
open class Fil(path: String): File(path) {
   /** @return false */
   override fun isDirectory() = false
   /** @return true */
   override fun isFile() = true
}

/** [File] that always [isDirectory] and never [isFile]. */
open class Dir(path: String): File(path) {
   /** @return true */
   override fun isDirectory() = true
   /** @return false */
   override fun isFile() = false
}

/** Directory. Application location. Working directory of the project. */
val `AppLocation` = `AppLocationObj`

object `AppLocationObj`: Dir(File("").absolutePath) {
   
   /** Directory. Directory containing application documentation. */
   val `documentation` = `documentationObj`
   
   object `documentationObj`: Dir(File("").absolutePath + separator + "documentation")
   
   /** Directory. Directory containing Java Development Kit (JDK). */
   val `java` = `javaObj`
   
   object `javaObj`: Dir(File("").absolutePath + separator + "java")
   
   /** Directory. Directory containing application libraries. */
   val `lib` = `libObj`
   
   object `libObj`: Dir(File("").absolutePath + separator + "lib")
   
   /** Directory. Directory containing application plugins. */
   val `plugins` = `pluginsObj`
   
   object `pluginsObj`: Dir(File("").absolutePath + separator + "plugins")
   
   /** Directory. Directory containing application resources. */
   val `resources` = `resourcesObj`
   
   object `resourcesObj`: Dir(File("").absolutePath + separator + "resources") {
      
      /** File. Contains predefined audio mood tag values. Value per line. UTF8. For autocompletion. */
      val `moods_yml` = `moods_ymlObj`
      
      object `moods_ymlObj`: Fil(File("").absolutePath + separator + "resources" + separator + "moods.yml")

      /** File. Contains predefined classes' fully qualified names. Value per line. UTF8. For autocompletion. */
      val `classes_yml` = `classes_ymlObj`
      
      object `classes_ymlObj`: Fil(File("").absolutePath + separator + "resources" + separator + "classes.yml")

      /** Directory. Directory containing application icons. */
      val `icons` = `iconsObj`
      
      object `iconsObj`: Dir(File("").absolutePath + separator + "resources" + separator + "icons") {
         
         /** File. Icon, 16px. */
         val `icon16_png` = `icon16_pngObj`
         
         object `icon16_pngObj`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon16.png")

         /** File. Icon, 24px. */
         val `icon24_png` = `icon24_pngObj`
         
         object `icon24_pngObj`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon24.png")

         /** File. Icon, 32px. */
         val `icon32_png` = `icon32_pngObj`
         
         object `icon32_pngObj`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon32.png")

         /** File. Icon, 48px. */
         val `icon48_png` = `icon48_pngObj`
         
         object `icon48_pngObj`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon48.png")

         /** File. Icon, 128px. */
         val `icon128_png` = `icon128_pngObj`
         
         object `icon128_pngObj`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon128.png")

         /** File. Icon, 256px. */
         val `icon256_png` = `icon256_pngObj`
         
         object `icon256_pngObj`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon256.png")

         /** File. Icon, 512px. */
         val `icon512_png` = `icon512_pngObj`
         
         object `icon512_pngObj`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon512.png")

         /** File. Icon, 512px. */
         val `icon512_ico` = `icon512_icoObj`
         
         object `icon512_icoObj`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon512.ico")

      }
   }
   
   /** Directory. Directory containing application skins. */
   val `skins` = `skinsObj`
   
   object `skinsObj`: Dir(File("").absolutePath + separator + "skins")
   
   /** Directory. Directory containing initial ui templates - persisted user ui bundled with the application. */
   val `templates` = `templatesObj`
   
   object `templatesObj`: Dir(File("").absolutePath + separator + "templates")
   
   /**
    * Directory.
    * Directory containing all user data created by application use; such as settings, customizations, library, etc.
    * 
    * Moving content of this directory to another application installation (of the same version) will effectively move
    * the 'state' of the application to the new installation.
    */
   val `user` = `userObj`
   
   object `userObj`: Dir(File("").absolutePath + separator + "user") {
      
      /** Directory. Directory containing persisted user ui and custom templates. */
      val `layouts` = `layoutsObj`
      
      object `layoutsObj`: Dir(File("").absolutePath + separator + "user" + separator + "layouts") {
         
         /** Directory. Last persisted application ui state. */
         val `current` = `currentObj`
         
         object `currentObj`: Dir(File("").absolutePath + separator + "user" + separator + "layouts" + separator + "current")
      }
      
      /** Directory. Directory containing libraries. I.e., audio library, playlists, etc. */
      val `library` = `libraryObj`
      
      object `libraryObj`: Dir(File("").absolutePath + separator + "user" + separator + "library")
      
      /** Directory. Directory for application logging output. */
      val `log` = `logObj`
      
      object `logObj`: Dir(File("").absolutePath + separator + "user" + separator + "log")
      
      /** Directory. Directory for plugin user data. */
      val `plugins` = `pluginsObj`
      
      object `pluginsObj`: Dir(File("").absolutePath + separator + "user" + separator + "plugins")
      
      /**
       * Directory.
       * Both temporary and home directory (`java.io.tmpdir` and `user.home` redirect here).
       * 
       * It is safe to delete all contents (when application is not running).
       */
      val `tmp` = `tmpObj`
      
      object `tmpObj`: Dir(File("").absolutePath + separator + "user" + separator + "tmp")
      
      /** Directory. Directory for widget user data. */
      val `widgets` = `widgetsObj`
      
      object `widgetsObj`: Dir(File("").absolutePath + separator + "user" + separator + "widgets")
      
      /** File. File for application configuration. */
      val `application_json` = `application_jsonObj`
      
      object `application_jsonObj`: Fil(File("").absolutePath + separator + "user" + separator + "application.json")

   }
   
   /** Directory. Optional directory containing Vlc player installation. */
   val `vlc` = `vlcObj`
   
   object `vlcObj`: Dir(File("").absolutePath + separator + "vlc")
   
   /** Directory. Directory containing widgets - source files, class files and widget's resources. */
   val `widgets` = `widgetsObj`
   
   object `widgetsObj`: Dir(File("").absolutePath + separator + "widgets")
   
   /** File. File containing application source code and main class. Executed by java.exe when started. */
   val `SpitPlayer_jar` = `SpitPlayer_jarObj`
   
   object `SpitPlayer_jarObj`: Fil(File("").absolutePath + separator + "SpitPlayer.jar")

   /** File. Windows executable file that opens this application as gui application. */
   val `SpitPlayer_com` = `SpitPlayer_comObj`
   
   object `SpitPlayer_comObj`: Fil(File("").absolutePath + separator + "SpitPlayer.com")

   /** File. Windows executable file that opens this application as console application. */
   val `SpitPlayer_exe` = `SpitPlayer_exeObj`
   
   object `SpitPlayer_exeObj`: Fil(File("").absolutePath + separator + "SpitPlayer.exe")

   /** File. Linux/Mac executable file that opens this application. */
   val `SpitPlayer_sh` = `SpitPlayer_shObj`
   
   object `SpitPlayer_shObj`: Fil(File("").absolutePath + separator + "SpitPlayer.sh")

}