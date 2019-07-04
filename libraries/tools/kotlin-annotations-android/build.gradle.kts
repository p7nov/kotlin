import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension

description = "Kotlin annotations for Android"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

JvmProject.configure(project, "1.6")

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += listOf(
            "-Xallow-kotlin-package",
            "-module-name", project.name
    )
}

sourceSets {
    "main" {
        projectDefault()
    }
}

dependencies {
    compile(kotlinBuiltins())
}

publish()

sourcesJar()
javadocJar()
runtimeJar()
