/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.intellij.navigation;

import com.facebook.litho.intellij.LithoPluginUtils;
import com.facebook.litho.intellij.PsiSearchUtils;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public class LithoFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
  private final Function<PsiClass, PsiClass> findGeneratedClass;

  public LithoFindUsagesHandlerFactory() {
    findGeneratedClass =
        cls ->
            Optional.of(cls)
                .filter(LithoPluginUtils::isLithoSpec)
                .map(
                    specCls -> {
                      final String specFQN = specCls.getQualifiedName();
                      final String componentFQN =
                          LithoPluginUtils.getLithoComponentNameFromSpec(specFQN);
                      return PsiSearchUtils.getInstance()
                          .findOriginalClass(specCls.getProject(), componentFQN);
                    })
                .orElse(null);
  }

  @Override
  public boolean canFindUsages(PsiElement element) {
    return GeneratedClassFindUsagesHandler.canFindUsages(element)
        || SpecMethodFindUsagesHandler.canFindUsages(element);
  }

  @Nullable
  @Override
  public FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
    if (GeneratedClassFindUsagesHandler.canFindUsages(element)) {
      return new GeneratedClassFindUsagesHandler(element, findGeneratedClass);
    }
    if (SpecMethodFindUsagesHandler.canFindUsages(element)) {
      return new SpecMethodFindUsagesHandler(element, findGeneratedClass);
    }
    return null;
  }
}
