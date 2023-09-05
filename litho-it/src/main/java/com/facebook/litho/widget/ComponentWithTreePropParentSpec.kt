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

package com.facebook.litho.widget

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTreeProp
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.treeprops.SimpleTreeProp

@LayoutSpec
object ComponentWithTreePropParentSpec {

  @JvmStatic
  @OnCreateTreeProp
  fun onCreateTreePropA(c: ComponentContext, @Prop propA: SimpleTreeProp): SimpleTreeProp = propA

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @State isChecked: Boolean): Component =
      MountSpecWithTreeProp.create(c).viewWidth(100).viewHeight(100).build()
}
