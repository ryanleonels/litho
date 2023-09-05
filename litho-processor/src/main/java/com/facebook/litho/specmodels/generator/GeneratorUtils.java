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

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.model.MethodParamModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import java.util.List;
import java.util.Locale;

public final class GeneratorUtils {

  private GeneratorUtils() {
    throw new AssertionError("No instances.");
  }

  public static ParameterSpec parameter(
      MethodParamModel param, AnnotationSpec... extraAnnotations) {
    return parameter(param, param.getTypeName(), param.getName(), extraAnnotations);
  }

  public static ParameterSpec parameter(
      MethodParamModel param, TypeName type, String name, AnnotationSpec... extraAnnotations) {
    return parameter(type, name, param.getExternalAnnotations(), extraAnnotations);
  }

  public static ParameterSpec parameter(
      TypeName type,
      String name,
      List<AnnotationSpec> externalAnnotations,
      AnnotationSpec... extraAnnotations) {
    final ParameterSpec.Builder builder =
        ParameterSpec.builder(type, name).addAnnotations(externalAnnotations);

    for (AnnotationSpec annotation : extraAnnotations) {
      builder.addAnnotation(annotation);
    }

    return builder.build();
  }

  public static AnnotationSpec annotation(final ClassName className) {
    return AnnotationSpec.builder(className).build();
  }

  public static MethodSpec.Builder getter(FieldSpec field) {
    return MethodSpec.methodBuilder("get" + capitalize(field.name))
        .returns(field.type)
        .addStatement("return this.$N", field.name);
  }

  public static String capitalize(String str) {
    if (str == null || str.trim().equals("")) {
      return str;
    }

    return str.substring(0, 1).toUpperCase(Locale.US) + str.substring(1);
  }
}
