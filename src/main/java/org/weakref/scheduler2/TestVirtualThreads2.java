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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class TestVirtualThreads2
{
    public static void main(String[] args)
            throws InterruptedException
    {
        int processors = Runtime.getRuntime().availableProcessors();

        Semaphore semaphore = new Semaphore(processors);
        List<AtomicLong> counters = new ArrayList<>();
        for (int i = 0; i < 2 * processors; i++) {
            AtomicLong counter = new AtomicLong();
            counters.add(counter);
            final int x = i;
            Thread.ofVirtual().start(() -> {
                semaphore.acquireUninterruptibly();
                Thread.yield();
                semaphore.release();

                while (true) {
                    semaphore.acquireUninterruptibly();
                    counter.incrementAndGet();
                    semaphore.release();
                }
            });
        }

        Thread.sleep(10_000);

        counters.stream()
                .map(AtomicLong::get)
                .sorted()
                .forEach(System.out::println);
    }
}
