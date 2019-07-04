description = "Kotlin metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    compile(kotlinStdlib())
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}
