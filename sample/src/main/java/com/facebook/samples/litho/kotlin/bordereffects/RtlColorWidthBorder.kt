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

package com.facebook.samples.litho.kotlin.bordereffects

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.layoutDirection
import com.facebook.litho.kotlin.widget.Border
import com.facebook.litho.kotlin.widget.BorderEdge
import com.facebook.litho.kotlin.widget.Text
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaDirection

class RtlColorWidthBorder : KComponent() {

  override fun ComponentScope.render(): Component {

    return Row(
        style =
            Style.border(
                    Border(
                        edgeStart = BorderEdge(NiceColor.RED, 2f.dp),
                        edgeEnd = BorderEdge(NiceColor.GREEN, 8f.dp),
                        edgeTop = BorderEdge(NiceColor.YELLOW, 4f.dp),
                        edgeBottom = BorderEdge(NiceColor.BLUE, 16f.dp)))
                .layoutDirection(YogaDirection.RTL)) {
          child(Text("This component is RTL", textSize = 20f.dp))
        }
  }
}
