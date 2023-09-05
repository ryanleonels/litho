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

import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Map;

public class DefaultErrorReporter implements ErrorReporterDelegate {

  private static final String CATEGORY_PREFIX = "RenderCore:";

  @Override
  public void report(
      LogLevel level,
      String categoryKey,
      String message,
      @Nullable Throwable cause,
      int samplingFrequency,
      @Nullable Map<String, Object> metadata) {
    switch (level) {
      case WARNING:
        if (BuildConfig.DEBUG) {
          Log.w(CATEGORY_PREFIX + categoryKey, message, cause);
        }
        break;
      case ERROR:
        if (BuildConfig.DEBUG) {
          Log.e(CATEGORY_PREFIX + categoryKey, message, cause);
        }
        break;
      case FATAL:
        if (BuildConfig.DEBUG) {
          Log.e(CATEGORY_PREFIX + categoryKey, message, cause);
        }
        throw new RuntimeException(message);
    }
  }
}
