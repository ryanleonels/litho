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

import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.ClickEventTrackingImage
import com.facebook.litho.widget.ClickEventTrackingRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class ClickEventHandlerUpdateTest {
  @JvmField @Rule val lithoViewRule = LithoViewRule()

  @Test
  fun `click event handler on host should update`() {
    val context = lithoViewRule.context
    val tracker = mutableListOf<String>()
    val clickObserver: (String) -> Unit = { s: String -> tracker.add(s) }

    // render with tag 0
    val testView =
        lithoViewRule.render(widthPx = 1000, heightPx = 1000) {
          Row.create(context)
              .wrapInView()
              .child(ClickEventTrackingRow.create(context).id("0").clickObserver(clickObserver))
              .build()
        }

    val hostView = testView.findViewWithTag("clickable")

    hostView.callOnClick()
    assertThat(tracker).containsExactly("0")

    // This specifically tests the following scenario:
    // 1. A click handler is mounted on a Host
    // 2. A new layout is calculated which removes that click handler, but it isn't mounted (thus
    // the 'detachFromWindow' call)
    // 3. A second new layout is calculated which adds that click handler back, but capturing
    // different props/state
    // We want to make sure we dispatch an event with the updated props and state
    testView.detachFromWindow()
    testView.setRoot(Row.create(context))
    // render with tag 1
    testView.setRoot(
        Row.create(context)
            .wrapInView()
            .child(ClickEventTrackingRow.create(context).id("1").clickObserver(clickObserver))
            .build())
    testView.attachToWindow().measure().layout()

    // Make sure we use the same host - otherwise this isn't testing what we want
    assertThat(testView.findViewWithTag("clickable")).isSameAs(hostView)

    hostView.callOnClick()
    assertThat(tracker).containsExactly("0", "1")

    // render with tag 2
    testView.setRoot(
        Row.create(context)
            .wrapInView()
            .child(ClickEventTrackingRow.create(context).id("2").clickObserver(clickObserver))
            .build())

    assertThat(testView.findViewWithTag("clickable")).isSameAs(hostView)

    hostView.callOnClick()

    assertThat(tracker).containsExactly("0", "1", "2")
  }

  @Test
  fun `click event handler on drawable mountspec should update`() {
    val context = lithoViewRule.context
    val tracker = mutableListOf<String>()

    val clickObserver: (String) -> Unit = { s: String -> tracker.add(s) }

    // render with tag 0
    val testView =
        lithoViewRule.render(widthPx = 1000, heightPx = 1000) {
          Row.create(context)
              .wrapInView()
              .child(ClickEventTrackingImage.create(context).id("0").clickObserver(clickObserver))
              .build()
        }

    val hostView = testView.findViewWithTag("clickable")

    hostView.callOnClick()
    assertThat(tracker).containsExactly("0")

    // This specifically tests the following scenario:
    // 1. A click handler is mounted on a drawable (which will be promoted to a view)
    // 2. A new layout is calculated which removes that click handler, but it isn't mounted (thus
    // the 'detachFromWindow' call)
    // 3. A second new layout is calculated which adds that click handler back, but capturing
    // different props/state
    // We want to make sure we dispatch an event with the updated props and state
    testView.detachFromWindow()
    testView.setRoot(Row.create(context))
    // render with tag 1
    testView.setRoot(
        Row.create(context)
            .wrapInView()
            .child(ClickEventTrackingImage.create(context).id("1").clickObserver(clickObserver))
            .build())
    testView.attachToWindow().measure().layout()

    assertThat(testView.findViewWithTag("clickable")).isSameAs(hostView) // use the same host

    hostView.callOnClick()
    assertThat(tracker).containsExactly("0", "1")

    // render with tag 2
    testView.setRoot(
        Row.create(context)
            .wrapInView()
            .child(ClickEventTrackingImage.create(context).id("2").clickObserver(clickObserver))
            .build())

    assertThat(testView.findViewWithTag("clickable")).isSameAs(hostView) // use the same host

    hostView.callOnClick()

    assertThat(tracker).containsExactly("0", "1", "2")
  }

  @Test
  fun `click event handler on root host should update`() {
    val context = lithoViewRule.context
    val tracker = mutableListOf<String>()

    val clickObserver: (String) -> Unit = { s: String -> tracker.add(s) }

    // render with tag 0
    val testView =
        lithoViewRule.render(widthPx = 1000, heightPx = 1000) {
          ClickEventTrackingRow.create(context).id("0").clickObserver(clickObserver).build()
        }

    val hostView = testView.findViewWithTag("clickable")
    assertThat(hostView).describedAs("should be the root view").isEqualTo(testView.lithoView)

    hostView.callOnClick()
    assertThat(tracker).containsExactly("0")

    // This specifically tests the following scenario:
    // 1. A click handler is mounted on the root host
    // 2. A new layout is calculated which removes that click handler, but it isn't mounted (thus
    // the 'detachFromWindow' call)
    // 3. A second new layout is calculated which adds that click handler back, but capturing
    // different props/state
    // We want to make sure we dispatch an event with the updated props and state
    testView.detachFromWindow()
    testView.setRoot(Row.create(context))
    // render with tag 1
    testView.setRoot(
        ClickEventTrackingRow.create(context).id("1").clickObserver(clickObserver).build())
    testView.attachToWindow().measure().layout()

    assertThat(testView.findViewWithTag("clickable")).isSameAs(hostView) // use the same host

    hostView.callOnClick()
    assertThat(tracker).containsExactly("0", "1")

    // render with tag 2
    testView.setRoot(
        ClickEventTrackingRow.create(context).id("2").clickObserver(clickObserver).build())

    assertThat(testView.findViewWithTag("clickable")).isSameAs(hostView) // use the same host

    hostView.callOnClick()

    assertThat(tracker).containsExactly("0", "1", "2")
  }
}
