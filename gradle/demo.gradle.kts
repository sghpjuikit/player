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
   implementation("com.github.oshi", "oshi-demo", "6.2.1")
   implementation(project(":util"))
}