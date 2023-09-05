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

package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.processor.PsiMethodExtractorUtils.getMethodParams;

import com.facebook.litho.annotations.OnBindDynamicValue;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BindDynamicValueMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PsiBindDynamicValuesMethodExtractor {
  public static ImmutableList<SpecMethodModel<BindDynamicValueMethod, Void>>
      getOnBindDynamicValuesMethods(PsiClass psiClass) {
    final List<SpecMethodModel<BindDynamicValueMethod, Void>> methods = new ArrayList<>();
    for (PsiMethod psiMethod : psiClass.getAllMethods()) {
      final Annotation annotation =
          PsiAnnotationProxyUtils.findAnnotationInHierarchy(psiMethod, OnBindDynamicValue.class);
      if (annotation == null) {
        continue;
      }
      final List<MethodParamModel> methodParams =
          getMethodParams(
              psiMethod,
              Collections.singletonList(Prop.class),
              ImmutableList.of(),
              ImmutableList.of(),
              ImmutableList.of());
      final PsiType returnType = psiMethod.getReturnType();
      if (returnType == null) {
        continue;
      }

      final SpecMethodModel<BindDynamicValueMethod, Void> methodModel =
          new SpecMethodModel<>(
              ImmutableList.of(annotation),
              PsiModifierExtractor.extractModifiers(psiMethod.getModifierList()),
              psiMethod.getName(),
              PsiTypeUtils.generateTypeSpec(returnType),
              ImmutableList.of(),
              ImmutableList.copyOf(methodParams),
              psiMethod,
              null);
      methods.add(methodModel);
    }
    return ImmutableList.copyOf(methods);
  }
}
