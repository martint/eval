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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import javax.annotation.concurrent.GuardedBy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TaskHandle
{
    enum State {
        PENDING,
        RUNNING,
        BLOCKED,
    }

    private final String name;
    private Tracer tracer;

    private final Lock lock = new ReentrantLock();

    @GuardedBy("lock")
    private final Condition blocked = lock.newCondition();

    private volatile long start;
    private final AtomicLong scheduledTime = new AtomicLong();

    @GuardedBy("lock")
    private volatile State state = State.PENDING;

    private Span currentSpan;

    public Span span()
    {
        return taskSpan;
    }

    private final Span taskSpan;

    public TaskHandle(String name, Tracer tracer, Span taskSpan)
    {
        this.name = name;
        this.tracer = tracer;
        this.taskSpan = taskSpan;
    }

    /**
     * This method is called when the task is scheduled to run.
     * It is called by the scheduler thread.
     */
    public void signalRunning()
    {
        lock.lock();
        try {
            state = State.RUNNING;
            blocked.signal();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Called when the task is yielding. Called by the task thread.
     */
    public void transitionToPending()
    {
        taskSpan.addEvent("yield");
        currentSpan.addEvent("yield");
        currentSpan.end();
        currentSpan = null;

        lock.lock();
        try {
            state = State.PENDING;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Called when the task is blocked. Called by the task thread.
     */
    public void transitionToBlocked()
    {
        taskSpan.addEvent("blocked");
        currentSpan.addEvent("blocked");
        currentSpan.end();
        currentSpan = null;
        
        lock.lock();
        try {
            state = State.BLOCKED;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Called to wait for a slot to run the task. Called by the task thread.
     */
    public void awaitRunning()
    {
        lock.lock();
        try {
            while (state != State.RUNNING) {
                blocked.await();
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        finally {
            lock.unlock();
        }
        
        start = System.nanoTime();
        taskSpan.addEvent("running");
        currentSpan = tracer.spanBuilder("slice")
                .setParent(Context.current().with(taskSpan))
                .startSpan();
    }

    public void addTime(long time)
    {
        this.scheduledTime.addAndGet(time);
    }

    public long time()
    {
        return scheduledTime.get();
    }

    public String name()
    {
        return name;
    }

    public long start()
    {
        return start;
    }


    @Override
    public String toString()
    {
        return name + " (" + TimeUnit.NANOSECONDS.toMillis(scheduledTime.get()) + " ms)";
    }

    public State getState()
    {
        return state;
    }
}
