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
   implementation("org.apache.commons", "commons-text", "1.10.0")
   implementation("com.fasterxml.jackson.core", "jackson-core", "2.15.2")
   implementation("com.fasterxml.jackson.core", "jackson-databind", "2.15.2")
   implementation("com.fasterxml.jackson.core", "jackson-annotations", "2.15.2")
}