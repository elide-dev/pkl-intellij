package org.pkl.intellij.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.pkl.intellij.psi.PklModule

interface PklModuleUriCompletionExtension {
  fun schemes(globbable: Boolean): Sequence<LookupElement> = emptySequence()

  fun contribute(
    targetUri: String,
    isGlobImport: Boolean,
    sourceModule: PklModule,
    project: Project,
    resultSet: MutableCollection<LookupElement>,
  )

  companion object {
    val EP_NAME = ExtensionPointName.create<PklModuleUriCompletionExtension>("org.pkl.moduleUriCompletionContributor")
  }
}