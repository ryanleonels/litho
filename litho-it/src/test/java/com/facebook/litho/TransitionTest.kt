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

package com.facebook.litho

import android.graphics.Rect
import com.facebook.litho.Transition.RootBoundsTransition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.animation.DimensionValue
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import com.facebook.rendercore.transitions.TransitionUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(LithoTestRunner::class)
class TransitionTest {

  @Test
  fun testCollectRootBoundsTransitions() {
    val transition =
        Transition.create(Transition.TransitionKeyType.GLOBAL, "rootKey")
            .animate(AnimatedProperties.WIDTH)
    val rootTransitionId = TransitionId(TransitionId.Type.GLOBAL, "rootKey", null)
    val rootWidthTransition = RootBoundsTransition()
    TransitionUtils.collectRootBoundsTransitions(
        rootTransitionId, transition, AnimatedProperties.WIDTH, rootWidthTransition)
    assertThat(rootWidthTransition.hasTransition).isTrue
    assertThat(rootWidthTransition.appearTransition).isNull()
    val rootHeightTransition = RootBoundsTransition()
    TransitionUtils.collectRootBoundsTransitions(
        rootTransitionId, transition, AnimatedProperties.HEIGHT, rootHeightTransition)
    assertThat(rootHeightTransition.hasTransition).isFalse
  }

  @Test
  fun testCollectRootBoundsTransitionsAppearComesAfterAllLayout() {
    val transition =
        Transition.parallel(
            Transition.allLayout(),
            Transition.create(Transition.TransitionKeyType.GLOBAL, "rootKey")
                .animate(AnimatedProperties.HEIGHT)
                .appearFrom(10f),
            Transition.create(Transition.TransitionKeyType.GLOBAL, "otherkey")
                .animate(AnimatedProperties.ALPHA))
    val rootTransitionId = TransitionId(TransitionId.Type.GLOBAL, "rootKey", null)
    val rootHeightTransition = RootBoundsTransition()
    TransitionUtils.collectRootBoundsTransitions(
        rootTransitionId, transition, AnimatedProperties.HEIGHT, rootHeightTransition)
    assertThat(rootHeightTransition.hasTransition).isTrue
    assertThat(rootHeightTransition.appearTransition).isNotNull
  }

  @Test
  fun testCollectRootBoundsTransitionsAppearComesBeforeAllLayout() {
    val transition =
        Transition.parallel(
            Transition.create(Transition.TransitionKeyType.GLOBAL, "rootKey")
                .animate(AnimatedProperties.HEIGHT)
                .appearFrom(10f),
            Transition.allLayout(),
            Transition.create(Transition.TransitionKeyType.GLOBAL, "otherkey")
                .animate(AnimatedProperties.ALPHA))
    val rootTransitionId = TransitionId(TransitionId.Type.GLOBAL, "rootKey", null)
    val rootHeightTransition = RootBoundsTransition()
    TransitionUtils.collectRootBoundsTransitions(
        rootTransitionId, transition, AnimatedProperties.HEIGHT, rootHeightTransition)
    assertThat(rootHeightTransition.hasTransition).isTrue
    assertThat(rootHeightTransition.appearTransition).isNotNull
  }

  @Test
  fun testCollectRootBoundsTransitionsExtractAppearFrom() {
    val transition =
        Transition.create(Transition.TransitionKeyType.GLOBAL, "rootKey")
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(10f)
    val rootTransitionId = TransitionId(TransitionId.Type.GLOBAL, "rootKey", null)
    val rootHeightTransition = RootBoundsTransition()
    TransitionUtils.collectRootBoundsTransitions(
        rootTransitionId, transition, AnimatedProperties.HEIGHT, rootHeightTransition)
    assertThat(rootHeightTransition.hasTransition).isTrue
    assertThat(rootHeightTransition.appearTransition).isNotNull
    val layoutState = mock<LayoutState>()
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              SimpleMountSpecTester.create(c).build()
        }
    val rootUnit: LithoRenderUnit =
        MountSpecLithoRenderUnit.create(
            0, component, null, null, null, 0, 0, MountSpecLithoRenderUnit.STATE_UNKNOWN, null)
    whenever(layoutState.getMountableOutputAt(0))
        .thenReturn(create(rootUnit, Rect(0, 0, 300, 100), null, null))
    val animateFrom =
        Transition.getRootAppearFromValue(
                rootHeightTransition.appearTransition, layoutState, AnimatedProperties.HEIGHT)
            .toInt()
    assertThat(animateFrom).isEqualTo(10)
  }

  @Test
  fun testCollectRootBoundsTransitionsExtractAppearFromDimensionValue() {
    val transition =
        Transition.create(Transition.TransitionKeyType.GLOBAL, "rootKey")
            .animate(AnimatedProperties.HEIGHT)
            .appearFrom(DimensionValue.heightPercentageOffset(50f))
    val rootTransitionId = TransitionId(TransitionId.Type.GLOBAL, "rootKey", null)
    val rootHeightTransition = RootBoundsTransition()
    TransitionUtils.collectRootBoundsTransitions(
        rootTransitionId, transition, AnimatedProperties.HEIGHT, rootHeightTransition)
    assertThat(rootHeightTransition.hasTransition).isTrue
    assertThat(rootHeightTransition.appearTransition).isNotNull
    val layoutState = mock<LayoutState>()
    val bounds = Rect(0, 0, 300, 100)
    whenever(layoutState.animatableRootItem)
        .thenReturn(LithoAnimtableItem(0, Rect(0, 0, 300, 100), OutputUnitType.CONTENT, null, null))
    val animateFrom =
        Transition.getRootAppearFromValue(
                rootHeightTransition.appearTransition, layoutState, AnimatedProperties.HEIGHT)
            .toInt()
    val expectedAppearFrom = bounds.height() * 1.5f
    assertThat(animateFrom).isEqualTo(expectedAppearFrom.toInt())
  }
}
