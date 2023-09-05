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

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.VisibleEvent
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.State
import com.facebook.litho.annotations.TreeProp
import com.facebook.litho.widget.ItemCardComponentSpec.TreeProps

@LayoutSpec
internal object CardActionsComponentSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(c: ComponentContext, isEnabled: StateValue<Boolean?>) {
    isEnabled.set(true)
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @State isEnabled: Boolean,
      @TreeProp props: ItemCardComponentSpec.TreeProps?
  ): Component =
      Column.create(c)
          .child(Text.create(c).text("controls"))
          .backgroundColor(Color.GRAY)
          .enabled(props == null || !props.areCardToolsDisabled)
          .child(
              Row.create(c)
                  .enabled(isEnabled)
                  .wrapInView()
                  .visibleHandler(CardActionsComponent.onVisible(c))
                  .child(SolidColor.create(c).color(Color.RED).widthDip(25f).heightDip(25f))
                  .child(SolidColor.create(c).color(Color.GREEN).widthDip(25f).heightDip(25f))
                  .child(SolidColor.create(c).color(Color.BLUE).widthDip(25f).heightDip(25f)))
          .build()

  @JvmStatic
  @OnEvent(VisibleEvent::class)
  fun onVisible(
      c: ComponentContext,
      @FromEvent content: Any?,
      @TreeProp props: ItemCardComponentSpec.TreeProps?
  ) {
    props?.onCardActionViewVisible?.call(content)
  }
}
