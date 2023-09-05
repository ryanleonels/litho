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

import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

/** Helper for extracting annotations from a given {@link Element}. */
public class AnnotationExtractor {
  public static ImmutableList<AnnotationSpec> extractValidAnnotations(Element element) {
    final List<AnnotationSpec> annotations = new ArrayList<>();
    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      if (isValidAnnotation(annotationMirror)) {
        annotations.add(AnnotationSpec.get(annotationMirror));
      }
    }

    return ImmutableList.copyOf(annotations);
  }

  /**
   * We consider an annotation to be valid for extraction if it's not an internal annotation (i.e.
   * is in the <code>com.facebook.litho</code> package and is not a source-only annotation.
   *
   * <p>We also do not consider the kotlin.Metadata annotation to be valid as it represents the
   * metadata of the Spec class and not of the class that we are generating.
   *
   * <p>The generated component is Java code, and annotations with package name "kotlin.jvm" are
   * kotlin only, which should be ignored as well.
   *
   * @return Whether or not to extract the given annotation.
   */
  private static boolean isValidAnnotation(AnnotationMirror annotation) {
    final Retention retention =
        annotation.getAnnotationType().asElement().getAnnotation(Retention.class);

    if (retention != null && retention.value() == RetentionPolicy.SOURCE) {
      return false;
    }

    String annotationName = annotation.getAnnotationType().toString();

    return !annotationName.startsWith("com.facebook.")
        && !annotationName.equals("kotlin.Metadata")
        && !annotationName.startsWith("kotlin.jvm");
  }
}
