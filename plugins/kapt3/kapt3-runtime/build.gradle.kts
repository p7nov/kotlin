description = "Kotlin Annotation Processing Runtime"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
