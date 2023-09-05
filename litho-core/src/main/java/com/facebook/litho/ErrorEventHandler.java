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

package com.facebook.litho;

import static com.facebook.litho.Component.ERROR_EVENT_HANDLER_ID;

import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;

/**
 * This class is an error event handler that clients can optionally set on a {@link ComponentTree}
 * to gracefully handle uncaught/unhandled exceptions thrown from the framework while resolving a
 * layout.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class ErrorEventHandler extends EventHandler<ErrorEvent>
    implements HasEventDispatcher, EventDispatcher {

  public ErrorEventHandler() {
    // sets up HasEventDispatcher immediately after constructing EventHandler
    super(null, ERROR_EVENT_HANDLER_ID, null);
    this.dispatchInfo.hasEventDispatcher = this;
  }

  @Override
  public @Nullable Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    if (eventHandler.id == Component.ERROR_EVENT_HANDLER_ID) {
      final Exception e = ((ErrorEvent) eventState).exception;
      final ComponentContext cc =
          Preconditions.checkNotNull(((ErrorEvent) eventState).componentContext);
      final Component component = onError(cc, e);
      if (component != null && cc.getErrorComponentReceiver() != null) {
        cc.getErrorComponentReceiver().onErrorComponent(component);
      }
    }
    return null;
  }

  @Override
  public void dispatchEvent(ErrorEvent event) {
    dispatchOnEvent(this, event);
  }

  @Override
  public EventDispatcher getEventDispatcher() {
    return this;
  }

  /** Action performed when exception occurred. */
  @Nullable
  public abstract Component onError(ComponentContext cc, Exception e);
}
