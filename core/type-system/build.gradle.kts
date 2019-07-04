plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(project(":core:util.runtime"))
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
