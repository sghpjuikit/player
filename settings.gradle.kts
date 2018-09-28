
rootProject.buildFileName = "gradle/project.gradle.kts"

include(":widgets")
val widgets = project(":widgets")
widgets.projectDir = file("working dir/widgets")
widgets.buildFileName = "../../gradle/widgets.gradle.kts"
