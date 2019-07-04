import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.8")

dependencies {
    testCompile(project(":kotlin-scripting-compiler-impl"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(projectTests(":generators:test-generator"))
    testRuntime(project(":kotlin-reflect"))
    testRuntime(intellijDep())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    systemProperty("idea.home.path", intellijRootDir().canonicalPath)
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJava8TestsKt")

testsJar()
