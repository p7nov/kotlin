import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Mock Runtime for Tests"

plugins {
    kotlin("jvm")
    `maven-publish`
}

JvmProject.configure(project, "1.6")

val builtins by configurations.creating

val runtime by configurations
val runtimeJar by configurations.creating {
    runtime.extendsFrom(this)
}

dependencies {
    compileOnly(project(":kotlin-stdlib"))
    builtins(project(":core:builtins"))
}

sourceSets {
    "main" {
        java.apply {
            srcDir(File(buildDir, "src"))
        }
    }
    "test" {}
}

val copySources by task<Sync> {
    val stdlibProjectDir = project(":kotlin-stdlib").projectDir

    from(stdlibProjectDir.resolve("runtime"))
        .include("kotlin/TypeAliases.kt",
                 "kotlin/text/TypeAliases.kt")
    from(stdlibProjectDir.resolve("src"))
        .include("kotlin/collections/TypeAliases.kt")
    from(stdlibProjectDir.resolve("../src"))
        .include("kotlin/util/Standard.kt",
                 "kotlin/internal/Annotations.kt",
                 "kotlin/contracts/ContractBuilder.kt",
                 "kotlin/contracts/Effect.kt")
    into(File(buildDir, "src"))
}

tasks.withType<KotlinCompile> {
    dependsOn(copySources)
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-module-name",
            "kotlin-stdlib",
            "-Xmulti-platform",
            "-Xuse-experimental=kotlin.contracts.ExperimentalContracts",
            "-Xuse-experimental=kotlin.Experimental"
        )
    }
}

val jar = runtimeJar {
    archiveFileName.set("kotlin-stdlib-minimal-for-test.jar")
    dependsOn(builtins)
    from(provider { zipTree(builtins.singleFile) }) { include("kotlin/**") }
}

publishing {
    publications {
        create<MavenPublication>("internal") {
            artifactId = "kotlin-stdlib-minimal-for-test"
            artifact(jar)
        }
    }

    repositories {
        maven("${rootProject.buildDir}/internal/repo")
    }
}

