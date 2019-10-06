@file:Suppress("RemoveCurlyBracesFromTemplate", "SpellCheckingInspection")

import org.gradle.api.DefaultTask
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.NotLinkException
import java.nio.file.Paths
import java.util.Stack
import kotlin.text.Charsets.UTF_8

operator fun File.div(childName: String) = this.resolve(childName)

infix fun String.group(block: () -> Unit) = block()

fun failIO(cause: Throwable? = null, message: () -> String): Nothing = throw IOException(message(), cause)

fun Boolean.orFailIO(message: () -> String) = also { if (!this) failIO(null, message) }

val String.sysProp: String?
   get() = System.getProperty(this)?.takeIf { it.isNotBlank() }

open class LinkJDK: AbstractTask() {
   /** Location of the link to the JDK. */
   @Internal lateinit var linkLocation: File
   /** JDK home as Path. */
   @Internal val jdkPath = "java.home".sysProp?.let(Paths::get) ?: failIO { "Unable to find JDK: java.home not set" }

   init {
      this.onlyIf {
         try {
            Files.readSymbolicLink(linkLocation.toPath())!=jdkPath
         } catch (e: NotLinkException) {
            true
         } catch (e: java.nio.file.NoSuchFileException) {
            true
         } catch (e: Throwable) {
            logger.error("Failed linkJdk task up to date check: $e")
            throw e
         }
      }
   }

   @TaskAction
   fun linkJdk() {
      linkLocation.delete() // delete invalid symbolic link
      logger.info("Creating link at $linkLocation to $jdkPath...")
      try {
         Files.createSymbolicLink(linkLocation.toPath(), jdkPath)
      } catch (e: Exception) {
         logger.warn("Couldn't create a symbolic link at $linkLocation to $jdkPath: $e")
         val isWindows = "os.name".sysProp?.startsWith("Windows")==true
         if (isWindows) {
            logger.info("Trying to create a Windows junction instead...")
            val process = Runtime.getRuntime().exec("""cmd.exe /c mklink /j "$linkLocation" "$jdkPath"""")
            val exitValue = process.waitFor()
            if (exitValue==0 && linkLocation.exists()) logger.info("Successfully created junction!")
            else failIO(e) { "Unable to make JDK locally accessible!\nmklink exit code: $exitValue" }
         } else {
            failIO(e) { "Unable to make JDK locally accessible!" }
         }
      }
   }
}

open class GenerateKtFileHierarchy: DefaultTask() {
   /** File describing the file hierarchy tree. Must have a single root. */
   @InputFile lateinit var inFileHierarchy: File
   /** Kotlin code that returns the java.io.File object representing the root of the file hierarchy. */
   @Input lateinit var outRootPath: String
   /** Package used for pakcage declaration of the output class file. */
   @Input lateinit var outPackage: String
   /** Exact indent used for indenting the output class file. */
   @Input lateinit var outIndent: String
   /** Output class file. Should be inside the project. */
   @OutputFile lateinit var outFileHierarchy: File

   @TaskAction
   fun generate() {
      val header = """
            @file:Suppress("RemoveRedundantBackticks", "unused", "ClassName", "ObjectPropertyName", "SpellCheckingInspection")
            
            package $outPackage
            
            import java.io.File
            
            /** [File] that always [isFile] and never [isDirectory]. */
            open class Fil(path: String): File(path) {
            ${outIndent}/** @return false */
            ${outIndent}override fun isDirectory() = false
            ${outIndent}/** @return true */
            ${outIndent}override fun isFile() = true
            }
            
            /** [File] that always [isDirectory] and never [isFile]. */
            open class Dir(path: String): File(path) {
            ${outIndent}/** @return true */
            ${outIndent}override fun isDirectory() = true
            ${outIndent}/** @return false */
            ${outIndent}override fun isFile() = false
            }
         """.trimIndent()
      val comments = ArrayList<String>()
      val hierarchy = Stack<Unit>()
      val paths = Stack<String>().apply { push(outRootPath) }

      val sb = StringBuilder("").appendln(header).appendln()
      fun StringBuilder.appendIndent() = (1..hierarchy.size).fold(this) { _, _ -> append(outIndent) }
      var isRoot = true

      inFileHierarchy.readText(UTF_8).lineSequence()
         .filter { it.isNotBlank() }
         .forEachIndexed { _, line ->
            val l = line.trimStart()
            val isComment = l.startsWith("#")
            val isFileEnd = !isComment && l.trim()=="}"
            val isFileStart = !isComment && !isFileEnd

            if (isComment)
               comments += l.replace('#', ' ')

            if (isFileEnd) {
               hierarchy.pop()
               paths.pop()
               sb.appendIndent().appendln("}")
            }

            if (isFileStart) {
               val descriptionLines = comments.joinToString("\n").trimIndent().lines()
               val name = l.substringBeforeLast("{").trim()
               val isFile = !l.endsWith('{')
               val descDoc = descriptionLines.joinToString("\n") { " * $it" }
               val descVal = descriptionLines.map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
               val defName = if (hierarchy.isEmpty()) """`${name.capitalize()}Location`""" else """`${name.capitalize().replace('.', '_')}`"""
               val defType = if (isFile) "Fil" else "Dir"
               val defTypeName = if (isFile) "file" else "directory"
               val path = if (isRoot) paths.peek() else (paths + sequenceOf("\"$name\"")).joinToString(""" + separator + """)
               val def = """
                  |/** ${defTypeName.capitalize()} child [$defName]. */
                  |val ${defName.toLowerCase()} = $defName
                  |
                  |/**
                  | * Compile-time object representing $defTypeName `${paths.joinToString("/")}`, usable in annotations.
                  | * 
                  |$descDoc
                  | */
                  |object $defName: $defType($path) {
                  |
                  |${outIndent}/** Same as [getName]. Compile-time constant. `$name`.*/
                  |${outIndent}const val fileName: String = ${'"'}${'"'}${'"'}$name${'"'}${'"'}${'"'}
                  |${outIndent}/** Description of this file. Compile-time constant. Same as documentation for this object. */
                  |${outIndent}const val fileDescription: String = ${'"'}${'"'}${'"'}$descVal${'"'}${'"'}${'"'}
                  |
                  """.trimMargin()

               def.lineSequence().forEach { sb.appendIndent().appendln(it) }
               if (isFile) sb.appendIndent().appendln("}").appendln()
               comments.clear()

               if (!isFile) {
                  if (!isRoot) paths.push(""""$name"""")
                  isRoot = false
                  hierarchy.push(Unit)
               }
            }

         }

      if (outFileHierarchy.exists()) outFileHierarchy.delete().orFailIO { "Failed to delete $outFileHierarchy" }
      outFileHierarchy.writeText(sb.toString(), UTF_8)
      outFileHierarchy.setReadOnly().orFailIO { "Failed to make $outFileHierarchy read only" }
   }
}

open class GenerateKtSettings: DefaultTask() {
   /** Settings hierarchy used as source for the output class file. */
   @Input var settings: Setting.SettingRoot? = null
   /** Package used for pakcage declaration of the output class file. */
   @Input lateinit var outPackage: String
   /** Exact indent used for indenting the output class file. */
   @Input lateinit var outIndent: String
   /** Output class file. Should be inside the project. */
   @OutputFile lateinit var outFile: File

   @TaskAction
   fun generate() {
      if (settings==null) {
         if (outFile.exists()) outFile.delete().orFailIO { "Failed to delete $outFile" }
         return
      }

      val header = """
            @file:Suppress("RemoveRedundantBackticks", "unused", "ClassName", "ObjectPropertyName", "SpellCheckingInspection")
            
            package $outPackage
            
            import sp.it.util.conf.ConfigDefinition
            import sp.it.util.conf.EditMode
         """.trimIndent()
      val sb = StringBuilder("").appendln(header).appendln()

      settings!!.writeClass(sb)

      if (outFile.exists()) outFile.delete().orFailIO { "Failed to delete $outFile" }
      outFile.writeText(sb.toString(), UTF_8)
      outFile.setReadOnly().orFailIO { "Failed to make $outFile read only" }
   }

   private fun Setting.writeClass(sb: StringBuilder, depth: Int = 0) {
      fun StringBuilder.appendIndentln(text: String) = apply {
         repeat(depth) { append(outIndent) }
         appendln(text)
      }
      fun defName(text: String) = text.split(" ").flatMap { it.split("-") }.joinToString("") { it.capitalize() }.decapitalize()

      when (this) {
         is Setting.SettingRoot -> {
            sb.appendIndentln("/** Application settings hierarchy. */")
            sb.appendIndentln("object ${outFile.nameWithoutExtension.replace(" ", "")} {")
            sb.appendln()
            children.forEach { it.writeClass(sb, depth + 1) }
            sb.appendIndentln("}")
         }
         is Setting.SettingGroup -> {
            sb.appendIndentln("object ${defName(name)} {")
            sb.appendIndentln("${outIndent}/** Name of the group. */")
            sb.appendIndentln("${outIndent}const val name = \"$name\"")
            sb.appendln()
            children.forEach { it.writeClass(sb, depth + 1) }
            sb.appendIndentln("}")
         }
         is Setting.SettingConfig -> {
            sb.appendIndentln("object ${defName(name)}: ConfigDefinition {")
            sb.appendIndentln("${outIndent}/** Name of the config. Compile-time constant. */")
            sb.appendIndentln("${outIndent}const val name: String = \"$name\"")
            sb.appendIndentln("${outIndent}/** Description of the config. Compile-time constant. */")
            sb.appendIndentln("${outIndent}const val info: String = \"$info\"")
            sb.appendIndentln("${outIndent}/** Group of the config. Compile-time constant. */")
            sb.appendIndentln("${outIndent}const val group: String = \"$group\"")
            sb.appendIndentln("${outIndent}/** Editability of the config. Compile-time constant. */")
            sb.appendIndentln("${outIndent}val editable: EditMode = EditMode.$editable")
            sb.appendIndentln("${outIndent}/** Equivalent to [name]. */")
            sb.appendIndentln("${outIndent}override val configName = name")
            sb.appendIndentln("${outIndent}/** Equivalent to [group]. */")
            sb.appendIndentln("${outIndent}override val configGroup = group")
            sb.appendIndentln("${outIndent}/** Equivalent to [info]. */")
            sb.appendIndentln("${outIndent}override val configInfo = info")
            sb.appendIndentln("${outIndent}/** Equivalent to [editable]. */")
            sb.appendIndentln("${outIndent}override val configEditable = editable")
            sb.appendIndentln("}")
         }
      }

   }
}

sealed class Setting(val name: String, val group: String): Serializable {

   init {
      if (name.isBlank()) throw AssertionError("Name can not be empty")
   }

   class SettingConfig(name: String, group: String, var info: String = "", var editable: EditMode = EditMode.USER): Setting(name, group)

   open class SettingGroup(name: String, group: String, val children: MutableList<Setting> = ArrayList()): Setting(name, group) {

      @Dsl
      open operator fun String.invoke(block: @Dsl SettingGroup.() -> Unit) {
         children += SettingGroup(this, "$group.$this").apply(block)
      }

      @Dsl
      fun config(name: String, block: @Dsl SettingConfig.() -> Unit = {}) {
         children += SettingConfig(name, group).apply(block)
      }

   }

   class SettingRoot: SettingGroup("root", "group") {

      @Dsl
      override operator fun String.invoke(block: @Dsl SettingGroup.() -> Unit) {
         children += SettingGroup(this, this).apply(block)
      }

   }

   companion object {
      fun root(block: @Dsl SettingRoot.() -> Unit) = SettingRoot().apply(block)
   }

}

enum class EditMode { USER, APP, NONE }

/** Denotes dsl. See [DslMarker]. */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Dsl