
buildDir = rootDir.resolve("build").resolve("widgets")

plugins {
	id("java")
	id("idea")
}

/*idea {
	module {
		sourceDirs += file(".")
	}
}*/

sourceSets {
	getByName("main") {
		java.srcDir(".")
	}
}

dependencies {
	// include all libs from the actual Project
	compile(rootProject)

	// include all libs in the widgets directory and its subdirectories
	// todo compile(fileTree(dir: ".", include: ["/**/*.jar"]))
}