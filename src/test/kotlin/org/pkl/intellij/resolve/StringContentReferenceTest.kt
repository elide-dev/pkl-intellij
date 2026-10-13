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

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.PsiReferenceRegistrarImpl
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.util.ProcessingContext
import org.assertj.core.api.Assertions.assertThat
import org.pkl.intellij.PklFileType
import org.pkl.intellij.PklLanguage
import org.pkl.intellij.PklTestCase
import org.pkl.intellij.psi.PklStringContent

class StringContentReferenceTest : PklTestCase() {
  fun `test string contents are visited by reference contributors`() {
    registerStringContentReferenceProvider()

    myFixture.configureByText(
      PklFileType,
      """
      foo: String = "ba<caret>r"
      """
        .trimIndent()
    )

    val reference = myFixture.getReferenceAtCaretPosition()
    assertThat(reference).isNotNull
    assertThat(reference!!.element).isInstanceOf(PklStringContent::class.java)
    assertThat(reference.resolve()).isEqualTo(myFixture.file)
  }

  fun `test string contents have no references if none are contributed`() {
    myFixture.configureByText(
      PklFileType,
      """
      foo: String = "ba<caret>r"
      """
        .trimIndent()
    )

    assertThat(myFixture.getReferenceAtCaretPosition()).isNull()
  }

  /**
   * Registers a reference provider that turns the entire text of every [PklStringContent] into a
   * reference to the containing file.
   */
  private fun registerStringContentReferenceProvider() {
    val registrar =
      ReferenceProvidersRegistry.getInstance().getRegistrar(PklLanguage)
        as PsiReferenceRegistrarImpl
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(PklStringContent::class.java),
      object : PsiReferenceProvider() {
        override fun getReferencesByElement(
          element: PsiElement,
          context: ProcessingContext
        ): Array<PsiReference> =
          arrayOf(
            object : PsiReferenceBase<PsiElement>(element, TextRange(0, element.textLength), true) {
              override fun resolve(): PsiElement = element.containingFile
            }
          )
      },
      PsiReferenceRegistrar.DEFAULT_PRIORITY,
      testRootDisposable
    )
  }
}
