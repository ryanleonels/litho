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

package com.facebook.litho.kotlin.widget

import android.graphics.drawable.Drawable
import android.widget.ImageView.ScaleType
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.kotlinStyle
import com.facebook.litho.widget.Image

/** Builder function for creating [ImageSpec] components. */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ResourcesScope.Image(
    drawable: Drawable,
    style: Style? = null,
    scaleType: ScaleType = ScaleType.FIT_CENTER
): Image = Image.create(context).drawable(drawable).scaleType(scaleType).kotlinStyle(style).build()
