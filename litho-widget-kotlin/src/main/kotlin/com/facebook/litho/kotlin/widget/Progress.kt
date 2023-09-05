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
import androidx.annotation.ColorInt
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Style
import com.facebook.litho.kotlinStyle
import com.facebook.litho.widget.Progress

/** Builder function for creating [ProgressSpec] components. */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ResourcesScope.Progress(
    @ColorInt color: Int? = null,
    style: Style? = null,
    indeterminateDrawable: Drawable? = null
): Progress =
    Progress.create(context)
        .apply {
          color?.let { color(color) }
          indeterminateDrawable?.let { indeterminateDrawable(indeterminateDrawable) }
        }
        .kotlinStyle(style)
        .build()
