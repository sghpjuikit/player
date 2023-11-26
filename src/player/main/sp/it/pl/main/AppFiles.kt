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
val `applocation` = `AppLocation`

object `AppLocation`: Dir(File("").absolutePath) {
   
   /** Directory. Directory containing application documentation. */
   val `documentation` = `Documentation`
   
   object `Documentation`: Dir(File("").absolutePath + separator + "documentation")
   
   /** Directory. Directory containing Java Development Kit (JDK). */
   val `java` = `Java`
   
   object `Java`: Dir(File("").absolutePath + separator + "java")
   
   /** Directory. Directory containing application libraries. */
   val `lib` = `Lib`
   
   object `Lib`: Dir(File("").absolutePath + separator + "lib")
   
   /** Directory. Directory containing application plugins. */
   val `plugins` = `Plugins`
   
   object `Plugins`: Dir(File("").absolutePath + separator + "plugins")
   
   /** Directory. Directory containing application resources. */
   val `resources` = `Resources`
   
   object `Resources`: Dir(File("").absolutePath + separator + "resources") {
      
      /** File. Contains predefined audio mood tag values. Value per line. UTF8. For autocompletion. */
      val `moods_yml` = `Moods_yml`
      
      object `Moods_yml`: Fil(File("").absolutePath + separator + "resources" + separator + "moods.yml")

      /** File. Contains predefined classes' fully qualified names. Value per line. UTF8. For autocompletion. */
      val `classes_yml` = `Classes_yml`
      
      object `Classes_yml`: Fil(File("").absolutePath + separator + "resources" + separator + "classes.yml")

      /** Directory. Directory containing application icons. */
      val `icons` = `Icons`
      
      object `Icons`: Dir(File("").absolutePath + separator + "resources" + separator + "icons") {
         
         /** File. Icon, 16px. */
         val `icon16_png` = `Icon16_png`
         
         object `Icon16_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon16.png")

         /** File. Icon, 24px. */
         val `icon24_png` = `Icon24_png`
         
         object `Icon24_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon24.png")

         /** File. Icon, 32px. */
         val `icon32_png` = `Icon32_png`
         
         object `Icon32_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon32.png")

         /** File. Icon, 48px. */
         val `icon48_png` = `Icon48_png`
         
         object `Icon48_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon48.png")

         /** File. Icon, 128px. */
         val `icon128_png` = `Icon128_png`
         
         object `Icon128_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon128.png")

         /** File. Icon, 256px. */
         val `icon256_png` = `Icon256_png`
         
         object `Icon256_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon256.png")

         /** File. Icon, 512px. */
         val `icon512_png` = `Icon512_png`
         
         object `Icon512_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon512.png")

         /** File. Icon, 512px. */
         val `icon512_ico` = `Icon512_ico`
         
         object `Icon512_ico`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon512.ico")

      }
   }
   
   /** Directory. Directory containing application skins. */
   val `skins` = `Skins`
   
   object `Skins`: Dir(File("").absolutePath + separator + "skins")
   
   /** Directory. Directory containing initial ui templates - persisted user ui bundled with the application. */
   val `templates` = `Templates`
   
   object `Templates`: Dir(File("").absolutePath + separator + "templates")
   
   /**
    * Directory.
    * Directory containing all user data created by application use; such as settings, customizations, library, etc.
    * 
    * Moving content of this directory to another application installation (of the same version) will effectively move
    * the 'state' of the application to the new installation.
    */
   val `user` = `User`
   
   object `User`: Dir(File("").absolutePath + separator + "user") {
      
      /** Directory. Directory containing persisted user ui and custom templates. */
      val `layouts` = `Layouts`
      
      object `Layouts`: Dir(File("").absolutePath + separator + "user" + separator + "layouts") {
         
         /** Directory. Last persisted application ui state. */
         val `current` = `Current`
         
         object `Current`: Dir(File("").absolutePath + separator + "user" + separator + "layouts" + separator + "current")
      }
      
      /** Directory. Directory containing libraries. I.e., audio library, playlists, etc. */
      val `library` = `Library`
      
      object `Library`: Dir(File("").absolutePath + separator + "user" + separator + "library")
      
      /** Directory. Directory for application logging output. */
      val `log` = `Log`
      
      object `Log`: Dir(File("").absolutePath + separator + "user" + separator + "log")
      
      /** Directory. Directory for plugin user data. */
      val `plugins` = `Plugins`
      
      object `Plugins`: Dir(File("").absolutePath + separator + "user" + separator + "plugins")
      
      /**
       * Directory.
       * Both temporary and home directory (`java.io.tmpdir` and `user.home` redirect here).
       * 
       * It is safe to delete all contents (when application is not running).
       */
      val `tmp` = `Tmp`
      
      object `Tmp`: Dir(File("").absolutePath + separator + "user" + separator + "tmp")
      
      /** Directory. Directory for widget user data. */
      val `widgets` = `Widgets`
      
      object `Widgets`: Dir(File("").absolutePath + separator + "user" + separator + "widgets")
      
      /** File. File for application configuration. */
      val `application_json` = `Application_json`
      
      object `Application_json`: Fil(File("").absolutePath + separator + "user" + separator + "application.json")

   }
   
   /** Directory. Optional directory containing Vlc player installation. */
   val `vlc` = `Vlc`
   
   object `Vlc`: Dir(File("").absolutePath + separator + "vlc")
   
   /** Directory. Directory containing widgets - source files, class files and widget's resources. */
   val `widgets` = `Widgets`
   
   object `Widgets`: Dir(File("").absolutePath + separator + "widgets")
   
   /** File. File containing application source code and main class. Executed by java.exe when started. */
   val `spitplayer_jar` = `SpitPlayer_jar`
   
   object `SpitPlayer_jar`: Fil(File("").absolutePath + separator + "SpitPlayer.jar")

   /** File. Windows executable file that opens this application as gui application. */
   val `spitplayer_com` = `SpitPlayer_com`
   
   object `SpitPlayer_com`: Fil(File("").absolutePath + separator + "SpitPlayer.com")

   /** File. Windows executable file that opens this application as console application. */
   val `spitplayer_exe` = `SpitPlayer_exe`
   
   object `SpitPlayer_exe`: Fil(File("").absolutePath + separator + "SpitPlayer.exe")

   /** File. Linux/Mac executable file that opens this application. */
   val `spitplayer_sh` = `SpitPlayer_sh`
   
   object `SpitPlayer_sh`: Fil(File("").absolutePath + separator + "SpitPlayer.sh")

}