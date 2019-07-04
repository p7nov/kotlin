/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

object JvmProject {
    private const val isConfiguredProperty = "isConfiguredJvmProjectFlag"

    fun isConfigured(project: Project): Boolean =
        project.extra.properties[isConfiguredProperty] as? Boolean ?: false

    fun configure(project: Project, targetJvmVersion: String) {
        val impl = project.configureJvmProjectImpl
        project.impl(targetJvmVersion)
        project.extra.set(isConfiguredProperty, true)
    }
}

@Suppress("UNCHECKED_CAST")
var Project.configureJvmProjectImpl: (Project.(String) -> Unit)
    get() = project.rootProject.extra["configureJvmProjectImpl"] as Project.(String) -> Unit
    set(value) {
        val extra = project.rootProject.extra
        if (extra.has("configureJvmProjectImpl")) {
            error("configureJvmProjectImpl is already set")
        }
        extra["configureJvmProjectImpl"] = value
    }
