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

package com.facebook.litho;

import androidx.annotation.Nullable;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaNode;

public class LithoYogaBaselineFunction implements YogaBaselineFunction {

  @Override
  public float baseline(YogaNode cssNode, float width, float height) {
    final LithoLayoutResult result = (LithoLayoutResult) cssNode.getData();
    final LithoNode node = result.getNode();
    if (!(node.getTailComponent() instanceof SpecGeneratedComponent)) {
      throw new IllegalStateException(
          "Trying to call onMeasureBaseline on a non-Spec component: "
              + node.getTailComponent().getSimpleName());
    }
    final SpecGeneratedComponent component = (SpecGeneratedComponent) node.getTailComponent();
    final @Nullable InterStagePropsContainer interStageProps =
        (InterStagePropsContainer) result.getLayoutData();

    return component.onMeasureBaseline(
        result.getContext(), (int) width, (int) height, interStageProps);
  }
}
