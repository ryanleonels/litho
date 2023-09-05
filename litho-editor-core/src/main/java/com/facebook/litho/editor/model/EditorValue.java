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

package com.facebook.litho.editor.model;

import androidx.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** A sealed class for values supported by the editor */
public abstract class EditorValue {

  // Constructors

  public static EditorValue number(Number n) {
    return new EditorNumber(n);
  }

  public static EditorValue color(Number n) {
    return new EditorColor(n);
  }

  public static EditorValue string(String s) {
    return new EditorString(s);
  }

  public static EditorValue bool(boolean b) {
    return new EditorBool(b);
  }

  public static EditorValue shape(Map<String, EditorValue> shape) {
    return new EditorShape(shape);
  }

  public static EditorValue array(List<EditorValue> array) {
    return new EditorArray(array);
  }

  public static EditorValue array(EditorValue... values) {
    return new EditorArray(values);
  }

  public static EditorValue pick(String selected, Set<String> otherValues) {
    return new EditorPick(otherValues, selected);
  }

  // Members

  /** Finds the real value of this EditorValue */
  public abstract <R> R when(EditorVisitor<R> visitor);

  public interface EditorVisitor<R> {

    R isShape(EditorShape object);

    R isArray(EditorArray array);

    R isPick(EditorPick pick);

    R isNumber(EditorNumber number);

    R isColor(EditorColor number);

    R isString(EditorString string);

    R isBool(EditorBool bool);
  }

  public abstract static class DefaultEditorVisitor implements EditorVisitor<Void> {

    public @Nullable Void isShape(EditorShape object) {
      return null;
    }

    public @Nullable Void isArray(EditorArray array) {
      return null;
    }

    @Override
    public @Nullable Void isPick(EditorPick pick) {
      return null;
    }

    public @Nullable Void isNumber(EditorNumber number) {
      return null;
    }

    public @Nullable Void isColor(EditorColor color) {
      return null;
    }

    public @Nullable Void isString(EditorString string) {
      return null;
    }

    public @Nullable Void isBool(EditorBool bool) {
      return null;
    }
  }

  /** Depth-first traversal of the tree nodes. Shortcircuits on true. */
  public void whenPrimitive(EditorPrimitiveVisitor visitor) {
    whenPrimitive(visitor, new ArrayDeque<String>());
  }

  public interface EditorPrimitiveVisitor {

    boolean isNumber(String[] path, EditorNumber number);

    boolean isColor(String[] path, EditorColor color);

    boolean isString(String[] path, EditorString string);

    boolean isBool(String[] path, EditorBool bool);

    boolean isPick(String[] path, EditorPick pick);
  }

  public abstract static class DefaultEditorPrimitiveVisitor implements EditorPrimitiveVisitor {

    @Override
    public boolean isNumber(String[] path, EditorNumber number) {
      return false;
    }

    @Override
    public boolean isColor(String[] path, EditorColor color) {
      return false;
    }

    @Override
    public boolean isString(String[] path, EditorString string) {
      return false;
    }

    @Override
    public boolean isBool(String[] path, EditorBool bool) {
      return false;
    }

    @Override
    public boolean isPick(String[] path, EditorPick pick) {
      return false;
    }
  }

  private boolean whenPrimitive(final EditorPrimitiveVisitor visitor, final Deque<String> path) {
    return when(
        new EditorVisitor<Boolean>() {
          @Override
          public Boolean isShape(EditorShape object) {
            for (Map.Entry<String, EditorValue> entry : object.value.entrySet()) {
              path.add(entry.getKey());
              if (entry.getValue().whenPrimitive(visitor, path)) {
                return true;
              }
              path.removeLast();
            }
            return false;
          }

          @Override
          public Boolean isArray(EditorArray array) {
            List<EditorValue> value = array.value;
            for (int i = 0; i < value.size(); i++) {
              EditorValue entry = value.get(i);
              path.add(String.valueOf(i));
              if (entry.whenPrimitive(visitor, path)) {
                return true;
              }
              path.removeLast();
            }
            return false;
          }

          @Override
          public Boolean isPick(EditorPick pick) {
            return visitor.isPick(path.toArray(new String[] {}), pick);
          }

          @Override
          public Boolean isNumber(EditorNumber number) {
            return visitor.isNumber(path.toArray(new String[] {}), number);
          }

          @Override
          public Boolean isColor(EditorColor color) {
            return visitor.isColor(path.toArray(new String[] {}), color);
          }

          @Override
          public Boolean isString(EditorString string) {
            return visitor.isString(path.toArray(new String[] {}), string);
          }

          @Override
          public Boolean isBool(EditorBool bool) {
            return visitor.isBool(path.toArray(new String[] {}), bool);
          }
        });
  }

  /** Traverses the tree enriching an initial value */
  public <R> R aggregate(final R init, final EditorAggregator<R> aggregator) {
    final AtomicReference<R> ref = new AtomicReference<>(init);
    when(
        new EditorVisitor<Boolean>() {

          @Override
          public Boolean isShape(EditorShape object) {
            ref.set(aggregator.addShape(ref.get(), object));
            return false;
          }

          @Override
          public Boolean isArray(EditorArray array) {
            ref.set(aggregator.addArray(ref.get(), array));
            return false;
          }

          @Override
          public Boolean isPick(EditorPick pick) {
            ref.set(aggregator.addString(ref.get(), new EditorString(pick.selected)));
            return false;
          }

          @Override
          public Boolean isNumber(EditorNumber number) {
            ref.set(aggregator.addNumber(ref.get(), number));
            return false;
          }

          @Override
          public Boolean isColor(EditorColor color) {
            ref.set(aggregator.addColor(ref.get(), color));
            return false;
          }

          @Override
          public Boolean isString(EditorString string) {
            ref.set(aggregator.addString(ref.get(), string));
            return false;
          }

          @Override
          public Boolean isBool(EditorBool bool) {
            ref.set(aggregator.addBool(ref.get(), bool));
            return false;
          }
        });

    return ref.get();
  }

  public interface EditorAggregator<R> {
    R addShape(R aggregator, EditorShape object);

    R addArray(R aggregator, EditorArray array);

    R addNumber(R aggregator, EditorNumber number);

    R addColor(R aggregator, EditorColor color);

    R addString(R aggregator, EditorString string);

    R addBool(R aggregator, EditorBool bool);
  }

  public Object getValue() {
    if (this instanceof EditorNumber) {
      return ((EditorNumber) this).value;
    } else if (this instanceof EditorString) {
      return ((EditorString) this).value;
    } else if (this instanceof EditorBool) {
      return ((EditorBool) this).value;
    } else if (this instanceof EditorShape) {
      return ((EditorShape) this).value;
    } else if (this instanceof EditorArray) {
      return ((EditorArray) this).value;
    } else if (this instanceof EditorPick) {
      return ((EditorPick) this).selected;
    }
    throw new IllegalStateException("No Value");
  }
}
