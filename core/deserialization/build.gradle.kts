plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(project(":core:metadata"))
    compile(project(":core:util.runtime"))
    compile(project(":core:descriptors"))
    compile(commonDep("javax.inject"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
