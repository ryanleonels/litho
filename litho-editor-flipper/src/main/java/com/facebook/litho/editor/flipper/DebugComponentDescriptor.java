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

package com.facebook.litho.editor.flipper;

import static com.facebook.flipper.plugins.inspector.InspectorValue.Type.Boolean;
import static com.facebook.flipper.plugins.inspector.InspectorValue.Type.Enum;
import static com.facebook.flipper.plugins.inspector.InspectorValue.Type.Number;
import static com.facebook.flipper.plugins.inspector.InspectorValue.Type.Picker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;
import com.facebook.flipper.core.FlipperDynamic;
import com.facebook.flipper.core.FlipperObject;
import com.facebook.flipper.plugins.inspector.HighlightedOverlay;
import com.facebook.flipper.plugins.inspector.InspectorValue;
import com.facebook.flipper.plugins.inspector.Named;
import com.facebook.flipper.plugins.inspector.NodeDescriptor;
import com.facebook.flipper.plugins.inspector.SetDataOperations;
import com.facebook.flipper.plugins.inspector.Touch;
import com.facebook.flipper.plugins.inspector.descriptors.ObjectDescriptor;
import com.facebook.flipper.plugins.inspector.descriptors.utils.ContextDescriptorUtils;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentTreeTimeMachine;
import com.facebook.litho.DebugComponent;
import com.facebook.litho.DebugLayoutNode;
import com.facebook.litho.DebugLayoutNodeEditor;
import com.facebook.litho.LithoView;
import com.facebook.litho.RenderSource;
import com.facebook.litho.StateContainer;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaValue;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

public class DebugComponentDescriptor extends NodeDescriptor<DebugComponent> {

  private Map<
          String, List<Pair<String[], Pair<SetDataOperations.FlipperValueHint, FlipperDynamic>>>>
      mOverrides = new HashMap<>();
  private DebugComponent.Overrider mOverrider =
      new DebugComponent.Overrider() {
        @Override
        public void applyComponentOverrides(String key, Component component) {
          final List<Pair<String[], Pair<SetDataOperations.FlipperValueHint, FlipperDynamic>>>
              overrides = mOverrides.get(key);
          if (overrides == null) {
            return;
          }

          for (Pair<String[], Pair<SetDataOperations.FlipperValueHint, FlipperDynamic>> override :
              overrides) {
            if (override.first[0].equals("Props")) {
              applyReflectiveOverride(
                  component, override.first, override.second.first, override.second.second);
            }
          }
        }

        @Override
        public void applyStateOverrides(String key, StateContainer stateContainer) {
          final List<Pair<String[], Pair<SetDataOperations.FlipperValueHint, FlipperDynamic>>>
              overrides = mOverrides.get(key);
          if (overrides == null) {
            return;
          }

          for (Pair<String[], Pair<SetDataOperations.FlipperValueHint, FlipperDynamic>> override :
              overrides) {
            if (override.first[0].equals("State")) {
              applyReflectiveOverride(
                  stateContainer, override.first, override.second.first, override.second.second);
            }
          }
        }

        @Override
        public void applyLayoutOverrides(String key, DebugLayoutNodeEditor node) {
          final List<Pair<String[], Pair<SetDataOperations.FlipperValueHint, FlipperDynamic>>>
              overrides = mOverrides.get(key);
          if (overrides == null) {
            return;
          }

          for (Pair<String[], Pair<SetDataOperations.FlipperValueHint, FlipperDynamic>> override :
              overrides) {
            if (override.first[0].equals("Layout")) {
              try {
                applyLayoutOverride(
                    node,
                    Arrays.copyOfRange(override.first, 1, override.first.length),
                    override.second.second);
              } catch (Exception ignored) {
              }
            }
          }
        }
      };

  @Override
  public void init(DebugComponent node) {
    // We rely on the LithoView being invalidated when a component hierarchy changes.
  }

  @Override
  public String getId(DebugComponent node) {
    return node.getGlobalKey();
  }

  @Override
  public String getName(DebugComponent node) throws Exception {
    NodeDescriptor componentDescriptor = descriptorForClass(node.getComponent().getClass());
    if (componentDescriptor.getClass() != ObjectDescriptor.class) {
      return componentDescriptor.getName(node.getComponent());
    }
    return node.getComponent().getSimpleName();
  }

  @Override
  public int getChildCount(DebugComponent node) {
    if (node.getMountedView() != null || node.getMountedDrawable() != null) {
      return 1;
    } else {
      return node.getChildComponents().size();
    }
  }

  @Override
  public Object getChildAt(DebugComponent node, int index) {
    final View mountedView = node.getMountedView();
    final Drawable mountedDrawable = node.getMountedDrawable();

    if (mountedView != null) {
      return mountedView;
    } else if (mountedDrawable != null) {
      return mountedDrawable;
    } else {
      return node.getChildComponents().get(index);
    }
  }

  @Override
  public List<Named<FlipperObject>> getData(DebugComponent node) throws Exception {
    NodeDescriptor componentDescriptor = descriptorForClass(node.getComponent().getClass());
    if (componentDescriptor.getClass() != ObjectDescriptor.class) {
      return componentDescriptor.getData(node.getComponent());
    }

    final List<Named<FlipperObject>> data = new ArrayList<>();

    final FlipperObject layoutData = getLayoutData(node);
    if (layoutData != null) {
      data.add(new Named<>("Layout", layoutData));
    }

    final List<Named<FlipperObject>> propData = getPropData(node);
    if (propData != null) {
      data.addAll(propData);
    }

    final FlipperObject mountingData = getMountingData(node);
    if (mountingData != null) {
      data.add(new Named<>("Mounting and Visibility", mountingData));
    }

    final FlipperObject stateData = getStateData(node);
    if (stateData != null) {
      data.add(new Named<>("State", stateData));
    }

    data.add(
        new Named<>(
            "Theme", ContextDescriptorUtils.themeData(node.getContext().getAndroidContext())));

    ComponentTreeTimeMachine timeMachine = node.getComponentTreeTimeMachine();
    if (timeMachine != null) {
      List<ComponentTreeTimeMachine.Revision> revisions = timeMachine.getRevisions();

      if (!revisions.isEmpty()) {
        final FlipperObject.Builder timeTravelDescriptionBuilder = new FlipperObject.Builder();
        final String timelineDocsLink = ComponentsConfiguration.timelineDocsLink;

        if (timelineDocsLink != null) {
          timeTravelDescriptionBuilder.put(
              "<DOCS>", InspectorValue.immutable(InspectorValue.Type.Text, timelineDocsLink));
        }

        String selectedRevisionId = timeMachine.getCurrentRevision().getId();

        final FlipperObject timeTravelDescription =
            timeTravelDescriptionBuilder
                .put(
                    "Component Root",
                    InspectorValue.immutable(
                        InspectorValue.Type.Text, timeMachine.getOriginalRootName()))
                .put(
                    "Revision",
                    InspectorValue.mutable(
                        InspectorValue.Type.Timeline,
                        new InspectorValue.Timeline(
                            makeTimeline(revisions, selectedRevisionId), selectedRevisionId)))
                .build();

        data.add(new Named<>("Time Travelling", timeTravelDescription));
      }
    }
    return data;
  }

  private static List<InspectorValue.Timeline.TimePoint> makeTimeline(
      List<ComponentTreeTimeMachine.Revision> revisions, String selectedRevisionId) {

    final List<InspectorValue.Timeline.TimePoint> inspectorTimeline = new ArrayList<>();
    for (final ComponentTreeTimeMachine.Revision revision : revisions) {
      final boolean isSelected = revision.getId().equals(selectedRevisionId);

      final Pair<String, String> displayAndColor =
          makeDisplayAndColor(isSelected, revision.getSource(), revision.getAttribution());
      final String displayString = displayAndColor.first;
      final String color = displayAndColor.second;
      inspectorTimeline.add(
          new InspectorValue.Timeline.TimePoint(
              revision.getId(),
              revision.getTimestamp(),
              displayString,
              color,
              new HashMap<String, String>() {
                {
                  put("Revision number", String.valueOf(revision.getVersion()));
                  put(
                      "Time created",
                      REVISION_DATE_FORMAT.format(new Date(revision.getTimestamp())));
                  put("Reason for update", displayAndColor.first);
                }
              }));
    }

    return inspectorTimeline;
  }

  // The colors are defined in Flipper on ui/components/colors.tsx
  // RGB values compatible with CSS are also accepted
  public static Pair<String, String> makeDisplayAndColor(
      boolean isSelected, @RenderSource int source, @Nullable String attribution) {
    final String attrib = attribution == null ? "" : ": " + attribution;
    final String selectedColor = "white";
    switch (source) {
      case RenderSource.MEASURE_SET_SIZE_SPEC:
        return new Pair<>("Measure set size sync" + attrib, isSelected ? selectedColor : "orange");
      case RenderSource.MEASURE_SET_SIZE_SPEC_ASYNC:
        return new Pair<>("Measure set size async" + attrib, isSelected ? selectedColor : "orange");
      case RenderSource.SET_SIZE_SPEC_ASYNC:
        return new Pair<>("Set size async" + attrib, isSelected ? selectedColor : "blueDark");
      case RenderSource.SET_SIZE_SPEC_SYNC:
        return new Pair<>("Set size sync" + attrib, isSelected ? selectedColor : "blueDark");
      case RenderSource.UPDATE_STATE_ASYNC:
        return new Pair<>(attribution + " async", isSelected ? selectedColor : "lemon");
      case RenderSource.UPDATE_STATE_SYNC:
        return new Pair<>(attribution + " sync", isSelected ? selectedColor : "lemon");
      case RenderSource.NONE:
        return new Pair<>("None" + attrib, isSelected ? selectedColor : "black");
      case RenderSource.TEST:
        return new Pair<>("Test" + attrib, isSelected ? selectedColor : "black");
      case RenderSource.SET_ROOT_ASYNC:
        return new Pair<>("Set root async" + attrib, isSelected ? selectedColor : "slate");
      case RenderSource.SET_ROOT_SYNC:
        return new Pair<>("Set root sync" + attrib, isSelected ? selectedColor : "slate");
      default:
        return new Pair<>("Unknown" + attrib, isSelected ? selectedColor : "red");
    }
  }

  @Nullable
  private static FlipperObject getLayoutData(DebugComponent node) {
    final DebugLayoutNode layout = node.getLayoutNode();
    if (layout == null) {
      return null;
    }

    final FlipperObject.Builder data = new FlipperObject.Builder();
    data.put("<PLAYGROUND>", InspectorValue.immutable("https://yogalayout.com/playground/"));

    data.put("background", DataUtils.fromDrawable(layout.getBackground()));
    data.put("foreground", DataUtils.fromDrawable(layout.getForeground()));

    data.put(
        "direction",
        InspectorValue.mutable(
            Picker,
            new InspectorValue.Picker(
                enumToSet(YogaDirection.values()), layout.getLayoutDirection().name())));
    data.put(
        "flex-direction",
        InspectorValue.mutable(
            Picker,
            new InspectorValue.Picker(
                enumToSet(YogaFlexDirection.values()), layout.getFlexDirection().name())));
    data.put(
        "justify-content",
        InspectorValue.mutable(
            Picker,
            new InspectorValue.Picker(
                enumToSet(YogaJustify.values()), layout.getJustifyContent().name())));
    data.put(
        "align-items",
        InspectorValue.mutable(
            Picker,
            new InspectorValue.Picker(
                enumToSet(YogaAlign.values()), layout.getAlignItems().name())));
    data.put(
        "align-self",
        InspectorValue.mutable(
            Picker,
            new InspectorValue.Picker(
                enumToSet(YogaAlign.values()), layout.getAlignSelf().name())));
    data.put(
        "align-content",
        InspectorValue.mutable(
            Picker,
            new InspectorValue.Picker(
                enumToSet(YogaAlign.values()), layout.getAlignContent().name())));
    data.put(
        "position-type",
        InspectorValue.mutable(
            Picker,
            new InspectorValue.Picker(
                enumToSet(YogaPositionType.values()), layout.getPositionType().name())));

    data.put("flex-grow", fromFloat(layout.getFlexGrow()));
    data.put("flex-shrink", fromFloat(layout.getFlexShrink()));
    data.put("flex-basis", fromYogaValue(layout.getFlexBasis()));

    data.put("width", fromYogaValue(layout.getWidth()));
    data.put("min-width", fromYogaValue(layout.getMinWidth()));
    data.put("max-width", fromYogaValue(layout.getMaxWidth()));

    data.put("height", fromYogaValue(layout.getHeight()));
    data.put("min-height", fromYogaValue(layout.getMinHeight()));
    data.put("max-height", fromYogaValue(layout.getMaxHeight()));

    data.put("aspect-ratio", fromFloat(layout.getAspectRatio()));

    data.put(
        "margin",
        new FlipperObject.Builder()
            .put("left", fromYogaValue(layout.getMargin(YogaEdge.LEFT)))
            .put("top", fromYogaValue(layout.getMargin(YogaEdge.TOP)))
            .put("right", fromYogaValue(layout.getMargin(YogaEdge.RIGHT)))
            .put("bottom", fromYogaValue(layout.getMargin(YogaEdge.BOTTOM)))
            .put("start", fromYogaValue(layout.getMargin(YogaEdge.START)))
            .put("end", fromYogaValue(layout.getMargin(YogaEdge.END)))
            .put("horizontal", fromYogaValue(layout.getMargin(YogaEdge.HORIZONTAL)))
            .put("vertical", fromYogaValue(layout.getMargin(YogaEdge.VERTICAL)))
            .put("all", fromYogaValue(layout.getMargin(YogaEdge.ALL))));

    data.put(
        "padding",
        new FlipperObject.Builder()
            .put("left", fromYogaValue(layout.getPadding(YogaEdge.LEFT)))
            .put("top", fromYogaValue(layout.getPadding(YogaEdge.TOP)))
            .put("right", fromYogaValue(layout.getPadding(YogaEdge.RIGHT)))
            .put("bottom", fromYogaValue(layout.getPadding(YogaEdge.BOTTOM)))
            .put("start", fromYogaValue(layout.getPadding(YogaEdge.START)))
            .put("end", fromYogaValue(layout.getPadding(YogaEdge.END)))
            .put("horizontal", fromYogaValue(layout.getPadding(YogaEdge.HORIZONTAL)))
            .put("vertical", fromYogaValue(layout.getPadding(YogaEdge.VERTICAL)))
            .put("all", fromYogaValue(layout.getPadding(YogaEdge.ALL))));

    data.put(
        "border",
        new FlipperObject.Builder()
            .put("left", fromFloat(layout.getBorderWidth(YogaEdge.LEFT)))
            .put("top", fromFloat(layout.getBorderWidth(YogaEdge.TOP)))
            .put("right", fromFloat(layout.getBorderWidth(YogaEdge.RIGHT)))
            .put("bottom", fromFloat(layout.getBorderWidth(YogaEdge.BOTTOM)))
            .put("start", fromFloat(layout.getBorderWidth(YogaEdge.START)))
            .put("end", fromFloat(layout.getBorderWidth(YogaEdge.END)))
            .put("horizontal", fromFloat(layout.getBorderWidth(YogaEdge.HORIZONTAL)))
            .put("vertical", fromFloat(layout.getBorderWidth(YogaEdge.VERTICAL)))
            .put("all", fromFloat(layout.getBorderWidth(YogaEdge.ALL))));

    data.put(
        "position",
        new FlipperObject.Builder()
            .put("left", fromYogaValue(layout.getPosition(YogaEdge.LEFT)))
            .put("top", fromYogaValue(layout.getPosition(YogaEdge.TOP)))
            .put("right", fromYogaValue(layout.getPosition(YogaEdge.RIGHT)))
            .put("bottom", fromYogaValue(layout.getPosition(YogaEdge.BOTTOM)))
            .put("start", fromYogaValue(layout.getPosition(YogaEdge.START)))
            .put("end", fromYogaValue(layout.getPosition(YogaEdge.END)))
            .put("horizontal", fromYogaValue(layout.getPosition(YogaEdge.HORIZONTAL)))
            .put("vertical", fromYogaValue(layout.getPosition(YogaEdge.VERTICAL)))
            .put("all", fromYogaValue(layout.getPosition(YogaEdge.ALL))));

    data.put("hasViewOutput", InspectorValue.immutable(Boolean, layout.hasViewOutput()));
    if (layout.hasViewOutput()) {
      data.put("alpha", fromFloat(layout.getAlpha()));
      data.put("scale", fromFloat(layout.getScale()));
      data.put("rotation", fromFloat(layout.getRotation()));
    }

    return data.build();
  }

  private static <E extends Enum<E>> HashSet<String> enumToSet(Enum<E>[] enums) {
    final HashSet<String> names = new HashSet<>();
    for (Enum<E> aEnum : enums) {
      names.add(aEnum.name());
    }
    return names;
  }

  @Nullable
  private static List<Named<FlipperObject>> getPropData(DebugComponent node) throws Exception {
    if (node.canResolve()) {
      return null;
    }

    final Component component = node.getComponent();
    return DataUtils.getPropData(component);
  }

  @Nullable
  private static FlipperObject getMountingData(DebugComponent node) throws Exception {

    return DataUtils.getMountingData(node);
  }

  @Nullable
  private static FlipperObject getStateData(DebugComponent node) {
    return DataUtils.getStateData(node.getStateContainer());
  }

  @Override
  public void setValue(
      DebugComponent node,
      String[] path,
      @Nullable SetDataOperations.FlipperValueHint kind,
      FlipperDynamic value) {

    if (path[0].contains("Time Travel")) {
      ComponentTreeTimeMachine timeMachine = node.getComponentTreeTimeMachine();
      if (timeMachine == null) return;
      timeMachine.restoreRevision(value.asString());
    } else {
      setNodeOverrides(node, path, kind, value);
    }
  }

  private void setNodeOverrides(
      DebugComponent node,
      String[] path,
      SetDataOperations.FlipperValueHint kind,
      FlipperDynamic value) {
    List<Pair<String[], Pair<SetDataOperations.FlipperValueHint, FlipperDynamic>>> overrides =
        mOverrides.get(node.getGlobalKey());
    if (overrides == null) {
      overrides = new ArrayList<>();
      mOverrides.put(node.getGlobalKey(), overrides);
    }
    overrides.add(new Pair<>(path, new Pair<>(kind, value)));

    node.setOverrider(mOverrider);
    node.rerender();
  }

  @Override
  public List<Named<String>> getAttributes(DebugComponent node) {
    final List<Named<String>> attributes = new ArrayList<>();
    final String key = node.getKey();
    final String testKey = node.getTestKey();

    if (key != null && key.trim().length() > 0) {
      attributes.add(new Named<>("key", key));
    }

    if (testKey != null && testKey.trim().length() > 0) {
      attributes.add(new Named<>("testKey", testKey));
    }

    return attributes;
  }

  @Override
  public FlipperObject getExtraInfo(DebugComponent node) {
    FlipperObject.Builder extraInfo = new FlipperObject.Builder();
    final NodeDescriptor descriptor = descriptorForClass(View.class);
    final View hostView = node.getComponentHost();
    final View lithoView = node.getLithoView();

    if (hostView != null) {
      try {
        extraInfo.put("linkedNode", descriptor.getId(hostView));
      } catch (Exception ignored) {
        // doesn't have linked node descriptor
      }
    } else if (lithoView != null) {
      try {
        extraInfo.put("linkedNode", descriptor.getId(lithoView)).put("expandWithParent", true);
      } catch (Exception ignored) {
        // doesn't add linked node descriptor
      }
    }
    final FlipperObject.Builder metaData = new FlipperObject.Builder();
    metaData.put("className", node.getComponent().getClass().getName());
    metaData.put("framework", "LITHO");

    extraInfo.put("metaData", metaData);

    return extraInfo.build();
  }

  @Override
  public void setHighlighted(DebugComponent node, boolean selected, boolean isAlignmentMode) {
    final LithoView lithoView = node.getLithoView();
    if (lithoView == null) {
      return;
    }

    if (!selected) {
      HighlightedOverlay.removeHighlight(lithoView);
      return;
    }

    final DebugLayoutNode layout = node.getLayoutNode();
    final boolean hasNode = layout != null;
    final Rect margin;
    if (!node.isRoot()) {
      margin =
          new Rect(
              hasNode ? (int) layout.getLayoutMargin(YogaEdge.START) : 0,
              hasNode ? (int) layout.getLayoutMargin(YogaEdge.TOP) : 0,
              hasNode ? (int) layout.getLayoutMargin(YogaEdge.END) : 0,
              hasNode ? (int) layout.getLayoutMargin(YogaEdge.BOTTOM) : 0);
    } else {
      // Margin not applied if you're at the root
      margin = new Rect();
    }

    final Rect padding =
        new Rect(
            hasNode ? (int) layout.getLayoutPadding(YogaEdge.START) : 0,
            hasNode ? (int) layout.getLayoutPadding(YogaEdge.TOP) : 0,
            hasNode ? (int) layout.getLayoutPadding(YogaEdge.END) : 0,
            hasNode ? (int) layout.getLayoutPadding(YogaEdge.BOTTOM) : 0);

    final Rect contentBounds = node.getBoundsInLithoView();
    HighlightedOverlay.setHighlighted(lithoView, margin, padding, contentBounds, isAlignmentMode);
  }

  @Override
  public @Nullable Bitmap getSnapshot(DebugComponent node, boolean includeChildren)
      throws Exception {
    final LithoView lithoView = node.getLithoView();
    if (lithoView == null) {
      return null;
    }

    final Rect contentBounds = node.getBoundsInLithoView();
    Bitmap bitmap =
        Bitmap.createBitmap(contentBounds.width(), contentBounds.height(), Bitmap.Config.ARGB_8888);

    Canvas c = new Canvas(bitmap);
    lithoView.draw(c);
    return bitmap;
  }

  @Override
  public void hitTest(DebugComponent node, Touch touch) {
    boolean finish = true;
    for (int i = getChildCount(node) - 1; i >= 0; i--) {
      final Object child = getChildAt(node, i);
      if (child instanceof DebugComponent) {
        final DebugComponent componentChild = (DebugComponent) child;
        final Rect bounds = componentChild.getBounds();

        if (touch.containedIn(bounds.left, bounds.top, bounds.right, bounds.bottom)) {
          touch.continueWithOffset(i, bounds.left, bounds.top);
          finish = false;
        }
      } else if (child instanceof View || child instanceof Drawable) {
        // Components can only mount one view or drawable and its bounds are the same as the
        // hosting component.
        touch.continueWithOffset(i, 0, 0);
        finish = false;
      }
    }

    if (finish) touch.finish();
  }

  @Override
  public String getDecoration(DebugComponent node) throws Exception {
    if (node.getComponent() != null) {
      NodeDescriptor componentDescriptor = descriptorForClass(node.getComponent().getClass());
      if (componentDescriptor.getClass() != ObjectDescriptor.class) {
        return componentDescriptor.getDecoration(node.getComponent());
      }
    }
    return "litho";
  }

  @Override
  public boolean matches(String query, DebugComponent node) throws Exception {
    NodeDescriptor descriptor = descriptorForClass(Object.class);
    return descriptor.matches(query, node) || getId(node).equals(query);
  }

  private static void applyLayoutOverride(
      DebugLayoutNodeEditor node, String[] path, FlipperDynamic value) {
    switch (path[0]) {
      case "background":
        node.setBackgroundColor(value.asInt());
        break;
      case "foreground":
        node.setForegroundColor(value.asInt());
        break;
      case "direction":
        node.setLayoutDirection(YogaDirection.valueOf(value.asString().toUpperCase()));
        break;
      case "flex-direction":
        node.setFlexDirection(YogaFlexDirection.valueOf(value.asString().toUpperCase()));
        break;
      case "justify-content":
        node.setJustifyContent(YogaJustify.valueOf(value.asString().toUpperCase()));
        break;
      case "align-items":
        node.setAlignItems(YogaAlign.valueOf(value.asString().toUpperCase()));
        break;
      case "align-self":
        node.setAlignSelf(YogaAlign.valueOf(value.asString().toUpperCase()));
        break;
      case "align-content":
        node.setAlignContent(YogaAlign.valueOf(value.asString().toUpperCase()));
        break;
      case "position-type":
        node.setPositionType(YogaPositionType.valueOf(value.asString().toUpperCase()));
        break;
      case "flex-grow":
        node.setFlexGrow(value.asFloat());
        break;
      case "flex-shrink":
        node.setFlexShrink(value.asFloat());
        break;
      case "flex-basis":
        node.setFlexBasis(YogaValue.parse(value.asString()));
        break;
      case "width":
        node.setWidth(YogaValue.parse(value.asString()));
        break;
      case "min-width":
        node.setMinWidth(YogaValue.parse(value.asString()));
        break;
      case "max-width":
        node.setMaxWidth(YogaValue.parse(value.asString()));
        break;
      case "height":
        node.setHeight(YogaValue.parse(value.asString()));
        break;
      case "min-height":
        node.setMinHeight(YogaValue.parse(value.asString()));
        break;
      case "max-height":
        node.setMaxHeight(YogaValue.parse(value.asString()));
        break;
      case "aspect-ratio":
        node.setAspectRatio(value.asFloat());
        break;
      case "margin":
        node.setMargin(edgeFromString(path[1]), YogaValue.parse(value.asString()));
        break;
      case "padding":
        node.setPadding(edgeFromString(path[1]), YogaValue.parse(value.asString()));
        break;
      case "border":
        node.setBorderWidth(edgeFromString(path[1]), value.asFloat());
        break;
      case "position":
        node.setPosition(edgeFromString(path[1]), YogaValue.parse(value.asString()));
        break;
      case "alpha":
        node.setAlpha(value.asFloat());
        break;
      case "scale":
        node.setScale(value.asFloat());
        break;
      case "rotation":
        node.setRotation(value.asFloat());
        break;
    }
  }

  private static YogaEdge edgeFromString(String s) {
    return YogaEdge.valueOf(s.toUpperCase());
  }

  // The path follows the pattern (Props|State)/field/(field|index)*
  private static void applyReflectiveOverride(
      Object o,
      final String[] path,
      @Nullable SetDataOperations.FlipperValueHint hint,
      final FlipperDynamic dynamic) {
    try {
      final Field field = o.getClass().getDeclaredField(path[1]);
      FlipperEditor.updateComponent(path, field, o, hint, dynamic);
    } catch (Exception ignored) {
    }
  }

  private static InspectorValue fromFloat(float f) {
    if (Float.isNaN(f)) {
      return InspectorValue.mutable(Enum, "undefined");
    }
    return InspectorValue.mutable(Number, f);
  }

  static InspectorValue fromYogaValue(YogaValue v) {
    // TODO add support for Type.Dimension or similar
    return InspectorValue.mutable(Enum, v.toString());
  }

  private static final DateFormat REVISION_DATE_FORMAT =
      new SimpleDateFormat("hh:mm:ss.SSS", Locale.getDefault());
}
