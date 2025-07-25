package org.pkl.intellij.psi

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.pkl.intellij.packages.dto.PklProject

/** Extension used to resolve custom module URIs that are not recognized by the Pkl plugin. */
interface PklModuleResolverExtension {
  /** Attempt to resolve a virtual file corresponding to a Pkl module with the given [uri]. */
  fun resolveModuleFile(uri: String, project: Project, context: PklProject?): VirtualFile?

  companion object {
    val EP_NAME = ExtensionPointName.create<PklModuleResolverExtension>("org.pkl.moduleResolver")
  }
}