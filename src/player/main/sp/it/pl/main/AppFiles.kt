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

/** Directory child [`AppLocation`]. */
val `applocation` = `AppLocation`

/**
 * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
 * 
 * Application location. Working directory of the project.
 */
object `AppLocation`: Dir(File("").absolutePath) {

   /** Same as [getName]. Compile-time constant. `app`.*/
   const val fileName: String = """app"""
   /** Description of this file. Compile-time constant. Same as documentation for this object. */
   const val fileDescription: String = """Application location. Working directory of the project."""

   /** Directory child [`Documentation`]. */
   val `documentation` = `Documentation`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing application documentation.
    */
   object `Documentation`: Dir(File("").absolutePath + separator + "documentation") {
   
      /** Same as [getName]. Compile-time constant. `documentation`.*/
      const val fileName: String = """documentation"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing application documentation."""
   
   }
   /** Directory child [`Java`]. */
   val `java` = `Java`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing Java Development Kit (JDK).
    */
   object `Java`: Dir(File("").absolutePath + separator + "java") {
   
      /** Same as [getName]. Compile-time constant. `java`.*/
      const val fileName: String = """java"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing Java Development Kit (JDK)."""
   
   }
   /** Directory child [`Kotlinc`]. */
   val `kotlinc` = `Kotlinc`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing Kotlin compiler.
    */
   object `Kotlinc`: Dir(File("").absolutePath + separator + "kotlinc") {
   
      /** Same as [getName]. Compile-time constant. `kotlinc`.*/
      const val fileName: String = """kotlinc"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing Kotlin compiler."""
   
   }
   /** Directory child [`Lib`]. */
   val `lib` = `Lib`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing application libraries.
    */
   object `Lib`: Dir(File("").absolutePath + separator + "lib") {
   
      /** Same as [getName]. Compile-time constant. `lib`.*/
      const val fileName: String = """lib"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing application libraries."""
   
   }
   /** Directory child [`Plugins`]. */
   val `plugins` = `Plugins`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing application plugins.
    */
   object `Plugins`: Dir(File("").absolutePath + separator + "plugins") {
   
      /** Same as [getName]. Compile-time constant. `plugins`.*/
      const val fileName: String = """plugins"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing application plugins."""
   
   }
   /** Directory child [`Resources`]. */
   val `resources` = `Resources`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing application resources.
    */
   object `Resources`: Dir(File("").absolutePath + separator + "resources") {
   
      /** Same as [getName]. Compile-time constant. `resources`.*/
      const val fileName: String = """resources"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing application resources."""
   
      /** File child [`Moods_txt`]. */
      val `moods_txt` = `Moods_txt`
      
      /**
       * Compile-time object representing file `File("").absolutePath/"resources"`, usable in annotations.
       * 
       * File containing predefined audio mood tag values. Value per line. UTF8.
       */
      object `Moods_txt`: Fil(File("").absolutePath + separator + "resources" + separator + "moods.txt") {
      
         /** Same as [getName]. Compile-time constant. `moods.txt`.*/
         const val fileName: String = """moods.txt"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """File containing predefined audio mood tag values. Value per line. UTF8."""
      
      }

      /** Directory child [`Icons`]. */
      val `icons` = `Icons`
      
      /**
       * Compile-time object representing directory `File("").absolutePath/"resources"`, usable in annotations.
       * 
       * Directory containing application icons.
       */
      object `Icons`: Dir(File("").absolutePath + separator + "resources" + separator + "icons") {
      
         /** Same as [getName]. Compile-time constant. `icons`.*/
         const val fileName: String = """icons"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """Directory containing application icons."""
      
         /** File child [`Icon16_png`]. */
         val `icon16_png` = `Icon16_png`
         
         /**
          * Compile-time object representing file `File("").absolutePath/"resources"/"icons"`, usable in annotations.
          * 
          * Icon, 16px.
          */
         object `Icon16_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon16.png") {
         
            /** Same as [getName]. Compile-time constant. `icon16.png`.*/
            const val fileName: String = """icon16.png"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Icon, 16px."""
         
         }

         /** File child [`Icon24_png`]. */
         val `icon24_png` = `Icon24_png`
         
         /**
          * Compile-time object representing file `File("").absolutePath/"resources"/"icons"`, usable in annotations.
          * 
          * Icon, 24px.
          */
         object `Icon24_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon24.png") {
         
            /** Same as [getName]. Compile-time constant. `icon24.png`.*/
            const val fileName: String = """icon24.png"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Icon, 24px."""
         
         }

         /** File child [`Icon32_png`]. */
         val `icon32_png` = `Icon32_png`
         
         /**
          * Compile-time object representing file `File("").absolutePath/"resources"/"icons"`, usable in annotations.
          * 
          * Icon, 32px.
          */
         object `Icon32_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon32.png") {
         
            /** Same as [getName]. Compile-time constant. `icon32.png`.*/
            const val fileName: String = """icon32.png"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Icon, 32px."""
         
         }

         /** File child [`Icon48_png`]. */
         val `icon48_png` = `Icon48_png`
         
         /**
          * Compile-time object representing file `File("").absolutePath/"resources"/"icons"`, usable in annotations.
          * 
          * Icon, 48px.
          */
         object `Icon48_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon48.png") {
         
            /** Same as [getName]. Compile-time constant. `icon48.png`.*/
            const val fileName: String = """icon48.png"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Icon, 48px."""
         
         }

         /** File child [`Icon128_png`]. */
         val `icon128_png` = `Icon128_png`
         
         /**
          * Compile-time object representing file `File("").absolutePath/"resources"/"icons"`, usable in annotations.
          * 
          * Icon, 128px.
          */
         object `Icon128_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon128.png") {
         
            /** Same as [getName]. Compile-time constant. `icon128.png`.*/
            const val fileName: String = """icon128.png"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Icon, 128px."""
         
         }

         /** File child [`Icon256_png`]. */
         val `icon256_png` = `Icon256_png`
         
         /**
          * Compile-time object representing file `File("").absolutePath/"resources"/"icons"`, usable in annotations.
          * 
          * Icon, 256px.
          */
         object `Icon256_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon256.png") {
         
            /** Same as [getName]. Compile-time constant. `icon256.png`.*/
            const val fileName: String = """icon256.png"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Icon, 256px."""
         
         }

         /** File child [`Icon512_png`]. */
         val `icon512_png` = `Icon512_png`
         
         /**
          * Compile-time object representing file `File("").absolutePath/"resources"/"icons"`, usable in annotations.
          * 
          * Icon, 512px.
          */
         object `Icon512_png`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon512.png") {
         
            /** Same as [getName]. Compile-time constant. `icon512.png`.*/
            const val fileName: String = """icon512.png"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Icon, 512px."""
         
         }

         /** File child [`Icon512_ico`]. */
         val `icon512_ico` = `Icon512_ico`
         
         /**
          * Compile-time object representing file `File("").absolutePath/"resources"/"icons"`, usable in annotations.
          * 
          * Icon, 512px.
          */
         object `Icon512_ico`: Fil(File("").absolutePath + separator + "resources" + separator + "icons" + separator + "icon512.ico") {
         
            /** Same as [getName]. Compile-time constant. `icon512.ico`.*/
            const val fileName: String = """icon512.ico"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Icon, 512px."""
         
         }

      }
   }
   /** Directory child [`Skins`]. */
   val `skins` = `Skins`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing application skins.
    */
   object `Skins`: Dir(File("").absolutePath + separator + "skins") {
   
      /** Same as [getName]. Compile-time constant. `skins`.*/
      const val fileName: String = """skins"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing application skins."""
   
   }
   /** Directory child [`Templates`]. */
   val `templates` = `Templates`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing initial ui templates - persisted user ui bundled with the application.
    */
   object `Templates`: Dir(File("").absolutePath + separator + "templates") {
   
      /** Same as [getName]. Compile-time constant. `templates`.*/
      const val fileName: String = """templates"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing initial ui templates - persisted user ui bundled with the application."""
   
   }
   /** Directory child [`User`]. */
   val `user` = `User`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing all user data created by application use; such as settings, customizations, library, etc.
    * 
    * Moving content of this directory to another application installation (of the same version) will effectively move
    * the 'state' of the application to the new installation.
    */
   object `User`: Dir(File("").absolutePath + separator + "user") {
   
      /** Same as [getName]. Compile-time constant. `user`.*/
      const val fileName: String = """user"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing all user data created by application use; such as settings, customizations, library, etc. Moving content of this directory to another application installation (of the same version) will effectively move the 'state' of the application to the new installation."""
   
      /** Directory child [`Layouts`]. */
      val `layouts` = `Layouts`
      
      /**
       * Compile-time object representing directory `File("").absolutePath/"user"`, usable in annotations.
       * 
       * Directory containing persisted user ui and custom templates.
       */
      object `Layouts`: Dir(File("").absolutePath + separator + "user" + separator + "layouts") {
      
         /** Same as [getName]. Compile-time constant. `layouts`.*/
         const val fileName: String = """layouts"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """Directory containing persisted user ui and custom templates."""
      
         /** Directory child [`Current`]. */
         val `current` = `Current`
         
         /**
          * Compile-time object representing directory `File("").absolutePath/"user"/"layouts"`, usable in annotations.
          * 
          * Last persisted application ui state.
          */
         object `Current`: Dir(File("").absolutePath + separator + "user" + separator + "layouts" + separator + "current") {
         
            /** Same as [getName]. Compile-time constant. `current`.*/
            const val fileName: String = """current"""
            /** Description of this file. Compile-time constant. Same as documentation for this object. */
            const val fileDescription: String = """Last persisted application ui state."""
         
         }
      }
      /** Directory child [`Library`]. */
      val `library` = `Library`
      
      /**
       * Compile-time object representing directory `File("").absolutePath/"user"`, usable in annotations.
       * 
       * Directory containing libraries. I.e., audio library, playlists, etc.
       */
      object `Library`: Dir(File("").absolutePath + separator + "user" + separator + "library") {
      
         /** Same as [getName]. Compile-time constant. `library`.*/
         const val fileName: String = """library"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """Directory containing libraries. I.e., audio library, playlists, etc."""
      
      }
      /** Directory child [`Log`]. */
      val `log` = `Log`
      
      /**
       * Compile-time object representing directory `File("").absolutePath/"user"`, usable in annotations.
       * 
       * Directory for application logging output.
       */
      object `Log`: Dir(File("").absolutePath + separator + "user" + separator + "log") {
      
         /** Same as [getName]. Compile-time constant. `log`.*/
         const val fileName: String = """log"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """Directory for application logging output."""
      
      }
      /** Directory child [`Plugins`]. */
      val `plugins` = `Plugins`
      
      /**
       * Compile-time object representing directory `File("").absolutePath/"user"`, usable in annotations.
       * 
       * Directory for plugin user data.
       */
      object `Plugins`: Dir(File("").absolutePath + separator + "user" + separator + "plugins") {
      
         /** Same as [getName]. Compile-time constant. `plugins`.*/
         const val fileName: String = """plugins"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """Directory for plugin user data."""
      
      }
      /** Directory child [`Tmp`]. */
      val `tmp` = `Tmp`
      
      /**
       * Compile-time object representing directory `File("").absolutePath/"user"`, usable in annotations.
       * 
       * Both temporary and home directory (`java.io.tmpdir` and `user.home` redirect here).
       * 
       * It is safe to delete all contents (when application is not running).
       */
      object `Tmp`: Dir(File("").absolutePath + separator + "user" + separator + "tmp") {
      
         /** Same as [getName]. Compile-time constant. `tmp`.*/
         const val fileName: String = """tmp"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """Both temporary and home directory (`java.io.tmpdir` and `user.home` redirect here). It is safe to delete all contents (when application is not running)."""
      
      }
      /** Directory child [`Widgets`]. */
      val `widgets` = `Widgets`
      
      /**
       * Compile-time object representing directory `File("").absolutePath/"user"`, usable in annotations.
       * 
       * Directory for widget user data.
       */
      object `Widgets`: Dir(File("").absolutePath + separator + "user" + separator + "widgets") {
      
         /** Same as [getName]. Compile-time constant. `widgets`.*/
         const val fileName: String = """widgets"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """Directory for widget user data."""
      
      }
      /** File child [`Application_properties`]. */
      val `application_properties` = `Application_properties`
      
      /**
       * Compile-time object representing file `File("").absolutePath/"user"`, usable in annotations.
       * 
       * File for application configuration.
       */
      object `Application_properties`: Fil(File("").absolutePath + separator + "user" + separator + "application.properties") {
      
         /** Same as [getName]. Compile-time constant. `application.properties`.*/
         const val fileName: String = """application.properties"""
         /** Description of this file. Compile-time constant. Same as documentation for this object. */
         const val fileDescription: String = """File for application configuration."""
      
      }

   }
   /** Directory child [`Vlc`]. */
   val `vlc` = `Vlc`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Optional directory containing Vlc player installation.
    */
   object `Vlc`: Dir(File("").absolutePath + separator + "vlc") {
   
      /** Same as [getName]. Compile-time constant. `vlc`.*/
      const val fileName: String = """vlc"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Optional directory containing Vlc player installation."""
   
   }
   /** Directory child [`Widgets`]. */
   val `widgets` = `Widgets`
   
   /**
    * Compile-time object representing directory `File("").absolutePath`, usable in annotations.
    * 
    * Directory containing widgets - source files, class files and widget's resources.
    */
   object `Widgets`: Dir(File("").absolutePath + separator + "widgets") {
   
      /** Same as [getName]. Compile-time constant. `widgets`.*/
      const val fileName: String = """widgets"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Directory containing widgets - source files, class files and widget's resources."""
   
   }
   /** File child [`SpitPlayer_jar`]. */
   val `spitplayer_jar` = `SpitPlayer_jar`
   
   /**
    * Compile-time object representing file `File("").absolutePath`, usable in annotations.
    * 
    * File containing application source code and main class. Executed by java.exe when started.
    */
   object `SpitPlayer_jar`: Fil(File("").absolutePath + separator + "SpitPlayer.jar") {
   
      /** Same as [getName]. Compile-time constant. `SpitPlayer.jar`.*/
      const val fileName: String = """SpitPlayer.jar"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """File containing application source code and main class. Executed by java.exe when started."""
   
   }

   /** File child [`SpitPlayerc_exe`]. */
   val `spitplayerc_exe` = `SpitPlayerc_exe`
   
   /**
    * Compile-time object representing file `File("").absolutePath`, usable in annotations.
    * 
    * Windows executable file that opens this application as gui application.
    */
   object `SpitPlayerc_exe`: Fil(File("").absolutePath + separator + "SpitPlayerc.exe") {
   
      /** Same as [getName]. Compile-time constant. `SpitPlayerc.exe`.*/
      const val fileName: String = """SpitPlayerc.exe"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Windows executable file that opens this application as gui application."""
   
   }

   /** File child [`SpitPlayer_exe`]. */
   val `spitplayer_exe` = `SpitPlayer_exe`
   
   /**
    * Compile-time object representing file `File("").absolutePath`, usable in annotations.
    * 
    * Windows executable file that opens this application as console application.
    */
   object `SpitPlayer_exe`: Fil(File("").absolutePath + separator + "SpitPlayer.exe") {
   
      /** Same as [getName]. Compile-time constant. `SpitPlayer.exe`.*/
      const val fileName: String = """SpitPlayer.exe"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Windows executable file that opens this application as console application."""
   
   }

   /** File child [`SpitPlayer_sh`]. */
   val `spitplayer_sh` = `SpitPlayer_sh`
   
   /**
    * Compile-time object representing file `File("").absolutePath`, usable in annotations.
    * 
    * Linux/Mac executable file that opens this application.
    */
   object `SpitPlayer_sh`: Fil(File("").absolutePath + separator + "SpitPlayer.sh") {
   
      /** Same as [getName]. Compile-time constant. `SpitPlayer.sh`.*/
      const val fileName: String = """SpitPlayer.sh"""
      /** Description of this file. Compile-time constant. Same as documentation for this object. */
      const val fileDescription: String = """Linux/Mac executable file that opens this application."""
   
   }

}
