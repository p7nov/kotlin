plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(project(":core:util.runtime"))
    compile(project(":core:type-system"))
    compile(kotlinStdlib())
    compile(project(":kotlin-annotations-jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
