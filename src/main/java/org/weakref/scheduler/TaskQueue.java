/*
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
package org.weakref.scheduler;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TaskQueue
{
    private static final int[] LEVEL_THRESHOLD_MILLIS = {100, 200, 400, 800};

    private final PriorityQueue<PrioritizedTask>[] queues;
    private final Lock lock = new ReentrantLock();

    private int count;
    private final Condition notEmpty = lock.newCondition();

    public TaskQueue()
    {
        this.queues = new PriorityQueue[LEVEL_THRESHOLD_MILLIS.length];

        for (int level = 0; level < LEVEL_THRESHOLD_MILLIS.length; level++) {
            queues[level] = new PriorityQueue<>();
        }
    }

    public void enqueue(TaskHandle task)
    {
        Priority priority = computePriority(task);

        task.span().addEvent("priority", Attributes.of(
                AttributeKey.longKey("level"),
                (long) priority.level()));

        lock.lock();
        try {
            queues[priority.level()].add(new PrioritizedTask(task, priority.levelPriority()));
            count++;
            notEmpty.signal();
        }
        finally {
            lock.unlock();
        }
    }

    private Priority computePriority(TaskHandle task)
    {
        long time = task.time();
        for (int i = 0; i < LEVEL_THRESHOLD_MILLIS.length; i++) {
            if (task.time() < TimeUnit.MILLISECONDS.toNanos(LEVEL_THRESHOLD_MILLIS[i])) {
                return new Priority(i, time);
            }
        }

        return new Priority(LEVEL_THRESHOLD_MILLIS.length - 1, time);
    }

    public TaskHandle dequeue()
            throws InterruptedException
    {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }

            for (PriorityQueue<PrioritizedTask> queue : queues) {
                PrioritizedTask task = queue.poll();
                if (task != null) {
                    return task.task();
                }
            }

            throw new AssertionError("unreachable");
        }
        finally {
            lock.unlock();
        }
    }

    record PrioritizedTask(TaskHandle task, long priority)
            implements Comparable<PrioritizedTask>
    {
        @Override
        public int compareTo(TaskQueue.PrioritizedTask other)
        {
            return Long.compare(priority, other.priority);
        }
    }
}
