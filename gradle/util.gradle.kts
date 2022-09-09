plugins {
   kotlin("jvm")
}

sourceSets {
   main {
      java.srcDir("main")
      resources.srcDir("main")
   }
   test {
      java.srcDir("test")
      resources.srcDir("test")
   }
}

dependencies {
   implementation("com.fasterxml.jackson.core", "jackson-core", "2.13.4")
   implementation("com.fasterxml.jackson.core", "jackson-databind", "2.13.4")
   implementation("com.fasterxml.jackson.core", "jackson-annotations", "2.13.4")
}