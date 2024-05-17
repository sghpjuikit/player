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
   implementation("org.apache.commons", "commons-text", "1.12.0")
   implementation("org.atteo", "evo-inflector", "1.3")
}