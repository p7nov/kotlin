import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(protobufLite())
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}