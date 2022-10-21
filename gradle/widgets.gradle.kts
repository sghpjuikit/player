plugins {
   kotlin("jvm")
   idea
}

idea {
   module {
      contentRoot = null
      sourceDirs.add(projectDir / "src")
      testSources.setFrom(projectDir / "tsc")
      resourceDirs.add(projectDir / "rsc")
      excludeDirs.add(projectDir / "out")
      inheritOutputDirs = false
      outputDir = projectDir / "out"
   }
}

sourceSets {
   main {
      java.srcDir("src")
      java.exclude("out")
      resources.setSrcDirs(listOf("rsc"))
   }
   test {
      java.setSrcDirs(listOf("tst"))
      resources.setSrcDirs(listOf<Any>())
   }
}

dependencies {
   compileOnly(projects.player)
   compileOnly(projects.util)
   compileOnly(files(projectDir.listFiles().orEmpty().filter { it.path.endsWith(".jar") }))
}