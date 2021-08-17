plugins {
   kotlin("jvm")
   idea
}

idea {
   module {
      contentRoot = null
      sourceDirs.add(projectDir)
      inheritOutputDirs = false
      outputDir = file("out")
   }
}

sourceSets {
   main {
      java.srcDir(".")
      java.exclude("out")
      resources.setSrcDirs(listOf<Any>())
   }
   test {
      java.setSrcDirs(listOf<Any>())
      resources.setSrcDirs(listOf<Any>())
   }
}

dependencies {
   compileOnly(projects.player)
   compileOnly(projects.util)
   compileOnly(files(projectDir.listFiles().orEmpty().filter { it.path.endsWith(".jar") }))
}