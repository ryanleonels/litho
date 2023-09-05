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

package com.facebook.litho.testing

import android.os.Looper
import com.facebook.litho.ComponentTree
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

/**
 * Helper class extracting looper functionality from [BackgroundLayoutLooperRule] that can be reused
 * in other TestRules
 */
class ThreadLooperController(
    val layoutLooper: ShadowLooper =
        Shadows.shadowOf(
            Whitebox.invokeMethod<Any>(ComponentTree::class.java, "getDefaultLayoutThreadLooper")
                as Looper)
) : BaseThreadLooperController {
  private lateinit var messageQueue: BlockingQueue<Message>
  private var isInitialized = false

  override fun init() {
    messageQueue = ArrayBlockingQueue(100)
    val layoutLooperThread = LayoutLooperThread(layoutLooper, messageQueue)
    layoutLooperThread.start()

    isInitialized = true
  }

  override fun clean() {
    if (!isInitialized) {
      return
    }

    quitSync()
    if (ShadowLooper.looperMode() != LooperMode.Mode.PAUSED) {
      layoutLooper.quitUnchecked()
    }
    isInitialized = false
  }

  private fun quitSync() {
    val semaphore = TimeOutSemaphore(0)
    messageQueue.add(Message(MessageType.QUIT, semaphore))
    semaphore.acquire()
  }

  /**
   * Runs one task on the background thread, blocking until it completes successfully or throws an
   * exception.
   */
  override fun runOneTaskSync() {
    val semaphore = TimeOutSemaphore(0)
    messageQueue.add(Message(MessageType.DRAIN_ONE, semaphore))
    semaphore.acquire()
  }

  /**
   * Runs through all tasks on the background thread, blocking until it completes successfully or
   * throws an exception.
   */
  override fun runToEndOfTasksSync() {
    val semaphore = TimeOutSemaphore(0)
    messageQueue.add(Message(MessageType.DRAIN_ALL, semaphore))
    semaphore.acquire()
  }

  override fun runToEndOfTasksAsync(): TimeOutSemaphore? {
    val semaphore = TimeOutSemaphore(0)
    messageQueue.add(Message(MessageType.DRAIN_ALL, semaphore))
    return semaphore
  }
}

enum class MessageType {
  DRAIN_ONE,
  DRAIN_ALL,
  QUIT
}

class Message constructor(val messageType: MessageType, val semaphore: TimeOutSemaphore)

@SuppressWarnings("CatchGeneralException")
private class LayoutLooperThread(layoutLooper: ShadowLooper, messages: BlockingQueue<Message>) :
    Thread(
        Runnable {
          var keepGoing = true
          while (keepGoing) {
            val message: Message =
                try {
                  messages.take()
                } catch (e: InterruptedException) {
                  throw RuntimeException(e)
                }
            try {
              when (message.messageType) {
                MessageType.DRAIN_ONE -> {
                  layoutLooper.runOneTask()
                  message.semaphore.release()
                }
                MessageType.DRAIN_ALL -> {
                  layoutLooper.runToEndOfTasks()
                  message.semaphore.release()
                }
                MessageType.QUIT -> {
                  keepGoing = false
                  message.semaphore.release()
                }
              }
            } catch (e: Exception) {
              message.semaphore.setException(e)
              message.semaphore.release()
            }
          }
        })
