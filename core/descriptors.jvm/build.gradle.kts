plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(project(":kotlin-annotations-jvm"))
    compile(project(":core:descriptors"))
    compile(project(":core:deserialization"))
    compile(project(":core:metadata.jvm"))
    compile(project(":core:util.runtime"))
    compile(commonDep("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
