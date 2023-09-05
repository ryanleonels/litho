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

package com.facebook.rendercore.transitions;

import androidx.annotation.Nullable;
import com.facebook.litho.AnimatableItem;
import com.facebook.litho.OutputUnitsAffinityGroup;
import com.facebook.litho.Transition;
import com.facebook.litho.TransitionId;
import com.facebook.rendercore.MountDelegateInput;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.Systracer;
import java.util.List;
import java.util.Map;

/**
 * Delegate Input needs to implement this interface to provide access to specific transitions
 * information.
 */
public interface TransitionsExtensionInput extends MountDelegateInput {
  // TODO: remove dependency to MountDelegateInput
  int getMountableOutputCount();

  RenderTreeNode getMountableOutputAt(int index);

  boolean needsToRerunTransitions();

  void setNeedsToRerunTransitions(boolean needsToRerunTransitions);

  int getTreeId();

  Map<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>> getTransitionIdMapping();

  @Nullable
  OutputUnitsAffinityGroup<AnimatableItem> getAnimatableItemForTransitionId(
      TransitionId transitionId);

  @Nullable
  String getRootName();

  @Nullable
  List<Transition> getTransitions();

  @Nullable
  TransitionId getRootTransitionId();

  @Nullable
  AnimatableItem getAnimatableRootItem();

  @Nullable
  AnimatableItem getAnimatableItem(long id);

  boolean renderUnitWithIdHostsRenderTrees(long id);

  void setInitialRootBoundsForAnimation(
      @Nullable Transition.RootBoundsTransition rootWidth,
      @Nullable Transition.RootBoundsTransition rootHeight);

  @Nullable
  List<Transition> getMountTimeTransitions();

  boolean isIncrementalMountEnabled();

  Systracer getTracer();
}
