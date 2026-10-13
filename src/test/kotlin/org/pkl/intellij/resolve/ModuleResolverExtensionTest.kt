/**
 * Copyright © 2026 Apple Inc. and the Pkl project authors. All rights reserved.
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
package org.pkl.intellij.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ExtensionTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.pkl.intellij.PklFileType
import org.pkl.intellij.PklTestCase
import org.pkl.intellij.packages.dto.PklProject
import org.pkl.intellij.psi.PklModuleResolverExtension

class ModuleResolverExtensionTest : PklTestCase() {
  /** Resolves `schema:<name>` to `schema/<name>` in the test project, like a plugin serving its own modules. */
  private inner class SchemaResolver : PklModuleResolverExtension {
    override fun resolveModuleFile(uri: String, project: Project, context: PklProject?): VirtualFile? =
      if (uri.startsWith("schema:")) myFixture.findFileInTempDir("schema/${uri.removePrefix("schema:")}") else null
  }

  private fun registerResolver() {
    ExtensionTestUtil.maskExtensions(
      PklModuleResolverExtension.EP_NAME,
      listOf(SchemaResolver()),
      testRootDisposable,
    )
  }

  private fun addSchema() {
    myFixture.addFileToProject("schema/Base.pkl", "open module Base\n\ncolor: String = \"red\"\n")
    myFixture.addFileToProject(
      "schema/project.pkl",
      "module project\n\nextends \"Base.pkl\"\n\nname: String?\n\nsize: Int = 1\n",
    )
  }

  fun `test a contributed scheme resolves to the module the extension returns`() {
    registerResolver()
    addSchema()
    myFixture.configureByText(PklFileType, "amends \"schema:proj<caret>ect.pkl\"\n")
    val target = myFixture.getReferenceAtCaretPosition()?.resolve()
    assertThat(target).isNotNull
    assertThat(target!!.containingFile.virtualFile.path).endsWith("schema/project.pkl")
  }

  fun `test members of a module behind a contributed scheme resolve, including its relative imports`() {
    registerResolver()
    addSchema()
    myFixture.configureByText(
      PklFileType,
      """
      amends "schema:project.pkl"

      name = "shop"
      size = 2
      color = "blue"
      """
        .trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  fun `test an unknown scheme stays unresolved without an extension`() {
    addSchema()
    myFixture.configureByText(PklFileType, "amends \"schema:proj<caret>ect.pkl\"\n")
    assertThat(myFixture.getReferenceAtCaretPosition()?.resolve()).isNull()
  }
}
