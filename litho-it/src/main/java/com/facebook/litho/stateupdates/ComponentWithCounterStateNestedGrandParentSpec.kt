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

package com.facebook.litho.stateupdates

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.SizeSpec
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec
import com.facebook.litho.annotations.Prop

@LayoutSpec
object ComponentWithCounterStateNestedGrandParentSpec {
  @JvmStatic
  @OnCreateLayoutWithSizeSpec
  fun onCreateLayoutWithSizeSpec(
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      @Prop(optional = true) caller: ComponentWithCounterStateLayoutSpec.Caller?
  ): Component =
      Column.create(c)
          .child(
              ComponentWithCounterStateNestedParent.create(c)
                  .caller(caller)
                  .widthPx(SizeSpec.getSize(widthSpec))
                  .heightPx(SizeSpec.getSize(heightSpec)))
          .build()
}
