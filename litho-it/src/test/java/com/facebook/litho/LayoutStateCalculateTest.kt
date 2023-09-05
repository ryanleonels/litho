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

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.SparseArray
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.accessibility.AccessibilityManager
import androidx.core.view.ViewCompat
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.TestLayoutComponent
import com.facebook.litho.testing.TestSizeDependentComponent
import com.facebook.litho.testing.TestViewComponent
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.logging.TestComponentsLogger
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ComponentCaching
import com.facebook.litho.widget.ItemCardComponent
import com.facebook.litho.widget.ItemCardComponentSpec
import com.facebook.litho.widget.LayoutSpecLifecycleTester
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.SolidColor
import com.facebook.litho.widget.TestNullLayoutComponent
import com.facebook.litho.widget.Text
import com.facebook.rendercore.Function
import com.facebook.rendercore.RenderTreeNode
import com.facebook.rendercore.utils.MeasureSpecUtils
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaPositionType
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAccessibilityManager

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LayoutStateCalculateTest {

  val config = ComponentsConfiguration.create().shouldAddHostViewForRootComponent(true).build()

  @JvmField @Rule val legacyLithoViewRule = LegacyLithoViewRule(config)
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    // invdalidate the cached accessibility value before each test runs so that we don't
    // have a value already cached.  If we don't do this, accessibility tests will fail when run
    // after non-accessibility tests, and vice-versa.
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    context = legacyLithoViewRule.context
  }

  @After
  fun validate() {
    Mockito.validateMockitoUsage()
  }

  @Test
  fun testNoUnnecessaryLayoutOutputsForLayoutSpecs() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Column.create(c).child(TestDrawableComponent.create(c)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(2)
  }

  @Test
  fun testLayoutOutputsForRootInteractiveLayoutSpecs() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c).child(TestDrawableComponent.create(c)).wrapInView().build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
  }

  @Test
  fun testLayoutOutputsForSpecsWithTouchExpansion() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c).widthPx(100).heightPx(10))
                  .child(
                      Row.create(c)
                          .viewTag(Any())
                          .child(TestDrawableComponent.create(c).widthPx(20).heightPx(90))
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .clickHandler(c.newEventHandler(1) as? EventHandler<ClickEvent>)
                                  .widthPx(50)
                                  .heightPx(50)
                                  .touchExpansionPx(YogaEdge.ALL, 5)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(6)
    val layoutData = layoutState.getMountableOutputAt(4).layoutData as LithoLayoutData
    assertThat(layoutData.expandedTouchBounds).isEqualTo(Rect(5, 5, 5, 5))
    val nodeInfo = LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(4)).nodeInfo
    assertThat(nodeInfo).isNotNull
    assertThat(nodeInfo?.clickHandler).isNotNull
    assertThat(nodeInfo?.longClickHandler).isNull()
    assertThat(nodeInfo?.focusChangeHandler).isNull()
    assertThat(nodeInfo?.touchHandler).isNull()
  }

  @Test
  fun testLayoutOutputsForSpecsWithClickHandling() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .clickHandler(c.newEventHandler(1) as? EventHandler<ClickEvent>))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val nodeInfo = LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo
    assertThat(nodeInfo).isNotNull
    assertThat(nodeInfo?.clickHandler).isNotNull
    assertThat(nodeInfo?.longClickHandler).isNull()
    assertThat(nodeInfo?.focusChangeHandler).isNull()
    assertThat(nodeInfo?.touchHandler).isNull()
  }

  @Test
  fun testLayoutOutputsForSpecsWithLongClickHandling() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .longClickHandler(c.newEventHandler(1) as? EventHandler<LongClickEvent>))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val nodeInfo = LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo
    assertThat(nodeInfo).isNotNull
    assertThat(nodeInfo?.clickHandler).isNull()
    assertThat(nodeInfo?.longClickHandler).isNotNull
    assertThat(nodeInfo?.focusChangeHandler).isNull()
    assertThat(nodeInfo?.touchHandler).isNull()
  }

  @Test
  fun testLayoutOutputsForSpecsWithFocusChangeHandling() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .focusChangeHandler(
                              c.newEventHandler(1) as? EventHandler<FocusChangedEvent>))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val nodeInfo = LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo
    assertThat(nodeInfo).isNotNull
    assertThat(nodeInfo?.clickHandler).isNull()
    assertThat(nodeInfo?.longClickHandler).isNull()
    assertThat(nodeInfo?.focusChangeHandler).isNotNull
    assertThat(nodeInfo?.touchHandler).isNull()
  }

  @Test
  fun testLayoutOutputsForSpecsWithInterceptTouchHandling() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .interceptTouchHandler(
                              c.newEventHandler(1) as? EventHandler<InterceptTouchEvent>))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val nodeInfo = LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo
    assertThat(nodeInfo).isNotNull
    assertThat(nodeInfo?.clickHandler).isNull()
    assertThat(nodeInfo?.longClickHandler).isNull()
    assertThat(nodeInfo?.interceptTouchHandler).isNotNull
    assertThat(nodeInfo?.touchHandler).isNull()
  }

  @Test
  fun testLayoutOutputsForSpecsWithTouchHandling() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .touchHandler(c.newEventHandler(1) as? EventHandler<TouchEvent>))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val nodeInfo = LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo
    assertThat(nodeInfo).isNotNull
    assertThat(nodeInfo?.touchHandler).isNotNull
    assertThat(nodeInfo?.clickHandler).isNull()
    assertThat(nodeInfo?.longClickHandler).isNull()
    assertThat(nodeInfo?.focusChangeHandler).isNull()
  }

  @Test
  fun testLayoutOutputsForDeepLayoutSpecs() {
    val paddingSize = 5
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .backgroundColor(-0x10000)
                  .child(
                      Row.create(c)
                          .justifyContent(YogaJustify.SPACE_AROUND)
                          .alignItems(YogaAlign.CENTER)
                          .positionType(YogaPositionType.ABSOLUTE)
                          .positionPx(YogaEdge.LEFT, 50)
                          .positionPx(YogaEdge.TOP, 50)
                          .positionPx(YogaEdge.RIGHT, 200)
                          .positionPx(YogaEdge.BOTTOM, 50)
                          .child(Text.create(c).text("textLeft1"))
                          .child(Text.create(c).text("textRight1"))
                          .paddingPx(YogaEdge.ALL, paddingSize)
                          .wrapInView())
                  .child(
                      Row.create(c)
                          .justifyContent(YogaJustify.SPACE_AROUND)
                          .alignItems(YogaAlign.CENTER)
                          .positionType(YogaPositionType.ABSOLUTE)
                          .positionPx(YogaEdge.LEFT, 200)
                          .positionPx(YogaEdge.TOP, 50)
                          .positionPx(YogaEdge.RIGHT, 50)
                          .positionPx(YogaEdge.BOTTOM, 50)
                          .child(
                              Text.create(c)
                                  .text("textLeft2")
                                  .wrapInView()
                                  .paddingPx(YogaEdge.ALL, paddingSize))
                          .child(TestViewComponent.create(c).wrapInView()))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Mountable Output Breakdown:
    // 0) Root
    // 1) Host with bg
    // 2) Row 1
    // 3) Text drawable 1
    // 4) Text drawable 2
    // 5) Row 2
    // 6) Text drawable
    // 7) Text view component

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(8)

    // Check quantity of HostComponents.
    var totalHosts = 0
    for (i in 0 until layoutState.mountableOutputCount) {
      val mountedComponent = getComponentAt(layoutState, i)
      if (isHostComponent(mountedComponent)) {
        totalHosts++
      }
    }
    assertThat(totalHosts).isEqualTo(4)

    // Check all the Layouts are in the correct position.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 2))).isTrue
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(Text::class.java)
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(Text::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue
    assertThat(getComponentAt(layoutState, 6)).isInstanceOf(Text::class.java)
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestViewComponent::class.java)

    // Check the text within the TextComponents.
    assertThat(getTextFromTextComponent(layoutState, 3)).isEqualTo("textLeft1")
    assertThat(getTextFromTextComponent(layoutState, 4)).isEqualTo("textRight1")
    assertThat(getTextFromTextComponent(layoutState, 6)).isEqualTo("textLeft2")
    val textLayoutBounds = layoutState.getMountableOutputAt(6).getAbsoluteBounds(Rect())
    val textBackgroundBounds = layoutState.getMountableOutputAt(5).getAbsoluteBounds(Rect())
    assertThat(textLayoutBounds.left - paddingSize).isEqualTo(textBackgroundBounds.left)
    assertThat(textLayoutBounds.top - paddingSize).isEqualTo(textBackgroundBounds.top)
    assertThat(textLayoutBounds.right + paddingSize).isEqualTo(textBackgroundBounds.right)
    assertThat(textLayoutBounds.bottom + paddingSize).isEqualTo(textBackgroundBounds.bottom)
  }

  @Test
  fun testLayoutOutputMountBounds() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .widthPx(30)
                  .heightPx(30)
                  .wrapInView()
                  .child(
                      Column.create(c)
                          .widthPx(10)
                          .heightPx(10)
                          .marginPx(YogaEdge.ALL, 10)
                          .wrapInView()
                          .child(TestDrawableComponent.create(c).widthPx(10).heightPx(10)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))
    var mountBounds = Rect()
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 30, 30))
    mountBounds = layoutState.getMountableOutputAt(1).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 30, 30))
    mountBounds = layoutState.getMountableOutputAt(2).bounds
    assertThat(mountBounds).isEqualTo(Rect(10, 10, 20, 20))
    mountBounds = layoutState.getMountableOutputAt(3).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 10, 10))
  }

  @Test
  fun testLayoutOutputsForDeepLayoutSpecsWithBackground() {
    val paddingSize = 5
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .backgroundColor(-0x10000)
                  .child(
                      Row.create(c)
                          .justifyContent(YogaJustify.SPACE_AROUND)
                          .alignItems(YogaAlign.CENTER)
                          .positionType(YogaPositionType.ABSOLUTE)
                          .positionPx(YogaEdge.LEFT, 50)
                          .positionPx(YogaEdge.TOP, 50)
                          .positionPx(YogaEdge.RIGHT, 200)
                          .positionPx(YogaEdge.BOTTOM, 50)
                          .child(Text.create(c).text("textLeft1"))
                          .child(Text.create(c).text("textRight1"))
                          .backgroundColor(-0x10000)
                          .foregroundColor(-0x10000)
                          .paddingPx(YogaEdge.ALL, paddingSize)
                          .wrapInView())
                  .child(
                      Row.create(c)
                          .justifyContent(YogaJustify.SPACE_AROUND)
                          .alignItems(YogaAlign.CENTER)
                          .positionType(YogaPositionType.ABSOLUTE)
                          .positionPx(YogaEdge.LEFT, 200)
                          .positionPx(YogaEdge.TOP, 50)
                          .positionPx(YogaEdge.RIGHT, 50)
                          .positionPx(YogaEdge.BOTTOM, 50)
                          .child(
                              Text.create(c)
                                  .text("textLeft2")
                                  .wrapInView()
                                  .backgroundColor(-0x10000)
                                  .paddingPx(YogaEdge.ALL, paddingSize))
                          .child(
                              TestViewComponent.create(c)
                                  .backgroundColor(-0x10000)
                                  .foregroundColor(0x0000FFFF)
                                  .paddingPx(YogaEdge.ALL, paddingSize)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(8)

    // Check quantity of HostComponents.
    var totalHosts = 0
    for (i in 0 until layoutState.mountableOutputCount) {
      val mountedComponent = getComponentAt(layoutState, i)
      if (isHostComponent(mountedComponent)) {
        totalHosts++
      }
    }
    assertThat(totalHosts).isEqualTo(4)

    // Check all the Layouts are in the correct position.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 2))).isTrue
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(Text::class.java)
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(Text::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue
    assertThat(getComponentAt(layoutState, 6)).isInstanceOf(Text::class.java)
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestViewComponent::class.java)

    // Check the text within the TextComponents.
    assertThat(getTextFromTextComponent(layoutState, 3)).isEqualTo("textLeft1")
    assertThat(getTextFromTextComponent(layoutState, 4)).isEqualTo("textRight1")
    assertThat(getTextFromTextComponent(layoutState, 6)).isEqualTo("textLeft2")
  }

  @Test
  fun testLayoutOutputsForMegaDeepLayoutSpecs() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c))
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c).wrapInView())
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestDrawableComponent.create(c))
                                  .wrapInView())
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestDrawableComponent.create(c)))
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestViewComponent.create(c)))
                          .wrapInView())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(18)

    // Check quantity of HostComponents.
    var totalHosts = 0
    for (i in 0 until layoutState.mountableOutputCount) {
      val mountedComponent = getComponentAt(layoutState, i)
      if (isHostComponent(mountedComponent)) {
        totalHosts++
      }
    }
    assertThat(totalHosts).isEqualTo(7)

    // Check all the Components match the right LithoRenderUnit positions.
    // Tree One.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestDrawableComponent::class.java)

    // Tree Two.
    assertThat(isHostComponent(getComponentAt(layoutState, 4))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue
    assertThat(getComponentAt(layoutState, 6)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 7))).isTrue
    assertThat(getComponentAt(layoutState, 8)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 9)).isInstanceOf(TestDrawableComponent::class.java)

    // Tree Three.
    assertThat(isHostComponent(getComponentAt(layoutState, 10))).isTrue
    assertThat(getComponentAt(layoutState, 11)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 12)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 13)).isInstanceOf(TestDrawableComponent::class.java)

    // Tree Four.
    assertThat(isHostComponent(getComponentAt(layoutState, 14))).isTrue
    assertThat(getComponentAt(layoutState, 15)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 16)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 17)).isInstanceOf(TestViewComponent::class.java)
  }

  @Test
  fun testLayoutOutputStableIds() {
    // Needed because of (legacy) usage two different InlineLayoutSpec that the test wants to treat
    // as the same component type.
    val COMPONENT_IDENTITY = 12_345
    val component1: Component =
        object : InlineLayoutSpec(COMPONENT_IDENTITY) {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .contentDescription("cd0"))
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c))
                          .contentDescription("cd1"))
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestViewComponent.create(c))
                          .contentDescription("cd2"))
                  .build()
        }
    val component2: Component =
        object : InlineLayoutSpec(COMPONENT_IDENTITY) {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .contentDescription("cd0"))
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c))
                          .contentDescription("cd1"))
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestViewComponent.create(c))
                          .contentDescription("cd2"))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component1,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(20, EXACTLY))
    val sameComponentLayoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component2,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(20, EXACTLY))
    assertThat(sameComponentLayoutState.mountableOutputCount)
        .isEqualTo(layoutState.mountableOutputCount)
    for (i in 0 until layoutState.mountableOutputCount) {
      assertThat(sameComponentLayoutState.getMountableOutputAt(i).renderUnit.id)
          .isEqualTo(layoutState.getMountableOutputAt(i).renderUnit.id)
    }
  }

  @Test
  fun testLayoutOutputStableIdsForMegaDeepComponent() {
    // Needed because of (legacy) usage two different InlineLayoutSpec that the test wants to treat
    // as the same component type.
    val COMPONENT_IDENTITY = 12_345
    val component1: Component =
        object : InlineLayoutSpec(COMPONENT_IDENTITY) {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c))
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c).wrapInView())
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestDrawableComponent.create(c))
                                  .wrapInView())
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestDrawableComponent.create(c)))
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestViewComponent.create(c)))
                          .wrapInView())
                  .build()
        }
    val component2: Component =
        object : InlineLayoutSpec(COMPONENT_IDENTITY) {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c))
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c).wrapInView())
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestDrawableComponent.create(c))
                                  .wrapInView())
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestDrawableComponent.create(c)))
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(
                              Column.create(c)
                                  .child(TestDrawableComponent.create(c))
                                  .child(TestViewComponent.create(c)))
                          .wrapInView())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component1,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(20, EXACTLY))
    val sameComponentLayoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component2,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(20, EXACTLY))
    assertThat(sameComponentLayoutState.mountableOutputCount)
        .isEqualTo(layoutState.mountableOutputCount)
    for (i in 0 until layoutState.mountableOutputCount) {
      assertThat(sameComponentLayoutState.getMountableOutputAt(i).renderUnit.id)
          .isEqualTo(layoutState.getMountableOutputAt(i).renderUnit.id)
    }
  }

  @Test
  fun testPartiallyStableIds() {
    // Needed because of (legacy) usage two different InlineLayoutSpec that the test wants to treat
    // as the same component type.
    val COMPONENT_IDENTITY = 12_345
    val component1: Component =
        object : InlineLayoutSpec(COMPONENT_IDENTITY) {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .child(TestDrawableComponent.create(c))
                  .build()
        }
    val component2: Component =
        object : InlineLayoutSpec(COMPONENT_IDENTITY) {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c)))
                  .build()
        }
    val layoutState1 =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component1,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(20, EXACTLY))
    val layoutState2 =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component2,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(20, EXACTLY))
    assertThat(layoutState2.getMountableOutputAt(0).renderUnit.id)
        .isEqualTo(layoutState1.getMountableOutputAt(0).renderUnit.id)
    assertThat(layoutState2.getMountableOutputAt(1).renderUnit.id)
        .isEqualTo(layoutState1.getMountableOutputAt(1).renderUnit.id)
    assertThat(layoutState1.mountableOutputCount).isEqualTo(3)
    assertThat(layoutState2.mountableOutputCount).isEqualTo(4)
  }

  @Test
  fun testDifferentIds() {
    // Needed because of (legacy) usage two different InlineLayoutSpec that the test wants to treat
    // as the same component type.
    val COMPONENT_IDENTITY = 12_345
    val component1: Component =
        object : InlineLayoutSpec(COMPONENT_IDENTITY) {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .child(TestDrawableComponent.create(c))
                  .build()
        }
    val component2: Component =
        object : InlineLayoutSpec(COMPONENT_IDENTITY) {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c).wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c))
                          .wrapInView())
                  .build()
        }
    val layoutState1 =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component1,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(20, EXACTLY))
    val layoutState2 =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component2,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(20, EXACTLY))
    assertThat(layoutState1.getMountableOutputAt(1).renderUnit.id)
        .isNotEqualTo(layoutState2.getMountableOutputAt(1).renderUnit.id)
  }

  @Test
  fun testLayoutOutputsWithInteractiveLayoutSpecAsLeafs() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .child(Column.create(c).child(TestLayoutComponent.create(c)).wrapInView())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 2))).isTrue
  }

  @Test
  fun testNoMeasureOnNestedComponentWithSameSpecs() {
    val baseContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val c =
        ComponentContextUtils.withComponentTree(
            baseContext, ComponentTree.create(baseContext).build())
    val resolveContext = c.setRenderStateContextForTests()
    val size = Size()
    val innerComponent = TestDrawableComponent.create(c, 0, 0, true, true, false, false).build()
    val widthSpec = makeSizeSpec(100, EXACTLY)
    val heightSpec = makeSizeSpec(100, EXACTLY)
    innerComponent.measure(c, widthSpec, heightSpec, size)
    val internalNode: LithoLayoutResult? = resolveContext.cache.getCachedResult(innerComponent)
    internalNode?.setSizeSpec(widthSpec, heightSpec)
    innerComponent.resetInteractions()
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Row.create(c).child(innerComponent).widthPx(100).heightPx(100))
                  .build()
        }
    calculateLayoutState(
        legacyLithoViewRule.componentTree.context,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY))

    // Currently, we create a clone of Component object and measure gets called on that cloned
    // Component.
    // Here we are checking if measure was called on Component object which was created
    // in test (actually it is getting called on cloned object but in useStatelessComponent we don't
    // clone the Component object)
    // Therefore different behaviour in useStatelessComponent
    assertThat(innerComponent.wasMeasureCalled()).isTrue
  }

  @Test
  fun testNoMeasureOnNestedComponentWithNewMeasureSpecExact() {
    val baseContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val c =
        ComponentContextUtils.withComponentTree(
            baseContext, ComponentTree.create(baseContext).build())
    val resolveContext = c.setRenderStateContextForTests()
    val size = Size()
    val innerComponent = TestDrawableComponent.create(c, 0, 0, true, true, false, false).build()
    val widthSpec = makeSizeSpec(100, AT_MOST)
    val heightSpec = makeSizeSpec(100, AT_MOST)
    innerComponent.measure(c, widthSpec, heightSpec, size)
    val internalNode: LithoLayoutResult? = resolveContext.cache.getCachedResult(innerComponent)
    internalNode?.setSizeSpec(widthSpec, heightSpec)
    innerComponent.resetInteractions()
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Row.create(c).child(innerComponent).widthPx(100).heightPx(100))
                  .build()
        }
    calculateLayoutState(
        legacyLithoViewRule.componentTree.context,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY))

    // Currently, we create a clone of Component object and measure gets called on that cloned
    // Component.
    // Here we are checking if measure was called on Component object which was created
    // in test (actually it is getting called on cloned object but in useStatelessComponent we don't
    // clone the Component object)
    // Therefore different behaviour in useStatelessComponent
    assertThat(innerComponent.wasMeasureCalled()).isTrue
  }

  @Test
  fun testNoMeasureOnNestedComponentWithNewMeasureSpecOldUnspecified() {
    val baseContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val c =
        ComponentContextUtils.withComponentTree(
            baseContext, ComponentTree.create(baseContext).build())
    val resolveContext = c.setRenderStateContextForTests()
    val size = Size()
    val innerComponent = TestDrawableComponent.create(c, 0, 0, true, true, false, false).build()
    val widthSpec = makeSizeSpec(0, UNSPECIFIED)
    val heightSpec = makeSizeSpec(0, UNSPECIFIED)
    innerComponent.measure(c, widthSpec, heightSpec, size)
    val internalNode: LithoLayoutResult? = resolveContext.cache.getCachedResult(innerComponent)
    internalNode?.setSizeSpec(widthSpec, heightSpec)
    innerComponent.resetInteractions()
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c).child(innerComponent).build()
        }
    calculateLayoutState(
        legacyLithoViewRule.componentTree.context,
        component,
        -1,
        makeSizeSpec(100, AT_MOST),
        makeSizeSpec(100, AT_MOST))

    // Currently, we create a clone of Component object and measure gets called on that cloned
    // Component.
    // Here we are checking if measure was called on Component object which was created
    // in test (actually it is getting called on cloned object but in useStatelessComponent we don't
    // clone the Component object)
    // Therefore different behaviour in useStatelessComponent
    assertThat(innerComponent.wasMeasureCalled()).isTrue
  }

  @Test
  fun testNoMeasureOnNestedComponentWithOldAndNewAtMost() {
    val baseContext = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val c =
        ComponentContextUtils.withComponentTree(
            baseContext, ComponentTree.create(baseContext).build())
    val resolveContext = c.setRenderStateContextForTests()
    val size = Size()
    val innerComponent = TestDrawableComponent.create(c, 0, 0, true, true, false, false).build()
    val widthSpec = makeSizeSpec(100, AT_MOST)
    val heightSpec = makeSizeSpec(100, AT_MOST)
    innerComponent.measure(c, widthSpec, heightSpec, size)
    val internalNode: LithoLayoutResult? = resolveContext.cache.getCachedResult(innerComponent)
    internalNode?.setSizeSpec(widthSpec, heightSpec)
    innerComponent.resetInteractions()
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c).child(Row.create(c).child(innerComponent).flexShrink(0f)).build()
        }
    calculateLayoutState(
        legacyLithoViewRule.componentTree.context,
        component,
        -1,
        makeSizeSpec(50, AT_MOST),
        makeSizeSpec(50, AT_MOST))

    // Currently, we create a clone of Component object and measure gets called on that cloned
    // Component.
    // Here we are checking if measure was called on Component object which was created
    // in test (actually it is getting called on cloned object but in useStatelessComponent we don't
    // clone the Component object)
    // Therefore different behaviour in useStatelessComponent
    assertThat(innerComponent.wasMeasureCalled()).isTrue
  }

  @Test
  fun testLayoutOutputsForTwiceNestedComponent() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(Column.create(c).child(TestDrawableComponent.create(c)))
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(Column.create(c).child(TestDrawableComponent.create(c)))
                          .child(Column.create(c).child(TestDrawableComponent.create(c))))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(5)
    val hostMarkerRoot = layoutState.getMountableOutputAt(0).renderUnit.id
    val hostMarkerOne = layoutState.getMountableOutputAt(1).renderUnit.id

    // First output is the inner host for the click handler
    assertThat(getHostId(layoutState.getMountableOutputAt(1))).isEqualTo(hostMarkerRoot)

    // Second output is the child of the inner host
    assertThat(getHostId(layoutState.getMountableOutputAt(2))).isEqualTo(hostMarkerOne)

    // Third and fourth outputs are children of the root view.
    assertThat(getHostId(layoutState.getMountableOutputAt(3))).isEqualTo(hostMarkerRoot)
    assertThat(getHostId(layoutState.getMountableOutputAt(4))).isEqualTo(hostMarkerRoot)
  }

  @Test
  fun testLayoutOutputsForComponentWithBackgrounds() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .backgroundColor(-0x10000)
                  .foregroundColor(-0x10000)
                  .child(TestDrawableComponent.create(c))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)

    // Host generated
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
  }

  @Test
  fun testLayoutOutputsForNonComponentClickableNode() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Column.create(c).child(TestDrawableComponent.create(c)).wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c))
                          .wrapInView())
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestViewComponent.create(c))
                          .wrapInView())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(9)
    val hostMarkerRoot = getHostId(layoutState.getMountableOutputAt(0))
    val hostMarkerZero = getHostId(layoutState.getMountableOutputAt(1))
    val hostMarkerTwo = getHostId(layoutState.getMountableOutputAt(4))
    val hostMarkerThree = getHostId(layoutState.getMountableOutputAt(7))
    assertThat(getHostId(layoutState.getMountableOutputAt(1))).isEqualTo(hostMarkerRoot)
    assertThat(getHostId(layoutState.getMountableOutputAt(3))).isEqualTo(hostMarkerZero)
    assertThat(getHostId(layoutState.getMountableOutputAt(5))).isEqualTo(hostMarkerTwo)
    assertThat(getHostId(layoutState.getMountableOutputAt(6))).isEqualTo(hostMarkerZero)
    assertThat(getHostId(layoutState.getMountableOutputAt(8))).isEqualTo(hostMarkerThree)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 3))).isTrue
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 5)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 6))).isTrue
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 8)).isInstanceOf(TestViewComponent::class.java)
  }

  @Test
  fun testLayoutOutputsForNonComponentContentDescriptionNode() {
    enableAccessibility()
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .contentDescription("cd0"))
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestDrawableComponent.create(c))
                          .contentDescription("cd1"))
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .child(TestViewComponent.create(c))
                          .contentDescription("cd2"))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(9)
    val hostMarkerRoot = getHostId(layoutState.getMountableOutputAt(0))
    val hostMarkerZero = getHostId(layoutState.getMountableOutputAt(1))
    val hostMarkerTwo = getHostId(layoutState.getMountableOutputAt(4))
    val hostMarkerThree = getHostId(layoutState.getMountableOutputAt(7))
    assertThat(getHostId(layoutState.getMountableOutputAt(1))).isEqualTo(hostMarkerRoot)
    assertThat(getHostId(layoutState.getMountableOutputAt(3))).isEqualTo(hostMarkerZero)
    assertThat(getHostId(layoutState.getMountableOutputAt(5))).isEqualTo(hostMarkerTwo)
    assertThat(getHostId(layoutState.getMountableOutputAt(6))).isEqualTo(hostMarkerZero)
    assertThat(getHostId(layoutState.getMountableOutputAt(8))).isEqualTo(hostMarkerThree)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 3))).isTrue
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 5)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 6))).isTrue
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(getComponentAt(layoutState, 8)).isInstanceOf(TestViewComponent::class.java)
  }

  @Test
  fun testLayoutOutputsForFocusableOnRoot() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c).child(TestDrawableComponent.create(c)).focusable(true).build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val rootOutput = layoutState.getMountableOutputAt(0)
    val hostOutput = layoutState.getMountableOutputAt(1)
    val drawableOutput = layoutState.getMountableOutputAt(2)
    assertThat(hostOutput.parent?.renderUnit?.id).isEqualTo(rootOutput.renderUnit.id)
    assertThat(drawableOutput.parent?.renderUnit?.id).isEqualTo(hostOutput.renderUnit.id)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo?.focusState)
        .isEqualTo(NodeInfo.FOCUS_SET_TRUE)
  }

  @Test
  fun testLayoutOutputsForFocusable() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Column.create(c).child(TestDrawableComponent.create(c)).focusable(true))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).nodeInfo).isNull()
    assertThat(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo?.focusState)
        .isEqualTo(NodeInfo.FOCUS_SET_TRUE)
  }

  @Test
  fun testLayoutOutputsForSelectedOnRoot() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c).child(TestDrawableComponent.create(c)).selected(true).build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val hostMarkerZero = getHostId(layoutState.getMountableOutputAt(0))
    assertThat(getHostId(layoutState.getMountableOutputAt(1))).isEqualTo(hostMarkerZero)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1))
                .nodeInfo
                ?.selectedState)
        .isEqualTo(NodeInfo.SELECTED_SET_TRUE)
  }

  @Test
  fun testLayoutOutputsForSelected() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(TestDrawableComponent.create(c))
                          .focusable(true)
                          .selected(true))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).nodeInfo).isNull()
    assertThat(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1))
                .nodeInfo
                ?.selectedState)
        .isEqualTo(NodeInfo.SELECTED_SET_TRUE)
  }

  @Test
  fun testLayoutOutputsForEnabledFalseDoesntWrap() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Column.create(c).child(TestDrawableComponent.create(c).enabled(false)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(2)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).nodeInfo).isNull()
    assertThat(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).component.simpleName)
        .isEqualTo("HostComponent")
    assertThat(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).component.simpleName)
        .isEqualTo("TestDrawableComponent")
    assertThat(
            LithoRenderUnit.isTouchableDisabled(
                LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).flags))
        .isTrue
  }

  @Test
  fun testLayoutOutputsForEnabledFalseInInnerWrappedComponentDrawable() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .child(
                              TestDrawableComponent.create(c)
                                  .clickHandler(c.newEventHandler(1) as? EventHandler<ClickEvent>)
                                  .enabled(false)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))

    // Because the TestDrawableComponent is disabled, we don't wrap it in a host.
    assertThat(layoutState.mountableOutputCount).isEqualTo(2)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).nodeInfo).isNull()
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).component)
        .isInstanceOf(HostComponent::class.java)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).component)
        .isInstanceOf(TestDrawableComponent::class.java)
    assertThat(
            LithoRenderUnit.isTouchableDisabled(
                LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).flags))
        .isTrue
  }

  @Test
  fun testLayoutOutputsForEnabledFalseInInnerComponentView() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(Column.create(c).child(TestViewComponent.create(c).enabled(false)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(2)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).nodeInfo).isNull()
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).component)
        .isInstanceOf(HostComponent::class.java)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).component)
        .isInstanceOf(TestViewComponent::class.java)
    assertThat(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1))
                .nodeInfo
                ?.enabledState)
        .isEqualTo(NodeInfo.ENABLED_SET_FALSE)
  }

  @Test
  fun testLayoutOutputsForEnabledFalseApplyToDescendent() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(
                      Column.create(c)
                          .enabled(false)
                          .child(TestViewComponent.create(c).enabled(true))
                          .child(
                              TestDrawableComponent.create(c)
                                  .clickHandler(c.newEventHandler(1) as? EventHandler<ClickEvent>))
                          .child(TestDrawableComponent.create(c).enabled(false)))
                  .child(
                      Column.create(c)
                          .child(TestViewComponent.create(c))
                          .child(TestDrawableComponent.create(c)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(6)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).nodeInfo).isNull()
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(0)).component)
        .isInstanceOf(HostComponent::class.java)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).component)
        .isInstanceOf(TestViewComponent::class.java)
    assertThat(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1))
                .nodeInfo
                ?.enabledState)
        .isEqualTo(NodeInfo.ENABLED_SET_FALSE)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(2)).component)
        .isInstanceOf(TestDrawableComponent::class.java)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(2)).nodeInfo).isNull()
    assertThat(
            LithoRenderUnit.isTouchableDisabled(
                LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(2)).flags))
        .isTrue
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(3)).component)
        .isInstanceOf(TestDrawableComponent::class.java)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(3)).nodeInfo).isNull()
    assertThat(
            LithoRenderUnit.isTouchableDisabled(
                LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(3)).flags))
        .isTrue
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(4)).component)
        .isInstanceOf(TestViewComponent::class.java)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(4)).nodeInfo).isNull()
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(5)).component)
        .isInstanceOf(TestDrawableComponent::class.java)
    assertThat(LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(5)).nodeInfo).isNull()
    assertThat(
            LithoRenderUnit.isTouchableDisabled(
                LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(5)).flags))
        .isFalse
  }

  @Test
  fun testLayoutOutputsForAccessibilityEnabled() {
    enableAccessibility()
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Row.create(c)
                  .alignItems(YogaAlign.CENTER)
                  .paddingDip(YogaEdge.ALL, 10f)
                  .contentDescription("This is root view")
                  .child(TestDrawableComponent.create(c).widthDip(30f).heightDip(30f))
                  .child(
                      TestDrawableComponent.create(c, true, true, true)
                          .flex(1f)
                          .flexBasisDip(0f)
                          .backgroundColor(Color.RED)
                          .marginDip(YogaEdge.HORIZONTAL, 10f))
                  .child(
                      Row.create(c)
                          .alignItems(YogaAlign.CENTER)
                          .paddingDip(YogaEdge.ALL, 10f)
                          .contentDescription("This is a container")
                          .child(
                              TestDrawableComponent.create(c)
                                  .widthDip(30f)
                                  .heightDip(30f)
                                  .contentDescription("This is an image"))
                          .child(
                              TestDrawableComponent.create(c, true, true, true)
                                  .flex(1f)
                                  .flexBasisDip(0f)
                                  .marginDip(YogaEdge.HORIZONTAL, 10f)))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))

    // Mountable output breakdown:
    // 0) Root
    // 1) Row host 1
    // 2) Drawable 1
    // 3) Host for Drawable 2
    // 4) Drawable 2
    // 5) Row host 2
    // 6) Host for drawable 3
    // 7) Drawable 3
    // 8) Host for drawable 4
    // 9) Drawable 4
    assertThat(layoutState.mountableOutputCount).isEqualTo(10)
    val rootOutput = layoutState.getMountableOutputAt(0)
    val rowOneOutput = layoutState.getMountableOutputAt(1)
    val drawableOnetOutput = layoutState.getMountableOutputAt(2)
    val drawableTwoHostOutput = layoutState.getMountableOutputAt(3)
    val drawableTwoOutput = layoutState.getMountableOutputAt(4)
    val rowTwoOutput = layoutState.getMountableOutputAt(5)
    val drawableThreeHostOutput = layoutState.getMountableOutputAt(6)
    val drawableThreeOutput = layoutState.getMountableOutputAt(7)
    val drawableFourHostOutput = layoutState.getMountableOutputAt(8)
    val drawableFourOutput = layoutState.getMountableOutputAt(9)
    assertThat(rowOneOutput.parent?.renderUnit?.id).isEqualTo(rootOutput.renderUnit.id)
    assertThat(drawableOnetOutput.parent?.renderUnit?.id).isEqualTo(rowOneOutput.renderUnit.id)
    assertThat(drawableTwoHostOutput.parent?.renderUnit?.id).isEqualTo(rowOneOutput.renderUnit.id)
    assertThat(drawableTwoOutput.parent?.renderUnit?.id)
        .isEqualTo(drawableTwoHostOutput.renderUnit.id)
    assertThat(rowTwoOutput.parent?.renderUnit?.id).isEqualTo(rowOneOutput.renderUnit.id)
    assertThat(drawableThreeHostOutput.parent?.renderUnit?.id).isEqualTo(rowTwoOutput.renderUnit.id)
    assertThat(drawableThreeOutput.parent?.renderUnit?.id)
        .isEqualTo(drawableThreeHostOutput.renderUnit.id)
    assertThat(drawableFourHostOutput.parent?.renderUnit?.id).isEqualTo(rowTwoOutput.renderUnit.id)
    assertThat(drawableFourOutput.parent?.renderUnit?.id)
        .isEqualTo(drawableFourHostOutput.renderUnit.id)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 3))).isTrue
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 6))).isTrue
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 8))).isTrue
    assertThat(getComponentAt(layoutState, 9)).isInstanceOf(TestDrawableComponent::class.java)
  }

  @Test
  fun testLayoutOutputsWithImportantForAccessibility() {
    enableAccessibility()
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .contentDescription("This is root view")
                  .child(TestDrawableComponent.create(c).widthDip(30f).heightDip(30f))
                  .child(
                      TestDrawableComponent.create(c, true, true, true)
                          .flex(1f)
                          .flexBasisDip(0f)
                          .backgroundColor(Color.RED)
                          .marginDip(YogaEdge.HORIZONTAL, 10f)
                          .importantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO))
                  .child(
                      Row.create(c)
                          .alignItems(YogaAlign.CENTER)
                          .paddingDip(YogaEdge.ALL, 10f)
                          .importantForAccessibility(
                              ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
                          .child(
                              TestDrawableComponent.create(c)
                                  .widthDip(30f)
                                  .heightDip(30f)
                                  .importantForAccessibility(
                                      ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
                                  .contentDescription("This is an image")))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(8)

    // Breakdown of mountable output:
    // getMountableOutputAt(0) = inlineLayout
    // getMountableOutputAt(1) = root host
    // getMountableOutputAt(2) = drawable zero
    // getMountableOutputAt(3) = drawable one host
    // getMountableOutputAt(4) = drawable one
    // getMountableOutputAt(5) = row
    // getMountableOutputAt(6) = drawable two host
    // getMountableOutputAt(7) = drawable two
    val inlineLayoutOutput = layoutState.getMountableOutputAt(0)
    val rootHostOutput = layoutState.getMountableOutputAt(1)
    val drawableZeroOutput = layoutState.getMountableOutputAt(2)
    val drawableOneHostOutput = layoutState.getMountableOutputAt(3)
    val drawableOneOutput = layoutState.getMountableOutputAt(4)
    val rowOutput = layoutState.getMountableOutputAt(5)
    val drawableTwoHostOutput = layoutState.getMountableOutputAt(6)
    val drawableTwoOutput = layoutState.getMountableOutputAt(7)
    assertThat(drawableZeroOutput.parent?.renderUnit?.id).isEqualTo(rootHostOutput.renderUnit.id)
    assertThat(drawableOneHostOutput.parent?.renderUnit?.id).isEqualTo(rootHostOutput.renderUnit.id)
    assertThat(drawableOneOutput.parent?.renderUnit?.id)
        .isEqualTo(drawableOneHostOutput.renderUnit.id)
    assertThat(rowOutput.parent?.renderUnit?.id).isEqualTo(rootHostOutput.renderUnit.id)
    assertThat(drawableTwoHostOutput.parent?.renderUnit?.id).isEqualTo(rowOutput.renderUnit.id)
    assertThat(drawableTwoOutput.parent?.renderUnit?.id)
        .isEqualTo(drawableTwoHostOutput.renderUnit.id)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 3))).isTrue
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 6))).isTrue
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestDrawableComponent::class.java)
    assertThat(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
        .isEqualTo(LithoRenderUnit.getRenderUnit(inlineLayoutOutput).importantForAccessibility)
    assertThat(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
        .isEqualTo(LithoRenderUnit.getRenderUnit(rootHostOutput).importantForAccessibility)
    assertThat(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
        .isEqualTo(LithoRenderUnit.getRenderUnit(drawableZeroOutput).importantForAccessibility)
    assertThat(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO)
        .isEqualTo(LithoRenderUnit.getRenderUnit(drawableOneHostOutput).importantForAccessibility)
    assertThat(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO)
        .isEqualTo(LithoRenderUnit.getRenderUnit(drawableOneOutput).importantForAccessibility)
    assertThat(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
        .isEqualTo(LithoRenderUnit.getRenderUnit(rowOutput).importantForAccessibility)
    assertThat(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
        .isEqualTo(LithoRenderUnit.getRenderUnit(drawableTwoHostOutput).importantForAccessibility)
    assertThat(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
        .isEqualTo(LithoRenderUnit.getRenderUnit(drawableTwoOutput).importantForAccessibility)
  }

  @Test
  fun testLayoutOutputsForClickHandlerAndViewTagsOnRoot() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .clickHandler(c.newEventHandler(1) as? EventHandler<ClickEvent>)
                  .viewTags(SparseArray())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val hostMarkerZero = getHostId(layoutState.getMountableOutputAt(0))
    assertThat(getHostId(layoutState.getMountableOutputAt(1))).isEqualTo(hostMarkerZero)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    val nodeInfo = LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo
    assertThat(nodeInfo).isNotNull
    assertThat(nodeInfo?.clickHandler).isNotNull
    assertThat(nodeInfo?.viewTags).isNotNull
  }

  @Test
  fun testLayoutOutputsForLongClickHandlerAndViewTagsOnRoot() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .longClickHandler(c.newEventHandler(1) as? EventHandler<LongClickEvent>)
                  .viewTags(SparseArray())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    val hostMarkerZero = getHostId(layoutState.getMountableOutputAt(0))
    assertThat(getHostId(layoutState.getMountableOutputAt(1))).isEqualTo(hostMarkerZero)
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    val nodeInfo = LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(1)).nodeInfo
    assertThat(nodeInfo).isNotNull
    assertThat(nodeInfo?.longClickHandler).isNotNull
    assertThat(nodeInfo?.viewTags).isNotNull
  }

  @Test
  fun testLayoutOutputsForForceWrappedComponent() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c).child(TestDrawableComponent.create(c).wrapInView()).build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    assertThat(getComponentAt(layoutState, 0)).isInstanceOf(HostComponent::class.java)
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(HostComponent::class.java)
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
  }

  @Test
  fun testLayoutOutputForRootNestedTreeComponent() {
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            TestSizeDependentComponent.create(
                    ComponentContext(ApplicationProvider.getApplicationContext<Context>()))
                .setFixSizes(true)
                .setDelegate(false)
                .build(),
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(4)
    var mountBounds = Rect()
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 350, 200))
    assertThat(getHostId(layoutState.getMountableOutputAt(0))).isEqualTo(0)
    // Check NestedTree
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    mountBounds = layoutState.getMountableOutputAt(1).bounds
    assertThat(mountBounds).isEqualTo(Rect(5, 5, 55, 55))
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(2).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 50, 50))
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(3).bounds
    assertThat(mountBounds).isEqualTo(Rect(8, 58, 342, 78))
  }

  @Test
  fun testLayoutOutputForDelegateNestedTreeComponentDelegate() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .paddingPx(YogaEdge.ALL, 2)
                  .child(
                      TestSizeDependentComponent.create(c)
                          .setFixSizes(true)
                          .setDelegate(true)
                          .marginPx(YogaEdge.ALL, 11))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
    var mountBounds = Rect()
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 350, 200))
    // Check NestedTree
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    mountBounds = layoutState.getMountableOutputAt(1).bounds
    assertThat(mountBounds).isEqualTo(Rect(13, 13, 63, 63))
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(2).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 50, 50))
  }

  @Test
  fun testLayoutOutputForDelegateNestedTreeComponent() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .paddingPx(YogaEdge.ALL, 2)
                  .child(
                      TestSizeDependentComponent.create(c)
                          .setFixSizes(true)
                          .setDelegate(false)
                          .marginPx(YogaEdge.ALL, 11))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(4)
    var mountBounds = Rect()
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 350, 200))
    assertThat(getHostId(layoutState.getMountableOutputAt(0))).isEqualTo(0)
    // Check NestedTree
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    mountBounds = layoutState.getMountableOutputAt(1).bounds
    assertThat(mountBounds).isEqualTo(Rect(18, 18, 68, 68))
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(2).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 50, 50))
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(3).bounds
    assertThat(mountBounds).isEqualTo(Rect(21, 71, 329, 91))
  }

  @Test
  fun testLayoutOutputForRootWithDelegateNestedTreeComponent() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              TestSizeDependentComponent.create(c).setFixSizes(true).setDelegate(false).build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(4)
    var mountBounds = Rect()
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 350, 200))
    assertThat(getHostId(layoutState.getMountableOutputAt(0))).isEqualTo(0)
    // Check NestedTree
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    mountBounds = layoutState.getMountableOutputAt(1).bounds
    assertThat(mountBounds).isEqualTo(Rect(5, 5, 55, 55))
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(2).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 50, 50))
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(3).bounds
    assertThat(mountBounds).isEqualTo(Rect(8, 58, 342, 78))
  }

  @Test
  fun testLayoutOutputRootWithPaddingOverridingDelegateNestedTreeComponent() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? {
            val nestedTreeRootComponent =
                TestSizeDependentComponent.create(c).setFixSizes(true).setDelegate(false).build()
            return Wrapper.create(c)
                .delegate(nestedTreeRootComponent)
                .paddingPx(YogaEdge.ALL, 10)
                .build()
          }
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))

    // Check total layout outputs.
    assertThat(layoutState.mountableOutputCount).isEqualTo(4)
    var mountBounds = Rect()
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 350, 200))
    assertThat(getHostId(layoutState.getMountableOutputAt(0))).isEqualTo(0)

    // Check NestedTree
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    mountBounds = layoutState.getMountableOutputAt(1).bounds
    assertThat(mountBounds).isEqualTo(Rect(10, 10, 60, 60))
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(2).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 50, 50))
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent::class.java)
    mountBounds = layoutState.getMountableOutputAt(3).bounds
    assertThat(mountBounds).isEqualTo(Rect(13, 63, 337, 83))
  }

  @Test
  fun testLayoutOutputForRootWithNullLayout() {
    val componentWithNullLayout: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? = null
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            componentWithNullLayout,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(0)
  }

  @Test
  fun testLayoutComponentForNestedTreeChildWithNullLayout() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .paddingPx(YogaEdge.ALL, 2)
                  .child(TestNullLayoutComponent.create(c))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(1)
    var mountBounds = Rect()
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 350, 200))
  }

  @Test
  fun `the size of layout results should be equal to measured size of the component`() {
    val width = 50
    val height = 30
    legacyLithoViewRule.render {
      Row.create(context)
          .child(
              MountSpecLifecycleTester.create(context)
                  .lifecycleTracker(LifecycleTracker())
                  .intrinsicSize(Size(width, height)))
          .build()
    }

    val result = legacyLithoViewRule.committedLayoutState!!.mLayoutResult!!.getChildAt(0)!!

    assertThat(result.node.tailComponent).isInstanceOf(MountSpecLifecycleTester::class.java)
    assertThat(result.width).isEqualTo(width)
    assertThat(result.height).isEqualTo(height)
  }

  @Test
  fun testNestedTreeComponentWithDoubleMeasurementsDoesntThrow() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Row.create(c)
                  .alignItems(YogaAlign.STRETCH)
                  .paddingPx(YogaEdge.ALL, 2)
                  .child(
                      TestSizeDependentComponent.create(c)
                          .setFixSizes(true)
                          .setDelegate(false)
                          .marginPx(YogaEdge.ALL, 11))
                  .child(TestDrawableComponent.create(c).heightPx(200).widthPx(200))
                  .build()
        }
    calculateLayoutState(
        legacyLithoViewRule.componentTree.context,
        component,
        -1,
        makeSizeSpec(350, EXACTLY),
        makeSizeSpec(0, UNSPECIFIED))

    // Testing that is not throwing an exception.
  }

  @Test
  fun testLayoutOutputForRootNestedTreeComponentWithAspectRatio() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestSizeDependentComponent.create(c).widthPx(100).aspectRatio(1f))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(0, UNSPECIFIED),
            makeSizeSpec(0, UNSPECIFIED))
    var mountBounds = Rect()
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 100, 100))
  }

  @Test
  fun testLayoutOutputForRootNestedTreeComponentWithPercentParentSizeDefined() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .alignItems(YogaAlign.FLEX_START)
                  .widthPx(100)
                  .heightPx(100)
                  .child(
                      TestSizeDependentComponent.create(c)
                          .widthPercent(50f)
                          .heightPercent(50f)
                          .backgroundColor(-0x10000))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(0, UNSPECIFIED),
            makeSizeSpec(0, UNSPECIFIED))
    var mountBounds = Rect()
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 100, 100))
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    mountBounds = layoutState.getMountableOutputAt(1).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 50, 50))
  }

  @Test
  fun testLayoutOutputForRootNestedTreeComponentWithPercent() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .alignItems(YogaAlign.FLEX_START)
                  .child(
                      TestSizeDependentComponent.create(c)
                          .setFixSizes(true)
                          .widthPercent(50f)
                          .heightPercent(50f)
                          .backgroundColor(-0x10000))
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(0, UNSPECIFIED),
            makeSizeSpec(0, UNSPECIFIED))
    var mountBounds = Rect()
    mountBounds = layoutState.getMountableOutputAt(0).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 60, 86))
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue
    mountBounds = layoutState.getMountableOutputAt(1).bounds
    assertThat(mountBounds).isEqualTo(Rect(0, 0, 60, 86))
  }

  @Test
  fun testLayoutOutputsForComponentWithBorderColorNoBorderWidth() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .border(Border.create(c).color(YogaEdge.ALL, Color.GREEN).build())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))

    // No layout output generated related with borders
    // if borderColor is supplied but not borderWidth.
    assertThat(layoutState.mountableOutputCount).isEqualTo(2)
  }

  @Test
  fun testLayoutOutputsForComponentWithBorderWidthNoBorderColor() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .border(Border.create(c).widthPx(YogaEdge.ALL, 10).build())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))

    // No layout output generated related with borders
    // if borderWidth supplied but not borderColor.
    assertThat(layoutState.mountableOutputCount).isEqualTo(2)
  }

  @Test
  fun testLayoutOutputsForComponentWithBorderWidthAllAndBorderColor() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .border(
                      Border.create(c)
                          .widthPx(YogaEdge.ALL, 10)
                          .color(YogaEdge.ALL, Color.GREEN)
                          .build())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)

    // Output at index 1 is BorderColorDrawable component.
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(DrawableComponent::class.java)
  }

  @Test
  fun testLayoutOutputsForComponentWithBorderWidthTopAndBorderColor() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c)
                  .child(TestDrawableComponent.create(c))
                  .border(
                      Border.create(c)
                          .widthPx(YogaEdge.TOP, 10)
                          .color(YogaEdge.TOP, Color.GREEN)
                          .build())
                  .build()
        }
    val layoutState =
        calculateLayoutState(
            legacyLithoViewRule.componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)

    // Output at index 1 is BorderColorDrawable component.
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(DrawableComponent::class.java)
  }

  @Test
  fun testWillRenderLayoutsOnce() {
    val c = legacyLithoViewRule.context
    val resolveContext = c.setRenderStateContextForTests()
    val steps: MutableList<StepInfo> = ArrayList()
    val component =
        Column.create(c)
            .child(
                LayoutSpecLifecycleTester.create(c).widthPx(100).heightPx(100).steps(steps).build())
            .build()
    Component.willRender(c, component)
    assertThat(LifecycleStep.getSteps(steps)).containsOnlyOnce(LifecycleStep.ON_CREATE_LAYOUT)
    steps.clear()
    val cachedLayout = component.getLayoutCreatedInWillRender(resolveContext)
    assertThat(cachedLayout).isNotNull
    val result = Resolver.resolve(resolveContext, c, component)
    assertThat(result).isEqualTo(cachedLayout)
    assertThat(component.getLayoutCreatedInWillRender(resolveContext)).isNull()
    assertThat(LifecycleStep.getSteps(steps)).doesNotContain(LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun testResolveLayoutUsesWillRenderResult() {
    var c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val resolveContext = c.setRenderStateContextForTests()
    val component: Component =
        TestLayoutComponent.create(c, 0, 0, true, true, false).key("global_key").build()
    c = ComponentContext.withComponentScope(c, component, "global_key")
    Component.willRender(c, component)
    val cachedLayout = component.getLayoutCreatedInWillRender(resolveContext)
    assertThat(cachedLayout).isNotNull
    val result = Resolver.resolve(resolveContext, c, component)
    assertThat(result).isEqualTo(cachedLayout)
    assertThat(component.getLayoutCreatedInWillRender(resolveContext)).isNull()
  }

  @Test
  fun testNewLayoutBuilderUsesWillRenderResult() {
    var c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val resolveContext = c.setRenderStateContextForTests()
    val component: Component =
        TestLayoutComponent.create(c, 0, 0, true, true, false).key("global_key").build()
    c = ComponentContext.withComponentScope(c, component, "global_key")
    Component.willRender(c, component)
    val cachedLayout = component.getLayoutCreatedInWillRender(resolveContext)
    assertThat(cachedLayout).isNotNull
    val result = Resolver.resolve(resolveContext, c, component)
    assertThat(result).isEqualTo(cachedLayout)
    assertThat(component.getLayoutCreatedInWillRender(resolveContext)).isNull()
  }

  @Test
  fun testCreateLayoutUsesWillRenderResult() {
    var c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val resolveContext = c.setRenderStateContextForTests()
    val component: Component =
        TestLayoutComponent.create(c, 0, 0, true, true, false).key("global_key").build()
    c = ComponentContext.withComponentScope(c, component, "global_key")
    Component.willRender(c, component)
    val cachedLayout = component.getLayoutCreatedInWillRender(resolveContext)
    assertThat(cachedLayout).isNotNull
    val result = Resolver.resolve(resolveContext, c, component)
    assertThat(result).isEqualTo(cachedLayout)
    assertThat(component.getLayoutCreatedInWillRender(resolveContext)).isNull()
  }

  @Test
  fun testWillRenderLayoutsOnceInColumn() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val componentSpy = Mockito.spy(TestLayoutComponent.create(c, 0, 0, true, true, false).build())
    val root: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? {
            willRender(c, componentSpy)
            return Column.create(c).child(componentSpy).build()
          }
        }
    calculateLayoutState(
        legacyLithoViewRule.componentTree.context,
        root,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY))
    verify(componentSpy, times(1))
        .render((anyOrNull()), (anyOrNull<ComponentContext>()), eq(0), eq(0))
  }

  @Test
  fun testWillRenderTwiceDoesNotReCreateLayout() {
    val c = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    val resolveContext = c.setRenderStateContextForTests()
    val component: Component = TestLayoutComponent.create(c, 0, 0, true, true, false).build()
    Component.willRender(c, component)
    val cachedLayout = component.getLayoutCreatedInWillRender(resolveContext)
    assertThat(cachedLayout).isNotNull
    assertThat(Component.willRender(c, component)).isTrue
    assertThat(component.getLayoutCreatedInWillRender(resolveContext)).isEqualTo(cachedLayout)
  }

  @Test
  fun testComponentsLoggerCanReturnNullPerfEventsDuringLayout() {
    val component: Component =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component? =
              Column.create(c).child(TestDrawableComponent.create(c)).wrapInView().build()
        }
    val logger: ComponentsLogger =
        object : TestComponentsLogger() {
          override fun newPerformanceEvent(eventId: Int): PerfEvent? = null
        }
    val componentTree =
        ComponentTree.create(legacyLithoViewRule.context)
            .componentsConfiguration(config)
            .logger(logger, "test")
            .build()
    val layoutState =
        calculateLayoutState(
            componentTree.context,
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
    assertThat(layoutState.mountableOutputCount).isEqualTo(3)
  }

  @Test
  fun whenAccessibleChildNodeExists_ParentNodeShouldImplementVirtualViews() {
    enableAccessibility()
    val component = Text.create(context).text("hello world").build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    val innerHost = legacyLithoViewRule.lithoView.getChildAt(0) as ComponentHost
    assertThat(innerHost.implementsVirtualViews())
        .describedAs("The parent output of the Text must implement virtual views")
        .isTrue
  }

  @Test
  fun whenNoAccessibleChildNodeExists_ParentNodeShouldNotImplementVirtualViews() {
    enableAccessibility()
    val component = SolidColor.create(context).color(Color.BLACK).build()
    legacyLithoViewRule.setRoot(component).attachToWindow().measure().layout()
    assertThat(legacyLithoViewRule.lithoView.implementsVirtualViews())
        .describedAs("The parent output of the drawable must not implement virtual views")
        .isFalse
  }

  @Test
  fun onMountItemUpdatesImplementVirtualViews_ComponentHostShouldAlsoUpdate() {
    enableAccessibility()
    legacyLithoViewRule
        .setRoot(Text.create(context).text("hello world").build())
        .attachToWindow()
        .measure()
        .layout()
    val innerHost = legacyLithoViewRule.lithoView.getChildAt(0) as ComponentHost
    assertThat(innerHost.implementsVirtualViews())
        .describedAs("The parent output of the Text must implement virtual views")
        .isTrue
    legacyLithoViewRule
        .setRootAndSizeSpecSync(
            SolidColor.create(context).color(Color.BLACK).build(),
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY))
        .measure()
        .layout()
    assertThat(legacyLithoViewRule.lithoView.implementsVirtualViews())
        .describedAs("The parent output of the drawable must not implement virtual views")
        .isFalse
    legacyLithoViewRule
        .setRootAndSizeSpecSync(
            Column.create(context)
                .child(Text.create(context).text("hello world").build())
                .child(SolidColor.create(context).color(Color.BLACK).build())
                .build(),
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(200, EXACTLY))
        .attachToWindow()
        .measure()
        .layout()
    assertThat(legacyLithoViewRule.lithoView.implementsVirtualViews())
        .describedAs("The root output must not implement virtual views")
        .isFalse
    val host = legacyLithoViewRule.lithoView.getChildAt(0) as ComponentHost
    assertThat(host.implementsVirtualViews())
        .describedAs("The parent output of the Text must implement virtual views")
        .isTrue
  }

  @Test
  fun onMountHierarchyWithParentDisabled_shouldDisableDescendants() {
    val c = legacyLithoViewRule.context
    val props = ItemCardComponentSpec.TreeProps()
    val view = Output<View>()
    val clicked = Output<Boolean>()
    clicked.set(false)
    props.onCardActionViewVisible = Function { arguments ->
      assertThat(arguments).isNotNull
      assertThat(arguments).isNotEmpty
      view.set(arguments[0] as View)
      null
    }
    props.onCardActionsTouched = Function {
      clicked.set(true)
      null
    }
    props.areCardToolsDisabled = true
    val root = ItemCardComponent.create(c).body(Text.create(c).text("hello").build()).id(1).build()
    legacyLithoViewRule
        .setTreeProp(ItemCardComponentSpec.TreeProps::class.java, props)
        .useComponentTree(ComponentTree.create(c).build())
        .setRoot(root)
        .attachToWindow()
        .measure()
        .layout()
    assertThat(view.get()).isNotNull
    assertThat(view.get()?.isEnabled).isFalse
  }

  @Test
  fun onLayoutCreateInMeasure_shouldBeReusedDuringLayoutIfCompatibleMeasures() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = ArrayList()
    val component =
        ComponentCaching.create(c)
            .component(
                LayoutSpecLifecycleTester.create(c).widthPx(100).heightPx(100).steps(steps).build())
            .widthSpec(MeasureSpecUtils.exactly(100))
            .heightSpec(MeasureSpecUtils.exactly(100))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).layout().measure()
    assertThat(LifecycleStep.getSteps(steps)).containsOnlyOnce(LifecycleStep.ON_CREATE_LAYOUT)
  }

  @Test
  fun onLayoutCreateInMeasure_shouldBeNotReusedDuringLayoutIfIncompatibleMeasures() {
    val c = legacyLithoViewRule.context
    val steps: List<StepInfo> = ArrayList()
    val component =
        ComponentCaching.create(c)
            .component(LayoutSpecLifecycleTester.create(c).steps(steps).build())
            .widthSpec(MeasureSpecUtils.exactly(100))
            .heightSpec(MeasureSpecUtils.exactly(100))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).layout().measure()
    assertThat(LifecycleStep.getSteps(steps))
        .contains(LifecycleStep.ON_CREATE_LAYOUT, LifecycleStep.ON_CREATE_LAYOUT)
  }

  private fun enableAccessibility() {
    val manager: ShadowAccessibilityManager =
        Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
    manager.setEnabled(true)
    manager.setTouchExplorationEnabled(true)
  }

  private fun calculateLayoutState(
      context: ComponentContext,
      component: Component,
      componentTreeId: Int,
      widthSpec: Int,
      heightSpec: Int
  ): LayoutState {
    val result =
        ResolveTreeFuture.resolve(context, component, TreeState(), -1, -1, null, null, null, null)
    return LayoutTreeFuture.layout(
        result, widthSpec, heightSpec, -1, componentTreeId, false, null, null, null, null)
  }

  companion object {
    private fun getComponentAt(layoutState: LayoutState, index: Int): Component =
        LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(index)).component

    private fun getTextFromTextComponent(layoutState: LayoutState, index: Int): CharSequence =
        Whitebox.getInternalState(
            LithoRenderUnit.getRenderUnit(layoutState.getMountableOutputAt(index)).component,
            "text")

    private fun isHostComponent(component: Component): Boolean = component is HostComponent

    private fun getHostId(node: RenderTreeNode): Long = node.parent?.renderUnit?.id ?: 0
  }
}
