/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.IdentifierHighlighterPassFactory
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.idea.IdeaTestApplication
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.impl.ProjectImpl
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.*
import com.intellij.testFramework.fixtures.EditorTestFixture
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.ThrowableRunnable
import com.intellij.util.indexing.UnindexedFilesUpdater
import com.intellij.util.io.exists
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.plugins.gradle.service.project.GradleProjectOpenProcessor
import java.io.File
import java.nio.file.Paths

abstract class AbstractPerformanceProjectsTest : UsefulTestCase() {

    // myProject is not required for all potential perf test cases
    protected var myProject: Project? = null
    private lateinit var jdk18: Sdk
    private lateinit var myApplication: IdeaTestApplication

    override fun setUp() {
        super.setUp()

        myApplication = IdeaTestApplication.getInstance()
        ApplicationManager.getApplication().runWriteAction {
            val jdkTableImpl = JavaAwareProjectJdkTableImpl.getInstanceEx()
            val homePath = if (jdkTableImpl.internalJdk.homeDirectory!!.name == "jre") {
                jdkTableImpl.internalJdk.homeDirectory!!.parent.path
            } else {
                jdkTableImpl.internalJdk.homePath!!
            }

            val javaSdk = JavaSdk.getInstance()
            jdk18 = javaSdk.createJdk("1.8", homePath)
            val internal = javaSdk.createJdk("IDEA jdk", homePath)

            val jdkTable = ProjectJdkTable.getInstance()
            jdkTable.addJdk(jdk18, testRootDisposable)
            jdkTable.addJdk(internal, testRootDisposable)
            KotlinSdkType.setUpIfNeeded()
        }
    }

    override fun tearDown() {
        RunAll(
            ThrowableRunnable { super.tearDown() },
            ThrowableRunnable {
                if (myProject != null) {
                    DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled = true // return default value to avoid unnecessary save
                    (StartupManager.getInstance(myProject!!) as StartupManagerImpl).checkCleared()
                    (DaemonCodeAnalyzer.getInstance(myProject!!) as DaemonCodeAnalyzerImpl).cleanupAfterTest()
                    closeProject(myProject!!)
                    myProject = null
                }
            }).run()
    }

    private fun simpleFilename(fileName: String): String {
        val lastIndexOf = fileName.lastIndexOf('/')
        return if (lastIndexOf >= 0) fileName.substring(lastIndexOf + 1) else fileName
    }

    protected fun perfOpenProject(name: String, stats: Stats, path: String = "idea/testData/perfTest") {
        myProject = innerPerfOpenProject(name, stats, path = path, note = "")
    }

    protected fun innerPerfOpenProject(
        name: String,
        stats: Stats,
        note: String,
        path: String = "idea/testData/perfTest"
    ): Project {
        val projectPath = File("$path/$name").canonicalPath

        val warmUpIterations = 1
        val iterations = 3
        val projectManagerEx = ProjectManagerEx.getInstanceEx()

        var lastProject: Project? = null
        var counter = 0

        stats.perfTest<Unit, Pair<Project, Boolean>>(
            warmUpIterations = warmUpIterations,
            iterations = iterations,
            testName = "open project${if (note.isNotEmpty()) " $note" else ""}",
            test = {
                val projectPathExists = Paths.get(projectPath, ".idea").exists()
                val project = if (projectPathExists) {
                    val project = ProjectUtil.openProject(projectPath, null, false)!!
                    project
                } else {
                    val project = projectManagerEx.loadAndOpenProject(projectPath)!!
                    initKotlinProject(project, projectPath, name)
                    project
                }

                (project as ProjectImpl).registerComponentImplementation(
                    FileEditorManager::class.java,
                    FileEditorManagerImpl::class.java
                )

                projectManagerEx.openTestProject(project)

                dispatchAllInvocationEvents()

                with(StartupManager.getInstance(project) as StartupManagerImpl) {
                    scheduleInitialVfsRefresh()
                    runPostStartupActivities()
                }

                with(ChangeListManager.getInstance(project) as ChangeListManagerImpl) {
                    waitUntilRefreshed()
                }

                it.value = Pair(project, projectPathExists)
            },
            tearDown = {
                it.value?.let { pair ->
                    val project = pair.first

                    // import gradle project if `$project/.idea` is present but modules are not imported
                    // it is a temporary dirty hack as it is fixed in latest IC:
                    // ProjectUtil.openProject picks up gradle import via extension point
                    if (pair.second && ModuleManager.getInstance(project).modules.isEmpty()) {
                        dispatchAllInvocationEvents()

                        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(projectPath)!!

                        FileDocumentManager.getInstance().saveAllDocuments()

                        GradleProjectOpenProcessor.openGradleProject(project, null, Paths.get(virtualFile.path))

                        dispatchAllInvocationEvents()
                        runInEdtAndWait {
                            PlatformTestUtil.saveProject(project)
                        }
                    }

                    assertTrue(
                        "project has to have at least one module",
                        ModuleManager.getInstance(project).modules.isNotEmpty()
                    )

                    lastProject = project
                    VirtualFileManager.getInstance().syncRefresh()

                    invalidateLibraryCache(project)

                    CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)

                    dispatchAllInvocationEvents()

                    // close all project but last - we're going to return and use it further
                    if (counter < warmUpIterations + iterations - 1) {
                        closeProject(project)
                    }
                    counter++
                }
            }
        )

        // indexing
        lastProject?.let { prj ->
            with(DumbService.getInstance(prj)) {
                queueTask(UnindexedFilesUpdater(prj))
                completeJustSubmittedTasks()
            }
            dispatchAllInvocationEvents()
        }

        return lastProject!!
    }

    private fun initKotlinProject(
        project: Project,
        projectPath: String,
        name: String
    ) {
        val modulePath = "$projectPath/$name${ModuleFileType.DOT_DEFAULT_EXTENSION}"
        val projectFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(projectPath))!!
        val srcFile = projectFile.findChild("src")!!
        val module = ApplicationManager.getApplication().runWriteAction(Computable<Module> {
            val projectRootManager = ProjectRootManager.getInstance(project)
            with(projectRootManager) {
                projectSdk = jdk18
            }
            val moduleManager = ModuleManager.getInstance(project)
            val module = moduleManager.newModule(modulePath, ModuleTypeId.JAVA_MODULE)
            PsiTestUtil.addSourceRoot(module, srcFile)
            module
        })
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, jdk18)
    }

    protected fun perfHighlightFile(name: String, stats: Stats): List<HighlightInfo> =
        perfHighlightFile(myProject!!, name, stats)

    protected fun perfHighlightFile(
        project: Project,
        fileName: String,
        stats: Stats,
        note: String = ""
    ): List<HighlightInfo> {
        var highlightInfos: List<HighlightInfo> = emptyList()
        IdentifierHighlighterPassFactory.doWithHighlightingEnabled {
            stats.perfTest<EditorFile, List<HighlightInfo>>(
                testName = "highlighting ${if (note.isNotEmpty()) "$note " else ""}${simpleFilename(fileName)}",
                setUp = {
                    it.setUpValue = openFileInEditor(project, fileName)
                },
                test = {
                    val file = it.setUpValue
                    it.value = highlightFile(project, file!!.psiFile)
                },
                tearDown = {
                    highlightInfos = it.value ?: emptyList()
                    commitAllDocuments()
                    FileEditorManager.getInstance(project).closeFile(it.setUpValue!!.psiFile.virtualFile)
                }
            )
        }
        //println("${"-".repeat(40)}\n$fileName ->\n${highlightInfos.joinToString("\n")}\n")

        return highlightInfos
    }

    private fun highlightFile(project: Project, psiFile: PsiFile): List<HighlightInfo> {
        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile)!!
        val editor = EditorFactory.getInstance().getEditors(document).first()
        val fixture = EditorTestFixture(project, editor, psiFile.virtualFile)
        return fixture.doHighlighting(true)
    }

    protected fun perfFileAnalysis(name: String, stats: Stats) =
        perfFileAnalysis(myProject!!, name, stats)

    private fun perfFileAnalysis(
        project: Project,
        fileName: String,
        stats: Stats,
        note: String = ""
    ) {
        val tools = emptyArray<InspectionProfileEntry>()

        configureInspections(tools, project, testRootDisposable)

        IdentifierHighlighterPassFactory.doWithHighlightingEnabled {
            stats.perfTest<EditorFile, List<HighlightInfo>>(
                testName = "fileAnalysis ${if (note.isNotEmpty()) "$note " else ""}${simpleFilename(fileName)}",
                setUp = {
                    it.setUpValue = openFileInEditor(project, fileName)

                    println("fileAnalysis -> $fileName\n")
                },
                test = {
                    val fileInEditor = it.setUpValue!!
                    it.value = highlightFile(project, fileInEditor.psiFile)
                },
                tearDown = {
                    commitAllDocuments()
                    println("fileAnalysis <- $fileName:\n${it.value?.joinToString("\n")}\n")
                    FileEditorManager.getInstance(project).closeFile(it.setUpValue!!.psiFile.virtualFile)
                    PsiManager.getInstance(project).dropPsiCaches()
                }
            )
        }
    }

    private fun openFileInEditor(project: Project, name: String): EditorFile {
        val fileDocumentManager = FileDocumentManager.getInstance()
        val fileEditorManager = FileEditorManager.getInstance(project)

        val psiFile = projectFileByName(project, name)
        val vFile = psiFile.virtualFile

        assertTrue("file $vFile is not indexed yet", FileIndexFacade.getInstance(project).isInContent(vFile))

        runInEdtAndWait {
            fileEditorManager.openFile(vFile, true)
        }
        val document = fileDocumentManager.getDocument(vFile)!!

        assertNotNull("doc not found for $vFile", EditorFactory.getInstance().getEditors(document))
        assertTrue("expected non empty doc", document.text.isNotEmpty())

        return EditorFile(psiFile = psiFile, document = document)
    }

    private fun projectFileByName(project: Project, name: String): PsiFile {
        val fileManager = VirtualFileManager.getInstance()
        val url = "file://${File("${project.basePath}/$name").absolutePath}"
        val virtualFile = fileManager.refreshAndFindFileByUrl(url)
        return virtualFile!!.toPsiFile(project)!!
    }

    private data class EditorFile(val psiFile: PsiFile, val document: Document)

}