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
import androidx.annotation.LayoutRes
import com.facebook.litho.ResourcesScope
import com.facebook.litho.eventHandler
import com.facebook.litho.widget.ItemSelectedEvent
import com.facebook.litho.widget.Spinner
import com.facebook.rendercore.Dimen
import com.facebook.rendercore.sp

/** Builder function for creating [SpinnerSpec] components. */
@Suppress("NOTHING_TO_INLINE", "FunctionName")
inline fun ResourcesScope.Spinner(
    options: List<String>,
    selectedOption: String,
    @LayoutRes itemLayout: Int = android.R.layout.simple_dropdown_item_1line,
    selectedTextSize: Dimen = 16.sp,
    @ColorInt selectedTextColor: Int = 0xDE000000.toInt(),
    caret: Drawable? = null,
    noinline onItemSelected: (ItemSelectedEvent) -> Unit
): Spinner =
    Spinner.create(context)
        .options(options)
        .selectedOption(selectedOption)
        .itemLayout(itemLayout)
        .selectedTextColor(selectedTextColor)
        .selectedTextSizePx(selectedTextSize.toPixels().toFloat())
        .caret(caret)
        .itemSelectedEventHandler(eventHandler(onItemSelected))
        .build()
