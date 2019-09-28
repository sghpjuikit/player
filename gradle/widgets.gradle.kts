plugins {
   kotlin("jvm")
   idea
}

idea {
   module {
      inheritOutputDirs = false
      outputDir = file("out")
   }
}

sourceSets {
   main {
      java.srcDir(".")
      java.exclude("**.class")
      resources.setSrcDirs(listOf<Any>())
   }
   test {
      java.setSrcDirs(listOf<Any>())
      resources.setSrcDirs(listOf<Any>())
   }
}

dependencies {
   compileOnly(rootProject)
   compileOnly(project(":util"))
   compileOnly(files(projectDir.listFiles().orEmpty().filter { it.path.endsWith(".jar") }))
}