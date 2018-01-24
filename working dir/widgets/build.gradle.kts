
buildDir = rootDir.resolve("build").resolve("widgets")

plugins {
	java
	kotlin("jvm")
	id("idea")
}

idea {
	module {
		sourceDirs = setOf(file("."))
	}
}

java.sourceSets {
	getByName("main") {
		java.srcDir(".")
	}
}

dependencies {
	// include all libs from the actual Project
	compile(rootProject)

	// include all libs in the widgets directory and its subdirectories
	compile(files(file(".").listFiles().flatMap { if(it.isDirectory) listOf<File>(*it.listFiles()) else listOf<File>(it) }.filter { it.extension == "jar" }))
}