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

import android.content.Context
import android.view.View
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.Diff
import com.facebook.litho.LifecycleStep
import com.facebook.litho.Size
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.ShouldUpdate

/**
 * A MountSpec that lets us test the function of ShouldUpdate/pure-render by supplying an object
 * who's identity determines the result of @ShouldUpdate.
 */
@MountSpec(isPureRender = true)
object MountSpecWithShouldUpdateSpec {

  @JvmStatic @OnCreateMountContent fun onCreateMountContent(c: Context): View = View(c)

  @JvmStatic
  @ShouldUpdate(onMount = true)
  fun shouldUpdate(@Prop objectForShouldUpdate: Diff<Any>): Boolean =
      objectForShouldUpdate.previous != objectForShouldUpdate.next

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop operationsOutput: MutableList<LifecycleStep>
  ) {
    size.height = 100
    size.width = 100
    operationsOutput.add(LifecycleStep.ON_MEASURE)
  }

  @JvmStatic
  @OnMount
  fun onMount(
      c: ComponentContext,
      v: View,
      @Prop operationsOutput: MutableList<LifecycleStep>,
      @Prop objectForShouldUpdate: Any?
  ) {
    operationsOutput.add(LifecycleStep.ON_MOUNT)
  }

  @JvmStatic
  @OnUnmount
  fun onUnmount(
      c: ComponentContext,
      v: View,
      @Prop operationsOutput: MutableList<LifecycleStep>,
      @Prop objectForShouldUpdate: Any?
  ) {
    operationsOutput.add(LifecycleStep.ON_UNMOUNT)
  }
}
