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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

public class Scheduler
{
    private final static long QUANTUM_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    private final ExecutorService schedulerExecutor;
    private final ExecutorService taskExecutor;
    private final List<TaskHandle> tasks = new ArrayList<>();
    private final Semaphore permits = new Semaphore(1);
    private final TaskQueue pending = new TaskQueue();
    private final Tracer tracer;
    private final Span span;

    public Scheduler(Tracer tracer)
    {
        this.tracer = tracer;
        schedulerExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setDaemon(true)
                .build());

        taskExecutor = Context.taskWrapping(
                Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .build()));

        span = tracer.spanBuilder("scheduler").startSpan();
    }

    public List<TaskHandle> threads()
    {
        return List.copyOf(tasks);
    }

    public void start()
            throws InterruptedException
    {
        schedulerExecutor.submit(() -> runScheduler());
    }

    public void close()
    {
        taskExecutor.shutdownNow();
        schedulerExecutor.shutdownNow();
        span.end();
    }

    private void runScheduler()
    {
        while (true) {
            try { 
                permits.acquire();
                span.addEvent("next");
                TaskHandle task = pending.dequeue();
                task.signalRunning();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public Future<?> submit(String name, Task task)
    {
        Span taskSpan = tracer.spanBuilder(name)
                .setParent(Context.current().with(span))
                .startSpan();
        
        TaskHandle handle = new TaskHandle(name, tracer, taskSpan);
        tasks.add(handle);

        Future<?> future = taskExecutor.submit(() -> runTask(task, handle, taskSpan));

        pending.enqueue(handle);

        return future;
    }

    private void runTask(Task task, TaskHandle handle, Span taskSpan)
    {
        try {
            handle.awaitRunning();
            try {
                task.run(new TaskContext(this, handle));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finally {
                if (handle.getState() == TaskHandle.State.RUNNING) {
                    handle.addTime(System.nanoTime() - handle.start());
                    permits.release();
                }
            }
        }
        finally {
            taskSpan.end();
        }
    }

    public void blocked(TaskHandle handle, ListenableFuture<?> future)
    {
        checkState(handle.getState() == TaskHandle.State.RUNNING, "Task is not running");

        long delta = System.nanoTime() - handle.start();
        handle.addTime(delta);

        handle.transitionToBlocked();

        future.addListener(() -> pending.enqueue(handle), MoreExecutors.directExecutor());
        permits.release();

        handle.awaitRunning();
    }

    public void yield(TaskHandle handle)
    {
        checkState(handle.getState() == TaskHandle.State.RUNNING, "Task is not running");

        long delta = System.nanoTime() - handle.start();
        if (delta < QUANTUM_NANOS) {
            return;
        }
        handle.addTime(delta);

        handle.transitionToPending();

        pending.enqueue(handle);
        permits.release();

        handle.awaitRunning();
    }
}
