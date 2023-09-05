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

import androidx.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public interface IncrementalMountExtensionInput {

  List<IncrementalMountOutput> getOutputsOrderedByTopBounds();

  List<IncrementalMountOutput> getOutputsOrderedByBottomBounds();

  @Nullable
  IncrementalMountOutput getIncrementalMountOutputForId(long id);

  /** Returns a collection of Incremental Mount Outputs iterable by insertion order. */
  Collection<IncrementalMountOutput> getIncrementalMountOutputs();

  int getIncrementalMountOutputCount();

  boolean renderUnitWithIdHostsRenderTrees(long id);
}
