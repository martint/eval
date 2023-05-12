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
import java.util.concurrent.Semaphore;

public class TestVirtualThreads
{
    public static void main(String[] args)
            throws ExecutionException, InterruptedException
    {
        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println("Processors: " + processors);

        Semaphore semaphore = new Semaphore(processors, true);
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 2 * processors; i++) {
            Entry entry = new Entry();
            entries.add(entry);
            Thread t = Thread.ofVirtual().unstarted(() -> {
                while (true) {
                    semaphore.acquireUninterruptibly();
                    long count = entry.count++;

//                    if (count == 1) {
//                        Thread.yield();
//                    }

                    semaphore.release();
//                    Thread.yield();
                }
            });
            entry.thread = t;
            t.start();
        }

        Thread.sleep(10_000);

        entries.stream()
                .sorted((a, b) -> Long.compare(b.count, a.count))
                .forEach(entry -> System.out.println(entry.count + "\t" + entry.thread.getState()));

//        List<Entry> sorted = entries.stream()
//                .collect(Collectors.toList());
//        sorted.stream()
//        System.out.println();
//        double max = sorted.get(0).count;
//        double min = sorted.get(sorted.size() - 1).count;
//        System.out.println("Max:   " + max);
//        System.out.println("Min:   " + min);
//        System.out.println("Range: " + (max - min));
    }

    private static class Entry
    {
        public volatile long count;
        public volatile Thread thread;
    }
}
