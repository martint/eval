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
package org.weakref.scheduler2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class Scheduler2
{
    private final Semaphore semaphore = new Semaphore(100);
    private final List<Context> threads = new ArrayList<>();
    private final ExecutorService executor;

    public interface Task
    {
        void run(Context context)
                throws InterruptedException;
    }

    public class Context
    {
        private final String name;

        private volatile long time;
        private volatile long start;
        private volatile long value;

        public Context(String name)
        {
            this.name = name;
        }

        void yield()
        {
            if (start != 0) {
                long now = System.nanoTime();
                time += now - start;
            }

            // TODO: park or yield based on spent, priorities, etc
//            Thread.yield();
            start = System.nanoTime();
        }

        public long time()
        {
            return time;
        }

        public void acquire()
        {
//            try {
//                semaphore.acquire();
                start = System.nanoTime();
//            }
//            catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
        }

        public void release()
        {
            time += System.nanoTime() - start;
//            semaphore.release();
//            Thread.yield();
        }

        public void value(long value)
        {
            this.value = value;
        }
    }

    public Scheduler2()
    {
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public Future<?> start(String name, Task task)
    {
        Context context = new Context(name);
        threads.add(context);
        return executor.submit(() -> {
//            context.yield(); // "schedule" the thread -- park or run if there's room
            try {
                task.run(context);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public List<Context> threads()
    {
        return List.copyOf(threads);
    }

    public static void main(String[] args)
            throws ExecutionException, InterruptedException
    {
        Scheduler2 scheduler = new Scheduler2();

        for (int t = 0; t < 100; t++) {
            scheduler.start("thread" + t, (context) -> {
                long sum = 0;
                long count = 0;
                while (true) {
                    context.acquire();
                    for (int i = 0; i < 1_000_000; i++) {
                        sum += ThreadLocalRandom.current().nextLong();
                        count++;
                    }
//                    Thread.sleep(0, 1);
//                    Thread.sleep(1);
                    context.release();
                    context.value(count);
                    Thread.yield();
                }
            });
        }

        Thread.sleep(10_000);

        scheduler.threads().stream()
                .sorted((a, b) -> Long.compare(b.time, a.time))
                .forEach((thread) -> System.out.println(thread.name + ": " + thread.value));
    }
}
