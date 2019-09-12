plugins {
   kotlin("jvm")
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