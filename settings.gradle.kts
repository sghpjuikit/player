// Gradle settings file for multi-project build. Executed during the initialization phase.
// Defines all projects and build script classpath.

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.apply {
   projectDir = file(".")
   buildFileName = "gradle/project.gradle.kts"


   // Project containing Java/Kotlin utility classes not directly related to the player
   include(":util")
   project(":util").apply {
      name = "util"
      projectDir = file("src/util")
      buildFileName = "../../gradle/util.gradle.kts"
   }

   // Project containing Java/Kotlin executable classes prototyping, showcasing & learning Java/Kotlin/lib features not
   // directly related to the player
   include(":demo")
   project(":demo").apply {
      name = "demo"
      projectDir = file("src/demo")
      buildFileName = "../../gradle/demo.gradle.kts"
   }

   // Set of projects containing player widgets. 1 project per widget. Projects share the same build file.
   include(":widgets")
   project(":widgets").apply {
      name = "widgets"
      projectDir = file("app/widgets")

      projectDir.listFiles().orEmpty().forEach {
         include(":widgets:${it.name}")
         project(":widgets:${it.name}").apply {
            name = "widget-${it.name}"
            projectDir = it
            buildFileName = "../../../gradle/widgets.gradle.kts"
         }
      }
   }
}