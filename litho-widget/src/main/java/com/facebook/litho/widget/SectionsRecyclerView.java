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

package com.facebook.litho.widget;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.facebook.litho.BaseMountingView;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.HasLithoViewChildren;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.rendercore.debug.DebugEvent;
import com.facebook.rendercore.debug.DebugEventAttribute;
import com.facebook.rendercore.debug.DebugEventDispatcher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Wrapper that encapsulates all the features {@link RecyclerSpec} provides such as sticky header
 * and pull-to-refresh
 */
public class SectionsRecyclerView extends SwipeRefreshLayout implements HasLithoViewChildren {

  private final LithoView mStickyHeader;
  private final RecyclerView mRecyclerView;
  private @Nullable SectionsRecyclerViewLogger mSectionsRecyclerViewLogger;
  private boolean mIsFirstLayout = true;

  /**
   * Indicates whether {@link RecyclerView} has been detached. In such case we need to make sure to
   * relayout its children eventually.
   */
  private boolean mHasBeenDetachedFromWindow = false;
  /**
   * When we set an ItemAnimator during mount, we want to store the one that was already set on the
   * RecyclerView so that we can reset it during unmount.
   */
  private ItemAnimator mDetachedItemAnimator;

  public SectionsRecyclerView(Context context, RecyclerView recyclerView) {
    super(context);

    mRecyclerView = recyclerView;

    // We need to draw first visible item on top of other children to support sticky headers
    mRecyclerView.setChildDrawingOrderCallback(
        new RecyclerView.ChildDrawingOrderCallback() {
          @Override
          public int onGetChildDrawingOrder(int childCount, int i) {
            return childCount - 1 - i;
          }
        });
    // ViewCache doesn't work well with RecyclerBinder which assumes that whenever item comes back
    // to viewport it should be rebound which does not happen with ViewCache. Consider this case:
    // LithoView goes out of screen and it is added to ViewCache, then its ComponentTree is assigned
    // to another LV which means our LithoView's ComponentTree reference is nullified. It comes back
    // to screen and it is not rebound therefore we will see 0 height LithoView which actually
    // happened in multiple product surfaces. Disabling it fixes the issue.
    mRecyclerView.setItemViewCacheSize(0);

    addView(mRecyclerView);
    mStickyHeader = new LithoView(new ComponentContext(getContext()), null);
    mStickyHeader.setLayoutParams(
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    addView(mStickyHeader);
  }

  public void setSectionsRecyclerViewLogger(SectionsRecyclerViewLogger lithoViewLogger) {
    mSectionsRecyclerViewLogger = lithoViewLogger;
  }

  public RecyclerView getRecyclerView() {
    return mRecyclerView;
  }

  public void setStickyComponent(ComponentTree componentTree) {
    @Nullable LithoView existingLithoView = componentTree.getLithoView();
    mStickyHeader.setComponentTree(componentTree);

    // Set this after because setComponentTree clears temporary detached component tree
    if (existingLithoView != null && mStickyHeader != existingLithoView) {
      existingLithoView.setTemporaryDetachedComponentTree(componentTree);
    }
    measureStickyHeader(getWidth());
  }

  public LithoView getStickyHeader() {
    return mStickyHeader;
  }

  public void setStickyHeaderVerticalOffset(int verticalOffset) {
    mStickyHeader.setTranslationY(verticalOffset);
  }

  public void showStickyHeader() {
    mStickyHeader.setVisibility(View.VISIBLE);

    layoutStickyHeader();
  }

  public void hideStickyHeader() {
    mStickyHeader.unmountAllItems();
    mStickyHeader.setVisibility(View.GONE);
  }

  public void maybeRestoreDetachedComponentTree(int stickyHeaderPosition) {
    if (stickyHeaderPosition <= -1
        || !ComponentsConfiguration.initStickyHeaderInLayoutWhenComponentTreeIsNull
        || mStickyHeader.getComponentTree() == null) {
      return;
    }

    @Nullable
    RecyclerView.ViewHolder viewHolder =
        mRecyclerView.findViewHolderForAdapterPosition(stickyHeaderPosition);
    @Nullable
    RecyclerBinderViewHolder recyclerBinderViewHolder =
        viewHolder instanceof RecyclerBinderViewHolder
            ? (RecyclerBinderViewHolder) viewHolder
            : null;
    @Nullable
    LithoView lithoView =
        recyclerBinderViewHolder != null ? recyclerBinderViewHolder.getLithoView() : null;
    if (lithoView != null
        && lithoView.getComponentTree() == null
        && lithoView.hasTemporaryDetachedComponentTree()) {
      lithoView.requestLayout();
    }
  }

  public boolean isStickyHeaderHidden() {
    return mStickyHeader.getVisibility() == View.GONE;
  }

  public void setItemAnimator(@Nullable ItemAnimator itemAnimator) {
    mDetachedItemAnimator = mRecyclerView.getItemAnimator();
    mRecyclerView.setItemAnimator(itemAnimator);
  }

  public void resetItemAnimator() {
    mRecyclerView.setItemAnimator(mDetachedItemAnimator);
    mDetachedItemAnimator = null;
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    measureStickyHeader(MeasureSpec.getSize(widthMeasureSpec));
  }

  private void measureStickyHeader(int parentWidth) {
    measureChild(
        mStickyHeader,
        MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY),
        MeasureSpec.UNSPECIFIED);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

    final @Nullable Integer traceId =
        DebugEventDispatcher.generateTraceIdentifier(DebugEvent.ViewOnLayout);
    if (traceId != null) {
      final Map<String, Object> attributes = new LinkedHashMap<>();
      attributes.put(DebugEventAttribute.Id, hashCode());
      attributes.put(DebugEventAttribute.Name, "SectionsRecyclerView");
      attributes.put(DebugEventAttribute.Bounds, new Rect(left, top, right, bottom));
      DebugEventDispatcher.beginTrace(traceId, DebugEvent.ViewOnLayout, "-1", attributes);
    }

    try {
      if (mSectionsRecyclerViewLogger != null) {
        mSectionsRecyclerViewLogger.onLayoutStarted(mIsFirstLayout);
      }
      super.onLayout(changed, left, top, right, bottom);
      layoutStickyHeader();
    } finally {
      if (mSectionsRecyclerViewLogger != null) {
        mSectionsRecyclerViewLogger.onLayoutEnded(mIsFirstLayout);
      }
      mIsFirstLayout = false;
      if (traceId != null) {
        DebugEventDispatcher.endTrace(traceId);
      }
    }
  }

  private void layoutStickyHeader() {
    if (mStickyHeader.getVisibility() == View.GONE) {
      return;
    }

    final int stickyHeaderLeft = getPaddingLeft();
    final int stickyHeaderTop = getPaddingTop();
    mStickyHeader.layout(
        stickyHeaderLeft,
        stickyHeaderTop,
        stickyHeaderLeft + mStickyHeader.getMeasuredWidth(),
        stickyHeaderTop + mStickyHeader.getMeasuredHeight());
  }

  static @Nullable SectionsRecyclerView getParentRecycler(RecyclerView recyclerView) {
    if (recyclerView.getParent() instanceof SectionsRecyclerView) {
      return (SectionsRecyclerView) recyclerView.getParent();
    }
    return null;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    mHasBeenDetachedFromWindow = true;
  }

  public boolean hasBeenDetachedFromWindow() {
    return mHasBeenDetachedFromWindow;
  }

  public void setHasBeenDetachedFromWindow(boolean hasBeenDetachedFromWindow) {
    mHasBeenDetachedFromWindow = hasBeenDetachedFromWindow;
  }

  @Override
  public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    super.requestDisallowInterceptTouchEvent(disallowIntercept);

    // SwipeRefreshLayout can ignore this request if nested scrolling is disabled on the child,
    // but it fails to delegate the request up to the parents.
    // This fixes a bug that can cause parents to improperly intercept scroll events from
    // nested recyclers.
    if (getParent() != null && !isNestedScrollingEnabled()) {
      getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
    }
  }

  @Override
  public void setOnTouchListener(OnTouchListener listener) {
    // When setting touch handler for RecyclerSpec we want RecyclerView to handle it.
    mRecyclerView.setOnTouchListener(listener);
  }

  @Override
  public void obtainLithoViewChildren(List<BaseMountingView> lithoViews) {
    lithoViews.add(mStickyHeader);
    for (int i = 0, size = mRecyclerView.getChildCount(); i < size; i++) {
      final View child = mRecyclerView.getChildAt(i);
      if (child instanceof LithoView) {
        lithoViews.add((LithoView) child);
      }
    }
  }

  /** Pass to a SectionsRecyclerView to do custom logging. */
  public interface SectionsRecyclerViewLogger {
    void onLayoutStarted(boolean isFirstLayoutAfterRecycle);

    void onLayoutEnded(boolean isFirstLayoutAfterRecycle);
  }
}
