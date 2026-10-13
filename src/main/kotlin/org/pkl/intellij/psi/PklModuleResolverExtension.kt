/**
 * Copyright © 2026 Apple Inc. and the Pkl project authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
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
