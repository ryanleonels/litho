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

package com.facebook.litho.specmodels.model;

import androidx.annotation.Nullable;
import com.squareup.javapoet.TypeName;

/** Describes the type required by the lifecycle method being overridden. */
public class LifecycleMethodArgumentType {

  public static final LifecycleMethodArgumentType COMPONENT_CONTEXT =
      new LifecycleMethodArgumentType(ClassNames.COMPONENT_CONTEXT);

  public static final LifecycleMethodArgumentType INT =
      new LifecycleMethodArgumentType(TypeName.INT);
  public static final LifecycleMethodArgumentType INTER_STAGE_PROPS_CONTAINER =
      new LifecycleMethodArgumentType(ClassNames.INTER_STAGE_PROPS_CONTAINER);

  public final TypeName type;
  public final @Nullable String argumentName;

  public LifecycleMethodArgumentType(final TypeName type) {
    this.type = type;
    this.argumentName = null;
  }

  public LifecycleMethodArgumentType(final TypeName type, final String argumentName) {
    this.type = type;
    this.argumentName = argumentName;
  }
}
