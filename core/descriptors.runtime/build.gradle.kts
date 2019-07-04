import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

JvmProject.configure(project, "1.6")

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))

    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":generators:test-generator"))

    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    Platform[192].orHigher {
        testRuntimeOnly(intellijDep()) { includeJars("platform-concurrency") }
        testRuntimeOnly(intellijPluginDep("jps-standalone")) { includeJars("jps-model") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateRuntimeDescriptorTestsKt")

projectTest(parallel = true) {
    workingDir = rootDir
}
