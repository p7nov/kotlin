plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(project(":core:metadata"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
