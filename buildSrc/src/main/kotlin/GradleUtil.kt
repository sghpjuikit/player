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

open class FileHierarchyInfo: DefaultTask() {
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
   fun generateInfo() {
      val header = """
            @file:Suppress("RemoveRedundantBackticks", "unused", "ClassName", "ObjectPropertyName", "SpellCheckingInspection")
            
            package $outPackage
            
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
               val defName = if (hierarchy.isEmpty()) """`${name.capitalize()}Location`""" else """`${name.capitalize().replace('.', ' ')}`"""
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