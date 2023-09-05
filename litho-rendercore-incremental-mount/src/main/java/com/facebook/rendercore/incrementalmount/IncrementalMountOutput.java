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

package com.facebook.rendercore.incrementalmount;

import android.graphics.Rect;
import androidx.annotation.Nullable;

/** The output object for incremental mount extension */
public class IncrementalMountOutput {
  private final int index;
  private @Nullable Rect originalBounds;
  private final Rect bounds;
  private final long id;
  private final @Nullable IncrementalMountOutput host;
  private boolean excludeFromIncrementalMount;

  public IncrementalMountOutput(
      final long id,
      final int index,
      final Rect bounds,
      final boolean excludeFromIncrementalMount,
      final @Nullable IncrementalMountOutput host) {
    this.id = id;
    this.index = index;
    this.bounds = new Rect(bounds);
    this.host = host;
    this.excludeFromIncrementalMount = excludeFromIncrementalMount;
    // TODO: invert API to add child to a host rather than passing the host to the child.
    if (host != null) {
      ensureHostBounds(this, host);
    }
  }

  public int getIndex() {
    return index;
  }

  public Rect getBounds() {
    return bounds;
  }

  public @Nullable Rect getOriginalBounds() {
    return originalBounds;
  }

  public long getId() {
    return id;
  }

  public @Nullable IncrementalMountOutput getHostOutput() {
    return host;
  }

  public boolean excludeFromIncrementalMount() {
    return excludeFromIncrementalMount;
  }

  /** Ensure the host IncrementalMountOuput's bounds are containing the bounds of this item. */
  private static void ensureHostBounds(
      final IncrementalMountOutput child, final @Nullable IncrementalMountOutput host) {
    if (host == null) {
      return;
    }

    final Rect childBounds = child.getBounds();
    final Rect hostBounds = host.getBounds();
    boolean needsUpdate = false;

    if (child.excludeFromIncrementalMount) {
      host.excludeFromIncrementalMount = true;
      needsUpdate = true;
    }

    if (childBounds.top < hostBounds.top) {
      maybeCacheOriginalBounds(host);
      hostBounds.top = childBounds.top;
      needsUpdate = true;
    }

    if (childBounds.bottom > hostBounds.bottom) {
      maybeCacheOriginalBounds(host);
      hostBounds.bottom = childBounds.bottom;
      needsUpdate = true;
    }

    if (childBounds.left < hostBounds.left) {
      maybeCacheOriginalBounds(host);
      hostBounds.left = childBounds.left;
      needsUpdate = true;
    }

    if (childBounds.right > hostBounds.right) {
      maybeCacheOriginalBounds(host);
      hostBounds.right = childBounds.right;
      needsUpdate = true;
    }

    if (needsUpdate) {
      ensureHostBounds(host, host.getHostOutput());
    }
  }

  private static void maybeCacheOriginalBounds(IncrementalMountOutput output) {
    if (output.originalBounds == null) {
      output.originalBounds = new Rect(output.bounds);
    }
  }
}
