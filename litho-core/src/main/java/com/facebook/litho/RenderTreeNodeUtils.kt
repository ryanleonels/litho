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

@file:JvmName("RenderTreeNodeUtils")

package com.facebook.litho

import android.graphics.Rect
import com.facebook.rendercore.RenderTreeNode

fun create(
    unit: LithoRenderUnit,
    bounds: Rect,
    layoutData: LithoLayoutData?,
    parent: RenderTreeNode?
): RenderTreeNode =
    RenderTreeNode(parent, unit, layoutData, bounds, null, parent?.childrenCount ?: 0)
