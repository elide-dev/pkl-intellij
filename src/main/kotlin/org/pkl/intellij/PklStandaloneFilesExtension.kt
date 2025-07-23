/**
 * Copyright © 2024-2025 Apple Inc. and the Pkl project authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pkl.intellij

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiFile

/**
 * Extension allowing third-party code to register "standalone" files, which are not part of any Pkl project, and do
 * not trigger the "Sync Pkl project" notification or the project aware settings file tracker.
 */
interface PklStandaloneFilesExtension {
  /** Returns whether the given [file] should be considered as "standalone" (i.e. not part of a regular Pkl project). */
  fun isStandaloneFile(file: PsiFile): Boolean

  companion object {
    @JvmStatic val EP_NAME = ExtensionPointName.create<PklStandaloneFilesExtension>("org.pkl.standaloneFiles")
  }
}
