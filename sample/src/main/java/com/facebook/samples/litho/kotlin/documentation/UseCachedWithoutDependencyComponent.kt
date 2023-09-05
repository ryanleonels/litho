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

package com.facebook.samples.litho.kotlin.documentation

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.view.onClick

// start_example
class UseCachedWithoutDependencyComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val number = useState { 1 }

    val expensiveValue = useCached {
      // state isn't declared as a dependency, so initial state value will always be used
      expensiveRepeatFunc("hello", number.value)
    }

    return Column(style = Style.onClick { number.update { n -> n + 1 } }) {
      child(Text(text = expensiveValue))
    }
  }
  // end_example
  companion object {
    private fun expensiveRepeatFunc(prefix: String, num: Int = 20): String {
      return StringBuilder().apply { repeat(num) { append(prefix) } }.toString()
    }
  }
}
