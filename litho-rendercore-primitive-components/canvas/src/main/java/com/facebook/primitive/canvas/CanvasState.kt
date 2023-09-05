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

package com.facebook.primitive.canvas

import android.annotation.SuppressLint
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.collection.LruCache
import androidx.core.util.Pools
import com.facebook.primitive.canvas.model.CanvasFill
import com.facebook.primitive.canvas.model.CanvasGradientShading
import com.facebook.primitive.canvas.model.CanvasLayer
import com.facebook.primitive.canvas.model.CanvasPathModel
import com.facebook.primitive.canvas.model.CanvasSolidColorShading
import com.facebook.primitive.canvas.model.CanvasStroke
import com.facebook.primitive.canvas.model.CanvasTransformModel

/**
 * The state of the Prmitive canvas component.
 *
 * Android [Paint] is a "heavy" object that is expensive to create and destroy. This class is
 * responsible for caching fill, stroke and layer Paint objects and reusing them for all drawing
 * commands. The Paint objects are updated only when necessary.
 *
 * Similarly [Matrix] and [Path] are expensive to create and destroy. [Matrix] objects are often
 * updated after they're created. [Path] objects on the other hand are updated less often. In order
 * to improve performance [Matrix] objects will be pooled and [Path] objects will be cached.
 *
 * In order to get the best performance, on instance of this class should be a component-scoped
 * object that survives state updates. When creating an instance of this class you should use:
 * - for Litho: [https://www.internalfb.com/intern/staticdocs/litho/docs/mainconcepts/use-cached/]
 * - for Bloks:
 *   [https://www.internalfb.com/intern/staticdocs/bloks/docs/bridging/android_binder_utils#controllerforcomponent]
 *
 * @param matrixPoolSize The Matrix pool max size
 * @param pathCacheSize The Path cache max size
 */
class CanvasState(
    matrixPoolSize: Int = DEFAULT_MATRIX_POOL_SIZE,
    pathCacheSize: Int = DEFAULT_PATH_CACHE_SIZE,
) {
  // The paint object used for filling shapes
  private var fillPaint: Paint? = null
  private var fillModel: CanvasFill? = null

  // The paint object used for stroking shapes
  private var strokePaint: Paint? = null
  private var strokeModel: CanvasStroke? = null

  // The paint object used for compositing layers
  private var layerPaint: Paint? = null
  private var prevLayerModel: CanvasLayer? = null

  private val matrixPool = Pools.SimplePool<Matrix>(matrixPoolSize)

  // A Path may be transformed with a transform Matrix and there is no way to reset that transform
  // once it has been applied, that's why the cache key is the CanvasPathModel and an optional
  // CanvasTransformModel
  private val pathCache =
      LruCache<Pair<CanvasPathModel, CanvasTransformModel?>, Path>(pathCacheSize)

  internal fun getMatrix(transformModel: CanvasTransformModel, block: (Matrix) -> Unit) {
    val matrix = matrixPool.acquire() ?: Matrix()
    transformModel.applyTo(matrix)
    block(matrix)
    matrix.reset()
    matrixPool.release(matrix)
  }

  @SuppressLint("KotlinPreferElvis")
  internal fun getOrCreatePath(
      pathModel: CanvasPathModel,
      transformModel: CanvasTransformModel? = null
  ): Path {
    val fromCache = pathCache.get(Pair(pathModel, transformModel))
    return if (fromCache != null) {
      fromCache
    } else {
      val newPath = pathModel.toAndroidPath(this)
      pathCache.put(Pair(pathModel, transformModel), newPath)
      newPath
    }
  }

  internal fun configureFillPaint(newFillModel: CanvasFill): Paint =
      getOrCreateFillPaint()
          .apply {
            if (newFillModel.shadow != fillModel?.shadow) {
              val shadow = newFillModel.shadow
              if (shadow != null) {
                setShadowLayer(shadow.radius, shadow.dx, shadow.dy, shadow.color)
              } else {
                clearShadowLayer()
              }
            }

            if (newFillModel.blendingMode != fillModel?.blendingMode) {
              newFillModel.blendingMode.applyTo(this)
            }

            if (newFillModel.shading != fillModel?.shading) {
              when (val shading = newFillModel.shading) {
                is CanvasSolidColorShading -> {
                  if (shader != null) {
                    shader = null
                  }
                  if (color != shading.color) {
                    color = shading.color
                  }
                }
                is CanvasGradientShading -> {
                  shader = shading.gradient.toShader()
                }
              }
            }
          }
          .also { fillModel = newFillModel }

  internal fun configureStrokePaint(newStrokeModel: CanvasStroke): Paint =
      getOrCreateStrokePaint()
          .apply {
            if (newStrokeModel.shadow != strokeModel?.shadow) {
              val shadow = newStrokeModel.shadow
              if (shadow != null) {
                setShadowLayer(shadow.radius, shadow.dx, shadow.dy, shadow.color)
              } else {
                clearShadowLayer()
              }
            }

            if (newStrokeModel.blendingMode != strokeModel?.blendingMode) {
              newStrokeModel.blendingMode.applyTo(this)
            }

            if (newStrokeModel.shading != strokeModel?.shading) {
              when (val shading = newStrokeModel.shading) {
                is CanvasSolidColorShading -> {
                  if (shader != null) {
                    shader = null
                  }
                  if (color != shading.color) {
                    color = shading.color
                  }
                }
                is CanvasGradientShading -> {
                  shader = shading.gradient.toShader()
                }
              }
            }

            if (newStrokeModel.lineWidth != strokeModel?.lineWidth &&
                strokeWidth != newStrokeModel.lineWidth) {
              strokeWidth = newStrokeModel.lineWidth
            }

            if (newStrokeModel.miterLimit != strokeModel?.miterLimit &&
                strokeMiter != newStrokeModel.miterLimit) {
              strokeMiter = newStrokeModel.miterLimit
            }

            if (newStrokeModel.lineCap != strokeModel?.lineCap) {
              newStrokeModel.lineCap.applyTo(this)
            }

            if (newStrokeModel.lineJoin != strokeModel?.lineJoin) {
              newStrokeModel.lineJoin.applyTo(this)
            }

            if (newStrokeModel.dashLengths != null) {
              val dashLengths = newStrokeModel.dashLengths
              if (!dashLengths.contentEquals(strokeModel?.dashLengths) ||
                  newStrokeModel.dashPhase != strokeModel?.dashPhase) {
                pathEffect = DashPathEffect(dashLengths, newStrokeModel.dashPhase)
              }
            } else if (pathEffect != null) {
              pathEffect = null
            }
          }
          .also { strokeModel = newStrokeModel }

  internal fun configureLayerPaint(newLayerModel: CanvasLayer): Paint =
      getOrCreateLayerPaint()
          .apply {
            if (newLayerModel.alpha != prevLayerModel?.alpha) {
              val newAlpha = (newLayerModel.alpha * 255f).toInt()
              if (alpha != newAlpha) {
                alpha = newAlpha
              }
            }
            if (newLayerModel.blendingMode != prevLayerModel?.blendingMode) {
              newLayerModel.blendingMode.applyTo(this)
            }
          }
          .also { prevLayerModel = newLayerModel }

  private fun getOrCreateFillPaint(): Paint {
    return fillPaint ?: createPaint().apply { style = Paint.Style.FILL }.also { fillPaint = it }
  }

  private fun getOrCreateStrokePaint(): Paint {
    return strokePaint
        ?: createPaint().apply { style = Paint.Style.STROKE }.also { strokePaint = it }
  }

  private fun getOrCreateLayerPaint(): Paint {
    return layerPaint ?: createPaint().also { layerPaint = it }
  }

  private fun createPaint(): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
  }

  companion object {
    const val DEFAULT_MATRIX_POOL_SIZE = 5
    const val DEFAULT_PATH_CACHE_SIZE = 10
  }
}
