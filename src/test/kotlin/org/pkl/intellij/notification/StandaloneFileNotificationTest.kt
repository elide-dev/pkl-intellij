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
package org.pkl.intellij.notification

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.ExtensionTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.pkl.intellij.PklStandaloneFilesExtension
import org.pkl.intellij.PklTestCase
import org.pkl.intellij.toolchain.pklCli

class StandaloneFileNotificationTest : PklTestCase() {
  private fun banner(name: String): Any? {
    val file = myFixture.configureByText(name, "foo = 1\n").virtualFile
    val editor = FileEditorManager.getInstance(project).getSelectedEditor(file)!!
    return PklToolchainNotificationProvider(project).collectNotificationData(project, file).apply(editor)
  }

  fun `test a standalone file gets no Pkl CLI banner`() {
    ExtensionTestUtil.maskExtensions(
      PklStandaloneFilesExtension.EP_NAME,
      listOf(
        object : PklStandaloneFilesExtension {
          override fun isStandaloneFile(file: PsiFile) = file.name == "tool.pkl"
        }
      ),
      testRootDisposable,
    )
    assertThat(banner("tool.pkl")).isNull()
  }

  fun `test other files still get the banner without a Pkl CLI`() {
    if (project.pklCli.isAvailable()) return
    assertThat(banner("other.pkl")).isNotNull
  }
}
