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

package com.facebook.litho

import com.facebook.infer.annotation.ThreadConfined
import kotlin.jvm.JvmField

/** Public API for MeasureOutput. */
@ThreadConfined(ThreadConfined.ANY)
class Size() {

  /** The width value in pixels. */
  @JvmField var width: Int = 0

  /** The height value in pixels. */
  @JvmField var height: Int = 0

  constructor(width: Int, height: Int) : this() {
    this.width = width
    this.height = height
  }
}
