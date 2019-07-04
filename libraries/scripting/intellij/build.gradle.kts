
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

JvmProject.configure(project, "1.6")

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

publish()

standardPublicJars()
