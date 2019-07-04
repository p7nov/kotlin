
plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compileOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

if (project.hasProperty("teamcity"))
tasks["compileJava"].dependsOn(":prepare:build.version:writeCompilerVersion")