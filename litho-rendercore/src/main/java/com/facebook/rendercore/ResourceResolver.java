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

package com.facebook.rendercore;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.ArrayRes;
import androidx.annotation.AttrRes;
import androidx.annotation.BoolRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntegerRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ResourceResolver {
  private final Resources mResources;
  private final Resources.Theme mTheme;

  @Nullable private final ResourceCache mResourceCache;
  private final Context mAndroidContext;

  public ResourceResolver(Context context, @Nullable ResourceCache resourceCache) {
    mAndroidContext = context;
    mResources = mAndroidContext.getResources();
    mTheme = mAndroidContext.getTheme();
    mResourceCache = resourceCache;
  }

  @Nullable
  public ResourceCache getResourceCache() {
    return mResourceCache;
  }

  public int dipsToPixels(float dips) {
    final float scale = mResources.getDisplayMetrics().density;
    return FastMath.round(dips * scale);
  }

  public int sipsToPixels(float sips) {
    final float scale = mResources.getDisplayMetrics().scaledDensity;
    return FastMath.round(sips * scale);
  }

  public float pixelsToDips(int pixels) {
    final float scale = mResources.getDisplayMetrics().density;
    return pixels / scale;
  }

  public float pixelsToSips(int pixels) {
    final float scale = mResources.getDisplayMetrics().scaledDensity;
    return pixels / scale;
  }

  public @Nullable String resolveStringRes(@StringRes int resId) {
    if (mResourceCache != null && resId != 0) {
      String cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      String result = mResources.getString(resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return null;
  }

  public @Nullable String resolveStringRes(@StringRes int resId, Object... formatArgs) {
    return resId != 0 ? mResources.getString(resId, formatArgs) : null;
  }

  public @Nullable String resolveQuantityStringRes(@StringRes int resId, int quantity) {
    return resId != 0 ? mResources.getQuantityString(resId, quantity) : null;
  }

  public @Nullable String resolveQuantityStringRes(
      @StringRes int resId, int quantity, Object... formatArgs) {
    return resId != 0 ? mResources.getQuantityString(resId, quantity, formatArgs) : null;
  }

  @Nullable
  public String[] resolveStringArrayRes(@ArrayRes int resId) {
    if (mResourceCache != null && resId != 0) {
      String[] cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      String[] result = mResources.getStringArray(resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return null;
  }

  public int resolveIntRes(@IntegerRes int resId) {
    if (mResourceCache != null && resId != 0) {
      Integer cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int result = mResources.getInteger(resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return 0;
  }

  @Nullable
  public final int[] resolveIntArrayRes(@ArrayRes int resId) {
    if (mResourceCache != null && resId != 0) {
      int[] cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int[] result = mResources.getIntArray(resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return null;
  }

  @Nullable
  public Integer[] resolveIntegerArrayRes(@ArrayRes int resId) {
    int[] resIds = resolveIntArrayRes(resId);
    if (resIds == null) {
      return null;
    }
    Integer[] result = new Integer[resIds.length];
    for (int i = 0; i < resIds.length; i++) {
      result[i] = resIds[i];
    }
    return result;
  }

  public boolean resolveBoolRes(@BoolRes int resId) {
    if (mResourceCache != null && resId != 0) {
      Boolean cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      boolean result = mResources.getBoolean(resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return false;
  }

  public @ColorInt int resolveColorRes(@ColorRes int resId) {
    if (mResourceCache != null && resId != 0) {
      Integer cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int result = ContextCompat.getColor(mAndroidContext, resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return 0;
  }

  public int resolveDimenSizeRes(@DimenRes int resId) {
    if (mResourceCache != null && resId != 0) {
      Integer cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int result = mResources.getDimensionPixelSize(resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return 0;
  }

  public int resolveDimenOffsetRes(@DimenRes int resId) {
    if (mResourceCache != null && resId != 0) {
      Integer cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      int result = mResources.getDimensionPixelOffset(resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return 0;
  }

  public float resolveFloatRes(@DimenRes int resId) {
    if (mResourceCache != null && resId != 0) {
      Float cached = mResourceCache.get(resId);
      if (cached != null) {
        return cached;
      }

      float result = mResources.getDimension(resId);
      mResourceCache.set(resId, result);

      return result;
    }

    return 0;
  }

  @Nullable
  public Drawable resolveDrawableRes(@DrawableRes int resId) {
    if (resId == 0) {
      return null;
    }
    return ContextCompat.getDrawable(mAndroidContext, resId);
  }

  @Nullable
  public String resolveStringAttr(@AttrRes int attrResId, @StringRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      String result = a.getString(0);
      if (result == null) {
        result = resolveStringRes(defResId);
      }

      return result;
    } finally {
      a.recycle();
    }
  }

  @Nullable
  public String[] resolveStringArrayAttr(@AttrRes int attrResId, @ArrayRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return resolveStringArrayRes(a.getResourceId(0, defResId));
    } finally {
      a.recycle();
    }
  }

  public int resolveIntAttr(@AttrRes int attrResId, @IntegerRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return a.getInt(0, resolveIntRes(defResId));
    } finally {
      a.recycle();
    }
  }

  @Nullable
  public int[] resolveIntArrayAttr(@AttrRes int attrResId, @ArrayRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return resolveIntArrayRes(a.getResourceId(0, defResId));
    } finally {
      a.recycle();
    }
  }

  @Nullable
  public Integer[] resolveIntegerArrayAttr(@AttrRes int attrResId, @ArrayRes int defResId) {
    int[] resIds = resolveIntArrayAttr(attrResId, defResId);
    if (resIds == null) {
      return null;
    }
    Integer[] result = new Integer[resIds.length];
    for (int i = 0; i < resIds.length; i++) {
      result[i] = resIds[i];
    }
    return result;
  }

  public boolean resolveBoolAttr(@AttrRes int attrResId, @BoolRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return a.getBoolean(0, resolveBoolRes(defResId));
    } finally {
      a.recycle();
    }
  }

  public int resolveColorAttr(@AttrRes int attrResId, @ColorRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return a.getColor(0, resolveColorRes(defResId));
    } finally {
      a.recycle();
    }
  }

  public int resolveDimenSizeAttr(@AttrRes int attrResId, @DimenRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return a.getDimensionPixelSize(0, resolveDimenSizeRes(defResId));
    } finally {
      a.recycle();
    }
  }

  public int resolveDimenOffsetAttr(@AttrRes int attrResId, @DimenRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return a.getDimensionPixelOffset(0, resolveDimenOffsetRes(defResId));
    } finally {
      a.recycle();
    }
  }

  public float resolveFloatAttr(@AttrRes int attrResId, @DimenRes int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return a.getDimension(0, resolveFloatRes(defResId));
    } finally {
      a.recycle();
    }
  }

  @Nullable
  public Drawable resolveDrawableAttr(@AttrRes int attrResId, @DrawableRes int defResId) {
    if (attrResId == 0) {
      return null;
    }

    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return resolveDrawableRes(a.getResourceId(0, defResId));
    } finally {
      a.recycle();
    }
  }

  public final int resolveResIdAttr(@AttrRes int attrResId, int defResId) {
    TypedArray a = mTheme.obtainStyledAttributes(new int[] {attrResId});

    try {
      return a.getResourceId(0, defResId);
    } finally {
      a.recycle();
    }
  }
}
