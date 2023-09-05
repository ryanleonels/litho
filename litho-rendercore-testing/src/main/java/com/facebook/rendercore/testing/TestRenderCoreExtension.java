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

package com.facebook.rendercore.testing;

import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.LayoutResultVisitor;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import java.util.ArrayList;
import java.util.function.Supplier;

public class TestRenderCoreExtension extends RenderCoreExtension {

  private final LayoutResultVisitor mLayoutResultVisitor;
  private final MountExtension mMountExtension;
  private final Supplier mInputSupplier;

  public TestRenderCoreExtension() {
    this(new TestLayoutResultVisitor(), new TestMountExtension(), ArrayList::new);
  }

  public TestRenderCoreExtension(
      final @Nullable LayoutResultVisitor layoutResultVisitor,
      final @Nullable Supplier inputSupplier) {
    this(layoutResultVisitor, new TestMountExtension(), inputSupplier);
  }

  public TestRenderCoreExtension(final @Nullable MountExtension mountExtension) {
    this(new TestLayoutResultVisitor(), mountExtension, ArrayList::new);
  }

  public TestRenderCoreExtension(
      final @Nullable LayoutResultVisitor layoutResultVisitor,
      final @Nullable MountExtension mountExtension,
      final @Nullable Supplier inputSupplier) {
    mLayoutResultVisitor = layoutResultVisitor;
    mMountExtension = mountExtension;
    mInputSupplier = inputSupplier;
  }

  @Override
  public @Nullable LayoutResultVisitor getLayoutVisitor() {
    return mLayoutResultVisitor;
  }

  @Nullable
  @Override
  public MountExtension getMountExtension() {
    return mMountExtension;
  }

  @Override
  public @Nullable Object createInput() {
    return mInputSupplier != null ? mInputSupplier.get() : super.createInput();
  }
}
