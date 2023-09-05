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

import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.RenderCoreExtension;

/**
 * This interface collates the APIs which maybe used by a {@link RenderCoreExtension}. This allow
 * both {@link RootHost} and {@link RenderTreeHost} remain distinct while sharing APIs common to
 * between them.
 */
public interface RenderCoreExtensionHost {

  /** Notifies the host the its visible bounds may have potentially changed. */
  void notifyVisibleBoundsChanged();

  /**
   * Notifies the host when its parent wants to start pre-mounting content.
   *
   * @param frameTimeMs the latest frame time
   */
  void onRegisterForPremount(@Nullable Long frameTimeMs);

  /** Notifies the host when its parent wants to stop pre-mounting content. */
  void onUnregisterForPremount();

  /** Sets a {@link RenderTreeUpdateListener} on the {@link RootHost}'s {@link MountState} */
  void setRenderTreeUpdateListener(RenderTreeUpdateListener listener);
}
