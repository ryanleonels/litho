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

import com.facebook.rendercore.extensions.ExtensionState;
import javax.annotation.Nullable;

/** This delegate allows overriding a {@link MountItem}'s unmount responsibility. */
public interface UnmountDelegateExtension<State> {

  /**
   * This method is called to check if this item's unmount needs to be delegated.
   *
   * @param extensionState
   * @param mountItem
   * @return
   */
  boolean shouldDelegateUnmount(ExtensionState<State> extensionState, MountItem mountItem);

  /**
   * This method is responsible for unmounting the item from the {@link Host} and unbinding the item
   * from the {@link MountDelegateTarget}.
   *
   * @param mountItem
   * @param host
   */
  void unmount(ExtensionState<State> extensionState, MountItem mountItem, @Nullable Host host);
}
