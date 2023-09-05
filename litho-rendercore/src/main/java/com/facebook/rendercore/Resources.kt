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

package com.facebook.rendercore

import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes

/** Return a string for a resource ID. */
fun BaseResourcesScope.stringRes(@StringRes id: Int): String =
    requireNotNull(resourceResolver.resolveStringRes(id)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/** Return a string for a resource ID, substituting the format arguments with [arg]. */
fun BaseResourcesScope.stringRes(@StringRes id: Int, arg: Any): String =
    requireNotNull(resourceResolver.resolveStringRes(id, arg)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/** Return a string for a resource ID, substituting the format arguments with [formatArgs]. */
fun BaseResourcesScope.stringRes(@StringRes id: Int, vararg formatArgs: Any): String =
    requireNotNull(resourceResolver.resolveStringRes(id, *formatArgs)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/**
 * @return a string for a resource ID and quantity, substituting the format arguments with
 *   [formatArgs].
 */
fun BaseResourcesScope.quantityStringRes(@StringRes id: Int, quantity: Int): String =
    requireNotNull(resourceResolver.resolveQuantityStringRes(id, quantity)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/** @return a string for a resource ID and quantity. */
fun BaseResourcesScope.quantityStringRes(
    @StringRes id: Int,
    quantity: Int,
    vararg formatArgs: Any
): String =
    requireNotNull(resourceResolver.resolveQuantityStringRes(id, quantity, formatArgs)) {
      "String resource not found for ID #0x${Integer.toHexString(id)}"
    }

/** Retrieve a [android.graphics.drawable.Drawable] for a resource ID as a [Drawable] instance. */
fun BaseResourcesScope.drawableRes(@DrawableRes id: Int): Drawable =
    requireNotNull(resourceResolver.resolveDrawableRes(id)) {
      "Drawable resource not found for ID #0x${Integer.toHexString(id)}"
    }

/**
 * Retrieve a [android.graphics.drawable.Drawable], corresponding to an attribute resource ID, as a
 * [Drawable] instance. If given attribute ID can not be found, default Drawable resource ID
 * [defResId] is used.
 */
fun BaseResourcesScope.drawableAttr(@AttrRes id: Int, @DrawableRes defResId: Int = 0): Drawable =
    drawableRes(resourceResolver.resolveResIdAttr(id, defResId))

/** Return a [ColorInt] value for a color resource ID. */
@ColorInt
fun BaseResourcesScope.colorRes(@ColorRes id: Int): Int = resourceResolver.resolveColorRes(id)

/**
 * Return a [ColorInt] value, corresponding to an attribute resource ID. If given attribute ID can
 * not be found, default color resource ID [defResId] is used.
 */
@ColorInt
fun BaseResourcesScope.colorAttr(@AttrRes id: Int, @ColorRes defResId: Int = 0): Int =
    resourceResolver.resolveColorAttr(id, defResId)

fun BaseResourcesScope.intRes(@IntegerRes id: Int): Int = resourceResolver.resolveIntRes(id)

fun BaseResourcesScope.floatRes(@DimenRes id: Int): Float = resourceResolver.resolveFloatRes(id)

/** Resolve a dimen resource ID as a [Dimen] value. */
fun BaseResourcesScope.dimenRes(@DimenRes id: Int): Dimen =
    resourceResolver.resolveDimenSizeRes(id).px
