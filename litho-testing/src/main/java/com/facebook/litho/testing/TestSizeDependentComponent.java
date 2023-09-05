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

package com.facebook.litho.testing;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.SpecGeneratedComponent;
import com.facebook.yoga.YogaEdge;

/**
 * @deprecated Component should not be directly subclassed, write a layout spec or mount spec
 *     instead
 */
@Deprecated
public class TestSizeDependentComponent extends SpecGeneratedComponent {

  private TestSizeDependentComponent() {
    super("TestSizeDependentComponent");
  }

  @Override
  protected Component onCreateLayoutWithSizeSpec(
      ComponentContext c, int widthSpec, int heightSpec) {

    final Component.Builder builder1 =
        TestDrawableComponent.create(c, true, true, false)
            .flexShrink(0)
            .backgroundColor(0xFFFF0000);
    final Component.Builder builder2 =
        TestViewComponent.create(c, true, true, false).flexShrink(0).marginPx(YogaEdge.ALL, 3);

    if (hasFixedSizes) {
      builder1.widthPx(50).heightPx(50);
      builder2.heightPx(20);
    }

    if (isDelegate) {
      return builder1.build();
    }

    return Column.create(c).paddingPx(YogaEdge.ALL, 5).child(builder1).child(builder2).build();
  }

  @Override
  public boolean isEquivalentProps(Component other, boolean shouldCompareCommonProps) {
    return this == other;
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  public MountType getMountType() {
    return MountType.NONE;
  }

  public static Builder create(ComponentContext context) {
    return new Builder(context, new TestSizeDependentComponent());
  }

  boolean hasFixedSizes;
  boolean isDelegate;

  public static class Builder extends com.facebook.litho.Component.Builder<Builder> {

    TestSizeDependentComponent mTestSizeDependentComponent;

    private Builder(ComponentContext context, TestSizeDependentComponent state) {
      super(context, 0, 0, state);
      mTestSizeDependentComponent = state;
    }

    @Override
    protected void setComponent(Component component) {
      mTestSizeDependentComponent = (TestSizeDependentComponent) component;
    }

    public Builder setFixSizes(boolean hasFixSizes) {
      mTestSizeDependentComponent.hasFixedSizes = hasFixSizes;
      return this;
    }

    public Builder setDelegate(boolean isDelegate) {
      mTestSizeDependentComponent.isDelegate = isDelegate;
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Component build() {
      return mTestSizeDependentComponent;
    }
  }
}
