
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(kotlinStdlib())
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}
